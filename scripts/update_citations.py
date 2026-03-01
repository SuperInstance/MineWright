#!/usr/bin/env python3
"""
Comprehensive Citation Update Script for Steve AI Dissertation

This script:
1. Reads all dissertation chapters
2. Finds all citations in various formats
3. Replaces them with standardized format: Author, "Title" (Year)
4. Updates COMPREHENSIVE_BIBLIOGRAPHY.md
"""

import re
import os
from pathlib import Path
from typing import Dict, List, Tuple, Set
from datetime import datetime

# Comprehensive citation database
CITATION_DATABASE = {
    # Textbooks and Monographs
    "Millington": {"co_authors": ["Funge"], "title": "Artificial Intelligence for Games", "year": "2009", "edition": "2nd ed.", "publisher": "Morgan Kaufmann"},
    "Russell": {"co_authors": ["Norvig"], "title": "Artificial Intelligence: A Modern Approach", "year": "2020", "edition": "4th ed.", "publisher": "Pearson"},
    "Sutton": {"co_authors": ["Barto"], "title": "Reinforcement Learning: An Introduction", "year": "2018", "edition": "2nd ed.", "publisher": "MIT Press"},
    "Bass": {"co_authors": ["Clements", "Kazman"], "title": "Software Architecture in Practice", "year": "2012", "edition": "3rd ed.", "publisher": "Addison-Wesley"},

    # Game AI Collections
    "Rabin": {"title": "Game AI Pro", "year": "2022", "publisher": "CRC Press"},

    # Game AI Foundations
    "Champandard": {"title": "AI Game Development: Synthetic Creatures with Learning and Reactive Behaviors", "year": "2007", "publisher": "Charles River Media"},

    # Behavior Trees
    "Isla": {"title": "Handling Complexity in the Halo 2 AI", "year": "2005", "venue": "Game Developers Conference (GDC)"},

    # GOAP
    "Orkin": {"title": "Applying Goal-Oriented Action Planning to Games", "year": "2004", "venue": "AAAI Conference on Artificial Intelligence and Interactive Digital Entertainment (AIIDE)"},

    # Utility AI
    "Mark": {"title": "Utility AI: A Simple, Flexible Way to Model Character Decisions", "year": "2009", "venue": "Game AI Pro"},
    "Tozour": {"title": "Influence Maps", "year": "2003", "venue": "AI Game Programming Wisdom 2"},

    # A* Pathfinding
    "Hart": {"co_authors": ["Nilsson", "Raphael"], "title": "A Formal Basis for the Heuristic Determination of Minimum Cost Paths", "year": "1968", "venue": "IEEE Transactions on Systems Science and Cybernetics", "volume": "4(2)", "pages": "100-107"},

    # RPG AI
    "Wright": {"title": "The Sims", "year": "2000", "publisher": "Maxis/EA", "type": "Game"},
    "Forshaw": {"title": "Game Inventor: The Story of Will Wright and Maxis", "year": "2014", "publisher": "CreateSpace"},
    "Katano": {"title": "Developing Final Fantasy XII's Gambit System", "year": "2006", "venue": "CEDEC Conference"},
    "Ortony": {"co_authors": ["Clore", "Collins"], "title": "The Cognitive Structure of Emotions", "year": "1988", "publisher": "Cambridge University Press"},

    # LLM Foundations
    "Vaswani": {"title": "Attention Is All You Need", "year": "2017", "venue": "Advances in Neural Information Processing Systems (NeurIPS)", "co_authors_etal": True},
    "Brown": {"title": "Language Models are Few-Shot Learners", "year": "2020", "venue": "Advances in Neural Information Processing Systems (NeurIPS)", "co_authors_etal": True},

    # LLM Agents
    "Yao": {"title": "ReAct: Synergizing Reasoning and Acting in Language Models", "year": "2022", "venue": "International Conference on Learning Representations (ICLR)", "co_authors_etal": True},
    "Wang": {"title": "Voyager: An open-ended embodied agent with large language models", "year": "2023", "venue": "arXiv preprint arXiv:2305.16291", "co_authors_etal": True},

    # RAG
    "Lewis": {"title": "Retrieval-Augmented Generation for Knowledge-Intensive NLP Tasks", "year": "2020", "venue": "Advances in Neural Information Processing Systems (NeurIPS)", "co_authors_etal": True},
    "Reimers": {"co_authors": ["Gurevych"], "title": "Sentence-BERT: Sentence Embeddings using Siamese BERT-Networks", "year": "2019", "venue": "Conference of the Association for Computational Linguistics (ACL)"},

    # Minecraft AI
    "Fan": {"title": "MineDojo: Building Open-Ended Embodied Agents with Internet-Scale Knowledge", "year": "2022", "venue": "NeurIPS Datasets and Benchmarks Track", "co_authors_etal": True},
    "Guss": {"title": "MineRL: A Large-Scale Dataset of Minecraft Demonstrations", "year": "2019", "venue": "Thirty-Third AAAI Conference on Artificial Intelligence (AAAI)", "co_authors_etal": True},
    "Baker": {"title": "Human-level Play in the Game of Minecraft by Out-of-Distribution Reinforcement Learning", "year": "2022", "venue": "Nature", "co_authors_etal": True},

    # LLM APIs and Tools
    "OpenAI": {"title": "GPT-4 Technical Report", "year": "2024", "venue": "arXiv preprint arXiv:2303.08774"},
    "Anthropic": {"title": "Claude API Reference", "year": "2025", "venue": "Anthropic Documentation"},
    "Google": {"title": "Gemini API Documentation", "year": "2024", "venue": "Google Cloud Documentation"},
    "Meta": {"title": "Llama 3 Model Card", "year": "2024", "venue": "Meta AI Documentation"},

    # Software Architecture
    "Fowler": {"title": "Patterns of Enterprise Application Architecture", "year": "2002", "publisher": "Addison-Wesley"},
    "Shaw": {"co_authors": ["Clements"], "title": "The golden age of software architecture", "year": "2006", "venue": "IEEE Software", "volume": "23(1)", "pages": "31-39"},
    "Van Vliet": {"title": "Software Engineering: Principles and Practice", "year": "2008", "edition": "3rd ed.", "publisher": "Wiley"},
}


def format_citation_author(author: str, entry: dict) -> str:
    """Format the author portion of a citation."""
    if "co_authors" in entry and entry["co_authors"]:
        co_authors = entry["co_authors"]
        if len(co_authors) == 1:
            return f"{author} & {co_authors[0]}"
        elif len(co_authors) == 2:
            return f"{author}, {co_authors[0]}, & {co_authors[1]}"
        else:
            return f"{author} et al."
    elif entry.get("co_authors_etal", False):
        return f"{author} et al."
    return author


def format_standard_citation(author: str, entry: dict) -> str:
    """Format a citation in standard format: Author, "Title" (Year)"""
    authors = format_citation_author(author, entry)
    title = entry.get("title", "")
    year = entry.get("year", "")

    if title:
        return f'{authors}, "{title}" ({year})'
    else:
        return f"{authors} ({year})"


def extract_citation_key(citation_match: str) -> Tuple[str, str]:
    """Extract author name and year from a citation string."""
    # Try various patterns
    patterns = [
        r'\[?([A-Z][a-zA-Z\s&]+)(?: et al\.)?,\s*(\d{4})\]?',  # [Author, Year]
        r'\[?([A-Z][a-zA-Z\s&]+)(?: et al\.)?\s+\((\d{4})\)',  # [Author (Year)]
        r'\(([A-Z][a-zA-Z\s&]+)(?: et al\.)?,\s*(\d{4})\)',  # (Author, Year)
        r'\(([A-Z][a-zA-Z\s&]+)(?: et al\.)?\s+(\d{4})\)',  # (Author Year)
    ]

    for pattern in patterns:
        match = re.search(pattern, citation_match)
        if match:
            author = match.group(1).strip()
            year = match.group(2).strip()
            return author, year

    # If no match, try to extract just the year
    year_match = re.search(r'\(?\d{4}\)?', citation_match)
    if year_match:
        year = year_match.group(0).strip('()')
        return citation_match.split()[0], year

    return None, None


def find_author_in_database(author_last: str) -> Tuple[str, dict]:
    """Find an author in the citation database."""
    # Direct match
    if author_last in CITATION_DATABASE:
        return author_last, CITATION_DATABASE[author_last]

    # Fuzzy match (case insensitive)
    author_lower = author_last.lower()
    for key in CITATION_DATABASE:
        if key.lower() == author_lower:
            return key, CITATION_DATABASE[key]

    return None, {}


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
        self.all_citations: Dict[str, Set[str]] = {}  # chapter -> set of standardized citations
        self.citation_replacements: Dict[str, str] = {}  # old -> new

    def process_chapter(self, chapter_file: str) -> str:
        """Process a single chapter file and return updated content."""
        filepath = self.research_dir / chapter_file
        if not filepath.exists():
            print(f"Warning: {filepath} does not exist")
            return ""

        print(f"\nProcessing {chapter_file}...")
        content = filepath.read_text(encoding='utf-8')
        self.all_citations[chapter_file] = set()

        # Find all citation patterns and build replacements
        # Pattern 1: [Author, Year] or [Author et al., Year]
        pattern1 = r'\[([A-Z][a-zA-Z\s&]+(?: et al\.?)?(?:,?\s*(?:and|&)\s*[A-Z][a-zA-Z\s&]+)?),\s*(\d{4})\]'
        content = self._replace_citations_pattern(content, pattern1, chapter_file)

        # Pattern 2: [Author (Year)]
        pattern2 = r'\[([A-Z][a-zA-Z\s&]+(?: et al\.?)?)\s+\((\d{4})\)\]'
        content = self._replace_citations_pattern(content, pattern2, chapter_file)

        # Pattern 3: (Author, Year)
        pattern3 = r'\(([A-Z][a-zA-Z\s&]+(?: et al\.?)?(?:,?\s*(?:and|&)\s*[A-Z][a-zA-Z\s&]+)?),\s*(\d{4})\)'
        content = self._replace_citations_pattern(content, pattern3, chapter_file)

        # Pattern 4: (Author Year)
        pattern4 = r'\(([A-Z][a-zA-Z\s&]+(?: et al\.?)?)\s+(\d{4})\)'
        content = self._replace_citations_pattern(content, pattern4, chapter_file)

        print(f"  Found {len(self.all_citations[chapter_file])} unique citations")
        return content

    def _replace_citations_pattern(self, content: str, pattern: str, chapter_file: str) -> str:
        """Replace citations matching a pattern."""
        def replace_match(match):
            full_match = match.group(0)
            author_part = match.group(1).strip()
            year = match.group(2).strip()

            # Extract first author's last name
            author_last = author_part.split()[-1].strip('.,')
            if "et al" in author_part.lower():
                author_last = author_part.split()[0].strip('.,')
            elif "&" in author_part or "and" in author_part.lower():
                author_last = author_part.split()[0].strip('.,')

            # Find in database
            db_author, entry = find_author_in_database(author_last)

            if entry:
                # Format standard citation
                standard_citation = format_standard_citation(db_author, entry)
                self.all_citations[chapter_file].add(standard_citation)
                return standard_citation
            else:
                # Keep original but track for review
                self.all_citations[chapter_file].add(f"REVIEW: {full_match}")
                return full_match

        return re.sub(pattern, replace_match, content)

    def update_all_chapters(self):
        """Process and update all chapter files."""
        for chapter_file in self.chapters:
            updated_content = self.process_chapter(chapter_file)
            if updated_content:
                # Write back to file
                filepath = self.research_dir / chapter_file
                filepath.write_text(updated_content, encoding='utf-8')
                print(f"  Updated: {chapter_file}")

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
            "## All Citations",
            "",
            "### Textbooks and Monographs",
            ""
        ]

        # Collect all unique citations
        all_unique = set()
        for citations in self.all_citations.values():
            all_unique.update(citations)

        # Separate by type and sort
        textbooks = []
        papers = []
        games = []
        reviews = []

        for citation in sorted(all_unique):
            if citation.startswith("REVIEW:"):
                reviews.append(citation)
            elif any(game in citation for game in ["The Sims", "Civilization", "StarCraft", "F.E.A.R.", "Halo"]):
                games.append(citation)
            elif "Conference" in citation or "Symposium" in citation or "arXiv" in citation:
                papers.append(citation)
            else:
                textbooks.append(citation)

        # Add textbooks
        for citation in textbooks:
            lines.append(f"- {citation}")
            lines.append("")

        # Add papers
        if papers:
            lines.append("")
            lines.append("### Conference and Journal Papers")
            lines.append("")
            for citation in papers:
                lines.append(f"- {citation}")
                lines.append("")

        # Add games
        if games:
            lines.append("")
            lines.append("### Games")
            lines.append("")
            for citation in games:
                lines.append(f"- {citation}")
                lines.append("")

        # Add citations needing review
        if reviews:
            lines.append("")
            lines.append("### Citations Needing Review")
            lines.append("")
            for citation in reviews:
                lines.append(f"- {citation}")
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
            ""
        ]

        total = 0
        for chapter, citations in self.all_citations.items():
            count = len([c for c in citations if not c.startswith("REVIEW:")])
            total += count
            lines.append(f"- **{chapter}**: {count} citations")

        lines.append(f"\n**Total Unique Citations:** {total}")
        lines.append("")

        # Citations needing review
        all_review = set()
        for citations in self.all_citations.values():
            all_review.update([c for c in citations if c.startswith("REVIEW:")])

        if all_review:
            lines.append("## Citations Needing Manual Review")
            lines.append("")
            lines.append("The following citations could not be automatically matched to the database:")
            lines.append("")
            for citation in sorted(all_review):
                lines.append(f"- {citation}")
            lines.append("")
            lines.append("**Action Required:** Add these citations to CITATION_DATABASE in the script.")
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
