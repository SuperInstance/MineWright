package com.minewright.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Unified configuration for the observability system.
 * Loads settings from properties files with sensible defaults.
 *
 * <p>This configuration manager provides:
 * <ul>
 *   <li>Loading from observability.properties</li>
 *   <li>Enable/disable flags for tracing and metrics</li>
 *   <li>Retention policies for data</li>
 *   <li>Export directory configuration</li>
 *   <li>Scheduled export tasks</li>
 *   <li>Graceful shutdown</li>
 * </ul>
 *
 * <p>Configuration is loaded from:
 * <ol>
 *   <li>System properties (-D flag)</li>
 *   <li>Environment variables</li>
 *   <li>observability.properties file</li>
 *   <li>Default values</li>
 * </ol>
 *
 * <p>Usage example:
 * <pre>{@code
 * ObservabilityConfig config = ObservabilityConfig.loadDefault();
 * if (config.isMetricsEnabled()) {
 *     MetricsCollector.getInstance().recordLLMCall(...);
 * }
 * }</pre>
 */
public class ObservabilityConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(ObservabilityConfig.class);
    private static final String DEFAULT_CONFIG_FILE = "observability.properties";
    private static final String DEFAULT_EXPORT_DIR = "observability/exports";

    // ========== TRACING CONFIGURATION ==========
    private boolean tracingEnabled = true;
    private boolean llmTracingEnabled = true;
    private boolean skillTracingEnabled = true;
    private boolean contractNetTracingEnabled = true;
    private long tracingRetentionHours = 24;

    // ========== METRICS CONFIGURATION ==========
    private boolean metricsEnabled = true;
    private boolean agentMetricsEnabled = true;
    private boolean llmMetricsEnabled = true;
    private boolean skillMetricsEnabled = true;
    private boolean systemHealthEnabled = true;
    private long metricsRetentionHours = 72;

    // ========== EXPORT CONFIGURATION ==========
    private boolean jsonExportEnabled = true;
    private boolean csvExportEnabled = true;
    private String exportDirectory = DEFAULT_EXPORT_DIR;
    private int exportQueueSize = 10000;
    private boolean autoExportEnabled = true;
    private long autoExportIntervalMinutes = 5;
    private boolean exportOnShutdown = true;

    // ========== SCHEDULER ==========
    private ScheduledExecutorService scheduler;
    private ExportTask exportTask;

    /** Singleton instance */
    private static volatile ObservabilityConfig instance;
    private static final Object INSTANCE_LOCK = new Object();

    /**
     * Gets the singleton observability configuration instance.
     *
     * @return the configuration instance
     */
    public static ObservabilityConfig getInstance() {
        if (instance == null) {
            synchronized (INSTANCE_LOCK) {
                if (instance == null) {
                    instance = loadDefault();
                }
            }
        }
        return instance;
    }

    /**
     * Loads the default configuration from observability.properties.
     *
     * @return the loaded configuration
     */
    public static ObservabilityConfig loadDefault() {
        ObservabilityConfig config = new ObservabilityConfig();
        try {
            Path configPath = Paths.get(DEFAULT_CONFIG_FILE);
            if (Files.exists(configPath)) {
                config.loadFromProperties(DEFAULT_CONFIG_FILE);
                LOGGER.info("Loaded observability configuration from {}", DEFAULT_CONFIG_FILE);
            } else {
                LOGGER.info("No observability.properties found, using defaults");
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to load observability.properties, using defaults: {}", e.getMessage());
        }
        return config;
    }

    /**
     * Creates a new observability configuration with defaults.
     */
    public ObservabilityConfig() {}

    /**
     * Loads configuration from a properties file.
     *
     * @param configPath the path to the properties file
     * @throws IOException if loading fails
     */
    public ObservabilityConfig(String configPath) throws IOException {
        loadFromProperties(configPath);
    }

    /**
     * Loads configuration from a properties file.
     *
     * @param configPath the path to the properties file
     * @throws IOException if loading fails
     */
    public void loadFromProperties(String configPath) throws IOException {
        Properties props = new Properties();
        try (InputStream input = Files.newInputStream(Paths.get(configPath))) {
            props.load(input);
        }

        // Tracing settings
        this.tracingEnabled = parseBoolean(getProperty(props, "tracing.enabled", "true"));
        this.llmTracingEnabled = parseBoolean(getProperty(props, "tracing.llm.enabled", "true"));
        this.skillTracingEnabled = parseBoolean(getProperty(props, "tracing.skill.enabled", "true"));
        this.contractNetTracingEnabled = parseBoolean(getProperty(props, "tracing.contractNet.enabled", "true"));
        this.tracingRetentionHours = parseLong(getProperty(props, "tracing.retentionHours", "24"), 24);

        // Metrics settings
        this.metricsEnabled = parseBoolean(getProperty(props, "metrics.enabled", "true"));
        this.agentMetricsEnabled = parseBoolean(getProperty(props, "metrics.agent.enabled", "true"));
        this.llmMetricsEnabled = parseBoolean(getProperty(props, "metrics.llm.enabled", "true"));
        this.skillMetricsEnabled = parseBoolean(getProperty(props, "metrics.skill.enabled", "true"));
        this.systemHealthEnabled = parseBoolean(getProperty(props, "metrics.systemHealth.enabled", "true"));
        this.metricsRetentionHours = parseLong(getProperty(props, "metrics.retentionHours", "72"), 72);

        // Export settings
        this.jsonExportEnabled = parseBoolean(getProperty(props, "export.json.enabled", "true"));
        this.csvExportEnabled = parseBoolean(getProperty(props, "export.csv.enabled", "true"));
        this.exportDirectory = getProperty(props, "export.directory", DEFAULT_EXPORT_DIR);
        this.exportQueueSize = parseInt(getProperty(props, "export.queueSize", "10000"), 10000);
        this.autoExportEnabled = parseBoolean(getProperty(props, "export.auto.enabled", "true"));
        this.autoExportIntervalMinutes = parseLong(getProperty(props, "export.auto.intervalMinutes", "5"), 5);
        this.exportOnShutdown = parseBoolean(getProperty(props, "export.onShutdown", "true"));

        LOGGER.debug("Loaded configuration: {}", this);
    }

    /**
     * Gets a property from the properties object, checking system properties first.
     *
     * @param props the properties object
     * @param key the property key
     * @param defaultValue the default value
     * @return the property value
     */
    private String getProperty(Properties props, String key, String defaultValue) {
        // Check system property first
        String systemValue = System.getProperty(key);
        if (systemValue != null) {
            return systemValue;
        }

        // Check environment variable
        String envKey = key.toUpperCase().replace(".", "_");
        String envValue = System.getenv(envKey);
        if (envValue != null) {
            return envValue;
        }

        // Fall back to properties file
        return props.getProperty(key, defaultValue);
    }

    /**
     * Parses a boolean value.
     *
     * @param value the string value
     * @return the parsed boolean
     */
    private boolean parseBoolean(String value) {
        if (value == null) return false;
        String lower = value.trim().toLowerCase();
        return lower.equals("true") || lower.equals("1") || lower.equals("yes") || lower.equals("on");
    }

    /**
     * Parses a long value with a default.
     *
     * @param value the string value
     * @param defaultValue the default value
     * @return the parsed long
     */
    private long parseLong(String value, long defaultValue) {
        if (value == null || value.trim().isEmpty()) return defaultValue;
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid long value: '{}', using default: {}", value, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Parses an int value with a default.
     *
     * @param value the string value
     * @param defaultValue the default value
     * @return the parsed int
     */
    private int parseInt(String value, int defaultValue) {
        if (value == null || value.trim().isEmpty()) return defaultValue;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid int value: '{}', using default: {}", value, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Initializes the scheduled export task if auto-export is enabled.
     */
    public void initializeScheduledExport() {
        if (!autoExportEnabled) {
            LOGGER.info("Auto-export is disabled");
            return;
        }

        if (scheduler != null && !scheduler.isShutdown()) {
            LOGGER.warn("Scheduler already initialized");
            return;
        }

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "observability-export");
            t.setDaemon(true);
            return t;
        });

        exportTask = new ExportTask();
        scheduler.scheduleAtFixedRate(
            exportTask,
            autoExportIntervalMinutes,
            autoExportIntervalMinutes,
            TimeUnit.MINUTES
        );

        LOGGER.info("Scheduled export task every {} minutes", autoExportIntervalMinutes);
    }

    /**
     * Shuts down the scheduled export task gracefully.
     */
    public void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            LOGGER.info("Shutting down observability scheduler");
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // Export on shutdown if enabled
        if (exportOnShutdown) {
            LOGGER.info("Exporting observability data on shutdown");
            exportTask.run();
        }
    }

    /**
     * Creates a TracingConfig instance from this configuration.
     *
     * @return the tracing configuration
     */
    public TracingConfig toTracingConfig() {
        TracingConfig config = new TracingConfig();
        config.setEnabled(tracingEnabled);
        config.setLLMTracingEnabled(llmTracingEnabled);
        config.setSkillTracingEnabled(skillTracingEnabled);
        config.setContractNetTracingEnabled(contractNetTracingEnabled);
        config.setJsonExportEnabled(jsonExportEnabled);
        config.setCsvExportEnabled(csvExportEnabled);
        config.setExportDirectory(exportDirectory);
        config.setRetentionHours(tracingRetentionHours);
        config.setExportQueueSize(exportQueueSize);
        config.setAutoExportEnabled(autoExportEnabled);
        config.setAutoExportIntervalMinutes(autoExportIntervalMinutes);
        return config;
    }

    // ========== GETTERS AND SETTERS ==========

    public boolean isTracingEnabled() { return tracingEnabled; }
    public void setTracingEnabled(boolean enabled) { this.tracingEnabled = enabled; }

    public boolean isLLMTracingEnabled() { return llmTracingEnabled; }
    public void setLLMTracingEnabled(boolean enabled) { this.llmTracingEnabled = enabled; }

    public boolean isSkillTracingEnabled() { return skillTracingEnabled; }
    public void setSkillTracingEnabled(boolean enabled) { this.skillTracingEnabled = enabled; }

    public boolean isContractNetTracingEnabled() { return contractNetTracingEnabled; }
    public void setContractNetTracingEnabled(boolean enabled) { this.contractNetTracingEnabled = enabled; }

    public long getTracingRetentionHours() { return tracingRetentionHours; }
    public void setTracingRetentionHours(long hours) { this.tracingRetentionHours = hours; }

    public boolean isMetricsEnabled() { return metricsEnabled; }
    public void setMetricsEnabled(boolean enabled) { this.metricsEnabled = enabled; }

    public boolean isAgentMetricsEnabled() { return agentMetricsEnabled; }
    public void setAgentMetricsEnabled(boolean enabled) { this.agentMetricsEnabled = enabled; }

    public boolean isLLMMetricsEnabled() { return llmMetricsEnabled; }
    public void setLLMMetricsEnabled(boolean enabled) { this.llmMetricsEnabled = enabled; }

    public boolean isSkillMetricsEnabled() { return skillMetricsEnabled; }
    public void setSkillMetricsEnabled(boolean enabled) { this.skillMetricsEnabled = enabled; }

    public boolean isSystemHealthEnabled() { return systemHealthEnabled; }
    public void setSystemHealthEnabled(boolean enabled) { this.systemHealthEnabled = enabled; }

    public long getMetricsRetentionHours() { return metricsRetentionHours; }
    public void setMetricsRetentionHours(long hours) { this.metricsRetentionHours = hours; }

    public boolean isJsonExportEnabled() { return jsonExportEnabled; }
    public void setJsonExportEnabled(boolean enabled) { this.jsonExportEnabled = enabled; }

    public boolean isCsvExportEnabled() { return csvExportEnabled; }
    public void setCsvExportEnabled(boolean enabled) { this.csvExportEnabled = enabled; }

    public String getExportDirectory() { return exportDirectory; }
    public void setExportDirectory(String directory) { this.exportDirectory = directory; }

    public int getExportQueueSize() { return exportQueueSize; }
    public void setExportQueueSize(int size) { this.exportQueueSize = size; }

    public boolean isAutoExportEnabled() { return autoExportEnabled; }
    public void setAutoExportEnabled(boolean enabled) { this.autoExportEnabled = enabled; }

    public long getAutoExportIntervalMinutes() { return autoExportIntervalMinutes; }
    public void setAutoExportIntervalMinutes(long minutes) { this.autoExportIntervalMinutes = minutes; }

    public boolean isExportOnShutdown() { return exportOnShutdown; }
    public void setExportOnShutdown(boolean enabled) { this.exportOnShutdown = enabled; }

    @Override
    public String toString() {
        return "ObservabilityConfig{" +
            "tracingEnabled=" + tracingEnabled +
            ", llmTracingEnabled=" + llmTracingEnabled +
            ", skillTracingEnabled=" + skillTracingEnabled +
            ", contractNetTracingEnabled=" + contractNetTracingEnabled +
            ", tracingRetentionHours=" + tracingRetentionHours +
            ", metricsEnabled=" + metricsEnabled +
            ", agentMetricsEnabled=" + agentMetricsEnabled +
            ", llmMetricsEnabled=" + llmMetricsEnabled +
            ", skillMetricsEnabled=" + skillMetricsEnabled +
            ", systemHealthEnabled=" + systemHealthEnabled +
            ", metricsRetentionHours=" + metricsRetentionHours +
            ", jsonExportEnabled=" + jsonExportEnabled +
            ", csvExportEnabled=" + csvExportEnabled +
            ", exportDirectory='" + exportDirectory + '\'' +
            ", exportQueueSize=" + exportQueueSize +
            ", autoExportEnabled=" + autoExportEnabled +
            ", autoExportIntervalMinutes=" + autoExportIntervalMinutes +
            ", exportOnShutdown=" + exportOnShutdown +
            '}';
    }

    /**
     * Scheduled export task for automatic data export.
     */
    private class ExportTask implements Runnable {
        private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd_HH-mm-ss";
        private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(TIMESTAMP_FORMAT);

        @Override
        public void run() {
            try {
                String timestamp = LocalDateTime.now().format(formatter);
                LOGGER.info("Running scheduled observability export at {}", timestamp);

                MetricsReporter reporter = new MetricsReporter();

                // Export metrics
                if (jsonExportEnabled) {
                    reporter.exportToJson(exportDirectory, "scheduled_metrics");
                }
                if (csvExportEnabled) {
                    reporter.exportToCsv(exportDirectory, "scheduled_metrics");
                }

                // Export traces
                TracingService tracing = TracingService.getInstance();
                if (jsonExportEnabled) {
                    tracing.exportToJson(exportDirectory);
                }
                if (csvExportEnabled) {
                    tracing.exportToCsv(exportDirectory);
                }

                LOGGER.info("Scheduled observability export completed");
            } catch (Exception e) {
                LOGGER.error("Failed to run scheduled export", e);
            }
        }
    }
}
