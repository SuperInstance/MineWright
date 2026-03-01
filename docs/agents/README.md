# Steve AI - Specialist Agent Onboarding Templates

**Version:** 1.0
**Last Updated:** 2026-02-28
**Project:** Steve AI - "Cursor for Minecraft"

---

## Overview

This directory contains onboarding templates for specialist AI agents that help develop the Steve AI Minecraft mod. Each template is designed to quickly bring a specialist agent up to speed on their role, responsibilities, and the project's architecture.

**Core Philosophy:** "One Abstraction Away" - LLMs plan and coordinate, while traditional game AI (behavior trees, FSMs, scripts) executes in real-time.

---

## Available Agent Templates

### 1. CODE_IMPLEMENTER.md
**For:** Agents that write Java code
**Focus:**
- Key code locations and package structure
- Coding patterns (tick-based actions, plugin architecture, async LLM calls)
- How to add actions, LLM clients, interceptors
- Testing requirements and mock patterns
- Code review checklist

**Use When:** Implementing features, fixing bugs, writing unit tests

---

### 2. RESEARCHER.md
**For:** Agents that do research and documentation
**Focus:**
- Key research documents and dissertation materials
- How to document findings with proper citations
- Citation standards ([Author, Year] format)
- Dissertation integration process
- Research workflow and quality standards

**Use When:** Exploring patterns, documenting findings, writing dissertation content

---

### 3. TESTER.md
**For:** Agents that write tests and find bugs
**Focus:**
- Current test coverage (13% - critical gap)
- Priority components needing tests
- Testing frameworks (JUnit 5, Mockito)
- Mock patterns for Minecraft classes
- Test patterns by component type
- Bug report template

**Use When:** Writing test suites, finding bugs, measuring coverage

---

### 4. SECURITY_REVIEWER.md
**For:** Agents that audit security
**Focus:**
- Known vulnerabilities (empty catch blocks, API keys, input validation)
- Security patterns (input validation, output encoding, least privilege)
- Security checklist for code reviews
- Vulnerability reporting template
- Security testing approaches

**Use When:** Auditing code, finding vulnerabilities, reviewing security

---

### 5. ORCHESTRATOR.md
**For:** Agents that coordinate other agents
**Focus:**
- How to spawn and manage specialist agents
- Task distribution patterns
- Research vs implementation balance (40/40/20 rule)
- Progress tracking and metrics
- Decision making framework
- Communication protocols

**Use When:** Coordinating multiple agents, balancing workload, tracking progress

---

## Quick Start Guide

### For New Agents

1. **Read ONBOARDING.md first** (if available)
2. **Read your specialist template** (CODE_IMPLEMENTER.md, RESEARCHER.md, etc.)
3. **Read CLAUDE.md** for project overview
4. **Start working** on assigned tasks

### For Orchestrators

1. **Read ORCHESTRATOR.md** first
2. **Review all specialist templates** to understand capabilities
3. **Read CLAUDE.md** for project context
4. **Start assigning tasks** to specialist agents

---

## Project Status Summary

**Overall Progress:** 60% complete, active development phase

**Code Implementation:**
- Core infrastructure: 100% complete
- Actions: 60% complete
- Multi-agent coordination: 40% complete
- Script layer generation: 10% complete

**Dissertation:**
- Overall: 85% complete (A-grade 92/100)
- Target: A+ (97+) requires 10-15 more hours
- Chapters 1, 4, 8: Complete
- Chapters 3, 6: In progress

**Test Coverage:**
- Current: 13% (CRITICAL GAP)
- Target: 70%
- Priority: ActionExecutor, AgentStateMachine, InterceptorChain

**Security:**
- Posture: Needs improvement
- Critical issues: Empty catch blocks, API key handling, input validation
- Priority: Fix before production

---

## Key Resources

### Documentation

| Document | Purpose | Location |
|----------|---------|----------|
| **CLAUDE.md** | Project overview, architecture, current state | `./CLAUDE.md` |
| **ONBOARDING.md** | General onboarding (if available) | `./ONBOARDING.md` |

### Research Documents

| Document | Purpose | Location |
|----------|---------|----------|
| **PRE_LLM_GAME_AUTOMATION.md** | Historical context | `docs/research/` |
| **NPC_SCRIPTING_EVOLUTION.md** | Philosophical foundation | `docs/research/` |
| **SCRIPT_GENERATION_SYSTEM.md** | LLM→Script pipeline | `docs/research/` |
| **COMPREHENSIVE_BIBLIOGRAPHY.md** | Complete reference list | `docs/research/` |

### Code Locations

| Component | Location | Purpose |
|-----------|----------|---------|
| **ActionExecutor** | `src/main/java/com/minewright/action/` | Main execution loop |
| **AgentStateMachine** | `src/main/java/com/minewright/execution/` | State management |
| **ActionRegistry** | `src/main/java/com/minewright/plugin/` | Plugin system |
| **TaskPlanner** | `src/main/java/com/minewright/llm/` | LLM integration |

---

## Common Workflows

### Adding a New Feature

1. **Researcher** documents the pattern
2. **Code Implementer** implements the feature
3. **Tester** writes tests
4. **Security Reviewer** audits for vulnerabilities
5. **Orchestrator** coordinates and validates

### Fixing a Bug

1. **Tester** files bug report
2. **Security Reviewer** assesses if security-related
3. **Code Implementer** implements fix
4. **Tester** validates fix
5. **Orchestrator** closes issue

### Writing Dissertation Content

1. **Researcher** gathers sources and documents findings
2. **Researcher** integrates into dissertation chapter
3. **Orchestrator** reviews and validates integration
4. **Researcher** updates bibliography

---

## Team Balance Rule

**40% Research** - Exploring patterns, documenting findings
**40% Implementation** - Writing code, fixing bugs
**20% Testing and Documentation** - Writing tests, updating docs

**Golden Rule:** Always keep at least one agent on actual code.

---

## Agent Communication Format

### Task Assignment
```
[TO: Agent Type]
Task: [Description]
Priority: [CRITICAL/HIGH/MEDIUM/LOW]
Context: [Why this matters]
Input: [What they need]
Output: [Expected deliverable]
Deadline: [When due]
```

### Status Update
```
[FROM: Agent Type]
Task: [Task name]
Status: [IN_PROGRESS/COMPLETE/BLOCKED]
Progress: [What's done]
Blockers: [What's blocking]
ETA: [When done]
```

---

## Quality Standards

### Code Quality
- Follows existing patterns
- JavaDoc on public APIs
- Unit tests (>70% coverage for new code)
- No security vulnerabilities
- Passes all checks (style, bugs, tests)

### Research Quality
- Proper citations ([Author, Year])
- Implementation details included
- Integrated into dissertation
- Sources in bibliography
- Critical analysis, not just summary

### Test Quality
- Success cases covered
- Failure cases covered
- Edge cases covered
- Mock patterns appropriate
- Bug reports filed if issues found

---

## Progress Tracking

Track progress weekly:
- Tasks completed
- Test coverage percentage
- Dissertation progress
- Research/implementation balance
- Security vulnerabilities resolved

Update:
- Status dashboard
- Weekly progress report
- Decision log
- Blocker list

---

## Escalation Paths

**Escalate to Orchestrator:**
- Architecture decisions needed
- Security critical issues
- Timeline at risk
- Resource conflicts

**Escalate to Project Lead:**
- Strategic direction
- Resource allocation
- Major architectural changes
- Dissertion deadline conflicts

---

## Version History

**v1.0 (2026-02-28)**
- Initial agent templates created
- All 5 specialist roles documented
- Quick start guide added
- Communication protocols defined

---

## Contact

**Project Lead:** [Contact info]
**Orchestrator:** Use ORCHESTRATOR.md template
**Questions:** Check specialist templates first, then escalate

---

**Remember:** These templates are living documents. Update them as the project evolves and new patterns emerge. The goal is continuous improvement in how we work together.
