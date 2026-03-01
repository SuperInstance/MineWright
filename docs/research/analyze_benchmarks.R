#!/usr/bin/env Rscript

# analyze_benchmarks.R
# Statistical analysis for Steve AI evaluation framework
# Usage: Rscript analyze_benchmarks.R /path/to/results.json

# Load required libraries
if (!require("jsonlite", quietly = TRUE)) install.packages("jsonlite")
if (!require("ggplot2", quietly = TRUE)) install.packages("ggplot2")
if (!require("dplyr", quietly = TRUE)) install.packages("dplyr")
if (!require("effsize", quietly = TRUE)) install.packages("effsize")
if (!require("Hmisc", quietly = TRUE)) install.packages("Hmisc")

library(jsonlite)
library(ggplot2)
library(dplyr)
library(effsize)
library(Hmisc)

# Command line arguments
args <- commandArgs(trailingOnly = TRUE)

if (length(args) < 1) {
  stop("Usage: Rscript analyze_benchmarks.R <results.json> [output_dir]")
}

json_file <- args[1]
output_dir <- if (length(args) >= 2) args[2] else dirname(json_file)

# Create output directory if it doesn't exist
if (!dir.exists(output_dir)) {
  dir.create(output_dir, recursive = TRUE)
}

cat("Loading results from:", json_file, "\n")

# Load JSON data
data <- fromJSON(json_file)
tasks <- data$benchmark_run$tasks
summary <- data$benchmark_run$summary

if (is.null(tasks) || length(tasks) == 0) {
  stop("No tasks found in JSON file")
}

# Convert to data frame
df <- as.data.frame(tasks)

cat("Loaded", nrow(df), "task results\n\n")

# ============================================================================
# DESCRIPTIVE STATISTICS
# ============================================================================

cat("=" %+% rep("=", 70) %+% "\n")
cat("DESCRIPTIVE STATISTICS\n")
cat("=" %+% rep("=", 70) %+% "\n\n")

# Function to calculate statistics
calc_stats <- function(x) {
  list(
    n = length(x),
    mean = mean(x, na.rm = TRUE),
    sd = sd(x, na.rm = TRUE),
    median = median(x, na.rm = TRUE),
    min = min(x, na.rm = TRUE),
    max = max(x, na.rm = TRUE),
    q25 = quantile(x, 0.25, na.rm = TRUE),
    q75 = quantile(x, 0.75, na.rm = TRUE)
  )
}

# Planning latency stats
plan_stats <- calc_stats(df$planning_latency_ms)
cat("Planning Latency (ms):\n")
cat("  N:", plan_stats$n, "\n")
cat("  Mean:", round(plan_stats$mean, 2), "\n")
cat("  SD:", round(plan_stats$sd, 2), "\n")
cat("  Median:", round(plan_stats$median, 2), "\n")
cat("  Min:", plan_stats$min, "\n")
cat("  Max:", plan_stats$max, "\n")
cat("  IQR:", round(plan_stats$q25, 2), "-", round(plan_stats$q75, 2), "\n\n")

# Execution latency stats
exec_stats <- calc_stats(df$execution_latency_ms)
cat("Execution Latency (ms):\n")
cat("  N:", exec_stats$n, "\n")
cat("  Mean:", round(exec_stats$mean, 2), "\n")
cat("  SD:", round(exec_stats$sd, 2), "\n")
cat("  Median:", round(exec_stats$median, 2), "\n")
cat("  Min:", exec_stats$min, "\n")
cat("  Max:", exec_stats$max, "\n")
cat("  IQR:", round(exec_stats$q25, 2), "-", round(exec_stats$q75, 2), "\n\n")

# Cost stats
cost_stats <- calc_stats(df$llm_cost_usd)
cat("LLM Cost (USD):\n")
cat("  N:", cost_stats$n, "\n")
cat("  Mean:", round(cost_stats$mean, 4), "\n")
cat("  SD:", round(cost_stats$sd, 4), "\n")
cat("  Median:", round(cost_stats$median, 4), "\n")
cat("  Total:", round(sum(df$llm_cost_usd, na.rm = TRUE), 4), "\n\n")

# Token stats
token_stats <- calc_stats(df$input_tokens + df$output_tokens)
cat("Token Usage (total):\n")
cat("  N:", token_stats$n, "\n")
cat("  Mean:", round(token_stats$mean, 2), "\n")
cat("  SD:", round(token_stats$sd, 2), "\n")
cat("  Median:", round(token_stats$median, 2), "\n")
cat("  Total:", round(sum(df$input_tokens + df$output_tokens, na.rm = TRUE), 0), "\n\n")

# Success rate
success_count <- sum(df$success, na.rm = TRUE)
total_count <- nrow(df)
success_rate <- success_count / total_count
cat("Success Rate:\n")
cat("  Successful:", success_count, "/", total_count, "\n")
cat("  Rate:", round(success_rate * 100, 2), "%\n")

# Wilson score interval for success rate
ci <- binconf(success_count, total_count, method = "wilson")
cat("  95% CI:", round(ci$Lower * 100, 2), "-", round(ci$Upper * 100, 2), "%\n\n")

# Correctness score
correctness_stats <- calc_stats(df$correctness_score)
cat("Correctness Score:\n")
cat("  Mean:", round(correctness_stats$mean, 3), "\n")
cat("  SD:", round(correctness_stats$sd, 3), "\n")
cat("  Median:", round(correctness_stats$median, 3), "\n\n")

# ============================================================================
# VISUALIZATIONS
# ============================================================================

cat("Generating visualizations...\n")

# 1. Planning latency distribution
p1 <- ggplot(df, aes(x = planning_latency_ms / 1000)) +
  geom_histogram(binwidth = 1, fill = "steelblue", color = "black", alpha = 0.7) +
  labs(title = "Planning Latency Distribution",
       x = "Planning Latency (seconds)",
       y = "Frequency") +
  theme_minimal()
ggsave(file.path(output_dir, "planning_latency_distribution.png"), p1, width = 8, height = 6, dpi = 300)

# 2. Execution latency distribution
p2 <- ggplot(df, aes(x = execution_latency_ms / 1000)) +
  geom_histogram(binwidth = 10, fill = "coral", color = "black", alpha = 0.7) +
  labs(title = "Execution Latency Distribution",
       x = "Execution Latency (seconds)",
       y = "Frequency") +
  theme_minimal()
ggsave(file.path(output_dir, "execution_latency_distribution.png"), p2, width = 8, height = 6, dpi = 300)

# 3. Cost distribution
p3 <- ggplot(df, aes(x = llm_cost_usd * 100)) +
  geom_histogram(binwidth = 0.5, fill = "green", color = "black", alpha = 0.7) +
  labs(title = "Cost Distribution",
       x = "Cost (cents USD)",
       y = "Frequency") +
  theme_minimal()
ggsave(file.path(output_dir, "cost_distribution.png"), p3, width = 8, height = 6, dpi = 300)

# 4. Success vs failure
p4 <- ggplot(df, aes(x = success, fill = success)) +
  geom_bar() +
  scale_fill_manual(values = c("FALSE" = "red", "TRUE" = "green"),
                    labels = c("Failed", "Succeeded")) +
  labs(title = "Task Success Rate",
       x = "Outcome",
       y = "Count") +
  theme_minimal()
ggsave(file.path(output_dir, "success_rate.png"), p4, width = 6, height = 6, dpi = 300)

# 5. Correctness score distribution
p5 <- ggplot(df, aes(x = correctness_score)) +
  geom_histogram(binwidth = 0.1, fill = "purple", color = "black", alpha = 0.7) +
  labs(title = "Correctness Score Distribution",
       x = "Correctness Score",
       y = "Frequency") +
  theme_minimal() +
  scale_x_continuous(limits = c(0, 1), breaks = seq(0, 1, 0.2))
ggsave(file.path(output_dir, "correctness_distribution.png"), p5, width = 8, height = 6, dpi = 300)

# 6. Scatter plot: cost vs latency
p6 <- ggplot(df, aes(x = planning_latency_ms / 1000, y = llm_cost_usd * 100)) +
  geom_point(alpha = 0.6, size = 3) +
  geom_smooth(method = "lm", se = TRUE, color = "red") +
  labs(title = "Cost vs Planning Latency",
       x = "Planning Latency (seconds)",
       y = "Cost (cents USD)") +
  theme_minimal()
ggsave(file.path(output_dir, "cost_vs_latency.png"), p6, width = 8, height = 6, dpi = 300)

# 7. Box plot: latency by success
p7 <- ggplot(df, aes(x = as.factor(success), y = planning_latency_ms / 1000, fill = as.factor(success))) +
  geom_boxplot() +
  scale_fill_manual(values = c("FALSE" = "red", "TRUE" = "green"),
                    labels = c("Failed", "Succeeded"),
                    name = "Success") +
  labs(title = "Planning Latency by Success",
       x = "Outcome",
       y = "Planning Latency (seconds)") +
  theme_minimal()
ggsave(file.path(output_dir, "latency_by_success.png"), p7, width = 6, height = 6, dpi = 300)

# ============================================================================
# COMPARATIVE ANALYSIS (if multiple groups)
# ============================================================================

# Check for agent_name column to enable group comparison
if ("agent_name" %in% colnames(df)) {
  cat("\nPerforming multi-agent analysis...\n")

  # Summary by agent
  agent_summary <- df %>%
    group_by(agent_name) %>%
    summarise(
      n = n(),
      success_rate = mean(success),
      avg_plan_latency = mean(planning_latency_ms),
      avg_exec_latency = mean(execution_latency_ms),
      avg_cost = mean(llm_cost_usd),
      avg_correctness = mean(correctness_score)
    )

  cat("\nAgent Summary:\n")
  print(agent_summary)

  # ANOVA for planning latency
  if (length(unique(df$agent_name)) > 1) {
    anova_result <- aov(planning_latency_ms ~ agent_name, data = df)
    cat("\nANOVA for Planning Latency by Agent:\n")
    print(summary(anova_result))

    # Tukey HSD post-hoc test
    tukey_result <- TukeyHSD(anova_result)
    cat("\nTukey HSD Post-Hoc Test:\n")
    print(tukey_result)
  }
}

# ============================================================================
# CORRELATION ANALYSIS
# ============================================================================

cat("\nCorrelation Analysis:\n")

# Calculate correlations
correlations <- cor(df[, c("planning_latency_ms", "execution_latency_ms",
                           "llm_cost_usd", "correctness_score",
                           "actions_started", "actions_successful")],
                   use = "complete.obs")

cat("\nCorrelation Matrix:\n")
print(round(correlations, 3))

# ============================================================================
# EXPORT SUMMARY REPORT
# ============================================================================

cat("\nGenerating summary report...\n")

report_file <- file.path(output_dir, "benchmark_report.txt")
con <- file(report_file)

writeLines(c(
  "Steve AI Benchmark Evaluation Report",
  paste("Generated:", Sys.time()),
  paste("Run ID:", data$benchmark_run$run_id),
  paste("Total Tasks:", nrow(df)),
  "",
  "=== SUMMARY STATISTICS ===",
  "",
  paste("Planning Latency (ms):",
        "Mean =", round(plan_stats$mean, 2),
        "SD =", round(plan_stats$sd, 2),
        "Median =", round(plan_stats$median, 2)),
  "",
  paste("Execution Latency (ms):",
        "Mean =", round(exec_stats$mean, 2),
        "SD =", round(exec_stats$sd, 2),
        "Median =", round(exec_stats$median, 2)),
  "",
  paste("Total Cost: $", round(sum(df$llm_cost_usd, na.rm = TRUE), 4)),
  paste("Cost per Task: $", round(cost_stats$mean, 4), "(", round(cost_stats$sd, 4), ")"),
  "",
  paste("Success Rate:", round(success_rate * 100, 2), "%"),
  paste("95% CI:", round(ci$Lower * 100, 2), "-", round(ci$Upper * 100, 2), "%"),
  "",
  paste("Mean Correctness:", round(correctness_stats$mean, 3)),
  "",
  "=== FILES GENERATED ===",
  paste("- planning_latency_distribution.png"),
  paste("- execution_latency_distribution.png"),
  paste("- cost_distribution.png"),
  paste("- success_rate.png"),
  paste("- correctness_distribution.png"),
  paste("- cost_vs_latency.png"),
  paste("- latency_by_success.png"),
  "",
  "=== END OF REPORT ==="
), con)

close(con)

cat("\nAnalysis complete!\n")
cat("Results saved to:", output_dir, "\n")
cat("Report saved to:", report_file, "\n")

# ============================================================================
# HELPER FUNCTIONS
# ============================================================================

# String concatenation helper
`%+%` <- function(a, b) paste0(a, b)
