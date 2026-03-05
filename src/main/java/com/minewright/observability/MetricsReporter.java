package com.minewright.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Report generation for the MineWright observability system.
 * Creates human-readable text reports and machine-readable exports.
 *
 * <p>This reporter provides:
 * <ul>
 *   <li>Text reports for console and in-game display</li>
 *   <li>JSON export for external dashboards and analysis tools</li>
 *   <li>CSV export for spreadsheet analysis</li>
 *   <li>Complete report packages combining metrics and traces</li>
 * </ul>
 *
 * <p>All JSON serialization is implemented manually without external
 * dependencies to maintain the zero-dependency requirement.
 *
 * <p>Usage example:
 * <pre>{@code
 * MetricsReporter reporter = new MetricsReporter();
 * String textReport = reporter.generateTextReport();
 * reporter.exportToJson("/path/to/export", "report");
 * }</pre>
 */
public class MetricsReporter {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetricsReporter.class);
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MetricsCollector collector;
    private final TracingService tracingService;

    /**
     * Creates a new metrics reporter using the default singleton instances.
     */
    public MetricsReporter() {
        this.collector = MetricsCollector.getInstance();
        this.tracingService = TracingService.getInstance();
    }

    /**
     * Creates a new metrics reporter with specific instances.
     *
     * @param collector the metrics collector
     * @param tracingService the tracing service
     */
    public MetricsReporter(MetricsCollector collector, TracingService tracingService) {
        this.collector = collector;
        this.tracingService = tracingService;
    }

    /**
     * Generates a human-readable text report.
     *
     * @return the formatted text report
     */
    public String generateTextReport() {
        StringBuilder report = new StringBuilder();
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);

        report.append("╔═══════════════════════════════════════════════════════════════════╗\n");
        report.append("║           MineWright Observability Report                        ║\n");
        report.append("║           ").append(timestamp).append("                       ║\n");
        report.append("╚═══════════════════════════════════════════════════════════════════╝\n\n");

        // System Health Section
        report.append(generateSectionHeader("System Health"));
        report.append(generateSystemHealthSection());

        // Agent Metrics Section
        report.append(generateSectionHeader("Agent Metrics"));
        report.append(generateAgentMetricsSection());

        // LLM Metrics Section
        report.append(generateSectionHeader("LLM Metrics"));
        report.append(generateLLMMetricsSection());

        // Skill Metrics Section
        report.append(generateSectionHeader("Skill Metrics"));
        report.append(generateSkillMetricsSection());

        // Tracing Statistics Section
        report.append(generateSectionHeader("Tracing Statistics"));
        report.append(generateTracingStatsSection());

        report.append("═════════════════════════════════════════════════════════════════════\n");
        report.append("End of Report\n");

        return report.toString();
    }

    /**
     * Generates a concise text report suitable for in-game display.
     *
     * @return the concise report
     */
    public String generateConciseReport() {
        StringBuilder report = new StringBuilder();
        Map<String, Object> systemHealth = collector.getSystemHealthSummary();
        Map<String, Object> llmSummary = collector.getLLMSummary();

        report.append("§6=== MineWright Status ===§r\n");
        report.append(String.format("§7Uptime:§r %.1f min\n",
            (double) systemHealth.get("uptime_seconds") / 60.0));
        report.append(String.format("§7Memory:§r %.1f%% (%d/%d MB)\n",
            systemHealth.get("memory_usage_percent"),
            systemHealth.get("used_memory_mb"),
            systemHealth.get("max_memory_mb")));
        report.append(String.format("§7Errors:§r %d  §7Warnings:§r %d\n",
            systemHealth.get("error_count"),
            systemHealth.get("warning_count")));
        report.append(String.format("§7LLM Calls:§r %d  §7Tokens:§r %d  §7Cost:§r $%.4f\n",
            llmSummary.get("total_calls"),
            llmSummary.get("total_tokens"),
            llmSummary.get("total_cost_usd")));
        report.append(String.format("§7Traces:§r %d queued\n",
            tracingService.getCompletedSpanCount()));

        return report.toString();
    }

    /**
     * Exports metrics to JSON format.
     *
     * @param exportPath the directory to export to
     * @param reportName the name of the report (without extension)
     * @return the path to the exported file
     * @throws IOException if export fails
     */
    public String exportToJson(String exportPath, String reportName) throws IOException {
        Path dir = Paths.get(exportPath);
        Files.createDirectories(dir);

        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        Path file = dir.resolve(reportName + "_" + timestamp + ".json");

        String json = generateJsonReport();
        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            writer.write(json);
        }

        LOGGER.info("Exported metrics report to JSON: {}", file);
        return file.toString();
    }

    /**
     * Exports metrics to CSV format.
     *
     * @param exportPath the directory to export to
     * @param reportName the name of the report (without extension)
     * @return the path to the exported file
     * @throws IOException if export fails
     */
    public String exportToCsv(String exportPath, String reportName) throws IOException {
        Path dir = Paths.get(exportPath);
        Files.createDirectories(dir);

        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        Path file = dir.resolve(reportName + "_" + timestamp + ".csv");

        String csv = generateCsvReport();
        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            writer.write(csv);
        }

        LOGGER.info("Exported metrics report to CSV: {}", file);
        return file.toString();
    }

    /**
     * Exports a complete report package including metrics and traces.
     *
     * @param exportPath the directory to export to
     * @param packageName the name of the package
     * @return the path to the package directory
     * @throws IOException if export fails
     */
    public String exportCompletePackage(String exportPath, String packageName) throws IOException {
        Path dir = Paths.get(exportPath);
        Files.createDirectories(dir);

        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        Path packageDir = dir.resolve(packageName + "_" + timestamp);
        Files.createDirectories(packageDir);

        // Export metrics JSON
        exportToJson(packageDir.toString(), "metrics");

        // Export metrics CSV
        exportToCsv(packageDir.toString(), "metrics");

        // Export traces JSON
        tracingService.exportToJson(packageDir.toString());

        // Export traces CSV
        tracingService.exportToCsv(packageDir.toString());

        // Export text report
        Path reportFile = packageDir.resolve("report.txt");
        try (BufferedWriter writer = Files.newBufferedWriter(reportFile)) {
            writer.write(generateTextReport());
        }

        // Export event log
        exportEventLog(packageDir.toString());

        LOGGER.info("Exported complete report package to: {}", packageDir);
        return packageDir.toString();
    }

    /**
     * Generates a JSON report.
     *
     * @return the JSON string
     */
    private String generateJsonReport() {
        StringBuilder json = new StringBuilder();
        Map<String, Object> summary = collector.getCompleteSummary();

        json.append("{\n");
        json.append("  \"generated_at\": \"").append(LocalDateTime.now().format(DATE_FORMATTER)).append("\",\n");
        json.append("  \"system_health\": ").append(mapToJson((Map<String, Object>) summary.get("system_health"))).append(",\n");
        json.append("  \"agents\": ").append(mapToJson(summary.get("agents"))).append(",\n");
        json.append("  \"llm\": ").append(mapToJson(summary.get("llm"))).append(",\n");
        json.append("  \"skills\": ").append(mapToJson(summary.get("skills"))).append(",\n");
        json.append("  \"tracing\": ").append(tracingStatsToJson()).append(",\n");
        json.append("  \"recent_events\": ").append(eventLogToJson()).append("\n");
        json.append("}");

        return json.toString();
    }

    /**
     * Generates a CSV report.
     *
     * @return the CSV string
     */
    private String generateCsvReport() {
        StringBuilder csv = new StringBuilder();
        Map<String, Object> summary = collector.getCompleteSummary();

        // System Health Section
        csv.append("=== SYSTEM HEALTH ===\n");
        Map<String, Object> health = (Map<String, Object>) summary.get("system_health");
        for (Map.Entry<String, Object> entry : health.entrySet()) {
            csv.append(entry.getKey()).append(",").append(escapeCsv(entry.getValue().toString())).append("\n");
        }
        csv.append("\n");

        // LLM Metrics Section
        csv.append("=== LLM METRICS ===\n");
        Map<String, Object> llm = (Map<String, Object>) summary.get("llm");
        csv.append("total_calls,").append(llm.get("total_calls")).append("\n");
        csv.append("total_tokens,").append(llm.get("total_tokens")).append("\n");
        csv.append("total_cost_usd,").append(llm.get("total_cost_usd")).append("\n");
        csv.append("avg_latency_ms,").append(llm.get("avg_latency_ms")).append("\n");

        Map<String, Object> byModel = (Map<String, Object>) llm.get("by_model");
        csv.append("\nMODEL BREAKDOWN\n");
        csv.append("model,calls,tokens,prompt_tokens,completion_tokens,cost_usd,avg_latency_ms\n");
        for (Map.Entry<String, Object> entry : byModel.entrySet()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> modelMetrics = (Map<String, Object>) entry.getValue();
            csv.append(entry.getKey()).append(",")
               .append(modelMetrics.get("calls")).append(",")
               .append(modelMetrics.get("tokens")).append(",")
               .append(modelMetrics.get("prompt_tokens")).append(",")
               .append(modelMetrics.get("completion_tokens")).append(",")
               .append(modelMetrics.get("cost_usd")).append(",")
               .append(modelMetrics.get("avg_latency_ms")).append("\n");
        }
        csv.append("\n");

        // Skill Metrics Section
        csv.append("=== SKILL METRICS ===\n");
        csv.append("skill,usage_count,success_count,success_rate,avg_time_ms\n");
        Map<String, Object> skills = (Map<String, Object>) summary.get("skills");
        for (Map.Entry<String, Object> entry : skills.entrySet()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> skillMetrics = (Map<String, Object>) entry.getValue();
            csv.append(entry.getKey()).append(",")
               .append(skillMetrics.get("usage_count")).append(",")
               .append(skillMetrics.get("success_count")).append(",")
               .append(String.format("%.2f", skillMetrics.get("success_rate"))).append(",")
               .append(String.format("%.2f", skillMetrics.get("avg_time_ms"))).append("\n");
        }

        return csv.toString();
    }

    /**
     * Exports the event log to JSON.
     *
     * @param exportPath the directory to export to
     * @throws IOException if export fails
     */
    private void exportEventLog(String exportPath) throws IOException {
        Path file = Paths.get(exportPath, "event_log.json");
        String json = eventLogToJson();
        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            writer.write(json);
        }
    }

    // ========== SECTION GENERATORS ==========

    private String generateSectionHeader(String title) {
        return String.format("\n╠═══ %s ═══\n", title);
    }

    private String generateSystemHealthSection() {
        Map<String, Object> health = collector.getSystemHealthSummary();
        StringBuilder section = new StringBuilder();

        section.append(String.format("  Uptime:        %.1f minutes\n", (double) health.get("uptime_seconds") / 60.0));
        section.append(String.format("  Memory Usage:  %.1f%% (%d / %d MB)\n",
            health.get("memory_usage_percent"),
            health.get("used_memory_mb"),
            health.get("max_memory_mb")));
        section.append(String.format("  Peak Memory:   %d MB\n", health.get("peak_memory_mb")));
        section.append(String.format("  CPUs:          %d\n", health.get("available_processors")));
        section.append(String.format("  Errors:        %d\n", health.get("error_count")));
        section.append(String.format("  Warnings:      %d\n", health.get("warning_count")));
        section.append("\n");

        return section.toString();
    }

    private String generateAgentMetricsSection() {
        Map<String, Object> agents = collector.getAgentSummary();
        StringBuilder section = new StringBuilder();

        if (agents.isEmpty()) {
            section.append("  No agent data available.\n\n");
            return section.toString();
        }

        for (Map.Entry<String, Object> entry : agents.entrySet()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> agentMetrics = (Map<String, Object>) entry.getValue();
            section.append(String.format("  Agent: %s\n", entry.getKey()));
            section.append(String.format("    Tasks:        %d\n", agentMetrics.get("total_tasks")));
            section.append(String.format("    Successes:    %d\n", agentMetrics.get("total_successes")));
            section.append(String.format("    Success Rate: %.1f%%\n",
                (double) agentMetrics.get("success_rate") * 100.0));
            section.append(String.format("    Avg Task Time: %.2f ms\n", agentMetrics.get("avg_task_time_ms")));
            section.append("\n");
        }

        return section.toString();
    }

    private String generateLLMMetricsSection() {
        Map<String, Object> llm = collector.getLLMSummary();
        StringBuilder section = new StringBuilder();

        section.append(String.format("  Total Calls:    %d\n", llm.get("total_calls")));
        section.append(String.format("  Total Tokens:   %d\n", llm.get("total_tokens")));
        section.append(String.format("  Total Cost:     $%.4f\n", llm.get("total_cost_usd")));
        section.append(String.format("  Avg Latency:    %.2f ms\n", llm.get("avg_latency_ms")));
        section.append("\n");

        @SuppressWarnings("unchecked")
        Map<String, Object> byModel = (Map<String, Object>) llm.get("by_model");
        if (!byModel.isEmpty()) {
            section.append("  Per-Model Breakdown:\n");
            for (Map.Entry<String, Object> entry : byModel.entrySet()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> modelMetrics = (Map<String, Object>) entry.getValue();
                section.append(String.format("    %s:\n", entry.getKey()));
                section.append(String.format("      Calls:   %d\n", modelMetrics.get("calls")));
                section.append(String.format("      Tokens:  %d (%d prompt + %d completion)\n",
                    modelMetrics.get("tokens"),
                    modelMetrics.get("prompt_tokens"),
                    modelMetrics.get("completion_tokens")));
                section.append(String.format("      Cost:    $%.4f\n", modelMetrics.get("cost_usd")));
                section.append(String.format("      Latency: %.2f ms avg\n", modelMetrics.get("avg_latency_ms")));
            }
            section.append("\n");
        }

        return section.toString();
    }

    private String generateSkillMetricsSection() {
        Map<String, Object> skills = collector.getSkillSummary();
        StringBuilder section = new StringBuilder();

        if (skills.isEmpty()) {
            section.append("  No skill data available.\n\n");
            return section.toString();
        }

        // Sort by usage count
        List<Map.Entry<String, Object>> sortedSkills = new ArrayList<>(skills.entrySet());
        sortedSkills.sort((e1, e2) -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> m1 = (Map<String, Object>) e1.getValue();
            @SuppressWarnings("unchecked")
            Map<String, Object> m2 = (Map<String, Object>) e2.getValue();
            return Long.compare((Long) m2.get("usage_count"), (Long) m1.get("usage_count"));
        });

        section.append(String.format("  %-20s %10s %10s %10s %12s\n",
            "Skill", "Usage", "Success", "Success %", "Avg Time (ms)"));
        section.append("  ").append("─".repeat(70)).append("\n");

        for (Map.Entry<String, Object> entry : sortedSkills) {
            @SuppressWarnings("unchecked")
            Map<String, Object> skillMetrics = (Map<String, Object>) entry.getValue();
            section.append(String.format("  %-20s %10d %10d %9.1f%% %12.2f\n",
                entry.getKey(),
                skillMetrics.get("usage_count"),
                skillMetrics.get("success_count"),
                (double) skillMetrics.get("success_rate") * 100.0,
                skillMetrics.get("avg_time_ms")));
        }
        section.append("\n");

        return section.toString();
    }

    private String generateTracingStatsSection() {
        Map<String, Long> stats = tracingService.getStatistics();
        StringBuilder section = new StringBuilder();

        section.append(String.format("  Total Spans Created:  %d\n", stats.get("total_spans_created")));
        section.append(String.format("  Total Spans Completed: %d\n", stats.get("total_spans_completed")));
        section.append(String.format("  Total Spans Errored:   %d\n", stats.get("total_spans_errored")));
        section.append(String.format("  Queued Spans:         %d\n", stats.get("queued_spans")));
        section.append("\n");

        return section.toString();
    }

    // ========== JSON SERIALIZATION HELPERS ==========

    private String mapToJson(Object obj) {
        if (obj == null) {
            return "null";
        } else if (obj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) obj;
            if (map.isEmpty()) {
                return "{}";
            }
            StringBuilder json = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (!first) json.append(",");
                first = false;
                json.append("\"").append(escapeJson(entry.getKey())).append("\":");
                json.append(valueToJson(entry.getValue()));
            }
            json.append("}");
            return json.toString();
        } else if (obj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) obj;
            if (list.isEmpty()) {
                return "[]";
            }
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) json.append(",");
                json.append(valueToJson(list.get(i)));
            }
            json.append("]");
            return json.toString();
        } else {
            return valueToJson(obj);
        }
    }

    private String valueToJson(Object value) {
        if (value == null) {
            return "null";
        } else if (value instanceof String) {
            return "\"" + escapeJson((String) value) + "\"";
        } else if (value instanceof Number) {
            return value.toString();
        } else if (value instanceof Boolean) {
            return value.toString();
        } else if (value instanceof Map) {
            return mapToJson(value);
        } else if (value instanceof List) {
            return mapToJson(value);
        } else {
            return "\"" + escapeJson(value.toString()) + "\"";
        }
    }

    private String tracingStatsToJson() {
        Map<String, Long> stats = tracingService.getStatistics();
        return mapToJson(stats);
    }

    private String eventLogToJson() {
        List<MetricsCollector.MetricEvent> events = collector.getEventLog(100);
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < events.size(); i++) {
            if (i > 0) json.append(",");
            MetricsCollector.MetricEvent event = events.get(i);
            json.append("{");
            json.append("\"timestamp\":\"").append(escapeJson(event.getTimestamp().toString())).append("\",");
            json.append("\"type\":\"").append(escapeJson(event.getType())).append("\",");
            json.append("\"attributes\":").append(mapToJson(event.getAttributes()));
            json.append("}");
        }
        json.append("]");
        return json.toString();
    }

    /**
     * Escapes special characters in JSON strings.
     */
    private String escapeJson(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    /**
     * Escapes a value for CSV format.
     */
    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
