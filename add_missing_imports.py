#!/usr/bin/env python3
"""
Add missing imports to Java files based on compilation errors.
"""

import re
import subprocess
import sys
from pathlib import Path

# Common imports that might be missing
COMMON_IMPORTS = {
    'List': 'java.util.List',
    'Map': 'java.util.Map',
    'HashMap': 'java.util.HashMap',
    'ArrayList': 'java.util.ArrayList',
    'LinkedList': 'java.util.LinkedList',
    'ConcurrentHashMap': 'java.util.concurrent.ConcurrentHashMap',
    'ExecutorService': 'java.util.concurrent.ExecutorService',
    'Executors': 'java.util.concurrent.Executors',
    'CompletableFuture': 'java.util.concurrent.CompletableFuture',
    'TimeUnit': 'java.util.concurrent.TimeUnit',
    'InputStream': 'java.io.InputStream',
    'OutputStream': 'java.io.OutputStream',
    'IOException': 'java.io.IOException',
    'BufferedReader': 'java.io.BufferedReader',
    'InputStreamReader': 'java.io.InputStreamReader',
    'PrintWriter': 'java.io.PrintWriter',
    'ByteArrayOutputStream': 'java.io.ByteArrayOutputStream',
    'LineUnavailableException': 'javax.sound.sampled.LineUnavailableException',
    'AudioFormat': 'javax.sound.sampled.AudioFormat',
    'AudioSystem': 'javax.sound.sampled.AudioSystem',
    'AudioInputStream': 'javax.sound.sampled.AudioInputStream',
    'Clip': 'javax.sound.sampled.Clip',
    'DataLine': 'javax.sound.sampled.DataLine',
    'TargetDataLine': 'javax.sound.sampled.TargetDataLine',
    'SourceDataLine': 'javax.sound.sampled.SourceDataLine',
    'FloatControl': 'javax.sound.sampled.FloatControl',
    'LineEvent': 'javax.sound.sampled.LineEvent',
    'LineListener': 'javax.sound.sampled.LineListener',
}

def get_compile_errors():
    """Get compilation errors from gradle."""
    result = subprocess.run(
        ['./gradlew', 'compileJava'],
        capture_output=True,
        text=True,
        timeout=120
    )
    return result.stdout + result.stderr

def parse_errors(output):
    """Parse compilation errors to find missing imports."""
    missing = {}

    for line in output.split('\n'):
        if 'error: cannot find symbol' in line:
            # Extract file path and line number
            match = re.search(r'(src\\main\\java\\[^:]+\.java):(\d+):', line)
            if match:
                file_path = match.group(1).replace('\\', '/')
                line_num = int(match.group(2))

                if file_path not in missing:
                    missing[file_path] = {}

                # Get the symbol name from next lines
                continue

        if 'symbol:' in line and file_path:
            # Extract symbol name
            match = re.search(r'symbol:\s+class\s+(\w+)', line)
            if match:
                symbol = match.group(1)
                if symbol not in missing[file_path]:
                    missing[file_path][symbol] = set()
                missing[file_path][symbol].add(line_num)

    return missing

def add_import_to_file(file_path, import_statement):
    """Add an import statement to a file."""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()

        # Find the last import line
        lines = content.split('\n')
        last_import_line = -1

        for i, line in enumerate(lines):
            if line.strip().startswith('import '):
                last_import_line = i

        if last_import_line == -1:
            # No imports found, add after package
            for i, line in enumerate(lines):
                if line.strip().startswith('package '):
                    lines.insert(i + 1, '')
                    lines.insert(i + 2, import_statement)
                    break
        else:
            # Add after last import
            lines.insert(last_import_line + 1, import_statement)

        new_content = '\n'.join(lines)

        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(new_content)

        return True
    except Exception as e:
        print(f"Error adding import to {file_path}: {e}")
        return False

def main():
    """Main entry point."""
    print("Getting compilation errors...")
    output = get_compile_errors()

    print("Parsing errors...")
    missing = parse_errors(output)

    if not missing:
        print("No missing imports found!")
        return

    print(f"Found missing imports in {len(missing)} files")

    for file_path, symbols in missing.items():
        full_path = Path(file_path)
        if not full_path.exists():
            full_path = Path('src/main/java') / file_path

        if not full_path.exists():
            print(f"File not found: {file_path}")
            continue

        print(f"\nFixing {file_path}:")
        for symbol in symbols:
            if symbol in COMMON_IMPORTS:
                import_statement = f"import {COMMON_IMPORTS[symbol]};"
                print(f"  Adding: {import_statement}")
                add_import_to_file(str(full_path), import_statement)
            else:
                print(f"  Unknown symbol: {symbol}")

if __name__ == '__main__':
    main()
