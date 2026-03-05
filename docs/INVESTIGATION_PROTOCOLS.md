# Investigation Protocols - Systematic Codebase Investigation

**Version:** 1.0
**Created:** 2026-03-05
**Purpose:** Teach systematic investigation methodology for complex codebases
**Target:** AI agents who need to understand, debug, or improve software systems

---

## Table of Contents

1. [Investigation Philosophy](#investigation-philosophy)
2. [The Universal Investigation Framework](#the-universal-investigation-framework)
3. [Rapid Codebase Assessment Protocol](#rapid-codebase-assessment-protocol)
4. [Deep Dive Investigation Protocol](#deep-dive-investigation-protocol)
5. [Bug Investigation Protocol](#bug-investigation-protocol)
6. [Performance Investigation Protocol](#performance-investigation-protocol)
7. [Tool Selection and Usage](#tool-selection-and-usage)

---

## Investigation Philosophy

### Core Principles

1. **Start with Questions, Not Answers**
   - Formulate hypotheses before gathering data
   - Let evidence guide conclusions, not vice versa
   - Remain open to being wrong

2. **Work Top-Down, Then Bottom-Up**
   - Understand the forest before the trees
   - Start with architecture, end with implementation
   - Validate high-level understanding with low-level details

3. **Follow the Data Flow**
   - Data structures are more stable than algorithms
   - Understanding flow reveals system intent
   - Interfaces show contracts, implementation shows strategy

4. **Pattern Recognition Over Memorization**
   - Recognize patterns, don't memorize details
   - Patterns generalize, specifics don't
   - Build mental models, not mental databases

5. **Investigate with Purpose**
   - Know what you're trying to understand
   - Set clear goals for each investigation
   - Know when you have enough information

### The Investigation Mindset

```
┌─────────────────────────────────────────────────────────────────┐
│                     INVESTIGATION MINDSET                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Curiosity    ──→  "Why is it this way?"                        │
│  Skepticism   ──→  "Is this really true?"                       │
│  Humility     ──→  "What am I missing?"                         │
│  Rigor        ──→  "Can I verify this?"                         │
│  Creativity   ──→  "What else could explain this?"             │
│  Patience     ──→  "Take the time to understand deeply"         │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## The Universal Investigation Framework

### The 5-Phase Investigation Cycle

```
┌─────────────────┐
│  1. DEFINE      │  What am I investigating? What are my goals?
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  2. SURVEY      │  Get the lay of the land. Identify key areas.
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  3. HYPOTHESIZE │  Form testable theories about how it works.
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  4. INVESTIGATE │  Deep dive into specific areas. Test hypotheses.
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  5. SYNTHESIZE  │  Integrate findings. Update mental models.
└─────────────────┘
         │
         └──→ Loop back as needed
```

### Phase 1: Define

**Questions to Answer:**
- What specifically am I trying to understand?
- What level of detail do I need?
- What decisions will this investigation inform?
- What are my success criteria?

**Output:** A clear investigation statement

**Example:**
```
BAD: "Understand the codebase"
GOOD: "Understand how MineWright coordinates multiple agents to build
      structures without conflicts, focusing on the Contract Net Protocol
      implementation and spatial coordination mechanisms."
```

### Phase 2: Survey

**Goals:**
- Identify major subsystems
- Understand overall architecture
- Locate relevant code areas
- Find documentation

**Time Budget:** 15-30 minutes

**Actions:**
1. Scan directory structure
2. Read README and main documentation
3. Identify entry points
4. Look for architecture diagrams
5. Find test files (they show intended usage)

**Output:** Mental map of the codebase

### Phase 3: Hypothesize

**Before Deep Diving, Make Predictions:**

Based on your survey, formulate hypotheses:
- "I expect component X to handle Y by doing Z"
- "The data probably flows through A, then B, then C"
- "This pattern is likely a Strategy/Observer/Factory"

**Why Hypothesize?**
- Forces you to think about structure
- Makes investigation more focused
- Surprises become learning opportunities
- Builds better mental models

### Phase 4: Investigate

**Systematic Deep Dive:**

1. **Follow the Data** - Trace data structures through the system
2. **Follow the Control** - Trace execution flow
3. **Follow the Errors** - Look at error handling
4. **Follow the Tests** - See what behavior is tested

**Always Ask:**
- Does this match my hypothesis?
- If not, why? What was I missing?
- What pattern is this following?
- What are the edge cases?

### Phase 5: Synthesize

**Integration:**
- Combine findings into a coherent model
- Identify patterns and principles
- Note surprises and lessons learned
- Document for future reference

**Validation:**
- Can I explain how the system works?
- Can I predict its behavior?
- Can I identify where to make changes?

---

## Rapid Codebase Assessment Protocol

### 20-Minute Codebase Understanding

**Goal:** Understand the essence of a codebase in 20 minutes

```
Minute 0-5:    Structure Scan
Minute 5-10:   Architecture Identification
Minute 10-15:  Trace One Flow
Minute 15-20:  Pattern Extraction
```

### Minute 0-5: Structure Scan

**What to Look For:**
- Package/module organization
- Main entry points
- Configuration files
- Documentation locations
- Test organization

**Tools:** Directory listing, package structure

**Output:** A map of the territory

```bash
# Quick structure scan
find . -type f -name "*.java" | head -20
ls -la src/main/java/com/minewright/
tree -L 2 -d src/main/java/
```

### Minute 5-10: Architecture Identification

**What to Identify:**
- Main subsystems (3-7 typically)
- Communication patterns between subsystems
- Key abstractions
- Data flow direction

**Questions:**
- What are the layers?
- How do components communicate?
- What are the main interfaces?
- What patterns are evident?

**Output:** Architecture diagram (mental or actual)

### Minute 10-15: Trace One Flow

**Pick ONE concrete example and trace it completely:**

Example flows in MineWright:
- User command → Agent execution
- LLM request → Response parsing
- Agent spawn → Task completion

**For the chosen flow:**
1. Find the entry point
2. Trace method calls
3. Identify key decisions
4. Note the data transformations
5. Follow error paths

**Output:** End-to-end understanding of one flow

### Minute 15-20: Pattern Extraction

**Identify Reusable Patterns:**

Ask:
- What patterns repeat across subsystems?
- What design principles are evident?
- What are the key abstractions?
- What could be generalized?

**Output:** Pattern library for this codebase

---

## Deep Dive Investigation Protocol

### When to Use

- Need comprehensive understanding of a subsystem
- Preparing for significant refactoring
- Investigating complex bug
- Designing new feature

### The Deep Dive Process

```
┌─────────────────────────────────────────────────────────────────┐
│                    PREPARATION (5 min)                           │
│  • Define scope and goals                                        │
│  • Identify relevant files                                       │
│  • Prepare investigation environment                             │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    CONTEXT (10 min)                              │
│  • Read surrounding documentation                                │
│  • Understand why this code exists                               │
│  • Identify related components                                   │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    STRUCTURE (15 min)                            │
│  • Identify classes and relationships                            │
│  • Map dependencies                                              │
│  • Understand data structures                                    │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    BEHAVIOR (20 min)                             │
│  • Trace execution flows                                         │
│  • Understand state transitions                                  │
│  • Identify key algorithms                                       │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    SYNTHESIS (10 min)                            │
│  • Extract patterns and principles                               │
│  • Document findings                                             │
│  • Identify improvement opportunities                            │
└─────────────────────────────────────────────────────────────────┘
```

### Deep Dive Checklist

**For Each Class/Component:**

- [ ] **Purpose**: What problem does it solve?
- [ ] **Responsibilities**: What is it responsible for?
- [ ] **Collaborators**: What does it depend on?
- [ ] **State**: What does it track?
- [ ] **Behavior**: What can it do?
- [ ] **Lifecycle**: When is it created/destroyed?
- [ ] **Constraints**: What are its limitations?
- [ ] **Testing**: How is it tested?

### Reading Code Effectively

**The Three-Pass Technique:**

**Pass 1: Skim (30 seconds)**
- Read class name and purpose
- Scan method names
- Note state variables
- Get the gist

**Pass 2: Understand (3 minutes)**
- Read important methods
- Understand the algorithm
- Identify patterns
- Form mental model

**Pass 3: Analyze (as needed)**
- Deep dive into complex logic
- Understand edge cases
- Verify understanding
- Extract principles

---

## Bug Investigation Protocol

### The Scientific Debugging Method

```
┌─────────────────────────────────────────────────────────────────┐
│                    OBSERVE                                       │
│  What is the bug? What are the symptoms?                        │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                    HYPOTHESIZE                                   │
│  What could cause this? Brainstorm causes.                      │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                    PREDICT                                       │
│  If my hypothesis is correct, what should I see?               │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                    TEST                                         │
│  Verify prediction through observation or testing.              │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
                    ┌─────┴─────┐
                    │ Confirmed?│
                    └─────┬─────┘
                      YES │   │ NO
                          ▼   ▼
                      Fix it  New Hypothesis
```

### Bug Investigation Questions

**Initial Questions:**
1. What exactly is the bug? (Be precise)
2. When does it occur? (Conditions, timing)
3. What should happen instead?
4. What are the error messages/stack traces?

**Context Questions:**
1. What changed recently?
2. What's similar that works?
3. What are the Preconditions?
4. What are the Triggers?

**Code Questions:**
1. Where in the code would this happen?
2. What code path is likely involved?
3. What are the recent changes to that area?
4. What are the dependencies?

### Systematic Bug Hunting

**Strategy 1: Binary Search**
- Isolate the problem by dividing possibilities
- Comment out half the code
- If bug disappears, it's in that half
- Repeat until found

**Strategy 2: Trace Execution**
- Add logging at key points
- Follow the execution path
- Identify where behavior diverges from expectation
- Narrow down to the problematic code

**Strategy 3: Minimal Reproduction**
- Create smallest possible test case
- Remove unnecessary complexity
- Isolate the core issue
- Fix becomes obvious

**Strategy 4: Rubber Ducking**
- Explain the code line by line
- Often the problem becomes clear during explanation
- Forces detailed understanding

---

## Performance Investigation Protocol

### The Performance Investigation Pyramid

```
                ┌─────────────────┐
                │   APPLICATION   │  ← End-user experience
                ├─────────────────┤
                │    CODE LEVEL   │  ← Algorithms, data structures
                ├─────────────────┤
                │   SYSTEM LEVEL  │  ← CPU, memory, I/O
                ├─────────────────┤
                │  ARCHITECTURE   │  ← Design, patterns, communication
                └─────────────────┘
```

### Investigation Steps

**Step 1: Measure First**
- Don't guess, measure
- Use profilers, not intuition
- Identify hot spots quantitatively

**Step 2: Identify the Bottleneck**
- Is it CPU bound? Memory bound? I/O bound?
- Is it algorithmic complexity?
- Is it architectural (excessive calls, locking)?

**Step 3: Analyze the Hot Path**
- Focus on code that executes frequently
- 20% of code typically accounts for 80% of execution
- Optimize the hot path first

**Step 4: Consider Alternatives**
- Better algorithm?
- Better data structure?
- Better architecture?
- Caching?
- Parallelization?

**Step 5: Verify Improvement**
- Measure before and after
- Ensure improvement is real
- Watch for regressions

### Performance Patterns to Look For

**Anti-Patterns:**
- N+1 queries (looping database calls)
- Unnecessary object creation
- Inefficient algorithms (O(n²) where O(n) possible)
- Excessive locking
- Cache misses
- String concatenation in loops
- Unboxing primitives

**Optimization Patterns:**
- Caching (memoization, lookup tables)
- Batching (reduce round trips)
- Lazy loading (defer work until needed)
- Parallel processing (use multiple cores)
- Streaming (process data incrementally)
- Object pools (reuse expensive objects)

---

## Tool Selection and Usage

### Investigation Toolbelt

**For Structure:**
```bash
# Find files by pattern
find . -name "*.java" | grep -i pattern

# Show directory structure
tree -L 3 -I 'target|build'

# Count lines of code
cloc . --exclude-dir=target,build

# Find large files
find . -type f -name "*.java" -exec wc -l {} + | sort -rn | head -20
```

**For Content:**
```bash
# Search for code patterns
grep -r "pattern" --include="*.java"

# Find class definitions
grep -r "^public class" --include="*.java"

# Find method signatures
grep -r "public.*(" --include="*.java"

# Search TODO/FIXME
grep -r "TODO\|FIXME" --include="*.java"
```

**For Relationships:**
```bash
# Find usages of a class
grep -r "ClassName" --include="*.java"

# Find imports (dependencies)
grep -r "^import" --include="*.java" | sort | uniq

# Find test files
find . -name "*Test.java" -o -name "*Tests.java"
```

### Reading Strategies

**Strategy 1: Focused Reading**
- Read with a specific question in mind
- Scan for relevant sections
- Don't read everything linearly

**Strategy 2: Test-First Reading**
- Read tests before implementation
- Tests show expected behavior
- Tests provide usage examples

**Strategy 3: Git Archaeology**
- Look at git blame for recent changes
- Read commit messages for context
- Understand evolution of code

**Strategy 4: Diff-Driven Learning**
- Look at recent PRs
- Understand what changed and why
- Learn from others' approaches

---

## Common Investigation Patterns

### Pattern 1: Follow the Interface

**When:** Understanding a subsystem

**How:**
1. Find the main interface
2. Understand the contract
3. Look at implementations
4. Understand how they differ

**Why:** Interfaces reveal intent without implementation complexity

### Pattern 2: Trace the Lifecycle

**When:** Understanding stateful components

**How:**
1. Find creation/initialization
2. Understand normal operation
3. Find cleanup/destruction
4. Identify state transitions

**Why:** Lifecycle reveals responsibilities and constraints

### Pattern 3: Follow the Error

**When:** Understanding robustness

**How:**
1. Find error handling code
2. Understand what can go wrong
3. See how errors are handled
4. Identify recovery strategies

**Why:** Error paths reveal system assumptions and edge cases

### Pattern 4: Read the Tests

**When:** Understanding expected behavior

**How:**
1. Find test files
2. Read test names (they document behavior)
3. Understand test setup
4. See what's tested (and what's not)

**Why:** Tests are executable documentation

---

## Investigation Quick Reference

### Starting a New Investigation

```
1. CLARIFY GOALS (1 min)
   - What am I trying to understand?
   - What decisions will this inform?

2. SURVEY TERRITORY (5-10 min)
   - Scan directory structure
   - Read main documentation
   - Identify entry points

3. FORM HYPOTHESES (2 min)
   - Based on survey, what do I expect?
   - What patterns should I see?

4. INVESTIGATE (as long as needed)
   - Focus on relevant areas
   - Test hypotheses
   - Follow data flow

5. SYNTHESIZE (5 min)
   - Integrate findings
   - Extract patterns
   - Document insights
```

### Debugging Quick Reference

```
1. REPRODUCE
   - Can I reproduce the bug?
   - What are the exact steps?

2. ISOLATE
   - What's the minimum reproduction?
   - What can I eliminate?

3. HYPOTHESIZE
   - What could cause this?
   - Rank hypotheses by likelihood

4. VERIFY
   - Test each hypothesis
   - Confirm root cause

5. FIX
   - Implement fix
   - Verify it works
   - Add test to prevent regression
```

### Performance Quick Reference

```
1. MEASURE
   - Profile before optimizing
   - Identify actual bottlenecks

2. ANALYZE
   - Is it algorithmic?
   - Is it architectural?
   - Is it implementation?

3. OPTIMIZE
   - Focus on hot path
   - Consider alternatives
   - Test improvements

4. VERIFY
   - Measure after optimization
   - Ensure no regressions
   - Document findings
```

---

## Conclusion: Effective Investigation

**The Key Principles:**

1. **Start with questions, not answers**
2. **Work top-down, then bottom-up**
3. **Form hypotheses and test them**
4. **Follow data flow and control flow**
5. **Extract patterns, don't memorize details**
6. **Use the right tools for the job**
7. **Document what you learn**

**The Goal:** Not just to understand, but to understand HOW to understand.

---

**Document Version:** 1.0
**Last Updated:** 2026-03-05
**Maintained By:** MineWright Project
**Status:** Active - Core Investigation Framework
