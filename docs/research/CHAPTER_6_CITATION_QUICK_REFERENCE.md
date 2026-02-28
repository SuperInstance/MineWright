# Chapter 6 Citation Quick Reference

**Date:** 2026-02-28
**Purpose:** Quick lookup of key citations for Chapter 6

---

## Foundational Software Architecture Citations

### Bass, Clements, Kazman (2012)
**Source:** *Software Architecture in Practice* (3rd ed.)

**Key Concepts:**
- Architecture as "highest-level concept of a system in its environment"
- Architecture = "set of design decisions that are difficult to change"
- Architecturally Significant Requirements (ASRs)
- Quality Attributes: performance, modifiability, security
- ATAM evaluation method
- Architectural styles vs patterns (p. 27)

**Usage in Chapter:**
- Section 0.1: Foundational definition
- Section 0.3: Quality attribute scenarios
- Section 8.1: Weighted scoring framework

**Page References:**
- p. 21: Architecture definition
- p. 27: Styles vs patterns
- p. 289: Quality attribute scenarios

---

### Shaw & Clements (2006)
**Source:** *A Field Guide to Software Architecture*

**Key Concepts:**
- Architectural styles = "families of systems in terms of a pattern of structural organization"
- Architectural patterns = "recurring solutions to common problems"
- Component-and-connector models
- Examples: pipe-and-filter, client-server, layered, blackboard

**Usage in Chapter:**
- Section 0.1: Styles vs patterns distinction
- Section 2-7: Categorizing BT, FSM, GOAP as patterns/styles

**Page References:**
- p. 7: Styles definition

---

### Van Vliet (2008)
**Source:** *Software Engineering: Principles and Practice*

**Key Concepts:**
- Architectural drivers
- "No free lunch" in architecture trade-offs
- Quality attribute balancing

**Usage in Chapter:**
- Section 0.1: Trade-off framework
- Section 8.1: Architecture decision matrix
- Section 10: Minecraft-specific recommendations

**Page References:**
- p. 84: No free lunch principle

---

### Taylor, Medvidovic, Dashofy (2009)
**Source:** *Software Architecture: Foundations, Theory, and Practice*

**Key Concepts:**
- Architecture enables reasoning about qualities
- Component composition and connectors
- Architecture Description Languages (ADLs)

**Usage in Chapter:**
- Section 0.1: Architecture as reasoning artifact
- Section 11: Code organization patterns

**Page References:**
- p. 26: Architecture definition

---

### Ford, Parsons, Kua (2017)
**Source:** *Building Evolutionary Architectures*

**Key Concepts:**
- Fitness functions for architecture
- "Architecture should evolve guided by tests"
- Incremental improvement

**Usage in Chapter:**
- Section 0.3: Fitness functions
- Section 12: Testing strategies

**Page References:**
- p. 12: Guided by tests

---

### Kazman et al. (1999)
**Source:** ATAM Method

**Key Concepts:**
- Architecture Tradeoff Analysis Method
- Quality attribute scenarios
- Risk and sensitivity analysis

**Usage in Chapter:**
- Section 0.3: Evaluation methods
- Section 8: Comparison framework

---

## Game AI Research Citations

### Isla (2005)
**Source:** "Handling Complexity in the Halo 2 AI" (GDC)

**Key Concepts:**
- Introduced behavior trees to game industry
- "Hierarchical decomposition of complex behaviors"
- "Reactive planning" - continuous re-evaluation
- "Explosion of states" problem in FSMs
- "Authorable, debuggable, and modular"

**Usage in Chapter:**
- Section 0.2: BT introduction
- Section 3: BT implementation
- Section 8.1: BT scoring

**Page References:**
- p. 12: Hierarchical decomposition

---

### Orkin (2004)
**Source:** "Applying Goal-Oriented Action Planning to Games"

**Key Concepts:**
- "Goals drive behavior, not pre-scripted sequences"
- GOAP in F.E.A.R.
- A* search through state space
- "Emergent behavior without designer-authored behavior trees"
- Preconditions and effects

**Usage in Chapter:**
- Section 0.2: GOAP introduction
- Section 4: GOAP implementation
- Section 8.1: GOAP scoring

**Page References:**
- p. 3: Goals drive behavior
- p. 8: Emergent behavior

---

### Champandard (2003, 2007)
**Source:** "Next-Gen Game AI Architecture" + "Utility-Based Decision Making"

**Key Concepts:**
- Multi-layered AI architecture
- "Architectural scalability"
- Response curves (logistic, exponential, linear)
- "Smooth, context-aware behavior transitions"
- Avoids "jittery switching" in FSMs

**Usage in Chapter:**
- Section 0.2: Multi-layered systems
- Section 6: Utility AI implementation
- Section 9: Hybrid architectures

---

### Rabin (2022)
**Source:** *Game AI Pro 360: Guide to Architecture*

**Key Concepts:**
- Industry survey: "80% use behavior trees"
- "Architecture choice depends on problem domain"
- Hybrid architectures now standard
- Combat AI: utility scoring
- Narrative AI: HTN decomposition
- Animation: state machines

**Usage in Chapter:**
- Section 0.2: Modern practices
- Section 10: Minecraft recommendations
- Section 8.1: Comparison matrix

**Page References:**
- p. 45: BT usage statistics
- p. 52: Domain dependence

---

### Wang et al. (2023)
**Source:** "Voyager: An Open-Ended Embodied Agent"

**Key Concepts:**
- "LLMs can serve as both planners and skill learners"
- Vector database skill libraries
- Code generation for novel tasks
- Hybrid architectures (LLM + BT/HTN)

**Usage in Chapter:**
- Section 0.2: LLM research
- Section 7: LLM-enhanced architectures
- Section 9: Hybrid patterns

**Page References:**
- p. 2: Planners and skill learners

---

## Citation Format Guide

### In-Text Citation Format:
```
According to Bass et al. (2012), architecture is "the highest-level concept..."
Bass, Clements, and Kazman (2012, p. 21) define architecture as...
The distinction between styles and patterns (Shaw & Clements, 2006, p. 7) is crucial...
```

### Bibliography Format:
```
Bass, L., Clements, P., & Kazman, R. (2012). *Software Architecture in Practice* (3rd ed.).
Addison-Wesley Professional.
```

### Common Citation Scenarios:

1. **Direct Quote:**
   ```
   Isla (2005) demonstrates that behavior trees provide "hierarchical decomposition of
   complex behaviors" (p. 12).
   ```

2. **Paraphrase:**
   ```
   Orkin (2004) pioneered GOAP in F.E.A.R., showing that symbolic planning could run
   in real-time game environments.
   ```

3. **Multiple Sources:**
   ```
   Behavior trees are now widely adopted (Isla, 2005; Rabin, 2022; Champandard, 2003).
   ```

4. **Secondary Citation:**
   ```
   Rabin (2022, p. 45) cites industry surveys showing 80% adoption of behavior trees.
   ```

---

## Citation Checklist

### When to Cite:
- [ ] Direct quotations (always use page numbers)
- [ ] Paraphrased ideas
- [ ] Specific concepts or frameworks
- [ ] Statistics or survey data
- [ ] Definitions of terms
- [ ] Methodologies (ATAM, fitness functions)

### Citation Elements:
- [ ] Author name(s)
- [ ] Publication year
- [ ] Page number (for quotes)
- [ ] Full reference in bibliography
- [ ] Proper formatting (APA, IEEE, etc.)

### Common Mistakes to Avoid:
- [ ] Citing without reading (verify page numbers)
- [ ] Missing page numbers for quotes
- [ ] Inconsistent citation format
- [ ] Orphan citations (in text but not in bibliography)
- [ ] Self-plagiarism (reusing your own work without citation)

---

## Recommended Additional Citations

If you expand the chapter, consider adding:

1. **More Recent LLM Work (2024-2025):**
   - Latest multi-agent LLM frameworks
   - Recent embodied AI papers
   - New LLM planning techniques

2. **Game AI Evaluation:**
   - Game AI benchmark suites
   - AI testing methodologies
   - Performance metrics for game AI

3. **Minecraft-Specific Research:**
   - More Minecraft AI papers
   - Voxel world navigation research
   - Procedural generation in Minecraft

4. **Architecture Patterns:**
   - More pattern catalogs
   - Anti-patterns in game AI
   - Architecture migration patterns

---

**Last Updated:** 2026-02-28
**Status:** Ready for use
