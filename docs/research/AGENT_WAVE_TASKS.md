# Agent Wave Task Queue

**Created:** 2026-03-02
**Purpose:** Maintain continuous agent activity with prioritized tasks
**Strategy:** Keep 3+ agents working at all times

---

## Wave 1 (IN PROGRESS)

| Agent ID | Task | Status |
|----------|------|--------|
| ae9c305 | Build configuration audit | Running |
| ac028f8 | Dissertation status review | Running |
| a983d7b | Bot research progress | Running |
| a6fa823 | Test coverage gaps | Running |

---

## Wave 2 (READY TO LAUNCH)

### Task 2.1: Chapter 3 Dissertation Integration
**Type:** Research & Documentation
**Priority:** HIGH (Dissertation A → A+)
**Prompt:**
```
Review dissertation Chapter 3 integration requirements from FUTURE_ROADMAP.md lines 374-386.

Tasks:
1. Read docs/research/EMOTIONAL_AI_FRAMEWORK.md (839 lines)
2. Read docs/research/CHAPTER_3_NEW_SECTIONS.md (2,322 lines)
3. Identify specific sections to merge into Chapter 3
4. List companion AI case studies to add (Shadow of Colossus, Last of Us Part II, Divinity OS2)
5. Create integration checklist with line numbers

Output: Detailed integration plan with specific actions.
```

### Task 2.2: Multi-Agent Coordination Gaps
**Type:** Code Analysis
**Priority:** MEDIUM
**Prompt:**
```
Analyze multi-agent coordination implementation status.

1. Find all files in src/main/java/com/minewright/coordination/
2. Read ContractNetProtocol.java if exists
3. Compare against FUTURE_ROADMAP.md lines 344-367 requirements
4. Identify what's implemented vs designed
5. List specific missing components

Output: Gap analysis with specific files and methods needed.
```

### Task 2.3: Evaluation Framework Requirements
**Type:** Design & Planning
**Priority:** MEDIUM
**Prompt:**
```
Review evaluation framework requirements from FUTURE_ROADMAP.md lines 527-543.

Tasks:
1. Check if BenchmarkSuite.java exists in src/test/java/
2. Check if MetricsCollector.java exists in src/main/java/
3. Read existing evaluation/ package files
4. Identify what metrics infrastructure exists
5. Design minimal viable evaluation system

Output: Implementation plan with file list and priorities.
```

### Task 2.4: Exception Handling Improvements
**Type:** Code Quality
**Priority:** MEDIUM
**Prompt:**
```
Based on EXCEPTION_HANDLING_AUDIT.md findings, implement improvements.

Focus areas from audit:
1. Script execution retry mechanisms (ScriptExecution.java:67-70)
2. Voice STT retry (WhisperSTT.java:127-131)
3. File I/O retry patterns
4. Circuit breaker integration

Tasks:
1. Read identified files with issues
2. Propose specific retry/fallback implementations
3. Identify which resilience4j patterns to apply
4. Create implementation checklist

Output: Specific code changes with line numbers.
```

---

## Wave 3 (QUEUED)

### Task 3.1: Citation Standardization
**Type:** Documentation
**Priority:** HIGH (Dissertation)
**Prompt:**
```
Standardize all dissertation citations per FUTURE_ROADMAP.md lines 389-406.

Standard format:
- Bass, Clements, and Kazman, "Software Architecture in Practice" (2003)
- Isla, "Handling Complexity in the Halo 2 AI" (2005)

Tasks:
1. Find all dissertation chapter files in docs/research/
2. Extract all current citations
3. Convert to standard format
4. Add missing citations (behavior trees, OCC model, companion AI)
5. Create unified bibliography

Output: List of citations to update with before/after examples.
```

### Task 3.2: Limitations Sections
**Type:** Writing
**Priority:** HIGH (Dissertation)
**Prompt:**
```
Draft limitations sections per FUTURE_ROADMAP.md lines 409-441.

Required sections:
1. Chapter 1 - Behavior Tree Limitations
2. Chapter 3 - Emotional Model Limitations
3. Chapter 6 - Architecture Limitations
4. Chapter 8 - LLM Limitations
5. Practical Chapter - Tick Budget Limitations

Tasks:
1. Read relevant chapter content
2. Draft 200-300 word limitations section for each
3. Focus on honest assessment of constraints
4. Include future work suggestions

Output: 5 limitations sections ready for integration.
```

### Task 3.3: MUD Automation Integration Analysis
**Type:** Research
**Priority:** MEDIUM
**Prompt:**
```
Analyze MUD automation patterns from PRE_LLM_GAME_AUTOMATION.md.

Tasks:
1. Read docs/research/PRE_LLM_GAME_AUTOMATION.md
2. Extract TinTin++ and ZMud patterns
3. Map patterns to current Script DSL system
4. Identify integration opportunities
5. Propose specific enhancements

Output: Integration plan with code examples.
```

### Task 3.4: Performance Profiling Opportunities
**Type:** Analysis
**Priority:** LOW
**Prompt:**
```
Identify performance optimization opportunities.

Tasks:
1. Find TickProfiler usage in codebase
2. Identify tick budget enforcement coverage
3. Look for vector search scalability issues
4. Check LLM cache efficiency
5. Propose profiling strategy

Output: List of optimization targets with rationale.
```

---

## Wave 4 (FUTURE)

### Task 4.1: Documentation Sync Audit
**Type:** Quality Assurance
**Prompt:**
```
Compare CLAUDE.md claims against actual code.

Check:
1. Test coverage (claimed 39% vs actual)
2. Feature completeness claims
3. Package structure accuracy
4. Recent additions documentation

Output: Sync report with discrepancies.
```

### Task 4.2: Large File Refactoring Analysis
**Type:** Code Quality
**Prompt:**
```
Analyze large files from FUTURE_ROADMAP.md lines 551-555.

Files:
- ActionExecutor.java (752 lines)
- MineWrightConfig.java (1,277 lines)
- ForemanEntity.java

Tasks:
1. Read each file
2. Identify logical divisions
3. Propose extraction targets
4. Estimate refactoring effort

Output: Refactoring plan with risk assessment.
```

### Task 4.3: Production Readiness Checklist
**Type:** Planning
**Prompt:**
```
Create production release checklist per FUTURE_ROADMAP.md lines 510-521.

Tasks:
1. Review stability requirements
2. Check performance optimization status
3. Assess packaging (shadowJar, reobf)
4. List user documentation needs
5. Create release checklist

Output: Actionable production checklist.
```

### Task 4.4: User Guide Creation Plan
**Type:** Documentation
**Prompt:**
```
Design user guide structure for CurseForge/Modrinth release.

Sections needed:
1. Installation (Forge 1.20.1)
2. Configuration (config/steve-common.toml)
3. First steps (spawn foreman, basic commands)
4. Advanced features (multi-agent, profiles)
5. Troubleshooting

Output: User guide outline with content requirements.
```

---

## Wave 5 (ADVANCED)

### Task 5.1: Small Model Fine-Tuning Research
**Type:** Research
**Prompt:**
```
Research small model fine-tuning viability per FUTURE_ROADMAP.md lines 473-487.

Tasks:
1. Identify high-frequency tasks (mining, building, pathfinding)
2. Estimate dataset requirements
3. Research fine-tuning approaches (LoRA, QLoRA)
4. Identify target models (2-8B parameters)
5. Cost-benefit analysis

Output: Fine-tuning feasibility report.
```

### Task 5.2: Hive Mind Architecture Design
**Type:** Architecture
**Prompt:**
```
Design Hive Mind architecture per FUTURE_ROADMAP.md lines 492-506.

Tasks:
1. Research Cloudflare Workers capabilities
2. Design state synchronization protocol
3. Plan fallback mechanisms
4. Estimate latency improvements
5. Create implementation roadmap

Output: Hive Mind architecture specification.
```

---

## Continuous Maintenance Tasks

### Ongoing: Test Coverage Improvement
**Spawn whenever agent available**
**Prompt:**
```
Identify next test file to create for maximum coverage gain.

Tasks:
1. Find source files without corresponding tests
2. Prioritize by complexity and criticality
3. Propose test structure
4. Create test file with basic cases

Output: New test file or coverage recommendation.
```

### Ongoing: Code Quality Improvements
**Spawn whenever agent available**
**Prompt:**
```
Find and fix code quality issues.

Focus:
1. TODO/FIXME comments
2. Empty catch blocks (should be none)
3. Missing JavaDoc on public APIs
4. Code duplication

Output: Specific improvements with code changes.
```

---

## Orchestration Rules

1. **Always maintain 3+ agents running**
2. **Prioritize dissertation tasks (A → A+)**
3. **Mix research + implementation tasks**
4. **Commit after each wave completes**
5. **Update CLAUDE.md after significant findings**

---

**Next Action:** Spawn Wave 2 tasks as Wave 1 agents complete
