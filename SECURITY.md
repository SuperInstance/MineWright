# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 1.x     | :white_check_mark: |
| < 1.0   | :x: (development)  |

## Reporting a Vulnerability

**DO NOT** open a public issue for security vulnerabilities.

### How to Report

1. **Email:** Send details to the project maintainers (check GitHub for contact info)
2. **GitHub:** Use the private vulnerability reporting feature at:
   `https://github.com/[owner]/steve/security/advisories/new`

### What to Include

- Description of the vulnerability
- Steps to reproduce
- Potential impact
- Suggested fix (if any)
- Your contact information for follow-up

### Response Timeline

- **Initial response:** Within 48 hours
- **Triage:** Within 1 week
- **Fix timeline:** Depends on severity
  - Critical: 1-3 days
  - High: 1 week
  - Medium: 2 weeks
  - Low: Next release

## Security Best Practices

### For Users

#### API Key Handling

**NEVER** commit API keys to the repository. Use environment variables:

```toml
# config/minewright-common.toml
[openai]
apiKey = "${OPENAI_API_KEY}"  # ✅ Correct

# NEVER do this:
# apiKey = "sk-abc123..."      # ❌ Wrong - will be committed!
```

Set environment variables:

```bash
# Linux/macOS
export OPENAI_API_KEY="sk-your-key-here"
export GROQ_API_KEY="gsk_your-key-here"

# Windows PowerShell
$env:OPENAI_API_KEY="sk-your-key-here"

# Windows Command Prompt
set OPENAI_API_KEY=sk-your-key-here
```

#### Configuration File Security

The `.gitignore` includes:

```gitignore
# Config with API keys
config/minewright-common.toml
run/config/minewright-common.toml
```

If you accidentally commit an API key:

1. **Immediately rotate the key** at your provider's dashboard
2. **Remove from git history** using `git filter-branch` or BFG Repo-Cleaner
3. **Force push** (if you have access)
4. **Report** so we can help cleanup

### For Developers

#### Input Validation

All user inputs must be validated:

```java
// Validate LLM prompts
public void sendPrompt(String prompt) {
    if (prompt == null || prompt.isBlank()) {
        throw new IllegalArgumentException("Prompt cannot be empty");
    }
    if (prompt.length() > MAX_PROMPT_LENGTH) {
        throw new IllegalArgumentException("Prompt too long");
    }
    // Sanitize any potential injection patterns
    String sanitized = sanitizePrompt(prompt);
    // ... proceed
}
```

#### Code Execution Sandbox

The GraalVM JS sandbox has strict restrictions:

```java
// Already implemented - do not relax these
context.getBindings("js").put("java", null);  // No Java access
context.getBindings("js").put("System", null); // No system access
```

#### Exception Handling

Never swallow exceptions silently:

```java
// ❌ Bad - silent failure
try {
    loadTemplate();
} catch (Exception e) {
    // Nothing happens - bad!
}

// ✅ Good - log and handle
try {
    loadTemplate();
} catch (IOException e) {
    LOGGER.error("Failed to load template: {}", name, e);
    return Optional.empty();
}
```

#### Dependency Security

- Keep dependencies updated
- Use Gradle dependency scanning
- Check for CVEs before upgrading

```bash
# Check for vulnerable dependencies
./gradlew dependencyCheckAnalyze
```

## Known Security Considerations

### LLM API Communication

- All API calls use HTTPS
- API keys are sent in headers (not URLs)
- Responses are validated before processing

### Multiplayer Considerations

- Agent commands require op permissions (configurable)
- Agent actions are logged for audit
- Rate limiting prevents DoS via agent commands

### Code Execution

User-provided scripts run in a sandbox with:
- No file system access
- No network access
- No Java interop
- Memory and time limits

## Security Audit History

| Date | Auditor | Scope | Status |
|------|---------|-------|--------|
| 2026-02-28 | Claude Code Audit | Full codebase | Issues identified & fixed |

## Contact

For security concerns:
- Open a private vulnerability advisory on GitHub
- Contact the maintainers directly

---

**Last Updated:** 2026-02-28
