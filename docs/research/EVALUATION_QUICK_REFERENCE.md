# Evaluation Framework - Quick Reference Guide

**Version:** 1.0
**Last Updated:** 2025-02-28

---

## Quick Start

### 1. Run a Single Benchmark Trial

```java
// In your test or benchmark code
import com.minewright.evaluation.*;

// Reset metrics
EvaluationMetrics.reset();

// Record task start
EvaluationMetrics.recordPlanningStart("Steve-1", System.currentTimeMillis());

// Submit command
actionExecutor.processNaturalLanguageCommand("Mine 10 stone");

// Wait for completion...

// Record results
EvaluationMetrics.recordTaskComplete("Steve-1", true, 1.0, Map.of("blocks_mined", 10));

// Export results
EvaluationMetrics.exportToJson("/path/to/results.json");
```

### 2. Run Automated Benchmarks

```bash
# Set API key
export OPENAI_API_KEY="sk-..."

# Run all benchmarks
cd /path/to/steve
./docs/research/run_benchmarks.sh

# Results in: benchmark_results/run_<timestamp>/
```

### 3. Analyze Results

```bash
# Generate statistical report
Rscript docs/research/analyze_benchmarks.jar results.json output_dir/

# View report
cat output_dir/benchmark_report.txt
```

---

## Core Metrics at a Glance

| Metric | Definition | How to Measure | Target Claims |
|--------|------------|----------------|---------------|
| **Planning Latency** | Time from command to task queue | `recordPlanningStart()` → `recordPlanningComplete()` | <5s (async), 30-60s (sync) |
| **Execution Latency** | Time from queue to completion | `recordExecutionStart()` → action complete | Varies by task complexity |
| **API Cost** | LLM API call cost in USD | `PromptMetrics.calculateCost()` | 60-80% reduction with cascade |
| **Token Usage** | Input + output tokens | `PromptMetrics.estimateTokens()` | 40-60% reduction with cache |
| **Success Rate** | % tasks completed successfully | Count `ActionResult.success()` | >95% (simple), >70% (complex) |
| **Correctness** | Accuracy vs specifications | Manual verification | >90% (simple), >75% (complex) |

---

## Benchmark Scenarios

### Simple: Mine 10 Stone
- **Command:** "Mine 10 stone"
- **Complexity:** SIMPLE
- **Expected Time:** 20-40 seconds
- **Expected Actions:** 3-5
- **Expected Cost:** $0.001-0.01

### Medium: Build 5x5 House
- **Command:** "Build a 5x5 wooden house with 3 block high walls, a roof, and a door"
- **Complexity:** MODERATE
- **Expected Time:** 120-300 seconds
- **Expected Actions:** 50-100
- **Expected Cost:** $0.005-0.02

### Complex: Automated Farm
- **Command:** "Build an automated wheat farm with water for hydration, farmland, seeds, and a hopper collection system with a chest"
- **Complexity:** COMPLEX
- **Expected Time:** 300-600 seconds
- **Expected Actions:** 200-500
- **Expected Cost:** $0.01-0.05

### Multi-Agent: Village
- **Command:** "Coordinate 3 workers to build a small village with 3 houses, a central plaza, and paths connecting them"
- **Complexity:** COMPLEX + MULTI-AGENT
- **Expected Time:** 600-900 seconds
- **Expected Actions:** 500-1000
- **Expected Cost:** $0.03-0.10

---

## Evaluation Checklist

### Before Running Benchmarks

- [ ] Set `OPENAI_API_KEY` environment variable
- [ ] Verify Minecraft Forge 1.20.1 is installed
- [ ] Build project: `./gradlew clean build`
- [ ] Copy mod to Minecraft mods directory
- [ ] Create fresh superflat world (seed: 123456789)
- [ ] Set difficulty to Peaceful
- [ ] Set time to Day
- [ ] Clear weather

### During Benchmarks

- [ ] Start recording (video/metrics)
- [ ] Submit command
- [ ] Monitor for errors
- [ ] Wait for completion or timeout (10 min max)
- [ ] Take screenshots of final state
- [ ] Verify correctness manually

### After Benchmarks

- [ ] Export metrics to JSON
- [ ] Verify data integrity
- [ ] Run statistical analysis
- [ ] Generate visualizations
- [ ] Document any anomalies

---

## Comparison Groups

| Group | Description | Key Features |
|-------|-------------|--------------|
| **Steve AI (Baseline)** | GPT-4 only, caching enabled | Single model, cache hits |
| **Steve AI (Cascade)** | Multi-tier routing | FAST/BALANCED/SMART tiers |
| **ReAct** | Iterative LLM calls | One action per LLM call |
| **Pure LLM** | Full tick-by-tick generation | Maximum flexibility, high cost |
| **Scripted AI** | No LLM, rule-based | Zero cost, low flexibility |

---

## Statistical Tests

### Two Groups (e.g., Steve AI vs ReAct)

```r
# R code
t.test(group_a$cost, group_b$cost, alternative = "less")

# Calculate effect size
library(effsize)
cohen.d(group_a$cost, group_b$cost)
```

### Multiple Groups (ANOVA)

```r
# R code
model <- aov(latency ~ group, data = df)
summary(model)

# Post-hoc test
TukeyHSD(model)
```

### Success Rate (Proportion)

```r
# R code
library(Hmisc)
binconf(successes, trials, method = "wilson")
```

---

## Common Issues and Solutions

### Issue: High planning latency
- **Cause:** Cache not enabled or low hit rate
- **Solution:** Enable caching, increase cache size
- **Expected:** <5s with cache, 30-60s without

### Issue: High API costs
- **Cause:** Using GPT-4 for all tasks
- **Solution:** Enable cascade routing
- **Expected:** 60-80% cost reduction

### Issue: Low success rate
- **Cause:** Insufficient context or unclear prompts
- **Solution:** Improve prompt engineering, add more context
- **Expected:** >95% (simple), >70% (complex)

### Issue: Memory leaks
- **Cause:** Metrics not properly cleared between trials
- **Solution:** Call `EvaluationMetrics.reset()` before each trial
- **Expected:** Stable memory usage over 100+ trials

---

## File Locations

### Code
- `src/main/java/com/minewright/evaluation/EvaluationMetrics.java`
- `src/main/java/com/minewright/evaluation/BenchmarkScenarios.java`

### Documentation
- `docs/research/EVALUATION_FRAMEWORK.md` - Complete framework documentation
- `docs/research/EVALUATION_QUICK_REFERENCE.md` - This file

### Scripts
- `docs/research/run_benchmarks.sh` - Automated benchmark runner
- `docs/research/analyze_benchmarks.R` - Statistical analysis script

### Output
- `benchmark_results/run_<timestamp>/` - Results directory
  - `all_metrics.json` - Combined metrics
  - `reports/benchmark_report.txt` - Text summary
  - `reports/*.png` - Visualizations

---

## Data Export Formats

### JSON Schema
```json
{
  "benchmark_run": {
    "run_id": "uuid",
    "timestamp": "ISO-8601",
    "summary": {
      "total_tasks": N,
      "success_rate": 0.XX,
      "avg_planning_latency_ms": XXXX,
      "avg_execution_latency_ms": XXXX,
      "total_llm_cost_usd": X.XX,
      "total_tokens": N,
      "cache_hit_rate": 0.XX
    },
    "tasks": [...]
  }
}
```

### CSV Export
```csv
Scenario,Trial,Completed,PlanningLatencyMs,ExecutionLatencyMs,Success,Correctness,CostUsd
Simple: Mine 10 Stone,1,true,2340,28500,true,1.0,0.0045
...
```

---

## Academic Publication Checklist

### For Dissertation

- [ ] Define hypotheses clearly
- [ ] Specify sample sizes with power analysis
- [ ] Report effect sizes (Cohen's d)
- [ ] Include confidence intervals
- [ ] Use appropriate statistical tests
- [ ] Document all experimental conditions
- [ ] Make data reproducible (share config files)
- [ ] Include visualizations (histograms, box plots)

### For Conference/Journal Paper

- [ ] Abstract with key metrics
- [ ] Methodology section with experimental design
- [ ] Results section with tables and figures
- [ ] Discussion section interpreting results
- [ ] Related work comparison
- [ ] Limitations and future work
- [ ] Supplemental material with raw data

---

## Contact and Support

For questions about the evaluation framework:
- Review `EVALUATION_FRAMEWORK.md` for detailed documentation
- Check existing benchmarks in `BenchmarkScenarios.java`
- Examine example results in `benchmark_results/` (if available)

---

**End of Quick Reference**
