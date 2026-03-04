# Build Guide

This document provides comprehensive build instructions for the Steve AI mod.

## Build Commands

### Standard Build

```bash
# Build the mod (development JAR)
./gradlew build

# Run client for testing
./gradlew runClient

# Run server for testing
./gradlew runServer

# Run tests
./gradlew test

# Run a single test
./gradlew test --tests MyTest

# Run a specific test method
./gradlew test --tests "ActionExecutorTest.testExecuteAction"
```

### Distribution Build

```bash
# Build distribution JAR with dependencies (for distribution)
./gradlew shadowJar

# Obfuscate distribution JAR
./gradlew shadowJar reobfShadowJar
```

### Code Quality

```bash
# Generate coverage report
./gradlew jacocoCoverageReport

# Check code style
./gradlew checkstyle

# Check for bugs
./gradlew spotbugs

# Clean build artifacts
./gradlew clean
```

## Build Output

| Artifact | Location | Description |
|----------|----------|-------------|
| **Development JAR** | `build/libs/minewright-1.0.0.jar` | For development/testing |
| **Distribution JAR** | `build/libs/minewright-1.0.0-all.jar` | For distribution (includes dependencies) |

## Configuration

Configuration file: `config/minewright-common.toml`

### LLM Provider Configuration

```toml
[llm]
provider = "groq"  # Options: openai, groq, gemini, zai

[openai]
apiKey = "${OPENAI_API_KEY}"  # Use environment variable
model = "gpt-4"

[groq]
apiKey = "${GROQ_API_KEY}"
model = "llama3-70b-8192"

[gemini]
apiKey = "${GEMINI_API_KEY}"
model = "gemini-pro"

[zai]
apiKey = "${ZAI_API_KEY}"
apiEndpoint = "https://api.z.ai/api/paas/v4/chat/completions"
foremanModel = "glm-5"
workerSimpleModel = "glm-4.7-air"
workerComplexModel = "glm-5"
```

### Environment Variables

Set API keys via environment variables for security:

```bash
# Linux/Mac
export OPENAI_API_KEY="sk-..."
export GROQ_API_KEY="gsk_..."
export GEMINI_API_KEY="..."
export ZAI_API_KEY="..."

# Windows (PowerShell)
$env:OPENAI_API_KEY="sk-..."
$env:GROQ_API_KEY="gsk_..."

# Windows (Command Prompt)
set OPENAI_API_KEY=sk-...
set GROQ_API_KEY=gsk_...
```

## Testing

### Running Tests

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests ActionExecutorTest

# Run specific test method
./gradlew test --tests "ActionExecutorTest.testExecuteAction"

# Run tests for a package
./gradlew test --tests "com.minewright.action.*"
```

### Coverage Reports

```bash
# Generate coverage report
./gradlew jacocoCoverageReport

# View report
open build/reports/jacoco/test/html/index.html
```

**Target Coverage:** 60% (configured in JaCoCo)

## Dependencies

| Dependency | Version | Purpose |
|------------|---------|---------|
| **Minecraft Forge** | 1.20.1 | Mod framework |
| **GraalVM JS** | 24.1.2 | Code execution (relocated) |
| **Resilience4j** | 2.3.0 | Circuit breaker, retry, rate limiting |
| **Caffeine** | 3.1.8 | High-performance caching |
| **Apache Commons Codec** | 1.17.1 | Utilities |
| **JUnit 5** | 11.4 | Testing |
| **Mockito** | 5.15.2 | Mocking |

## Troubleshooting

### Build Fails with Out of Memory

```bash
# Increase heap size
./gradlew build -Xmx4g
```

### Tests Fail with ClassNotFoundException

Ensure all dependencies are resolved:
```bash
./gradlew clean build --refresh-dependencies
```

### SpotBugs/Checkstyle Warnings

These tools are configured but warnings are currently ignored. To fix:
1. Run `./gradlew spotbugs` or `./gradlew checkstyle`
2. Review reports in `build/reports/`
3. Address warnings as needed

## Performance Tips

1. **Enable Parallel Builds**: Add `org.gradle.parallel=true` to `gradle.properties`
2. **Configure Heap**: Set `org.gradle.jvmargs=-Xmx4g` in `gradle.properties`
3. **Enable Build Cache**: Add `org.gradle.caching=true` to `gradle.properties`
