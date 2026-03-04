#!/usr/bin/env python3
"""
Fix NoWhitespaceBefore violations for method chaining.
Moves dots from the beginning of lines to the end of previous lines.

Example:
    .replace("\\", "\\\\")        .replace("\\", "\\\\")
    .replace("\"", "\\\"")    ->  .replace("\"", "\\\"")
"""

import re
from pathlib import Path
from typing import Tuple


def fix_method_chaining_dots(content: str) -> Tuple[str, int]:
    """
    Fix method chaining where dots are at the start of lines.
    Moves them to the end of the previous line.
    """
    lines = content.split('\n')
    result = []
    count = 0

    i = 0
    while i < len(lines):
        current = lines[i]
        stripped = current.strip()

        # Check if this line starts with a dot (method chaining)
        if stripped.startswith('.') and not stripped.startswith('..'):
            # Check if previous line ends with a method call or parenthesis
            if i > 0:
                prev_line = result[-1] if result else lines[i-1]
                prev_stripped = prev_line.rstrip()

                # Check if previous line ends with ), ], }, or a method name
                # We want to move the dot to the end of the previous line
                if (prev_stripped.endswith(')') or
                    prev_stripped.endswith(']') or
                    prev_stripped.endswith('}') or
                    # Allow dot after identifiers (method chaining)
                    re.search(r'\w$', prev_stripped)):

                    # Remove leading whitespace and dot from current line
                    method_call = stripped[1:].strip()  # Remove dot

                    # Add dot to end of previous line
                    if result:
                        result[-1] = prev_stripped + '.'

                        # Update current line to be indented method call
                        indent = len(current) - len(current.lstrip())
                        new_line = ' ' * indent + method_call
                        result.append(new_line)
                    else:
                        result.append(current)
                    count += 1
                    i += 1
                    continue

        result.append(current)
        i += 1

    return '\n'.join(result), count


def fix_method_chaining_simple(content: str) -> Tuple[str, int]:
    """
    Simpler approach: fix method chaining pattern by moving dots.
    Pattern: Lines that start with whitespace, then a dot, then method call.
    """
    lines = content.split('\n')
    result = []
    count = 0
    i = 0

    while i < len(lines):
        line = lines[i]

        # Pattern: line starts with whitespace followed by a dot (method chaining)
        if re.match(r'^\s+\.\w', line):
            # This is a method chaining line with dot at start
            # We need to move the dot to the previous line

            if i > 0 and result:
                prev_line = result[-1].rstrip()

                # Check if we can append the dot to previous line
                # Previous line should end with: ), ], }, identifier, or string
                if (prev_line.endswith(')') or
                    prev_line.endswith(']') or
                    prev_line.endswith('}') or
                    prev_line.endswith("'") or
                    prev_line.endswith('"') or
                    re.search(r'[a-zA-Z0-9_]$', prev_line)):

                    # Append dot to previous line
                    result[-1] = prev_line + '.'

                    # Add current line without the leading dot
                    indent_match = re.match(r'^(\s+)\.', line)
                    if indent_match:
                        indent = indent_match.group(1)
                        method_part = line[indent_match.end():]  # Get rest after dot
                        result.append(indent + method_part)
                        count += 1
                        i += 1
                        continue

        result.append(line)
        i += 1

    return '\n'.join(result), count


def apply_fixes_to_file(filepath: Path) -> dict:
    """Apply all fixes to a single file."""
    stats = {
        'method_chaining': 0,
        'modified': False
    }

    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            original = f.read()

        content = original

        # Apply method chaining fix (try simple version first)
        content, n = fix_method_chaining_simple(content)
        stats['method_chaining'] = n

        if content != original:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(content)
            stats['modified'] = True

    except Exception as e:
        print(f"\nWarning: {filepath}: {e}")

    return stats


def main():
    """Main entry point."""
    root = Path.cwd()
    src_dir = root / "src" / "main" / "java"

    if not src_dir.exists():
        print(f"Error: Source directory not found: {src_dir}")
        return

    java_files = list(src_dir.rglob('*.java'))
    total = len(java_files)

    print("=" * 70)
    print("Method Chaining Dot Fix - Week 4")
    print("=" * 70)
    print(f"\nSource directory: {src_dir}")
    print(f"Java files found: {total}")
    print("\n" + "-" * 70)

    global_stats = {
        'files_modified': 0,
        'method_chaining': 0,
    }

    for i, filepath in enumerate(java_files, 1):
        rel_path = filepath.relative_to(root)
        print(f"\r[{i:3d}/{total}] {rel_path}", end='')

        stats = apply_fixes_to_file(filepath)

        if stats['modified']:
            global_stats['files_modified'] += 1
            global_stats['method_chaining'] += stats['method_chaining']

    print("\n\n" + "=" * 70)
    print("PROCESSING COMPLETE")
    print("=" * 70)
    print(f"\nSummary:")
    print(f"   Files modified:     {global_stats['files_modified']}")
    print(f"   Method chaining fixes: {global_stats['method_chaining']}")
    print("\n" + "=" * 70)
    print("Done! Run './gradlew checkstyleMain' to verify fixes.")
    print("=" * 70)


if __name__ == "__main__":
    main()
