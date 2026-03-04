#!/usr/bin/env python3
"""
Fix wildcard imports in Java source files.
Replaces 'import java.util.*;' with specific imports based on usage.
"""

import re
import sys
from pathlib import Path
from typing import Set, Dict, List

# Common java.util classes that might be used
JAVA_UTIL_CLASSES = {
    'ArrayList', 'LinkedList', 'HashSet', 'TreeSet', 'HashMap', 'TreeMap',
    'LinkedHashMap', 'LinkedHashSet', 'ConcurrentHashMap', 'Collection',
    'List', 'Set', 'Map', 'Queue', 'Deque', 'SortedSet', 'SortedMap',
    'Iterator', 'ListIterator', 'Collections', 'Arrays', 'Objects',
    'Comparator', 'Comparable', 'Optional', 'OptionalInt', 'OptionalLong',
    'OptionalDouble', 'UUID', 'Random', 'Scanner', 'Locale', 'TimeZone',
    'Date', 'Instant', 'Duration', 'Period', 'Consumer', 'Function',
    'Predicate', 'Supplier', 'BiConsumer', 'BiFunction', 'BinaryOperator',
    'UnaryOperator', 'Stream', 'IntStream', 'LongStream', 'DoubleStream',
    'Collectors'
}

def extract_used_types(content: str, class_pattern: str) -> Set[str]:
    """Extract used types from Java content."""
    # Find all uses of classes from java.util
    # Look for patterns like: new ClassName, ClassName., <ClassName>, implements ClassName
    pattern = r'\b(' + '|'.join(JAVA_UTIL_CLASSES) + r')\b'
    matches = re.findall(pattern, content)
    return set(matches)

def determine_needed_imports(content: str) -> Set[str]:
    """Determine which specific java.util imports are needed."""
    # Remove the wildcard import line first to avoid false positives
    lines = content.split('\n')
    filtered_lines = [line for line in lines if not line.strip().startswith('import java.util.*;')]

    # Find references to Collection, List, Map, Set (interfaces commonly used)
    needed = set()

    # Check for generic type usage
    if re.search(r'<\s*List\s*>', '\n'.join(filtered_lines)):
        needed.add('java.util.List')
    if re.search(r'<\s*Map\s*,', '\n'.join(filtered_lines)):
        needed.add('java.util.Map')
    if re.search(r'<\s*Set\s*>', '\n'.join(filtered_lines)):
        needed.add('java.util.Set')
    if re.search(r'<\s*Collection\s*>', '\n'.join(filtered_lines)):
        needed.add('java.util.Collection')

    # Check for specific class usage
    if re.search(r'\bArrayList\b', '\n'.join(filtered_lines)):
        needed.add('java.util.ArrayList')
    if re.search(r'\bHashMap\b', '\n'.join(filtered_lines)):
        needed.add('java.util.HashMap')
    if re.search(r'\bHashSet\b', '\n'.join(filtered_lines)):
        needed.add('java.util.HashSet')
    if re.search(r'\bLinkedList\b', '\n'.join(filtered_lines)):
        needed.add('java.util.LinkedList')
    if re.search(r'\bLinkedHashMap\b', '\n'.join(filtered_lines)):
        needed.add('java.util.LinkedHashMap')
    if re.search(r'\bLinkedHashSet\b', '\n'.join(filtered_lines)):
        needed.add('java.util.LinkedHashSet')
    if re.search(r'\bTreeMap\b', '\n'.join(filtered_lines)):
        needed.add('java.util.TreeMap')
    if re.search(r'\bTreeSet\b', '\n'.join(filtered_lines)):
        needed.add('java.util.TreeSet')
    if re.search(r'\bConcurrentHashMap\b', '\n'.join(filtered_lines)):
        needed.add('java.util.ConcurrentHashMap')  # Actually in java.util.concurrent
    if re.search(r'\bCollections\b', '\n'.join(filtered_lines)):
        needed.add('java.util.Collections')
    if re.search(r'\bArrays\b', '\n'.join(filtered_lines)):
        needed.add('java.util.Arrays')
    if re.search(r'\bObjects\b', '\n'.join(filtered_lines)):
        needed.add('java.util.Objects')
    if re.search(r'\bComparator\b', '\n'.join(filtered_lines)):
        needed.add('java.util.Comparator')
    if re.search(r'\bOptional\b', '\n'.join(filtered_lines)):
        needed.add('java.util.Optional')
    if re.search(r'\bUUID\b', '\n'.join(filtered_lines)):
        needed.add('java.util.UUID')
    if re.search(r'\bRandom\b', '\n'.join(filtered_lines)):
        needed.add('java.util.Random')
    if re.search(r'\bScanner\b', '\n'.join(filtered_lines)):
        needed.add('java.util.Scanner')
    if re.search(r'\bLocale\b', '\n'.join(filtered_lines)):
        needed.add('java.util.Locale')
    if re.search(r'\bDate\b', '\n'.join(filtered_lines)):
        needed.add('java.util.Date')
    if re.search(r'\bInstant\b', '\n'.join(filtered_lines)):
        needed.add('java.time.Instant')  # Actually java.time
    if re.search(r'\bDuration\b', '\n'.join(filtered_lines)):
        needed.add('java.time.Duration')  # Actually java.time
    if re.search(r'\bConsumer\b', '\n'.join(filtered_lines)):
        needed.add('java.util.function.Consumer')  # Actually java.util.function
    if re.search(r'\bFunction\b', '\n'.join(filtered_lines)):
        needed.add('java.util.function.Function')  # Actually java.util.function
    if re.search(r'\bPredicate\b', '\n'.join(filtered_lines)):
        needed.add('java.util.function.Predicate')  # Actually java.util.function
    if re.search(r'\bSupplier\b', '\n'.join(filtered_lines)):
        needed.add('java.util.function.Supplier')  # Actually java.util.function
    if re.search(r'\bStream\b', '\n'.join(filtered_lines)):
        needed.add('java.util.stream.Stream')  # Actually java.util.stream
    if re.search(r'\bCollectors\b', '\n'.join(filtered_lines)):
        needed.add('java.util.stream.Collectors')  # Actually java.util.stream

    # Check for existing imports to avoid duplicates
    existing_imports = set()
    for line in lines:
        if line.strip().startswith('import java.util.') or line.strip().startswith('import java.time.') or line.strip().startswith('import java.util.concurrent.') or line.strip().startswith('import java.util.function.') or line.strip().startswith('import java.util.stream.'):
            existing_imports.add(line.strip().replace('import ', '').replace(';', '').strip())

    # Remove already imported
    needed = needed - existing_imports

    return needed

def fix_wildcard_imports(file_path: Path) -> bool:
    """Fix wildcard imports in a single file."""
    try:
        content = file_path.read_text(encoding='utf-8')
    except Exception as e:
        print(f"Error reading {file_path}: {e}", file=sys.stderr)
        return False

    # Check if file has wildcard import
    if 'import java.util.*;' not in content:
        return False

    # Determine needed imports
    needed = determine_needed_imports(content)

    # Find and replace the wildcard import
    lines = content.split('\n')
    new_lines = []
    wildcard_found = False

    for line in lines:
        if 'import java.util.*;' in line:
            if not wildcard_found:  # Only replace first occurrence
                wildcard_found = True
                # Add sorted specific imports
                for imp in sorted(needed):
                    new_lines.append(f'import {imp};')
            # Skip the wildcard import line
        else:
            new_lines.append(line)

    if wildcard_found:
        try:
            file_path.write_text('\n'.join(new_lines) + '\n', encoding='utf-8')
            print(f"Fixed: {file_path} ({len(needed)} imports added)")
            return True
        except Exception as e:
            print(f"Error writing {file_path}: {e}", file=sys.stderr)
            return False

    return False

def main():
    """Main entry point."""
    src_dir = Path('src/main/java/com/minewright')

    if not src_dir.exists():
        print(f"Source directory {src_dir} not found", file=sys.stderr)
        sys.exit(1)

    # Find all Java files
    java_files = list(src_dir.rglob('*.java'))

    fixed_count = 0
    for java_file in java_files:
        if fix_wildcard_imports(java_file):
            fixed_count += 1

    print(f"\nFixed {fixed_count} files")

if __name__ == '__main__':
    main()
