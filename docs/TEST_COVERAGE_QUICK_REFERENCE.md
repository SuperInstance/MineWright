# Test Coverage Quick Reference

**Last Updated:** 2026-03-01

---

## Common Commands

### Generate Coverage Report

```bash
# Generate coverage report (recommended for development)
./gradlew jacocoCoverageReport

# Run tests and generate coverage
./gradlew test jacocoTestReport

# Verify coverage meets thresholds (fails if below 40%)
./gradlew jacocoTestCoverageVerification
```

### View Coverage Report

```bash
# Open HTML report (Mac/Linux)
open build/reports/jacoco/test/html/index.html

# Open HTML report (Windows)
start build/reports/jacoco/test/html/index.html

# Open HTML report (Windows PowerShell)
Invoke-Item build/reports/jacoco/test/html/index.html
```

### Run Specific Tests with Coverage

```bash
# Run specific test class
./gradlew test --tests InputSanitizerTest jacocoTestReport

# Run specific test method
./gradlew test --tests InputSanitizerTest.shouldDetectPromptInjection jacocoTestReport

# Run all tests in a package
./gradlew test --tests com.minewright.security.* jacocoTestReport
```

---

## Coverage Targets

| Package | Target | Priority |
|---------|--------|----------|
| `com.minewright.security` | 80% | CRITICAL |
| `com.minewright.action` | 50% | HIGH |
| `com.minewright.execution` | 50% | HIGH |
| `com.minewright.memory` | 30% | MEDIUM |
| `com.minewright.behavior` | 30% | MEDIUM |
| `com.minewright.htn` | 30% | MEDIUM |
| `com.minewright.pathfinding` | 30% | MEDIUM |
| Other packages | 30% | MEDIUM |

**Overall Target:** 40% minimum, 60% long-term goal

---

## Excluded from Coverage

- `**/config/**` - Configuration classes
- `**/entity/**` - Entity classes
- `**/client/**` - Client GUI code
- `**/generated/**` - Auto-generated code
- `**/*Test*` - Test classes
- `**/communication/message/**` - Message DTOs
- `**/event/dto/**` - Event DTOs
- `**/exception/**` - Exception classes

---

## Interpreting Results

### Color Coding

- 🟢 **Green** (>80%): Excellent coverage
- 🟡 **Yellow** (50-80%): Good coverage
- 🟠 **Orange** (30-50%): Needs improvement
- 🔴 **Red** (<30%): Critical gap

### Key Metrics

- **Instruction Coverage:** Most accurate for Java
- **Branch Coverage:** Important for logic quality
- **Line Coverage:** Easy to understand
- **Complexity Coverage:** Indicates test thoroughness

---

## Troubleshooting

### Coverage report not generated?

```bash
# Clean and rebuild
./gradlew clean build test jacocoTestReport
```

### Coverage verification fails?

```bash
# View detailed report
open build/reports/jacoco/test/html/index.html

# Check which packages are below threshold
./gradlew jacocoTestCoverageVerification --info
```

### Tests passing but 0% coverage?

```bash
# Verify JaCoCo agent is attached
./gradlew test --debug | grep jacoco

# Clean and rebuild
./gradlew clean build test
```

---

## Workflow

1. **Write tests** for new code
2. **Run tests** with `./gradlew test`
3. **Generate coverage** with `./gradlew jacocoTestReport`
4. **View report** in browser
5. **Improve coverage** for red/orange areas
6. **Verify thresholds** with `./gradlew jacocoTestCoverageVerification`

---

## Resources

- **Full Documentation:** [TEST_COVERAGE.md](./TEST_COVERAGE.md)
- **Configuration:** `config/jacoco/coverage.yml`
- **Build Configuration:** `build.gradle` (search for "jacoco")

---

## Quick Tips

✅ **DO:**
- Test public APIs thoroughly
- Test edge cases and error conditions
- Mock external dependencies
- Keep tests independent
- Use descriptive test names

❌ **DON'T:**
- Test getters/setters
- Test private methods directly
- Test generated code
- Duplicate test code
- Rely on test execution order

---

**Need Help?** See [TEST_COVERAGE.md](./TEST_COVERAGE.md) for detailed documentation.
