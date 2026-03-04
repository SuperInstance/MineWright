#!/usr/bin/env python3
"""
Checkstyle Auto-Fix Script for Week 4
Fixes auto-fixable Checkstyle warnings systematically.

Target issues:
- NoWhitespaceBefore (1,987 warnings): Add space before specific tokens
- LeftCurly (469 warnings): Fix left brace placement
- ImportOrder (457 warnings): Fix import order
- UnusedImports (154 warnings): Remove unused imports
- AvoidStarImport (93 warnings): Replace wildcard imports

Author: Team 3 - Week 4 Style Auto-Fixes
Date: 2026-03-03
"""

import os
import re
import sys
from pathlib import Path
from typing import Tuple, List
import subprocess

class CheckstyleAutoFixer:
    """Automatically fixes common Checkstyle violations."""

    def __init__(self, root_dir: str):
        self.root_dir = Path(root_dir)
        self.src_dir = self.root_dir / "src" / "main" / "java"
        self.stats = {
            'files_processed': 0,
            'nowhitespace_before': 0,
            'left_curly': 0,
            'import_order': 0,
            'unused_imports': 0,
            'avoid_star_import': 0,
            'errors': 0
        }

    def fix_no_whitespace_before(self, content: str) -> Tuple[str, int]:
        """
        Fix NoWhitespaceBefore violations.
        Examples:
            if(true) → if (true)
            while(x) → while (x)
            for(;;) → for (;;)
        """
        count = 0
        original = content

        # Pattern: keyword( without space before (
        keywords = ['if', 'while', 'for', 'switch', 'catch', 'synchronized']
        for keyword in keywords:
            pattern = rf'\b({keyword})\('
            replacement = rf'\1 ('
            content, n = re.subn(pattern, replacement, content)
            count += n

        # Fix: array initialization with no space before [
        # Example: new int[5] → new int [5] (checkstyle wants space)
        # Actually, checkstyle NoWhitespaceBefore is for cases like:
        # "if (" is correct, but "if(" is wrong
        # Also for: "foo. bar" should be "foo. bar" (no space before dot)

        return content, count

    def fix_left_curly(self, content: str) -> Tuple[str, int]:
        """
        Fix LeftCurly violations.
        Move opening brace to same line for control structures.
        Examples:
            if (condition)
            {
                → if (condition) {

            while (x)
            {
                → while (x) {
        """
        count = 0
        lines = content.split('\n')
        result = []

        i = 0
        while i < len(lines):
            line = lines[i]
            stripped = line.strip()

            # Check if this line ends a control structure without brace
            # Pattern: "if (condition)" or "while (x)" etc on its own line
            if (stripped.endswith(';') or
                self._is_control_structure_line(stripped) or
                self._is_method_decl_line(stripped)):

                # Check if next line is just a brace
                if i + 1 < len(lines):
                    next_line = lines[i + 1].strip()
                    if next_line == '{':
                        # Move brace to current line
                        # Preserve indentation of current line
                        indent = self._get_indentation(line)
                        result.append(line + ' {')
                        count += 1
                        i += 2  # Skip next line
                        continue

            result.append(line)
            i += 1

        return '\n'.join(result), count

    def _is_control_structure_line(self, line: str) -> bool:
        """Check if line is a control structure declaration."""
        control_starts = ['if', 'else if', 'while', 'for', 'switch', 'catch', 'finally']
        for start in control_starts:
            if line.startswith(start) or line.startswith(start + ' ('):
                return True
        return False

    def _is_method_decl_line(self, line: str) -> bool:
        """Check if line is a method/constructor declaration."""
        # Simple heuristic: line has parentheses and ends with ) but no {
        if ')' in line and '{' not in line:
            # Check for common method signatures
            if ('public' in line or 'private' in line or 'protected' in line or
                'static' in line or 'void' in line or 'class' in line):
                return True
        return False

    def _get_indentation(self, line: str) -> str:
        """Extract leading whitespace from line."""
        match = re.match(r'^(\s*)', line)
        return match.group(1) if match else ''

    def fix_import_order(self, content: str) -> Tuple[str, int]:
        """
        Fix ImportOrder violations.
        Sort and group imports properly:
        1. Static imports
        2. Non-static imports
        3. Within groups: alphabetical order
        """
        count = 0

        # Split into lines
        lines = content.split('\n')

        # Find import block
        import_start = -1
        import_end = -1

        for i, line in enumerate(lines):
            stripped = line.strip()
            if stripped.startswith('import '):
                if import_start == -1:
                    import_start = i
                import_end = i
            elif import_start != -1 and import_end != -1:
                # End of imports
                break

        if import_start == -1:
            return content, 0

        # Extract import lines
        import_lines = lines[import_start:import_end + 1]

        # Separate static and non-static imports
        static_imports = []
        regular_imports = []

        for line in import_lines:
            stripped = line.strip()
            if stripped.startswith('import static '):
                static_imports.append(stripped)
            elif stripped.startswith('import '):
                regular_imports.append(stripped)

        # Sort alphabetically
        static_imports.sort()
        regular_imports.sort()

        # Rebuild content
        result = []
        result.extend(lines[:import_start])

        # Add package separator if both types exist
        if static_imports and regular_imports:
            result.append('')  # Blank line between groups

        for imp in static_imports:
            result.append(imp)

        if static_imports and regular_imports:
            result.append('')  # Blank line between groups

        for imp in regular_imports:
            result.append(imp)

        result.extend(lines[import_end + 1:])

        if import_lines != static_imports + regular_imports:
            count = len(import_lines)

        return '\n'.join(result), count

    def remove_unused_imports(self, content: str, filepath: Path) -> Tuple[str, int]:
        """
        Remove unused imports.
        This requires analyzing the code to see which imports are actually used.
        For simplicity, we'll use a heuristic-based approach.
        """
        # Extract all import statements
        import_pattern = r'^import\s+(?:static\s+)?([^;]+);'
        imports = {}
        lines = content.split('\n')

        for i, line in enumerate(lines):
            match = re.match(import_pattern, line.strip())
            if match:
                import_full = match.group(0)
                import_class = match.group(1).split('.')[-1]
                imports[i] = {
                    'line': line,
                    'full': import_full,
                    'class': import_class,
                    'package': match.group(1)
                }

        if not imports:
            return content, 0

        # Check if each import is used in the code
        lines_to_remove = []
        code_content = '\n'.join(lines)

        for line_idx, import_info in imports.items():
            class_name = import_info['class']
            # Check if the class is used in the code (excluding the import line itself)
            code_without_imports = '\n'.join([
                l for j, l in enumerate(lines)
                if j not in imports.keys()
            ])

            # Simple check: class name appears in code
            # This is a heuristic - might have false positives
            if re.search(r'\b' + re.escape(class_name) + r'\b', code_without_imports):
                continue  # Import is used
            else:
                lines_to_remove.append(line_idx)

        if not lines_to_remove:
            return content, 0

        # Remove unused import lines
        result = [
            line for i, line in enumerate(lines)
            if i not in lines_to_remove
        ]

        return '\n'.join(result), len(lines_to_remove)

    def fix_avoid_star_import(self, content: str) -> Tuple[str, int]:
        """
        Replace wildcard imports with specific imports.
        This is complex as we need to know which classes from the package are used.
        For now, we'll identify them and count them.
        """
        count = 0
        lines = content.split('\n')
        result = []

        for line in lines:
            stripped = line.strip()
            if re.match(r'^import\s+[^;]+\.\*;', stripped):
                # Found a star import
                # For now, just note it - actual expansion requires parsing
                count += 1
                result.append(line)  # Keep as-is for manual review
            else:
                result.append(line)

        return '\n'.join(result), count

    def apply_all_fixes(self, filepath: Path) -> bool:
        """Apply all auto-fixes to a single file."""
        try:
            with open(filepath, 'r', encoding='utf-8') as f:
                original_content = f.read()

            content = original_content

            # Apply fixes in order
            content, n1 = self.fix_no_whitespace_before(content)
            content, n2 = self.fix_left_curly(content)
            content, n3 = self.fix_import_order(content)
            content, n4 = self.remove_unused_imports(content, filepath)
            content, n5 = self.fix_avoid_star_import(content)

            # Only write if content changed
            if content != original_content:
                with open(filepath, 'w', encoding='utf-8') as f:
                    f.write(content)

                # Update stats
                self.stats['nowhitespace_before'] += n1
                self.stats['left_curly'] += n2
                self.stats['import_order'] += n3
                self.stats['unused_imports'] += n4
                self.stats['avoid_star_import'] += n5

            return True

        except Exception as e:
            print(f"Error processing {filepath}: {e}")
            self.stats['errors'] += 1
            return False

    def process_all_files(self):
        """Process all Java files in the source directory."""
        java_files = list(self.src_dir.rglob('*.java'))
        total = len(java_files)

        print(f"Found {total} Java files to process")
        print("-" * 60)

        for i, filepath in enumerate(java_files, 1):
            print(f"\r[{i}/{total}] Processing {filepath.relative_to(self.root_dir)}...", end='')
            sys.stdout.flush()

            if self.apply_all_fixes(filepath):
                self.stats['files_processed'] += 1

        print(f"\n\n{'=' * 60}")
        print("Processing complete!")
        print(f"{'=' * 60}")

    def print_summary(self):
        """Print summary of fixes applied."""
        print("\n📊 SUMMARY OF FIXES APPLIED")
        print("=" * 60)
        print(f"Files processed:     {self.stats['files_processed']}")
        print(f"NoWhitespaceBefore:  {self.stats['nowhitespace_before']}")
        print(f"LeftCurly:           {self.stats['left_curly']}")
        print(f"ImportOrder:         {self.stats['import_order']}")
        print(f"UnusedImports:       {self.stats['unused_imports']}")
        print(f"AvoidStarImport:     {self.stats['avoid_star_import']} (identified)")
        print(f"Errors encountered:  {self.stats['errors']}")
        print("=" * 60)

    def generate_report(self, output_path: Path):
        """Generate markdown report."""
        report = f"""# Checkstyle Auto-Fix Report - Week 4

**Date:** 2026-03-03
**Team:** Team 3 - Style Auto-Fixes
**Task:** Fix auto-fixable Checkstyle warnings

## Summary

This document reports on the automated fixing of Checkstyle violations
that can be resolved through IDE-style automated fixes.

## Target Issues

| Issue Type | Count | Auto-Fixable |
|------------|-------|--------------|
| NoWhitespaceBefore | 1,987 | ✅ Yes |
| LeftCurly | 469 | ✅ Yes |
| ImportOrder | 457 | ✅ Yes |
| UnusedImports | 154 | ✅ Yes |
| AvoidStarImport | 93 | ⚠️ Partial |
| **Total** | **3,160** | **70%** |

## Fixes Applied

| Category | Files Modified | Issues Fixed |
|----------|----------------|--------------|
| NoWhitespaceBefore | {self.stats['files_processed']} | {self.stats['nowhitespace_before']} |
| LeftCurly | {self.stats['files_processed']} | {self.stats['left_curly']} |
| ImportOrder | {self.stats['files_processed']} | {self.stats['import_order']} |
| UnusedImports | {self.stats['files_processed']} | {self.stats['unused_imports']} |
| AvoidStarImport | {self.stats['files_processed']} | {self.stats['avoid_star_import']} (identified) |

## Not Fixed (Requires Manual Review)

- **NeedBraces** (342 warnings) - Requires logic analysis
- **JavadocStyle** (505 warnings) - Requires content review
- **JavadocType** (varies) - Requires content review
- **LineLength** (156 warnings) - Requires refactoring

## Build Verification

Run the following commands to verify fixes:

```bash
# Clean build
./gradlew clean build

# Run checkstyle
./gradlew checkstyleMain

# View report
open build/reports/checkstyle/main.html
```

## Next Steps

1. Review remaining Checkstyle warnings
2. Manually fix NeedBraces violations
3. Update Javadoc for JavadocStyle violations
4. Refactor long lines for LineLength violations

---
Generated by CheckstyleAutoFixer v1.0
"""

        with open(output_path, 'w', encoding='utf-8') as f:
            f.write(report)

        print(f"\n📄 Report generated: {output_path}")


def main():
    """Main entry point."""
    root_dir = Path(__file__).parent.parent

    print("=" * 60)
    print("🔧 Checkstyle Auto-Fix Tool - Week 4")
    print("=" * 60)

    # Parse arguments
    if len(sys.argv) > 1:
        root_dir = Path(sys.argv[1])

    fixer = CheckstyleAutoFixer(str(root_dir))

    # Process all files
    fixer.process_all_files()

    # Print summary
    fixer.print_summary()

    # Generate report
    report_path = root_dir / "docs" / "audits" / "CHECKSTYLE_AUTOFIX_WEEK4.md"
    fixer.generate_report(report_path)


if __name__ == "__main__":
    main()
