# Status Badges for MineWright

## Quick Reference

Copy and paste these badges into your README.md or other documentation.

**Note:** Replace `YOUR_USERNAME` and `YOUR_REPO` with your actual GitHub username and repository name.

## CI/CD Status Badges

### Pipeline Status

```markdown
![CI](https://github.com/YOUR_USERNAME/YOUR_REPO/workflows/CI/badge.svg)
![Release](https://github.com/YOUR_USERNAME/YOUR_REPO/workflows/Release/badge.svg)
```

### Code Quality

```markdown
![CodeQL](https://github.com/YOUR_USERNAME/YOUR_REPO/workflows/CodeQL%20Analysis/badge.svg)
![Dependency Review](https://github.com/YOUR_USERNAME/YOUR_REPO/workflows/Dependency%20Review/badge.svg)
```

## Coverage Badge

```markdown
[![codecov](https://codecov.io/gh/YOUR_USERNAME/YOUR_REPO/branch/main/graph/badge.svg)](https://codecov.io/gh/YOUR_USERNAME/YOUR_REPO)
```

## Dependency Badges

### Dependabot

```markdown
[![Dependabot](https://badgen.net/github/dependabot/YOUR_USERNAME/YOUR_REPO)](https://github.com/YOUR_USERNAME/YOUR_REPO/issues?q=is%3Apr+label%3Adependencies)
```

### Dependencies Count

```markdown
[![Dependencies](https://badgen.net/github/dependencies/YOUR_USERNAME/YOUR_REPO)](https://github.com/YOUR_USERNAME/YOUR_REPO/network/dependencies)
```

## Version Badges

### Maven Central (when published)

```markdown
[![Maven Central](https://img.shields.io/maven-central/v/com.minewright/minewright)](https://central.sonatype.com/artifact/com.minewright/minewright)
```

### GitHub Release

```markdown
[![GitHub release](https://img.shields.io/github/v/release/YOUR_USERNAME/YOUR_REPO)](https://github.com/YOUR_USERNAME/YOUR_REPO/releases)
```

## Project Health

### License

```markdown
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
```

### Activity

```markdown
[![GitHub commits](https://img.shields.io/github/commits-since/YOUR_USERNAME/YOUR_REPO/latest.svg)](https://github.com/YOUR_USERNAME/YOUR_REPO/commits/main)
```

### Last Commit

```markdown
![GitHub last commit](https://img.shields.io/github/last-commit/YOUR_USERNAME/YOUR_REPO)
```

## Platform Badges

### Minecraft Version

```markdown
[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-brightgreen.svg)](https://minecraft.net)
```

### Forge Version

```markdown
[![Forge](https://img.shields.io/badge/Forge-47.2.0-orange.svg)](https://files.minecraftforge.net)
```

### Java Version

```markdown
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/projects/jdk/17/)
```

## Community Badges

### GitHub Stars

```markdown
[![GitHub stars](https://img.shields.io/github/stars/YOUR_USERNAME/YOUR_REPO?style=social)](https://github.com/YOUR_USERNAME/YOUR_REPO/stargazers)
```

### GitHub Forks

```markdown
[![GitHub forks](https://img.shields.io/github/forks/YOUR_USERNAME/YOUR_REPO?style=social)](https://github.com/YOUR_USERNAME/YOUR_REPO/network/members)
```

### GitHub Watchers

```markdown
[![GitHub watchers](https://img.shields.io/github/watchers/YOUR_USERNAME/YOUR_REPO?style=social)](https://github.com/YOUR_USERNAME/YOUR_REPO/watchers)
```

### Issues

```markdown
[![GitHub issues](https://img.shields.io/github/issues/YOUR_USERNAME/YOUR_REPO)](https://github.com/YOUR_USERNAME/YOUR_REPO/issues)
```

### PRs

```markdown
[![GitHub pull requests](https://img.shields.io/github/issues-pr/YOUR_USERNAME/YOUR_REPO)](https://github.com/YOUR_USERNAME/YOUR_REPO/pulls)
```

## Combined Badge Sets

### Full Project Header

```markdown
# MineWright

![CI](https://github.com/YOUR_USERNAME/YOUR_REPO/workflows/CI/badge.svg)
![Release](https://github.com/YOUR_USERNAME/YOUR_REPO/workflows/Release/badge.svg)
![CodeQL](https://github.com/YOUR_USERNAME/YOUR_REPO/workflows/CodeQL%20Analysis/badge.svg)
[![codecov](https://codecov.io/gh/YOUR_USERNAME/YOUR_REPO/branch/main/graph/badge.svg)](https://codecov.io/gh/YOUR_USERNAME/YOUR_REPO)

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-brightgreen.svg)](https://minecraft.net)
[![Forge](https://img.shields.io/badge/Forge-47.2.0-orange.svg)](https://files.minecraftforge.net)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/projects/jdk/17/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
```

### Minimal Set

```markdown
![CI](https://github.com/YOUR_USERNAME/YOUR_REPO/workflows/CI/badge.svg)
[![codecov](https://codecov.io/gh/YOUR_USERNAME/YOUR_REPO/branch/main/graph/badge.svg)](https://codecov.io/gh/YOUR_USERNAME/YOUR_REPO)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-brightgreen.svg)](https://minecraft.net)
```

## Badge Services

| Service | URL | Purpose |
|---------|-----|---------|
| **Shields.io** | https://shields.io | Custom badges |
| **Badgen.net** | https://badgen.net | Fast, minimal badges |
| **Codecov** | https://codecov.io | Code coverage |
| **Sonatype** | https://central.sonatype.com | Maven Central |

## Color Schemes

Use these colors for custom badges:

| Color | Hex | Usage |
|-------|-----|-------|
| Bright Green | `brightgreen` | Passing, stable |
| Green | `green` | Good, success |
| Yellow | `yellow` | Warning, unstable |
| Orange | `orange` | Java, moderate |
| Red | `red` | Failing, critical |
| Blue | `blue` | Information |
| Light Grey | `lightgrey` | Inactive, disabled |

## Example Usage

### README.md Header

```markdown
# MineWright AI

> "Cursor for Minecraft" - Autonomous AI agents that play with you

![CI](https://github.com/YOUR_USERNAME/YOUR_REPO/workflows/CI/badge.svg)
![Release](https://github.com/YOUR_USERNAME/YOUR_REPO/workflows/Release/badge.svg)
[![codecov](https://codecov.io/gh/YOUR_USERNAME/YOUR_REPO/branch/main/graph/badge.svg)](https://codecov.io/gh/YOUR_USERNAME/YOUR_REPO)

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-brightgreen.svg)](https://minecraft.net)
[![Forge](https://img.shields.io/badge/Forge-47.2.0-orange.svg)](https://files.minecraftforge.net)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/projects/jdk/17/)

[![GitHub stars](https://img.shields.io/github/stars/YOUR_USERNAME/YOUR_REPO?style=social)](https://github.com/YOUR_USERNAME/YOUR_REPO/stargazers)
[![GitHub issues](https://img.shields.io/github/issues/YOUR_USERNAME/YOUR_REPO)](https://github.com/YOUR_USERNAME/YOUR_REPO/issues)
```

## Updating Badges

After replacing `YOUR_USERNAME` and `YOUR_REPO`:

1. Test badge URLs in browser
2. Verify they render correctly
3. Add to README.md
4. Commit and push changes

## Notes

- Replace `main` with `clean-main` if using that branch
- Update version numbers (Minecraft, Forge, Java) as needed
- Add Maven Central badge once published
- Configure Codecov token for coverage badge
