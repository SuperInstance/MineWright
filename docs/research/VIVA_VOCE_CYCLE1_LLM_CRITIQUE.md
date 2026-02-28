# Viva Voce Examination: Chapter 8 - LLM Enhancement

**Examiner:** PhD Reviewer specializing in LLM Integration and AI Augmentation
**Date:** 2026-02-28
**Candidate:** Game AI Dissertation - Chapter 8
**Examination Type:** Cycle 1 - Initial Review

---

## Overall Assessment: **Minor Revisions**

The chapter presents a compelling vision for hybrid LLM-traditional AI architectures with strong practical implementation. However, several significant gaps in coverage of contemporary LLM techniques, insufficient academic rigor in prompt engineering discussion, and missing critical references prevent this from receiving a "Pass" without revision.

**Strengths:**
- Clear articulation of the "one abstraction away" principle
- Comprehensive coverage of implementation architecture
- Excellent cost optimization strategies
- Strong code examples with production-ready patterns

**Critical Weaknesses:**
- Major gaps in LLM technique coverage (RAG, agents, function calling, etc.)
- Insufficient grounding in academic literature
- Limited discussion of evaluation methodologies
- Missing treatment of modern LLM paradigms (2023-2025)

---

## Ten Specific Criticisms

### 1. **Missing Retrieval-Augmented Generation (RAG) Discussion**

**Severity:** High

The chapter makes no mention of RAG architectures, which have become foundational for LLM systems requiring domain-specific knowledge. Given that Minecraft has extensive wikis, documentation, and community knowledge, RAG would be a natural enhancement.

**Required Addition:**
- Section on "LLM as Knowledge Retrieval Interface" using RAG
- Discussion of embedding-based retrieval for:
  - Minecraft crafting recipes
  - Block properties and mechanics
  - Building templates and patterns
  - Community knowledge bases

**References to Add:**
- Lewis et al. (2020). "Retrieval-Augmented Generation for Knowledge-Intensive NLP Tasks." NeurIPS.
- Gao et al. (2023). "Retrieval-Augmented Generation for Large Language Models: A Survey." arXiv.

---

### 2. **Inadequate Coverage of LLM Agent Architectures**

**Severity:** High

The dissertation describes a simple "LLM generates scripts" pattern but fails to engage with contemporary research on LLM agents that:
- Use tools dynamically
- Maintain multi-step reasoning chains
- Employ memory systems beyond simple conversation history
- Exhibit emergent behaviors from composition

**Required Addition:**
- Subsection 8.3.5: "Comparison with Modern LLM Agent Frameworks"
- Analysis of how Steve AI relates to:
  - ReAct (Reasoning + Acting) patterns
  - AutoGPT-style autonomous agents
  - BabyAGI task decomposition
  - LangChain Agent frameworks

**References to Add:**
- Yao et al. (2022). "ReAct: Synergizing Reasoning and Acting in Language Models." ICLR.
- Significant Gravitas (2023). "AutoGPT: An Autonomous GPT-4 Agent."
- Nakano et al. (2021). "WebGPT: Browser-assisted question-answering."

---

### 3. **Function Calling / Tool Use Under-Specified**

**Severity:** High

The chapter describes "action scripts" but doesn't explicitly frame this within the context of function calling APIs (OpenAI Functions, Gemini Tool Use, etc.), which have become standard since 2023.

**Current Treatment:**
- Section 8.7.1 mentions "AVAILABLE ACTIONS" but presents this as prompt text rather than structured function definitions

**Required Addition:**
- Explicit discussion of function calling as a distinct paradigm
- Comparison of:
  - Prompt-based action specification (current approach)
  - Structured function calling (JSON schema definitions)
  - Tool use with dynamic selection
- Code examples showing OpenAI function calling integration

**References to Add:**
- OpenAI (2023). "Function Calling and Other API Updates."
- Schick et al. (2023). "Toolformer: Language Models Can Teach Themselves to Use Tools."

---

### 4. **Insufficient Prompt Engineering Rigor**

**Severity:** Medium-High

Section 8.7 covers prompt engineering but lacks academic depth. It reads more like a blog post than a dissertation chapter, missing:

**Missing Elements:**
- No discussion of prompt optimization techniques (gradient-based search, DSPy)
- No analysis of prompt sensitivity or robustness
- No quantitative evaluation of prompt variants
- No mention of prompt injection attacks or security concerns

**Required Addition:**
- Subsection 8.7.6: "Prompt Optimization and Evaluation"
- A/B testing methodology for prompt variants (mentioned but not detailed)
- Prompt injection security considerations
- DSPy or similar prompt optimization frameworks

**References to Add:**
- Zhou et al. (2022). "Large Language Models Are Human-Level Prompt Engineers." ICLR.
- Liu et al. (2023). "Prompt Engineering for Large Language Models: A Comprehensive Guide."
- Kandasamy et al. (2023). "Prompt Optimization using Text Gradient Descent."

---

### 5. **Missing Multi-Modal LLM Discussion**

**Severity:** Medium

Chapter 13.1 briefly mentions "Multimodal LLMs" as a future direction, but this deserves significant expansion given:

- Minecraft is inherently visual
- GPT-4V, Gemini Pro Vision, and LLaVA can process screenshots
- The codebase includes `MinecraftVisionClient.java` but it's not discussed

**Required Addition:**
- Move multimodal discussion from "Future Directions" to main chapter
- Section on "Vision-Language Models for Scene Understanding"
- Analysis of how visual input changes prompt architecture
- Cost/benefit analysis of text-only vs. multimodal approaches

**References to Add:**
- OpenAI (2023). "GPT-4V(ision) System Card."
- Google (2023). "Gemini: A Family of Highly Capable Multimodal Models."
- Liu et al. (2023). "Visual Instruction Tuning." (LLaVA)

---

### 6. **Inadequate Treatment of Fine-Tuning vs. Prompting**

**Severity:** Medium

The chapter focuses entirely on prompt-based approaches but never addresses:
- When fine-tuning might be preferable
- Cost/benefit analysis of fine-tuning vs. prompting
- Parameter-efficient fine-tuning (PEFT, LoRA)
- Domain adaptation techniques

**Required Addition:**
- Section 8.6.5: "When to Fine-Tune vs. Prompt"
- Decision framework based on:
  - Task specificity
  - Data availability
  - Update frequency requirements
  - Cost considerations

**References to Add:**
- Hu et al. (2021). "LoRA: Low-Rank Adaptation of Large Language Models."
- Liu et al. (2023). "LoRA Learns Less and Forgets Less." (QLoRA)
- Wei et al. (2022). "Chain-of-Thought Prompting Elicits Reasoning in Large Language Models."

---

### 7. **Insufficient Evaluation Methodology**

**Severity:** Medium-High

Section 8.12 presents "Real-World Performance" but lacks scientific rigor:

**Current Issues:**
- No controlled experiments
- No baseline comparisons (LLM vs. traditional-only)
- Anecdotal case studies rather than systematic evaluation
- No statistical significance testing
- No discussion of confounding variables

**Required Addition:**
- Section 8.10.5: "Experimental Design and Evaluation"
- Controlled A/B testing methodology
- Metrics beyond latency and cost:
  - Task success rate (controlled conditions)
  - Plan quality measures
  - User satisfaction (survey methodology)
  - Comparison with baselines

**References to Add:**
- Clark et al. (2021). "All That's 'Human' Is Not Gold: Evaluating Human Evaluation of Generated Text."
- Bowman & Dahl (2021). "What Will It Take to Fix Benchmarking in Natural Language Understanding?"

---

### 8. **Outdated References (Pre-2023 Bias)**

**Severity:** Medium

The references section is heavily skewed toward 2020-2022 papers, missing crucial 2023-2025 developments:

**Missing Landmark Papers:**
- Llama 2 & Llama 3 model cards (Meta 2023, 2024)
- GPT-4 Technical Report (OpenAI 2023)
- Gemini paper (Google 2023)
- Mixtral papers (Mistral 2024)
- Agent frameworks (AutoGPT, LangChain, etc.)

**Required Addition:**
- Update references with at least 15 papers from 2023-2025
- Include model cards for all discussed LLMs
- Add recent LLM agent surveys

---

### 9. **Cost Analysis Lacks Total Cost of Ownership (TCO)**

**Severity:** Medium

Section 8.6.3 presents a cost analysis but doesn't account for:

**Missing Cost Factors:**
- Engineering time to maintain prompts
- Monitoring and debugging overhead
- API rate limit waiting costs (user experience degradation)
- Storage and compute for caching infrastructure
- Opportunity cost of API downtime

**Required Addition:**
- Comprehensive TCO model
- Sensitivity analysis showing cost under different usage patterns
- Comparison with local LLM deployment costs

**References to Add:**
- Bagen et al. (2024). "Holistic Evaluation of Text-To-Image Models."
- Operational cost studies for production LLM systems.

---

### 10. **Missing Discussion of LLM Security and Safety**

**Severity:** Medium-High

The chapter has no treatment of:
- Prompt injection attacks
- Jailbreaking attempts
- PII leakage through prompts
- Adversarial command inputs
- Content moderation for generated plans

**Required Addition:**
- Section 8.8.5: "Security and Safety Considerations"
- Input validation strategies
- Output sanitization
- Redaction of sensitive information from prompts
- Content moderation for generated actions

**References to Add:**
- Greshake et al. (2023). "Not what you've signed up for: Compromising Real-World LLM-Integrated Applications with Indirect Prompt Injection."
- Wallace et al. (2024). "Jailbreak: A Novel Dataset and Method for Automatic Jailbreaking across LLMs."

---

## Recommended Additions Summary

### New Sections Required:

1. **8.2.6: Retrieval-Augmented Generation (RAG)**
   - Embedding-based knowledge retrieval
   - Integration with Minecraft documentation
   - Comparison with prompt-based approaches

2. **8.3.5: LLM Agent Architectures**
   - ReAct patterns
   - Tool use frameworks
   - Comparison with Steve AI approach

3. **8.6.5: Fine-Tuning Decision Framework**
   - When to fine-tune vs. prompt
   - PEFT techniques (LoRA, QLoRA)
   - Cost/benefit analysis

4. **8.7.6: Prompt Optimization Techniques**
   - DSPy framework
   - A/B testing methodology
   - Prompt injection security

5. **8.8.5: Security and Safety**
   - Input validation
   - Prompt injection mitigation
   - Content moderation

6. **8.10.5: Experimental Methodology**
   - Controlled experiments
   - Statistical significance
   - Baseline comparisons

7. **8.13: Multimodal Integration** (expand from brief mention)
   - Vision-language models
   - Screenshot-based planning
   - Implementation architecture

---

## Missing References - Top 20 to Add

### Core LLM Architectures:
1. **OpenAI (2023)**. "GPT-4 Technical Report." arXiv:2303.08774.
2. **OpenAI (2024)**. "GPT-4V System Card."
3. **Meta (2023)**. "Llama 2: Open Foundation and Fine-Tuned Chat Models." arXiv:2307.09288.
4. **Meta (2024)**. "The Llama 3 Herd of Models." arXiv:2407.21783.
5. **Google (2023)**. "Gemini: A Family of Highly Capable Multimodal Models." arXiv:2312.11805.

### Agent Architectures:
6. **Yao et al. (2022)**. "ReAct: Synergizing Reasoning and Acting in Language Models." ICLR 2023.
7. **Schick et al. (2023)**. "Toolformer: Language Models Can Teach Themselves to Use Tools." NeurIPS 2023.
8. **Nakano et al. (2021)**. "WebGPT: Browser-assisted Question-Answering with Human Feedback." arXiv:2112.09332.
9. **Kadhao et al. (2023)**. "HuggingGPT: Solving AI Tasks with ChatGPT and its Friends in Hugging Face." arXiv:2303.17580.

### RAG and Knowledge Retrieval:
10. **Lewis et al. (2020)**. "Retrieval-Augmented Generation for Knowledge-Intensive NLP Tasks." NeurIPS 2020.
11. **Gao et al. (2023)**. "Retrieval-Augmented Generation for Large Language Models: A Survey." arXiv:2312.10997.
12. **Karpukhin et al. (2020)**. "Dense Passage Retrieval for Open-Domain Question Answering." EMNLP 2020.

### Prompt Engineering:
13. **Wei et al. (2022)**. "Chain-of-Thought Prompting Elicits Reasoning in Large Language Models." NeurIPS 2022.
14. **Kojima et al. (2022)**. "Large Language Models are Zero-Shot Reasoners." NeurIPS 2022.
15. **Zhou et al. (2022)**. "Least-to-Most Prompting Enables Complex Reasoning in Large Language Models." ICLR 2023.

### Fine-Tuning:
16. **Hu et al. (2021)**. "LoRA: Low-Rank Adaptation of Large Language Models." ICLR 2022.
17. **Liu et al. (2023)**. "LoRA Learns Less and Forgets Less." (QLoRA) arXiv:2305.14314.

### Security:
18. **Greshake et al. (2023)**. "Not What You've Signed Up For: Compromising Real-World LLM-Integrated Applications with Indirect Prompt Injection." arXiv:2302.12173.
19. **Wallace et al. (2024)**. "Jailbreak: A Novel Dataset and Method for Automatic Jailbreaking across LLMs." arXiv:2401.08546.

### Evaluation:
20. **Chang et al. (2023)**. "What Makes Good In-Context Examples? A Systematic Study on In-Context Learning." arXiv:2310.03006.

---

## Minor Issues

### Style and Presentation:
1. **Code Examples:** Generally excellent, but some are overly long (e.g., Section 8.5.3)
2. **Diagrams:** ASCII art is clear but could be enhanced with actual figures
3. **Tables:** Good use of comparison tables (e.g., Section 8.6.1)

### Technical Clarity:
1. **Async/Await Pattern:** Section 8.5.3 is excellent
2. **Circuit Breaker:** Good explanation but could reference original pattern paper
3. **Cascade Routing:** Innovative but needs more academic justification

### Writing Quality:
1. Generally clear and well-structured
2. Some sections read more like documentation than academic text
3. Transitions between technical and conceptual content could be smoother

---

## Challenges to "Brain-Script-Execution" Architecture

The candidate should be prepared to defend:

### Challenge 1: Why Not Direct Execution?
**Q:** "Why have the LLM generate scripts that are then executed, rather than having the LLM directly control the agent through function calling?"

**Expected Defense:**
- Latency separation (planning vs. execution)
- Validation layer between LLM and game
- Enables traditional AI to take over during LLM failures
- Caching of generated plans

**Counter-Argument:** Function calling with streaming can provide similar benefits while reducing complexity.

### Challenge 2: Is This Really Novel?
**Q:** "The 'LLM generates scripts' pattern has been used since GPT-3 in 2020. What is novel here?"

**Expected Defense:**
- Game AI specific application
- Hybrid architecture with graceful degradation
- Cascade routing for cost optimization
- Production-hardened implementation

**Risk:** This may be viewed as incremental rather than novel contribution.

### Challenge 3: Generalizability
**Q:** "To what extent does this architecture generalize beyond Minecraft? What principles are transferable?"

**Expected Defense:**
- Separation of strategic/tactical layers
- Prompt engineering patterns
- Cost optimization strategies
- Resilience patterns

**Risk:** The dissertation may be too Minecraft-specific.

---

## Grade: **B+**

**Justification:**

The chapter demonstrates:
- Strong practical implementation
- Clear architectural vision
- Good cost optimization strategies
- Production-ready code examples

However, it suffers from:
- Significant gaps in LLM technique coverage (RAG, agents, function calling)
- Insufficient academic rigor in evaluation
- Missing modern references (2023-2025)
- Limited discussion of security considerations
- Inadequate treatment of prompt optimization

**Grade Breakdown:**
- **Technical Content:** A- (excellent implementation details)
- **Academic Rigor:** C+ (weak evaluation, missing modern literature)
- **Novelty:** B (application of known patterns to new domain)
- **Completeness:** C (major gaps in coverage)
- **Writing Quality:** B+ (clear but inconsistent tone)

**Overall:** B+

**Recommendation:** Minor revisions with focus on:
1. Adding missing LLM technique coverage (RAG, agents, function calling)
2. Strengthening evaluation methodology
3. Updating references with 2023-2025 papers
4. Adding security and safety discussion
5. Expanding multimodal coverage

**Estimated Revision Time:** 20-30 hours of focused work.

---

## Examination Committee Questions for Viva

### Technical Questions:
1. "How does your cascade routing compare with speculative decoding techniques?"
2. "What is the failure mode when all LLM tiers are unavailable?"
3. "How do you handle versioning of prompts when the underlying model changes?"
4. "What is the latency budget for each layer in your architecture?"

### Academic Questions:
1. "How would you formally evaluate the 'quality' of a generated plan?"
2. "What are the theoretical limits of your caching strategy?"
3. "How does your approach compare with symbolic planning methods like STRIPS?"
4. "What hypotheses does your work test, and what are the null hypotheses?"

### Philosophical Questions:
1. "To what extent is the LLM 'reasoning' versus 'retrieving patterns from training data'?"
2. "What does your work tell us about the complementarity of neural and symbolic AI?"
3. "If LLMs continue to improve, will your hybrid architecture become obsolete?"

---

## Post-Viva Action Items

### Priority 1 (Must Complete):
- [ ] Add RAG section with implementation details
- [ ] Add LLM agent comparison subsection
- [ ] Add function calling discussion
- [ ] Add security/safety section
- [ ] Update references with 15+ papers from 2023-2025

### Priority 2 (Should Complete):
- [ ] Expand multimodal coverage
- [ ] Add fine-tuning decision framework
- [ ] Add prompt optimization techniques
- [ ] Strengthen evaluation methodology
- [ ] Add cost sensitivity analysis

### Priority 3 (Nice to Have):
- [ ] Add formal experimental design section
- [ ] Include more diagrams/figures
- [ ] Add glossary of LLM terms
- [ ] Include user survey methodology
- [ ] Add appendix with prompt templates

---

**Examiner:** [Your Name]
**Date:** 2026-02-28
**Next Review:** Post-Revisions (Estimated: 2026-03-15)

**Status:** **MINOR REVISIONS REQUIRED**
**Expected Timeline for Resubmission:** 3-4 weeks
