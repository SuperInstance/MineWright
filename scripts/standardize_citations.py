#!/usr/bin/env python3
"""
Citation Standardization Script for Steve AI Dissertation

This script reads all dissertation chapters, extracts citations, and standardizes them to:
Author, "Title" (Year)

Examples:
- Bass, Clements, and Kazman, "Software Architecture in Practice" (2003)
- Isla, "Handling Complexity in the Halo 2 AI" (2005)
- Orkin, "Applying Goal-Oriented Action Planning to Games" (2005)
"""

import re
import os
from pathlib import Path
from typing import Dict, List, Tuple
import json

# Citation format: Author, "Title" (Year)
STANDARD_CITATION_PATTERN = r'([A-Z][a-zA-Z\s&,\.]+(?:\set\sal\.|\sand\s[A-Z][a-zA-Z\s]+)*),\s*"([^"]+)"\s+\((\d{4})\)'

# Patterns to find various citation formats
CITATION_PATTERNS = [
    # Author, "Title" (Year) - Already standard
    r'([A-Z][a-zA-Z\s&,\.]+(?:\set\sal\.|\sand\s[A-Z][a-zA-Z\s]+)*),\s*"([^"]+)"\s+\((\d{4})\)',
    # (Author, Year) style
    r'\(([A-Z][a-zA-Z\s&]+),\s*(\d{4})\)',
    # (Author Year) style
    r'\(([A-Z][a-zA-Z\s&]+)\s+(\d{4})\)',
    # [Author, Year] style
    r'\[([A-Z][a-zA-Z\s&]+),\s*(\d{4})\]',
    # Author (Year) inline
    r'([A-Z][a-zA-Z\s&]+)\s+\((\d{4})\)',
]

# Mapping of author names to their full works
CITATION_DATABASE = {
    # Game AI Foundations
    "Millington": {
        "Funge": {
            "title": "Artificial Intelligence for Games",
            "year": "2009",
            "full_authors": "Millington & Funge"
        }
    },
    "Russell": {
        "Norvig": {
            "title": "Artificial Intelligence: A Modern Approach",
            "year": "2020",
            "full_authors": "Russell & Norvig"
        }
    },
    "Sutton": {
        "Barto": {
            "title": "Reinforcement Learning: An Introduction",
            "year": "2018",
            "full_authors": "Sutton & Barto"
        }
    },
    "Bass": {
        "Clements": {
            "Kazman": {
                "title": "Software Architecture in Practice",
                "year": "2012",
                "full_authors": "Bass, Clements, & Kazman"
            }
        }
    },
    "Rabin": {
        "title": "Game AI Pro",
        "year": "2022",
        "full_authors": "Rabin"
    },

    # Behavior Trees
    "Isla": {
        "title": "Handling Complexity in the Halo 2 AI",
        "year": "2005",
        "full_authors": "Isla"
    },
    "Champandard": {
        "title": "AI Game Development: Synthetic Creatures with Learning and Reactive Behaviors",
        "year": "2007",
        "full_authors": "Champandard"
    },

    # GOAP
    "Orkin": {
        "title": "Applying Goal-Oriented Action Planning to Games",
        "year": "2004",
        "full_authors": "Orkin"
    },

    # Utility AI
    "Mark": {
        "title": "Utility AI: A Simple, Flexible Way to Model Character Decisions",
        "year": "2009",
        "full_authors": "Mark"
    },
    "Tozour": {
        "title": "Influence Maps",
        "year": "2003",
        "full_authors": "Tozour"
    },
    "Hart": {
        "Nilsson": {
            "Raphael": {
                "title": "A Formal Basis for the Heuristic Determination of Minimum Cost Paths",
                "year": "1968",
                "full_authors": "Hart, Nilsson, & Raphael"
            }
        }
    },

    # RPG/Adventure AI
    "Wright": {
        "title": "The Sims",
        "year": "2000",
        "full_authors": "Wright"
    },
    "Katano": {
        "title": "Developing Final Fantasy XII's Gambit System",
        "year": "2006",
        "full_authors": "Katano"
    },
    "Ortony": {
        "Clore": {
            "Collins": {
                "title": "The Cognitive Structure of Emotions",
                "year": "1988",
                "full_authors": "Ortony, Clore, & Collins"
            }
        }
    },

    # LLM Agents
    "Vaswani": {
        "title": "Attention Is All You Need",
        "year": "2017",
        "full_authors": "Vaswani et al."
    },
    "Brown": {
        "title": "Language Models are Few-Shot Learners",
        "year": "2020",
        "full_authors": "Brown et al."
    },
    "Yao": {
        "title": "ReAct: Synergizing Reasoning and Acting in Language Models",
        "year": "2022",
        "full_authors": "Yao et al."
    },
    "Wang": {
        "title": "Voyager: An open-ended embodied agent with large language models",
        "year": "2023",
        "full_authors": "Wang et al."
    },
    "Lewis": {
        "title": "Retrieval-Augmented Generation for Knowledge-Intensive NLP Tasks",
        "year": "2020",
        "full_authors": "Lewis et al."
    },
    "Fan": {
        "title": "MineDojo: Building Open-Ended Embodied Agents with Internet-Scale Knowledge",
        "year": "2022",
        "full_authors": "Fan et al."
    },
    "Baker": {
        "title": "Human-level Play in the Game of Minecraft by Out-of-Distribution Reinforcement Learning",
        "year": "2022",
        "full_authors": "Baker et al."
    },

    # Minecraft AI
    "Guss": {
        "title": "MineRL: A Large-Scale Dataset of Minecraft Demonstrations",
        "year": "2019",
        "full_authors": "Guss et al."
    },

    # Software Architecture
    "Fowler": {
        "title": "Patterns of Enterprise Application Architecture",
        "year": "2002",
        "full_authors": "Fowler"
    },
    "Shaw": {
        "title": "The golden age of software architecture",
        "year": "2006",
        "full_authors": "Shaw & Clements"
    },
    "Van Vliet": {
        "title": "Software Engineering: Principles and Practice",
        "year": "2008",
        "full_authors": "Van Vliet"
    },
}


class CitationStandardizer:
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
        self.all_citations = {}

    def extract_citations_from_file(self, filepath: Path) -> Dict[str, dict]:
        """Extract all citations from a single file."""
        content = filepath.read_text(encoding='utf-8')
        citations = {}

        # Find all citation patterns
        for pattern in CITATION_PATTERNS:
            matches = re.finditer(pattern, content)
            for match in matches:
                if len(match.groups()) == 3:
                    # Author, Title, Year format
                    authors, title, year = match.groups()
                    citation_key = self._get_citation_key(authors, year)
                    citations[citation_key] = {
                        'authors': authors.strip(),
                        'title': title.strip() if title else "",
                        'year': year.strip(),
                        'full_text': match.group(0)
                    }
                elif len(match.groups()) == 2:
                    # (Author, Year) format
                    author, year = match.groups()
                    citation_key = self._get_citation_key(author, year)
                    if citation_key not in citations:
                        citations[citation_key] = {
                            'authors': author.strip(),
                            'title': "",
                            'year': year.strip(),
                            'full_text': match.group(0)
                        }

        return citations

    def _get_citation_key(self, authors: str, year: str) -> str:
        """Generate a unique key for a citation."""
        # Extract first author's last name
        first_author = authors.split(',')[0].strip().split()[-1]
        return f"{first_author}_{year}"

    def standardize_citation(self, citation: dict) -> str:
        """Convert a citation to standard format."""
        # Check if we have database info
        author_last = citation['authors'].split()[-1] if citation['authors'] else ""

        # If we have title info in database, use it
        if author_last in CITATION_DATABASE:
            db_entry = CITATION_DATABASE[author_last]
            if isinstance(db_entry, dict) and 'title' in db_entry:
                return f"{db_entry['full_authors']}, \"{db_entry['title']}\" ({db_entry['year']})"

        # If we have title from citation, use it
        if citation['title']:
            return f"{citation['authors']}, \"{citation['title']}\" ({citation['year']})"

        # Otherwise return authors and year only
        return f"{citation['authors']} ({citation['year']})"

    def process_all_chapters(self):
        """Process all dissertation chapters and standardize citations."""
        for chapter_file in self.chapters:
            filepath = self.research_dir / chapter_file
            if not filepath.exists():
                print(f"Warning: {filepath} does not exist")
                continue

            print(f"\nProcessing {chapter_file}...")
            citations = self.extract_citations_from_file(filepath)
            self.all_citations[chapter_file] = citations

            print(f"  Found {len(citations)} unique citations")

            # Print sample citations
            for i, (key, citation) in enumerate(list(citations.items())[:5]):
                std = self.standardize_citation(citation)
                print(f"    {key}: {citation['full_text'][:60]}... -> {std}")

    def generate_bibliography(self) -> str:
        """Generate COMPREHENSIVE_BIBLIOGRAPHY.md content."""
        lines = [
            "# Comprehensive Bibliography",
            "## MineWright \"One Abstraction Away\" AI Dissertation",
            "",
            f"**Date:** 2026-03-01",
            "**Purpose:** Complete bibliography for citation standardization",
            "",
            "---",
            "",
            "## Standardized Citations",
            ""
        ]

        # Collect all unique citations across all chapters
        all_std_citations = {}
        for chapter, citations in self.all_citations.items():
            for key, citation in citations.items():
                std_citation = self.standardize_citation(citation)
                if std_citation not in all_std_citations:
                    all_std_citations[std_citation] = []
                all_std_citations[std_citation].append(chapter)

        # Sort by first author name
        for citation in sorted(all_std_citations.keys()):
            chapters = sorted(all_std_citations[citation])
            lines.append(f"**{citation}**")
            lines.append(f"- **Used in:** {', '.join(chapters)}")
            lines.append("")

        return "\n".join(lines)

    def save_bibliography(self):
        """Save the updated bibliography."""
        bibliography_path = self.research_dir / "COMPREHENSIVE_BIBLIOGRAPHY.md"
        content = self.generate_bibliography()
        bibliography_path.write_text(content, encoding='utf-8')
        print(f"\nBibliography saved to: {bibliography_path}")


def main():
    """Main execution function."""
    base_dir = Path(__file__).parent.parent
    standardizer = CitationStandardizer(str(base_dir))

    print("=" * 80)
    print("Citation Standardization Script")
    print("=" * 80)

    # Process all chapters
    standardizer.process_all_chapters()

    # Generate and save bibliography
    print("\n" + "=" * 80)
    print("Generating Bibliography...")
    print("=" * 80)
    standardizer.save_bibliography()

    print("\n" + "=" * 80)
    print("Citation Standardization Complete!")
    print("=" * 80)
    print("\nNext steps:")
    print("1. Review the generated COMPREHENSIVE_BIBLIOGRAPHY.md")
    print("2. Add missing citations to CITATION_DATABASE if needed")
    print("3. Run the script again to update chapters with standardized citations")


if __name__ == "__main__":
    main()
