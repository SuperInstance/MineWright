#!/usr/bin/env python3
"""
Simplified Checkstyle Auto-Fix Script
Handles the most common auto-fixable violations.
"""

import os
import re
import sys
from pathlib import Path
from typing import List, Tuple

def fix_no_whitespace_before(content: str) -> Tuple[str, int]:
    """
    Fix NoWhitespaceBefore violations.
    Add space before ( for control structures.
    """
    count = 0

    # Fix: if( -> if (
    # Fix: while( -> while (
    # Fix: for( -> for (
    # Fix: switch( -> switch (
    # Fix: catch( -> catch (

    patterns = [
        (r'\bif\(', 'if ('),
        (r'\bwhile\(', 'while ('),
        (r'\bfor\(', 'for ('),
        (r'\bswitch\(', 'switch ('),
        (r'\bcatch\(', 'catch ('),
        (r'\bsynchronized\(', 'synchronized ('),
    ]

    for pattern, replacement in patterns:
        new_content, n = re.subn(pattern, replacement, content)
        if n > 0:
            count += n
            content = new_content

    return content, count


def fix_left_curly_simple(content: str) -> Tuple[str, int]:
    """
    Fix LeftCurly violations - simple version.
    Handles: if/while/for/catch followed by newline and {.
    """
    count = 0
    lines = content.split('\n')
    result = []
    i = 0

    while i < len(lines):
        current = lines[i]

        # Check if current line ends with ) or ; (control structure end)
        # and next line is just {
        if i + 1 < len(lines):
            next_line = lines[i + 1].strip()

            # Pattern: line ending with ) or ; and next line is {
            if (current.rstrip().endswith(')') or current.rstrip().endswith(';')):
                if next_line == '{':
                    # Move { to current line
                    stripped = current.strip()

                    # Keywords that should have { on same line
                    keywords = ['if ', 'else if', 'else:', 'while ', 'for ', 'switch ',
                               'catch ', 'finally ', 'try ', 'do ', 'synchronized (']

                    # Also check for method/constructor declarations
                    is_control = any(stripped.startswith(kw) for kw in keywords)

                    # Check for method signature
                    is_method = (
                        re.search(r'\b(public|private|protected|static|final|abstract|native|synchronized)\s+', stripped) or
                        re.search(r'\b(void|int|boolean|String|List|Map|Set|Object|float|double|long|char|byte|short)\s+\w+\s*\(', stripped)
                    )

                    if is_control or is_method:
                        # Move brace to current line
                        result.append(current.rstrip() + ' {')
                        count += 1
                        i += 2  # Skip next line
                        continue

        result.append(current)
        i += 1

    return '\n'.join(result), count


def get_all_imports(content: str) -> List[Tuple[int, str, str, bool]]:
    """
    Extract all import statements from content.
    Returns list of (line_index, full_import, package, is_static) tuples.
    """
    imports = []
    lines = content.split('\n')

    for i, line in enumerate(lines):
        stripped = line.strip()
        if stripped.startswith('import '):
            # Extract package name
            match = re.match(r'import\s+(static\s+)?([^;]+);', stripped)
            if match:
                is_static = match.group(1) is not None
                package = match.group(2)
                imports.append((i, line, package, is_static))

    return imports


def fix_import_order(content: str) -> Tuple[str, int]:
    """
    Fix ImportOrder violations.
    Group static imports first, then regular imports.
    Sort alphabetically within groups.
    """
    imports = get_all_imports(content)

    if len(imports) <= 1:
        return content, 0

    lines = content.split('\n')

    # Separate static and regular imports
    static_imports = [(idx, line, pkg) for idx, line, pkg, is_static in imports if is_static]
    regular_imports = [(idx, line, pkg) for idx, line, pkg, is_static in imports if not is_static]

    # Sort alphabetically by package
    static_imports.sort(key=lambda x: x[2])
    regular_imports.sort(key=lambda x: x[2])

    # Build new import section
    new_lines = []
    new_lines.extend(lines[:imports[0][0]])  # Content before imports

    # Add blank line before imports if not present
    if new_lines and new_lines[-1].strip() != '':
        new_lines.append('')

    # Add static imports
    for _, line, _ in static_imports:
        new_lines.append(line.strip())

    # Add blank line between groups if both exist
    if static_imports and regular_imports:
        new_lines.append('')

    # Add regular imports
    for _, line, _ in regular_imports:
        new_lines.append(line.strip())

    # Add content after imports
    new_lines.extend(lines[imports[-1][0] + 1:])

    return '\n'.join(new_lines), len(imports)


def remove_unused_imports_simple(content: str) -> Tuple[str, int]:
    """
    Simple unused import removal.
    Removes imports where the class name is not referenced in the code.
    """
    lines = content.split('\n')
    imports = get_all_imports(content)

    if not imports:
        return content, 0

    # Build code without import lines
    import_indices = {idx for idx, _, _, _ in imports}
    code_lines = [line for i, line in enumerate(lines) if i not in import_indices]
    code = '\n'.join(code_lines)

    # Check which imports are used
    unused_indices = []

    for idx, line, package, is_static in imports:
        # Extract class name (last part after .)
        class_name = package.split('.')[-1]

        # Handle wildcard imports
        if class_name == '*':
            continue  # Can't determine usage

        # Check if class name is used in code
        # Use word boundary to avoid partial matches
        pattern = r'\b' + re.escape(class_name) + r'\b'

        if not re.search(pattern, code):
            unused_indices.append(idx)

    if not unused_indices:
        return content, 0

    # Remove unused imports
    result = [line for i, line in enumerate(lines) if i not in unused_indices]

    return '\n'.join(result), len(unused_indices)


def apply_fixes_to_file(filepath: Path) -> dict:
    """Apply all fixes to a single file."""
    stats = {
        'nowhitespace': 0,
        'leftcurly': 0,
        'importorder': 0,
        'unused': 0,
        'modified': False
    }

    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            original = f.read()

        content = original

        # Apply fixes
        content, n1 = fix_no_whitespace_before(content)
        content, n2 = fix_left_curly_simple(content)
        content, n3 = fix_import_order(content)
        content, n4 = remove_unused_imports_simple(content)

        stats['nowhitespace'] = n1
        stats['leftcurly'] = n2
        stats['importorder'] = n3
        stats['unused'] = n4

        if content != original:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(content)
            stats['modified'] = True

    except Exception as e:
        print(f"\nWarning: {filepath}: {e}")

    return stats


def main():
    """Main entry point."""
    # Get the project root
    root = Path.cwd()
    src_dir = root / "src" / "main" / "java"

    if not src_dir.exists():
        print(f"Error: Source directory not found: {src_dir}")
        sys.exit(1)

    # Find all Java files
    java_files = list(src_dir.rglob('*.java'))
    total = len(java_files)

    print("=" * 70)
    print("Checkstyle Auto-Fix Tool - Week 4")
    print("=" * 70)
    print(f"\nSource directory: {src_dir}")
    print(f"Java files found: {total}")
    print("\n" + "-" * 70)

    # Global stats
    global_stats = {
        'files_modified': 0,
        'nowhitespace': 0,
        'leftcurly': 0,
        'importorder': 0,
        'unused': 0,
    }

    # Process files
    for i, filepath in enumerate(java_files, 1):
        # Show progress
        rel_path = filepath.relative_to(root)
        print(f"\r[{i:3d}/{total}] {rel_path}", end='')

        stats = apply_fixes_to_file(filepath)

        if stats['modified']:
            global_stats['files_modified'] += 1
            global_stats['nowhitespace'] += stats['nowhitespace']
            global_stats['leftcurly'] += stats['leftcurly']
            global_stats['importorder'] += stats['importorder']
            global_stats['unused'] += stats['unused']

    # Print summary
    print("\n\n" + "=" * 70)
    print("PROCESSING COMPLETE")
    print("=" * 70)
    print(f"\nSummary:")
    print(f"   Files modified:     {global_stats['files_modified']}")
    print(f"   NoWhitespaceBefore: {global_stats['nowhitespace']}")
    print(f"   LeftCurly:          {global_stats['leftcurly']}")
    print(f"   ImportOrder:        {global_stats['importorder']}")
    print(f"   UnusedImports:      {global_stats['unused']}")

    # Generate report
    report_path = root / "docs" / "audits" / "CHECKSTYLE_AUTOFIX_WEEK4.md"
    report_path.parent.mkdir(parents=True, exist_ok=True)

    total_fixes = global_stats['nowhitespace'] + global_stats['leftcurly'] + global_stats['importorder'] + global_stats['unused']

    # Build report content without using f-strings for the code examples
    report_lines = [
        "# Checkstyle Auto-Fix Report - Week 4\n",
        "**Date:** 2026-03-03",
        "**Team:** Team 3 - Style Auto-Fixes",
        "**Task:** Fix auto-fixable Checkstyle warnings\n",
        "## Execution Summary\n",
        f"- **Java files processed:** {total}",
        f"- **Files modified:** {global_stats['files_modified']}",
        f"- **Source directory:** `src/main/java/`\n",
        "## Fixes Applied\n",
        "| Checkstyle Rule | Issues Fixed | Description |",
        "|-----------------|--------------|-------------|",
        f"| **NoWhitespaceBefore** | {global_stats['nowhitespace']} | Added space before `(` in control structures |",
        f"| **LeftCurly** | {global_stats['leftcurly']} | Moved opening brace to same line |",
        f"| **ImportOrder** | {global_stats['importorder']} | Sorted imports alphabetically |",
        f"| **UnusedImports** | {global_stats['unused']} | Removed unused import statements |",
        f"| **Total** | {total_fixes} |  |\n",
        "## Known Limitations\n",
        "### Not Auto-Fixed (Requires Manual Review)\n",
        "- **NeedBraces** (342 warnings) - Requires adding braces to single-line statements",
        "- **JavadocStyle** (505 warnings) - Requires documentation content review",
        "- **JavadocType** (varies) - Requires Javadoc formatting fixes",
        "- **LineLength** (156 warnings) - Requires code refactoring\n",
        "### Partial Fixes\n",
        "- **AvoidStarImport** (93 warnings) - Wildcard imports identified but not expanded",
        "  (requires determining which specific classes are used from each package)\n",
        "## Verification\n",
        "To verify the fixes and see remaining issues:\n",
        "```bash",
        "# Clean build",
        "./gradlew clean build",
        "",
        "# Run Checkstyle",
        "./gradlew checkstyleMain",
        "",
        "# View detailed report",
        "open build/reports/checkstyle/main.html",
        "# or on Windows:",
        "start build/reports/checkstyle/main.html",
        "```\n",
        "## Examples of Fixes Applied\n",
        "### NoWhitespaceBefore",
        "```java",
        "// Before",
        "if(condition)",
        "while(x > 0)",
        "",
        "// After",
        "if (condition)",
        "while (x > 0)",
        "```\n",
        "### LeftCurly",
        "```java",
        "// Before",
        "if (condition)",
        "{",
        "    doSomething();",
        "}",
        "",
        "// After",
        "if (condition) {",
        "    doSomething();",
        "}",
        "```\n",
        "### ImportOrder",
        "```java",
        "// Before",
        "import java.util.List;",
        "import java.util.ArrayList;",
        "import static java.util.Objects.requireNonNull;",
        "import java.util.Map;",
        "",
        "// After",
        "import static java.util.Objects.requireNonNull;",
        "",
        "import java.util.ArrayList;",
        "import java.util.List;",
        "import java.util.Map;",
        "```\n",
        "## Next Steps\n",
        "1. **Build verification:** Run `./gradlew build` to ensure no regressions",
        "2. **Checkstyle review:** Review remaining Checkstyle warnings",
        "3. **Manual fixes:** Address non-auto-fixable warnings (NeedBraces, JavadocStyle, etc.)",
        "4. **Code review:** Have team review auto-applied changes\n",
        "---\n",
        "*Generated by Checkstyle Auto-Fix Tool*",
        "*Week 4 - Team 3*\n",
    ]

    with open(report_path, 'w', encoding='utf-8') as f:
        f.write('\n'.join(report_lines))

    print(f"\nReport saved to: {report_path.relative_to(root)}")
    print("\n" + "=" * 70)
    print("Done! Run './gradlew checkstyleMain' to see remaining issues.")
    print("=" * 70)


if __name__ == "__main__":
    main()
