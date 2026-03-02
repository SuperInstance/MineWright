# Citation Format Quick Reference
## MineWright "One Abstraction Away" AI Dissertation

**Version:** 2.0 (Enhanced with HTN, OCC, and LLM Framework Citations)
**Last Updated:** 2026-03-01
**Purpose:** Quick reference for adding citations to dissertation chapters

---

## In-Text Citation Format

### Basic Rules

```
Single author:           [Author, Year]
Two authors:             [Author1 & Author2, Year]
Three or more authors:   [First Author et al., Year]
Multiple citations:      [Author1, Year1; Author2, Year2]
```

### Examples by Type

#### Academic Papers
```
[Orkin, 2004]
[Erol et al., 1994]
[Isla, 2005; Orkin, 2004]
```

#### Books
```
[Millington & Funge, 2009]
[Russell & Norvig, 2020]
```

#### Games and Software
```
[BioWare, 2009]
[Bethesda, 2006]
[OpenAI, 2024]
```

#### Organizations
```
[OpenAI, 2024]
[Anthropic, 2025]
```

---

## Foundational Citations by Category

### Hierarchical Task Networks (HTN)

**Always cite these when discussing HTN planning:**
```
[Erol et al., 1994]  - HTN planning formalization
[Nau et al., 2003]   - SHOP2 HTN planning system
[Cheng et al., 2018] - Adaptive HTN for dynamic environments
```

**When to use:**
- First mention of HTN in a chapter
- HTN algorithm explanations
- HTN vs other planning comparisons
- HTN implementation discussions

---

### Behavior Trees

**Key citation:**
```
[Isla, 2005] - Behavior trees in Halo 2
```

**When to use:**
- Behavior tree introduction
- BT architecture discussions
- BT vs FSM comparisons

---

### Goal-Oriented Action Planning (GOAP)

**Key citation:**
```
[Orkin, 2004] - GOAP in F.E.A.R.
```

**When to use:**
- GOAP introduction
- GOAP algorithm explanations
- GOAP vs HTN comparisons

---

### OCC Emotional Model

**Primary citation:**
```
[Ortony et al., 1988] - The Cognitive Structure of Emotions
```

**When to use:**
- First mention of OCC model
- Emotion taxonomy discussions
- Appraisal theory explanations
- Computational affective computing

---

### Software Architecture

**Foundational citations:**
```
[Bass et al., 2012]       - Software Architecture in Practice
[Shaw & Clements, 2006]   - Software Architecture Perspectives
[Kazman et al., 1999]     - ATAM evaluation method
[Ford et al., 2017]       - Evolutionary Architecture
```

**When to use:**
- Architecture pattern discussions
- Evaluation methodology
- Quality attribute scenarios
- Architectural trade-offs

---

### LLM Foundations

**Key citations:**
```
[Vaswani et al., 2017] - Attention Is All You Need (Transformers)
[Brown et al., 2020]   - GPT-3 paper
```

**When to use:**
- LLM architecture discussions
- Transformer explanations
- Attention mechanism descriptions

---

### Retrieval-Augmented Generation (RAG)

**Foundational citations:**
```
[Lewis et al., 2020]      - RAG foundation paper
[Gao et al., 2023]        - RAG vs Long-Context LLMs
[Reimers & Gurevych, 2019] - Sentence-BERT embeddings
```

**When to use:**
- RAG system discussions
- Vector database explanations
- Embedding model descriptions
- RAG performance analysis

---

### LLM Agent Frameworks

**Key citations:**
```
[Yao et al., 2022]    - ReAct (Reasoning + Acting)
[Graves, 2023]       - AutoGPT
[Harrison, 2023]     - LangChain
[Nakajima, 2023]     - BabyAGI
[Wang et al., 2023]  - Voyager (Minecraft LLM agent)
```

**When to use:**
- Agent framework comparisons
- ReAct pattern discussions
- Autonomous agent architectures
- Task decomposition systems

---

### Game AI Collections

**Key citations:**
```
[Champandard, 2007] - AI Game Development
[Rabin, 2022]       - Game AI Pro (industry survey)
[Millington & Funge, 2009] - AI for Games (textbook)
```

**When to use:**
- Game AI history
- Industry practices
- Architecture pattern surveys

---

## Citation Placement Guidelines

### Where to Place Citations

**DO place citations:**
1. After first mention of a technique/concept
   ```
   "Hierarchical Task Network (HTN) planning [Erol et al., 1994] decomposes..."
   ```

2. After direct quotes
   ```
   "goals drive behavior, not pre-scripted sequences" [Orkin, 2004, p. 3]
   ```

3. After specific claims or findings
   ```
   "HTN planners can outperform classical planners by orders of magnitude [Nau et al., 2003]"
   ```

4. After algorithm descriptions
   ```
   "The HTN decomposition algorithm recursively breaks down tasks [Erol et al., 1994; Nau et al., 2003]"
   ```

**DON'T place citations:**
1. In the middle of a sentence (unless quoting)
2. Multiple times for the same concept in one paragraph
3. For common knowledge (e.g., "Minecraft is a voxel-based game")

---

## Common Citation Mistakes to Avoid

### ❌ Incorrect

```
"According to [Erol et al., 1994], HTN planning is useful."
"The HTN planner [Erol et al., 1994], [Nau et al., 2003], and [Cheng et al., 2018] all show..."
"Erol et al. (1994) says that..."  (use [Erol et al., 1994] instead)
```

### ✅ Correct

```
"HTN planning is a hierarchical approach to automated planning [Erol et al., 1994]."
"HTN planning has been shown to outperform classical planners [Erol et al., 1994; Nau et al., 2003]."
"Erol et al. (1994) formalized HTN planning as a hierarchical approach."
```

---

## Quick Citation Templates

### Introducing a New Technique

```
**TECHNIQUE NAME** was first introduced by [Citation] as a solution to PROBLEM.
The key insight is INSIGHT [Citation]. This approach differs from PREVIOUS APPROACH
in that DIFFERENCE [Citation].
```

**Example:**
```
Hierarchical Task Network (HTN) planning was first formalized by Erol, Hendler,
and Nau (1994) as a hierarchical approach to automated planning. The key insight
is that many real-world planning problems have natural hierarchical structure,
and exploiting this structure dramatically reduces search complexity [Erol et al., 1994].
```

### Comparing Techniques

```
Unlike TECHNIQUE A [Citation A], which APPROACH, TECHNIQUE B [Citation B] APPROACH.
This makes TECHNIQUE B more suitable for USE CASE [Citation B].
```

**Example:**
```
Unlike classical planning [Erol et al., 1994], which searches through flat action
spaces, HTN planning recursively decomposes high-level tasks into primitive actions
using domain-specific methods [Nau et al., 2003]. This makes HTN planning more
suitable for real-time game AI [Cheng et al., 2018].
```

### Citing Multiple Sources

```
Multiple researchers have studied TOPIC [Author1, Year; Author2, Year; Author3, Year].
Consensus is that FINDING [Author1, Year; Author3, Year], though some debate remains
about ASPECT [Author2, Year].
```

---

## Chapter-Specific Citation Patterns

### Chapter 1: RTS Games
```
HTN Planning: [Erol et al., 1994], [Nau et al., 2003], [Cheng et al., 2018]
Behavior Trees: [Isla, 2005]
GOAP: [Orkin, 2004]
RTS History: [Buro, 2004], [Weber & Mateas, 2009], [Vinyals et al., 2019]
```

### Chapter 3: RPG Games
```
OCC Model: [Ortony et al., 1988]
Companion Systems: [Wright, 2000], [BioWare, 2009], [Druckmann, 2020]
NPC AI: [Bethesda, 2006], [Barone, 2016]
```

### Chapter 6: Architecture
```
Software Architecture: [Bass et al., 2012], [Shaw & Clements, 2006], [Kazman et al., 1999]
Game AI Architecture: [Isla, 2005], [Orkin, 2004], [Rabin, 2022]
HTN Planning: [Erol et al., 1994], [Nau et al., 2003], [Cheng et al., 2018]
Evolutionary Architecture: [Ford et al., 2017]
```

### Chapter 8: LLM Enhancement
```
Transformers: [Vaswani et al., 2017]
RAG: [Lewis et al., 2020], [Gao et al., 2023]
Agent Frameworks: [Yao et al., 2022], [Graves, 2023], [Harrison, 2023], [Nakajima, 2023]
LLM Agents: [Wang et al., 2023], [Guss et al., 2022]
Tool Calling: [OpenAI, 2024], [Anthropic, 2025]
```

---

## Bibliography Entry Format

### Books
```
Author, A. A., & Author, B. B., "Title of Book" (Year). *Publisher*.
```

### Journal Articles
```
Author, A. A., Author, B. B., & Author, C. C., "Title of Article" (Year). *Journal Name* Volume(Issue), pages.
```

### Conference Papers
```
Author, A. A., "Title of Paper" (Year). *Conference Name*.
```

### Games/Software
```
Studio/Developer, "Game Title" (Year). *Publisher*.
```

### Online Resources
```
Author, A. A., "Title" (Year). *Website/URL*.
```

---

## Quick Checklist

Before submitting any dissertation content:

- [ ] All techniques mentioned have citations on first mention
- [ ] Citations follow [Author, Year] format
- [ ] Multiple citations separated by semicolons
- [ ] No bare URLs in citations
- [ ] All cited sources appear in bibliography
- [ ] Bibliography entries match in-text citations
- [ ] No "citation needed" placeholders remain
- [ ] Proper use of "et al." for 3+ authors
- [ ] Consistent formatting throughout chapter

---

## Troubleshooting

### "I don't know what citation to use"

1. **Check the bibliography first** - COMPREHENSIVE_BIBLIOGRAPHY.md has 87 entries
2. **Search for the technique name** - Use Ctrl+F to find existing citations
3. **Check Chapter 6** - It has the most comprehensive citation coverage
4. **Use foundational citations** - When in doubt, cite the original paper

### "The bibliography is missing my citation"

1. **Find the full citation details** (author, year, title, venue)
2. **Add to COMPREHENSIVE_BIBLIOGRAPHY.md** in the appropriate category
3. **Update this document** with the new citation pattern
4. **Notify reviewers** of the bibliography update

### "I'm not sure if I need a citation"

**You probably need one if:**
- Introducing a named technique or algorithm
- Making a specific claim or finding
- Quoting or paraphrasing a source
- Comparing approaches
- Providing statistics or measurements

**You probably don't need one if:**
- Stating common knowledge in the field
- Describing your own implementation
- Explaining obvious relationships

---

**Last Updated:** 2026-03-01
**Maintained By:** Citation Enhancement Task Force
**Related Documents:**
- COMPREHENSIVE_BIBLIOGRAPHY.md
- CITATION_IMPLEMENTATION_SUMMARY.md
- CITATION_PROGRESS_TRACKER.md
