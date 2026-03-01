# Contributing to MineWright

Thank you for your interest in contributing to MineWright! This document provides guidelines and instructions for contributing.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [How to Contribute](#how-to-contribute)
- [Pull Request Process](#pull-request-process)
- [Coding Standards](#coding-standards)
- [Testing Guidelines](#testing-guidelines)
- [Documentation](#documentation)

## Code of Conduct

By participating in this project, you agree to maintain a respectful and inclusive environment. Be constructive, be kind, and help each other learn.

## Getting Started

### Prerequisites

- **Java 17** - Required for Minecraft 1.20.1 mods
- **Gradle 8.x** - Build system (wrapper included)
- **Minecraft 1.20.1** - Target Minecraft version
- **IDE** - IntelliJ IDEA recommended (Community Edition works)

### Fork and Clone

```bash
# Fork the repository on GitHub, then:
git clone https://github.com/YOUR_USERNAME/steve.git
cd steve
```

## Development Setup

### Initial Build

```bash
# Build the project
./gradlew build

# Generate IDE files (IntelliJ)
./gradlew genIntellijRuns

# Open in IntelliJ
# File > Open > select the steve directory
```

### Run Configuration

```bash
# Run Minecraft client for testing
./gradlew runClient

# Run Minecraft server for testing
./gradlew runServer

# Run tests
./gradlew test
```

### Configuration

Create a local configuration file at `config/minewright-common.toml`:

```toml
[ai]
provider = "groq"  # or "openai", "gemini", "zai"

[groq]
apiKey = "${GROQ_API_KEY}"  # Use environment variable
model = "llama3-70b-8192"

[openai]
apiKey = "${OPENAI_API_KEY}"
model = "gpt-4"
```

**Important:** Never commit API keys! Use environment variables.

## How to Contribute

### Reporting Bugs

1. Check existing issues to avoid duplicates
2. Use the bug report template
3. Include:
   - Minecraft version
   - MineWright version
   - Steps to reproduce
   - Expected vs actual behavior
   - Logs (use spoiler tags for long logs)

### Suggesting Features

1. Check existing issues and discussions
2. Use the feature request template
3. Describe the use case and expected behavior

### Submitting Code

1. Create a feature branch from `main`
2. Make your changes
3. Add/update tests
4. Update documentation
5. Submit a pull request

## Pull Request Process

### Before Submitting

- [ ] Code compiles without warnings
- [ ] All tests pass (`./gradlew test`)
- [ ] New code has tests
- [ ] Documentation updated if needed
- [ ] No API keys or secrets in code
- [ ] Follows coding standards

### PR Title Format

```
type(scope): brief description

Types: feat, fix, docs, test, refactor, chore, perf
Scopes: action, llm, memory, voice, gui, etc.
```

Examples:
- `feat(action): add farming automation action`
- `fix(llm): resolve timeout handling in async client`
- `docs(memory): update vector store documentation`

### Review Process

1. At least one approval required
2. CI must pass (build + tests)
3. No merge conflicts
4. Squash and merge to main

## Coding Standards

### Java Style

- **Indentation:** 4 spaces (no tabs)
- **Line limit:** 120 characters
- **Naming:**
  - `PascalCase` for classes
  - `camelCase` for methods/variables
  - `SCREAMING_SNAKE_CASE` for constants
- **Braces:** Opening brace on same line

### Example

```java
/**
 * Executes a mining action for a specific block type.
 */
public class MineAction extends BaseAction {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_RADIUS = 32;

    private final Block targetBlock;
    private final int quantity;
    private int blocksMined = 0;

    public MineAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
        this.targetBlock = task.getTargetBlock();
        this.quantity = task.getQuantity();
    }

    @Override
    protected void onTick() {
        if (blocksMined >= quantity) {
            complete();
            return;
        }
        // Mining logic...
    }
}
```

### Best Practices

- **No empty catch blocks** - Always log or handle exceptions
- **Use try-with-resources** for auto-closeable resources
- **Prefer immutability** where practical
- **Document public APIs** with JavaDoc
- **Keep methods focused** - Single responsibility
- **Avoid deep nesting** - Extract to helper methods

## Testing Guidelines

### Test Structure

```
src/test/java/com/minewright/
├── action/
│   ├── ActionExecutorTest.java
│   └── actions/
│       └── MineActionTest.java
├── llm/
│   └── ResponseParserTest.java
└── memory/
    └── CompanionMemoryTest.java
```

### Test Naming

```java
// Pattern: methodName_scenario_expectedResult
@Test
void planTasks_withValidCommand_returnsTaskList() {
    // ...
}

@Test
void execute_whenLlmUnavailable_fallsBackToCache() {
    // ...
}
```

### Test Categories

- **Unit tests:** Fast, isolated, no Minecraft dependencies
- **Integration tests:** Multiple components, may need mocks
- **Game tests:** Require Minecraft runtime (tag with `@GameTest`)

### Running Tests

```bash
# All tests
./gradlew test

# Specific test class
./gradlew test --tests ActionExecutorTest

# Specific test method
./gradlew test --tests "ActionExecutorTest.planTasks_withValidCommand_returnsTaskList"

# With coverage
./gradlew test jacocoTestReport
```

## Documentation

### Where to Document

| Type | Location |
|------|----------|
| API docs | JavaDoc comments |
| User guides | `docs/` directory |
| Architecture | `docs/ARCHITECTURE_OVERVIEW.md` |
| Research | `docs/research/` |
| Onboarding | `docs/ONBOARDING.md` |

### Documentation Style

- Use Markdown formatting
- Include code examples
- Keep lines under 100 characters for readability
- Use relative links for internal references

### Updating CLAUDE.md

When adding significant new features, update `CLAUDE.md`:

1. Add to "Key Components" if a new system
2. Update implementation status
3. Add to package structure if new package
4. Update build commands if new tasks

## Getting Help

- **Documentation:** Start with `docs/ONBOARDING.md`
- **Architecture:** See `docs/ARCHITECTURE_OVERVIEW.md`
- **Issues:** Open a GitHub issue
- **Discussions:** Use GitHub Discussions for questions

## Recognition

Contributors are recognized in:
- Git history (obviously)
- Release notes for significant contributions
- Project documentation for major features

---

Thank you for contributing to MineWright! Your help makes AI-powered Minecraft companions better for everyone.
