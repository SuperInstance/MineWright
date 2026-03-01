# GitHub Actions CI/CD Pipeline Guide

## Overview

This document describes the GitHub Actions CI/CD pipeline setup for the MineWright project.

## Pipeline Files

| File | Purpose | Triggers |
|------|---------|----------|
| `.github/workflows/ci.yml` | Main CI pipeline | Push to main/clean-main, PRs |
| `.github/workflows/release.yml` | Release automation | Version tags, manual dispatch |
| `.github/workflows/codeql.yml` | CodeQL security analysis | Push, PRs, weekly schedule |
| `.github/workflows/dependency-review.yml` | Dependency and license checks | PRs, manual dispatch |

## Main CI Pipeline (ci.yml)

### Stages

#### 1. Build Stage
- Runs on: `ubuntu-latest`
- Java: 17 (Temurin distribution)
- Caches: Gradle dependencies
- Outputs: Build artifacts, version number

**Commands:**
```bash
./gradlew build --no-daemon --stacktrace
```

#### 2. Test Stage (Matrix)
- Runs on: `ubuntu-latest`
- Matrix testing: Java 17, 21
- Generates test reports (JUnit format)
- Uploads test results as artifacts
- Publishes test results to PRs

**Commands:**
```bash
./gradlew test --no-daemon --stacktrace --continue
./gradlew testReport --no-daemon
```

#### 3. Coverage Stage
- Runs on: `ubuntu-latest`
- Generates Jacoco coverage reports
- Uploads to Codecov (optional)
- Comments coverage on PRs

**Commands:**
```bash
./gradlew jacocoTestReport --no-daemon
```

#### 4. Static Analysis Stage
- Runs on: `ubuntu-latest`
- Checkstyle validation
- SpotBugs analysis
- Continues on errors (fails PR only on critical issues)

**Commands:**
```bash
./gradlew checkstyleMain checkstyleTest --no-daemon --continue
./gradlew spotbugsMain spotbugsTest --no-daemon --continue
```

#### 5. Security Scan Stage
- Dependency check analysis
- Trivy vulnerability scanner
- Uploads SARIF to GitHub Security

**Commands:**
```bash
./gradlew dependencyCheckAnalyze --no-daemon
trivy fs --sarif .
```

### Artifacts

| Artifact | Retention | Contents |
|----------|-----------|----------|
| `build-artifacts` | 7 days | Compiled JAR files |
| `test-results-java-*` | 14 days | Test reports and results |
| `coverage-report` | 14 days | Jacoco HTML/XML reports |
| `checkstyle-report` | 14 days | Checkstyle violations |
| `spotbugs-report` | 14 days | SpotBugs analysis |
| `dependency-check-report` | 30 days | Dependency vulnerabilities |

## Release Pipeline (release.yml)

### Triggered By
- Git tags matching `v*.*.*` (e.g., `v1.0.0`)
- Manual workflow dispatch

### Stages

#### 1. Validation Stage
- Runs full test suite
- Static analysis
- Coverage verification

#### 2. Build Release Stage
- Builds distribution JAR (shadowJar)
- Obfuscates JAR (reobfShadowJar)
- Generates checksums (SHA256, MD5)
- Creates release notes from git log

#### 3. Create Release Stage
- Creates GitHub Release
- Uploads JAR as release asset
- Publishes release notes

#### 4. Publish Stage (Optional)
- Publishes to GitHub Packages
- Requires tag push (not manual dispatch)

#### 5. Notification Stage
- Sends Slack notification (if configured)
- Posts release summary

### Release Artifacts

| Artifact | Description |
|----------|-------------|
| `minewright-*-all.jar` | Distribution JAR with dependencies |
| `SHA256SUMS` | SHA256 checksums |
| `MD5SUMS` | MD5 checksums |

## CodeQL Analysis (codeql.yml)

### Triggered By
- Push to main/clean-main/develop
- Pull requests to main/clean-main/develop
- Weekly schedule (Sundays at 2 AM UTC)

### Languages Analyzed
- Java
- JavaScript (GraalVM scripts)

### Queries
- Default security queries
- Extended security queries
- Quality queries

## Dependency Review (dependency-review.yml)

### Triggered By
- Pull requests
- Manual dispatch

### Stages

#### 1. Dependency Review
- Fails on moderate+ severity vulnerabilities
- Blocks GPL-3.0 and AGPL-3.0 licenses

#### 2. License Scan
- Generates license inventory
- Uploads license report

## Status Badges

Add these badges to your README.md:

```markdown
# CI Status
![CI](https://github.com/your-username/steve/workflows/CI/badge.svg)
![Release](https://github.com/your-username/steve/workflows/Release/badge.svg)
![CodeQL](https://github.com/your-username/steve/workflows/CodeQL%20Analysis/badge.svg)

# Coverage
[![codecov](https://codecov.io/gh/your-username/steve/branch/main/graph/badge.svg)](https://codecov.io/gh/your-username/steve)

# Dependencies
[![Dependabot](https://badgen.net/github/dependabot/your-username/steve)](https://github.com/your-username/steve/issues?q=is%3Apr+label%3Adependencies)
```

## Required Secrets

Configure these in GitHub repository settings (Settings → Secrets and variables → Actions):

| Secret | Purpose | Required |
|--------|---------|----------|
| `GITHUB_TOKEN` | GitHub API access | Automatic |
| `CODECOV_TOKEN` | Codecov upload | Optional |
| `SLACK_WEBHOOK_URL` | Slack notifications | Optional |
| `GRADLE_ENCRYPTION_KEY` | Gradle cache encryption | Optional |

## Dependabot Configuration

Automated dependency updates are configured in `.github/dependabot.yml`:

### Ecosystems
- Gradle (weekly on Sundays)
- GitHub Actions (weekly on Sundays)
- Docker (weekly on Sundays)

### Update Strategy
- Max 5 PRs per week (Gradle)
- Max 3 PRs per week (Actions)
- Ignore major version updates (stability)
- Groups: Minecraft, LLM, Testing dependencies

## Contributing

### Pull Request Checklist

When submitting a PR, ensure:
- [ ] All CI checks pass
- [ ] Tests pass on Java 17 and 21
- [ ] Coverage not decreased
- [ ] No new Checkstyle/SpotBugs violations
- [ ] No security vulnerabilities introduced
- [ ] Dependencies updated if needed

### PR Labels

Apply these labels to PRs:
- `bug` - Bug fixes
- `enhancement` - New features
- `dependencies` - Dependency updates
- `documentation` - Documentation changes
- `ci` - CI/CD changes
- `security` - Security fixes

### Code Review

- All PRs require approval from code owners
- Required approval: 1 reviewer
- Dismiss stale reviews on new commits
- Require status checks to pass before merge

## Troubleshooting

### Build Failures

**Gradle cache issues:**
```bash
# Clear Gradle cache in Actions
# Repository → Settings → Actions → Caches → Delete cache
```

**Test failures:**
```bash
# Run tests locally first
./gradlew test --no-daemon --stacktrace

# Run specific test
./gradlew test --tests ActionExecutorTest
```

### Coverage Issues

**Coverage report not generated:**
```bash
# Verify Jacoco plugin is applied
./gradlew jacocoTestReport --info

# Check coverage thresholds
./gradlew jacocoTestCoverageVerification
```

### Static Analysis Issues

**Checkstyle violations:**
```bash
# Run checkstyle locally
./gradlew checkstyleMain checkstyleTest

# Auto-fix some issues (if supported)
./gradlew checkstyleMain checkstyleTest -x test
```

**SpotBugs warnings:**
```bash
# Run SpotBugs locally
./gradlew spotbugsMain spotbugsTest

# View report
open build/reports/spotbugs/main.html
```

## Customization

### Adding New Jobs

Add to `.github/workflows/ci.yml`:

```yaml
new-job:
  name: Custom Job
  runs-on: ubuntu-latest
  steps:
    - uses: actions/checkout@v4
    - name: Run custom command
      run: ./gradlew customTask
```

### Adding Build Matrix

Test multiple configurations:

```yaml
strategy:
  matrix:
    java: ['17', '21']
    os: [ubuntu-latest, windows-latest, macos-latest]
```

### Conditional Execution

```yaml
- name: Optional Step
  if: github.event_name == 'pull_request'
  run: ./gradlew optionalTask
```

## Best Practices

1. **Cache Management**
   - Gradle dependencies are cached
   - Read-only cache on PRs
   - Cache cleared on main branch pushes

2. **Parallel Execution**
   - Test jobs run in parallel (Java 17, 21)
   - Analysis jobs run independently

3. **Artifact Retention**
   - Build artifacts: 7 days
   - Test/analysis artifacts: 14-30 days
   - Release artifacts: 90 days

4. **Security**
   - Dependabot for dependency updates
   - CodeQL for code analysis
   - Dependency review for license checks

5. **Notifications**
   - Slack integration for releases
   - PR comments for coverage and static analysis
   - GitHub Status API for build status

## Related Documentation

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Gradle Build Tool](https://docs.gradle.org/)
- [Codecov Documentation](https://docs.codecov.com/)
- [CodeQL Documentation](https://codeql.github.com/docs/)
- [Dependabot Documentation](https://docs.github.com/en/code-security/dependabot)

## Support

For CI/CD issues:
1. Check Actions logs in GitHub
2. Review this guide
3. Open an issue with the `ci` label
