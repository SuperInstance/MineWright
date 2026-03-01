# GitHub Actions Workflow Setup

The workflow files in this directory require `workflow` scope permissions to push directly.
This is a GitHub security feature to prevent unauthorized workflow modifications.

## Option 1: Add via GitHub Web UI

1. Go to your repository on GitHub
2. Navigate to `.github/workflows/` directory
3. Click "Add file" → "Create new file"
4. Create each workflow file:
   - `ci.yml` - Main CI pipeline
   - `release.yml` - Release automation
   - `codeql.yml` - Security analysis
   - `dependency-review.yml` - Dependency scanning

## Option 2: Use Personal Access Token with Workflow Scope

1. Go to GitHub Settings → Developer settings → Personal access tokens
2. Create a new token with `workflow` scope checked
3. Use the token for git operations:
   ```bash
   git remote set-url origin https://<TOKEN>@github.com/SuperInstance/MineWright.git
   git push origin clean-main
   ```

## Workflow Files Summary

| File | Purpose |
|------|---------|
| `ci.yml` | Main CI pipeline - build, test, coverage, static analysis |
| `release.yml` | Release automation - validation, build, GitHub release |
| `codeql.yml` | Weekly security analysis |
| `dependency-review.yml` | Dependency and license scanning |

## Current Status

- All code and documentation pushed to `clean-main` branch
- Workflow files ready locally, pending manual addition to GitHub
- See `.github/workflows/` directory for workflow file contents

## Workflow File Locations

The workflow files are saved in the repository but excluded from pushes.
To add them manually, copy from:
- `.github/workflows/ci.yml`
- `.github/workflows/release.yml`
- `.github/workflows/codeql.yml`
- `.github/workflows/dependency-review.yml`
