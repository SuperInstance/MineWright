# GitHub Actions CI/CD Pipeline - Setup Complete

## Created Files

### Workflow Files (.github/workflows/)

1. **ci.yml** - Main CI pipeline
   - Build stage (Java 17, Gradle cache)
   - Test stage (Matrix: Java 17, 21)
   - Coverage stage (Jacoco + Codecov)
   - Static analysis (Checkstyle + SpotBugs)
   - Security scan (Dependency check + Trivy)
   - Build summary job

2. **release.yml** - Release automation
   - Validation stage (full test suite)
   - Build release stage (shadowJar + reobfShadowJar)
   - Create GitHub release
   - Publish to Maven (optional)
   - Notification (Slack)

3. **codeql.yml** - CodeQL security analysis
   - Java + JavaScript analysis
   - Weekly scheduled runs
   - Extended security queries

4. **dependency-review.yml** - Dependency & license checks
   - Dependency review (vulnerability blocking)
   - License scan
   - Outdated dependency detection

### Configuration Files

5. **dependabot.yml** - Automated dependency updates
   - Gradle dependencies (weekly)
   - GitHub Actions (weekly)
   - Docker dependencies (weekly)
   - Grouped updates (Minecraft, LLM, Testing)

6. **CODEOWNERS** - Code review ownership
   - Project maintainer: @casey
   - Core component ownership
   - Security and CI ownership

### Templates

7. **PULL_REQUEST_TEMPLATE.md** - PR template
   - Summary, changes, testing sections
   - Checklist for contributors

8. **ISSUE_TEMPLATE/bug_report.md** - Bug report template
   - Environment, logs, screenshots sections

9. **ISSUE_TEMPLATE/feature_request.md** - Feature request template
   - Problem statement, solution, alternatives

### Documentation

10. **CI_CD_GUIDE.md** - Comprehensive CI/CD documentation
    - Pipeline descriptions
    - Troubleshooting guide
    - Best practices
    - Customization instructions

11. **STATUS_BADGES.md** - Status badge reference
    - CI/CD badges
    - Coverage badges
    - Platform badges
    - Community badges

## Quick Start

### 1. Update Repository References

Replace `YOUR_USERNAME` with your GitHub username in:
- **STATUS_BADGES.md** - All badge URLs
- **CODEOWNERS** - Add actual maintainers

### 2. Configure Secrets (Optional)

Go to **Settings → Secrets and variables → Actions**:

| Secret | Purpose | Required |
|--------|---------|----------|
| `CODECOV_TOKEN` | Coverage upload | Optional |
| `SLACK_WEBHOOK_URL` | Slack notifications | Optional |
| `GRADLE_ENCRYPTION_KEY` | Gradle cache encryption | Optional |

### 3. Add Status Badges to README.md

Copy badges from **STATUS_BADGES.md** to your main README:

```markdown
# MineWright AI

![CI](https://github.com/YOUR_USERNAME/steve/workflows/CI/badge.svg)
![Release](https://github.com/YOUR_USERNAME/steve/workflows/Release/badge.svg)
[![codecov](https://codecov.io/gh/YOUR_USERNAME/steve/branch/main/graph/badge.svg)](https://codecov.io/gh/YOUR_USERNAME/steve)
```

### 4. Push to GitHub

```bash
git add .github/
git commit -m "Add GitHub Actions CI/CD pipeline"
git push origin clean-main
```

### 5. Verify Workflows

Go to **Actions** tab in GitHub:
- CI workflow should run automatically
- Check all jobs pass successfully
- Verify artifacts are uploaded

## Pipeline Triggers

### When CI Runs (ci.yml)
- Push to `main`, `clean-main`, `develop`
- Pull requests to `main`, `clean-main`, `develop`
- Manual workflow dispatch

### When Release Runs (release.yml)
- Git tags matching `v*.*.*` (e.g., `v1.0.0`)
- Manual workflow dispatch

### When CodeQL Runs (codeql.yml)
- Push to `main`, `clean-main`, `develop`
- Pull requests to `main`, `clean-main`, `develop`
- Weekly: Sundays at 2 AM UTC

### When Dependency Review Runs (dependency-review.yml)
- Pull requests
- Manual workflow dispatch

## Artifacts

| Artifact | Retention | Location |
|----------|-----------|----------|
| Build JARs | 7 days | Actions → Artifacts |
| Test results | 14 days | Actions → Artifacts |
| Coverage reports | 14 days | Actions → Artifacts |
| Static analysis | 14 days | Actions → Artifacts |
| Dependency checks | 30 days | Actions → Artifacts |
| Release JARs | 90 days | Releases page |

## Branch Protection Rules (Recommended)

Enable in **Settings → Branches**:

For `main` and `clean-main`:
- [ ] Require status checks to pass
- [ ] Require branches to be up to date
- [ ] Require pull request reviews (1)
- [ ] Require status checks: `build`, `test`, `coverage`, `static-analysis`

## Next Steps

1. **Test Locally**
   ```bash
   ./gradlew build test jacocoTestReport checkstyleMain spotbugsMain
   ```

2. **Configure Codecov** (Optional)
   - Sign up at https://codecov.io
   - Add repository
   - Copy token to GitHub Secrets

3. **Configure Slack** (Optional)
   - Create Incoming Webhook
   - Add `SLACK_WEBHOOK_URL` secret

4. **Review Security**
   - Check CodeQL results in Security tab
   - Review dependency vulnerabilities
   - Update Dependabot settings

## Troubleshooting

### CI Fails on First Run

**Gradle cache missing:**
- CI will build cache on first run
- Second run will be faster

**Tests fail locally but pass on CI:**
- Check Java version (CI uses 17 and 21)
- Verify Gradle wrapper version

### Release Workflow Fails

**Tag not pushed:**
```bash
git tag v1.0.0
git push origin v1.0.0
```

**Obfuscation fails:**
- Ensure Forge mappings are configured
- Check `build.gradle` for Forge setup

### Coverage Badge Not Showing

**Codecov not configured:**
- Sign up at https://codecov.io
- Get token from repository settings
- Add `CODECOV_TOKEN` to GitHub Secrets

## Support

For issues or questions:
1. Check **CI_CD_GUIDE.md** for detailed documentation
2. Review workflow logs in Actions tab
3. Open an issue with the `ci` label

## File Locations

```
.github/
├── workflows/
│   ├── ci.yml                  # Main CI pipeline
│   ├── release.yml             # Release automation
│   ├── codeql.yml              # CodeQL security
│   └── dependency-review.yml   # Dependency checks
├── ISSUE_TEMPLATE/
│   ├── bug_report.md           # Bug report template
│   └── feature_request.md      # Feature request template
├── CODEOWNERS                  # Code review ownership
├── dependabot.yml              # Automated updates
├── PULL_REQUEST_TEMPLATE.md    # PR template
├── CI_CD_GUIDE.md              # Full documentation
└── STATUS_BADGES.md            # Badge reference
```

---

**Status:** Ready to use
**Created:** 2026-03-01
**Version:** 1.0
