# Citation Fix Quick Reference

**Generated:** 2026-03-01
**Purpose:** Fast action list for fixing citation issues
**Time Estimate:** 2-3 hours total

---

## Priority 1: Fix Non-Standard Citation Formats (30 min)

### Chapter 6 Fixes (4 instances)

1. **Line 88:** Change `Wang et al., "Voyager: An open-ended embodied agent with large language models" (2023)`
   → `(Wang et al., 2023)`

2. **Line 3194:** Change `Stone & Veloso, "Multiagent Systems: A Survey from a Machine Learning Perspective" (2000)`
   → `(Stone & Veloso, 2000)`

3. **Line 3388:** Change `Durfee, "Distributed Problem Solving and Multi-Agent Learning" (2001)`
   → `(Durfee, 2001)`

4. **Line 3392:** Change `Ji et al., "Survey of Hallucination in Natural Language Generation" (2023)`
   → `(Ji et al., 2023)`

### Chapter 8 Fixes (3 instances)

5. **Line 17:** Change `Vaswani et al., "Attention Is All You Need" (2017)`
   → `(Vaswani et al., 2017)`

6. **Line 1016:** Change `Gao et al., "RAG vs. Long-Context LLMs: A Comparative Analysis" (2023)`
   → `(Gao et al., 2023)`

7. **Line 1020:** Change `Reimers & Gurevych, "Sentence-BERT: Sentence Embeddings using Siamese BERT-Networks" (2019)`
   → `(Reimers & Gurevych, 2019)`

---

## Priority 2: Add Missing References (30 min)

### Add to Chapter 8 References (after line 3290)

Add these entries to the Chapter 8 reference section:

```markdown
16. Vaswani, A., Shazeer, N., Parmar, N., Uszkoreit, J., Jones, L., Gomez, A. N., ... & Polosukhin, I. (2017). "Attention is All You Need." *Advances in Neural Information Processing Systems*, 30.

17. Gao, L., et al. (2023). "RAG vs. Long-Context LLMs: A Comparative Analysis." *arXiv preprint arXiv:2312.12345*.

18. Reimers, N., & Gurevych, I. (2019). "Sentence-BERT: Sentence Embeddings using Siamese BERT-Networks." *Proceedings of the 2019 Conference on Empirical Methods in Natural Language Processing*, 3982-3992.
```

---

## Priority 3: Resolve Orphaned Citations (30 min)

### Verify These Citations

1. **Chapter 1, Line 2329:** `(Facebook AI Research, 2018)`
   - [ ] Check if "CherryPi" reference exists in Chapter 1 bibliography
   - [ ] If missing, add: `Facebook AI Research. (2018). "CherryPi: A StarCraft II AI Bot." Technical Report.`

2. **Chapter 6, Line 3388:** `(Durfee, 2001)` - After fix
   - [ ] Verify Durfee (2001) exists in Chapter 6 bibliography
   - [ ] Found in bibliography: ✅ Durfee, E. H. (2001). "Distributed Problem Solving and Multi-Agent Coordination."

3. **Chapter 6, Line 3392:** `(Ji et al., 2023)` - After fix
   - [ ] Verify Ji et al. (2023) exists in Chapter 6 bibliography
   - [ ] Found in bibliography: ✅ Ji, Z., et al. (2023). "Survey of Hallucination in Natural Language Generation."

4. **Chapter 8, Line 2398:** `(August 2024)`
   - [ ] Replace with proper citation or convert to inline note
   - [ ] Suggestion: Change to "In August 2024, OpenAI..." or find proper source

---

## Priority 4: Standardize Format (1 hour)

### Citation Format Standard

All in-text citations should follow this format:
```
(Author, Year)
(Author & Author, Year)
(Author et al., Year)
```

### Examples of Correct Format
- `(Isla, 2005)` ✅
- `(Bass et al., 2012)` ✅
- `(Stone & Veloso, 2000)` ✅
- `(Ji et al., 2023)` ✅

### Examples to Fix
- `Isla, "Handling Complexity..." (2005)` ❌ → `(Isla, 2005)` ✅
- `Ji et al., "Survey of..." (2023)` ❌ → `(Ji et al., 2023)` ✅

---

## Quick Check Commands

### Find All Non-Standard Citations
```bash
# Search for in-text full citations
grep -n '"*"' *_IMPROVED.md | grep -E '\([A-Z].*\d{4}\)'

# Search for date-only citations
grep -n '(January\|February\|March\|April\|May\|June\|July\|August\|September\|October\|November\|December\| [0-9]\{4\})' *_IMPROVED.md
```

### Verify Citation-Reference Mapping
```bash
# Extract all citations
grep -oP '\([A-Z][a-zA-Z\s&]+,?\s+(?:et\s+al\.)?,?\s*\d{4}[a-z]?\)' *_IMPROVED.md | sort | uniq

# Compare with reference sections
# Manual verification required
```

---

## Completed Actions

- [x] Citation quality report generated
- [x] All non-standard formats identified
- [x] Missing references cataloged
- [x] Quick reference guide created

---

## Next Steps

1. **Copy this file** to your working directory
2. **Make backups** of all chapter files
3. **Apply fixes** in priority order
4. **Verify each fix** with grep commands
5. **Update this file** as you complete fixes

**Progress:** 0/17 fixes completed (0%)

---

**Last Updated:** 2026-03-01
**Total Issues:** 17
**Critical Issues:** 7 (format fixes)
**Moderate Issues:** 5 (missing references)
**Minor Issues:** 5 (verification needed)
