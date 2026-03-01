#!/usr/bin/env python3
"""
Final Citation Update Script for Steve AI Dissertation

Updates all citations to standard format: Author, "Title" (Year)

This version includes the complete citation database.
"""

import re
import sys
from pathlib import Path
from datetime import datetime

# Import complete citation database
sys.path.insert(0, str(Path(__file__).parent))
from citation_database import COMPLETE_CITATION_DATABASE, get_citation_entry


def format_citation_authors(author: str, entry: dict) -> str:
    """Format the author portion of a citation."""
    if entry.get("co_authors_etal", False):
        return f"{author} et al."
    elif "co_authors" in entry and entry["co_authors"]:
        co_authors = entry["co_authors"]
        if len(co_authors) == 1:
            return f"{author} & {co_authors[0]}"
        elif len(co_authors) == 2:
            return f"{author}, {co_authors[0]}, & {co_authors[1]}"
        else:
            return f"{author} et al."
    return author


def format_standard_citation(author: str, entry: dict) -> str:
    """Format a citation in standard format: Author, "Title" (Year)"""
    authors = format_citation_authors(author, entry)
    title = entry.get("title", "")
    year = entry.get("year", "")

    if title:
        return f'{authors}, "{title}" ({year})'
    else:
        return f"{authors} ({year})"


def extract_author_from_match(author_part: str) -> str:
    """Extract the primary author's last name from a match string."""
    # Remove common patterns
    author_part = author_part.strip()

    # Handle "et al" cases
    if "et al" in author_part.lower():
        return author_part.split()[0].strip('.,')

    # Handle "&" or "and" cases
    if "&" in author_part:
        return author_part.split("&")[0].strip()
    if " and " in author_part.lower():
        return author_part.lower().split(" and ")[0].strip()

    # Get last word (assumes last name is last)
    parts = author_part.split()
    return parts[-1].strip('.,')


class CitationUpdater:
    def __init__(self, base_dir: str):
        self.base_dir = Path(base_dir)
        self.research_dir = self.base_dir / "docs" / "research"
        self.chapters = [
            "DISSERTATION_CHAPTER_1_RTS_IMPROVED.md",
            "DISSERTATION_CHAPTER_3_RPG_IMPROVED.md",
            "DISSERTATION_CHAPTER_4_STRATEGY_IMPROVED.md",
            "DISSERTATION_CHAPTER_6_ARCHITECTURE_IMPROVED.md",
            "DISSERTATION_CHAPTER_8_LLM_ENHANCEMENT_IMPROVED.md"
        ]
        self.all_citations: dict = {}  # chapter -> dict of standardized citations
        self.stats = {
            "total_found": 0,
            "total_updated": 0,
            "total_review": 0
        }

    def process_chapter(self, chapter_file: str) -> str:
        """Process a single chapter file and return updated content."""
        filepath = self.research_dir / chapter_file
        if not filepath.exists():
            print(f"Warning: {filepath} does not exist")
            return ""

        print(f"\nProcessing {chapter_file}...")
        content = filepath.read_text(encoding='utf-8')
        self.all_citations[chapter_file] = {}

        # Define citation patterns in order of specificity
        patterns = [
            # [Author, Year] or [Author & Other, Year]
            (r'\[([A-Z][a-zA-Z\s&]+(?: et al\.?)?(?:,?\s*(?:and|&)\s*[A-Z][a-zA-Z\s&]+)*),\s*(\d{4})\]', 1),
            # [Author (Year)]
            (r'\[([A-Z][a-zA-Z\s&]+(?: et al\.?)?)\s+\((\d{4})\)\]', 1),
            # (Author, Year)
            (r'\(([A-Z][a-zA-Z\s&]+(?: et al\.?)?(?:,?\s*(?:and|&)\s*[A-Z][a-zA-Z\s&]+)*),\s*(\d{4})\)', 1),
            # (Author Year)
            (r'\(([A-Z][a-zA-Z\s&]+(?: et al\.?)?)\s+(\d{4})\)', 1),
        ]

        # Track replacements to avoid re-replacing
        seen = set()

        def replace_match(match):
            full_match = match.group(0)
            if full_match in seen:
                return full_match

            author_part = match.group(1).strip()
            year = match.group(2).strip()

            # Extract primary author
            author_last = extract_author_from_match(author_part)

            # Find in database
            entry = get_citation_entry(author_last)

            if entry:
                # Format standard citation
                standard_citation = format_standard_citation(author_last, entry)
                seen.add(full_match)
                self.all_citations[chapter_file][standard_citation] = entry
                self.stats["total_updated"] += 1
                return standard_citation
            else:
                # Keep original but track for review
                seen.add(full_match)
                self.all_citations[chapter_file][f"REVIEW: {full_match}"] = None
                self.stats["total_review"] += 1
                return full_match

        self.stats["total_found"] += len(seen)

        # Apply all patterns
        for pattern, _ in patterns:
            content = re.sub(pattern, replace_match, content)

        print(f"  Updated: {len([c for c in self.all_citations[chapter_file] if not c.startswith('REVIEW:')])} citations")
        print(f"  Review needed: {len([c for c in self.all_citations[chapter_file] if c.startswith('REVIEW:')])} citations")

        return content

    def update_all_chapters(self):
        """Process and update all chapter files."""
        for chapter_file in self.chapters:
            updated_content = self.process_chapter(chapter_file)
            if updated_content:
                # Write back to file
                filepath = self.research_dir / chapter_file
                filepath.write_text(updated_content, encoding='utf-8')
                print(f"  Saved: {chapter_file}")

    def generate_bibliography(self) -> str:
        """Generate COMPREHENSIVE_BIBLIOGRAPHY.md content."""
        lines = [
            "# Comprehensive Bibliography",
            "## MineWright \"One Abstraction Away\" AI Dissertation",
            "",
            f"**Generated:** {datetime.now().strftime('%Y-%m-%d')}",
            "**Purpose:** Complete bibliography with standardized citation format",
            "",
            "**Citation Format:** Author, \"Title\" (Year)",
            "",
            "---",
            "",
            "## Table of Contents",
            "",
            "1. [Textbooks and Monographs](#textbooks-and-monographs)",
            "2. [Game AI Collections](#game-ai-collections)",
            "3. [Game AI Architecture Patterns](#game-ai-architecture-patterns)",
            "4. [RPG and Companion AI](#rpg-and-companion-ai)",
            "5. [RTS and Strategy AI](#rts-and-strategy-ai)",
            "6. [LLM Foundations](#llm-foundations)",
            "7. [LLM Agents](#llm-agents)",
            "8. [Retrieval-Augmented Generation](#retrieval-augmented-generation)",
            "9. [Minecraft AI](#minecraft-ai)",
            "10. [Software Architecture](#software-architecture)",
            "11. [Emotional AI and Psychology](#emotional-ai-and-psychology)",
            "12. [Multi-Agent Coordination](#multi-agent-coordination)",
            "13. [API and Tool Documentation](#api-and-tool-documentation)",
            "",
            "---",
            "",
            ""
        ]

        # Collect all unique citations
        all_unique = {}
        for chapter_citations in self.all_citations.values():
            for citation, entry in chapter_citations.items():
                if citation not in all_unique:
                    all_unique[citation] = entry

        # Organize by category
        categories = {
            "Textbooks and Monographs": [],
            "Game AI Collections": [],
            "Game AI Architecture Patterns": [],
            "RPG and Companion AI": [],
            "RTS and Strategy AI": [],
            "LLM Foundations": [],
            "LLM Agents": [],
            "Retrieval-Augmented Generation": [],
            "Minecraft AI": [],
            "Software Architecture": [],
            "Emotional AI and Psychology": [],
            "Multi-Agent Coordination": [],
            "API and Tool Documentation": [],
            "Citations Needing Review": []
        }

        # Categorize citations
        review_keywords = ["REVIEW:", "Skyline", "Breach", "Mode", "MAC", "August"]
        for citation in sorted(all_unique.keys()):
            if any(kw in citation for kw in review_keywords):
                categories["Citations Needing Review"].append(citation)
            elif "Artificial Intelligence" in citation or "Software Architecture" in citation or "Patterns of Enterprise" in citation:
                categories["Textbooks and Monographs"].append(citation)
            elif "Game AI Pro" in citation:
                categories["Game AI Collections"].append(citation)
            elif "Handling Complexity" in citation or "Behavior Tree" in citation or "GOAP" in citation or "Utility AI" in citation:
                categories["Game AI Architecture Patterns"].append(citation)
            elif "The Sims" in citation or "Final Fantasy" in citation or "Dragon Age" in citation or "Divinity" in citation or "Shadow of the Colossus" in citation or "The Last of Us" in citation:
                categories["RPG and Companion AI"].append(citation)
            elif "StarCraft" in citation or "RTS" in citation or "Grandmaster" in citation:
                categories["RTS and Strategy AI"].append(citation)
            elif "Attention Is All" in citation or "Language Models are Few-Shot" in citation:
                categories["LLM Foundations"].append(citation)
            elif "Voyager" in citation or "ReAct" in citation or "AutoGPT" in citation or "BabyAGI" in citation or "LangChain" in citation:
                categories["LLM Agents"].append(citation)
            elif "Retrieval-Augmented" in citation or "Sentence-BERT" in citation or "RAG" in citation or "Dense Passage" in citation:
                categories["Retrieval-Augmented Generation"].append(citation)
            elif "MineDojo" in citation or "MineRL" in citation or "Minecraft" in citation:
                categories["Minecraft AI"].append(citation)
            elif "golden age" in citation or "Software Engineering" in citation:
                categories["Software Architecture"].append(citation)
            elif "Uncanny Valley" in citation or "Emotion" in citation:
                categories["Emotional AI and Psychology"].append(citation)
            elif "Multiagent" in citation or "Multi-Agent" in citation or "Social Network" in citation:
                categories["Multi-Agent Coordination"].append(citation)
            elif "API" in citation or "Documentation" in citation or "Technical Report" in citation:
                categories["API and Tool Documentation"].append(citation)
            else:
                categories["Game AI Architecture Patterns"].append(citation)

        # Generate output for each category
        for category, citations in categories.items():
            if citations:
                # Create anchor
                anchor = category.lower().replace(" ", "-").replace("/", "-")
                lines.append(f"## {category}")
                lines.append("")
                for citation in citations:
                    lines.append(f"- {citation}")
                    lines.append("")
                lines.append("")

        return "\n".join(lines)

    def save_bibliography(self):
        """Save the updated bibliography."""
        bibliography_path = self.research_dir / "COMPREHENSIVE_BIBLIOGRAPHY.md"
        content = self.generate_bibliography()
        bibliography_path.write_text(content, encoding='utf-8')
        print(f"\nBibliography saved to: {bibliography_path}")

    def generate_report(self) -> str:
        """Generate a summary report."""
        lines = [
            "# Citation Standardization Report",
            f"**Generated:** {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}",
            "",
            "## Summary",
            "",
            f"- **Total Chapters Processed:** {len(self.chapters)}",
            f"- **Total Citations Updated:** {self.stats['total_updated']}",
            f"- **Total Citations Needing Review:** {self.stats['total_review']}",
            "",
            "## Chapter Breakdown",
            ""
        ]

        for chapter_file in self.chapters:
            if chapter_file in self.all_citations:
                citations = self.all_citations[chapter_file]
                updated = len([c for c in citations if not c.startswith("REVIEW:")])
                review = len([c for c in citations if c.startswith("REVIEW:")])
                lines.append(f"- **{chapter_file}**:")
                lines.append(f"  - Updated: {updated}")
                lines.append(f"  - Review needed: {review}")
                lines.append("")

        # Citations needing review
        all_review = set()
        for chapter_citations in self.all_citations.values():
            all_review.update([c for c in chapter_citations if c.startswith("REVIEW:")])

        if all_review:
            lines.append("")
            lines.append("## Citations Needing Manual Review")
            lines.append("")
            lines.append("The following citations could not be automatically matched to the database:")
            lines.append("")
            for citation in sorted(all_review):
                lines.append(f"- {citation}")
            lines.append("")
            lines.append("**Action Required:** Add these citations to COMPLETE_CITATION_DATABASE in `citation_database.py`.")
            lines.append("")
            lines.append("Common missing types:")
            lines.append("- Game titles (Skyline, Civilization, Into the Breach)")
            lines.append("- Technical documentation (JSON Mode, MAC)")
            lines.append("- Date-only citations (August 2024)")
            lines.append("- Company names (Facebook AI Research)")
            lines.append("")

        return "\n".join(lines)


def main():
    """Main execution function."""
    base_dir = Path(__file__).parent.parent
    updater = CitationUpdater(str(base_dir))

    print("=" * 80)
    print("Citation Standardization and Update Script")
    print("=" * 80)

    # Update all chapters
    print("\nStep 1: Processing chapters and updating citations...")
    updater.update_all_chapters()

    # Generate bibliography
    print("\n" + "=" * 80)
    print("Step 2: Generating bibliography...")
    print("=" * 80)
    updater.save_bibliography()

    # Generate report
    print("\n" + "=" * 80)
    print("Step 3: Generating report...")
    print("=" * 80)
    report = updater.generate_report()
    report_path = Path(base_dir) / "docs" / "research" / "CITATION_STANDARDIZATION_REPORT.md"
    report_path.write_text(report, encoding='utf-8')
    print(f"Report saved to: {report_path}")

    print("\n" + "=" * 80)
    print("Citation Standardization Complete!")
    print("=" * 80)
    print("\nFiles updated:")
    print("1. All dissertation chapters (standardized citations)")
    print("2. COMPREHENSIVE_BIBLIOGRAPHY.md (complete bibliography)")
    print("3. CITATION_STANDARDIZATION_REPORT.md (summary and review items)")
    print("\nPlease review the changes and commit.")


if __name__ == "__main__":
    main()
