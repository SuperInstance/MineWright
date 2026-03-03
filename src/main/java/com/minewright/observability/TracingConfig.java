package com.minewright.observability;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for the observability system.
 * Loads settings from properties files with sensible defaults.
 */
public class TracingConfig {
    private static final String DEFAULT_CONFIG_FILE = "observability.properties";
    private static final String DEFAULT_EXPORT_DIR = "observability/exports";
    private static final long DEFAULT_RETENTION_HOURS = 24;

    private boolean enabled = true;
    private boolean llmTracingEnabled = true;
    private boolean skillTracingEnabled = true;
    private boolean contractNetTracingEnabled = true;
    private boolean jsonExportEnabled = true;
    private boolean csvExportEnabled = true;
    private String exportDirectory = DEFAULT_EXPORT_DIR;
    private long retentionHours = DEFAULT_RETENTION_HOURS;
    private int exportQueueSize = 10000;
    private boolean autoExportEnabled = true;
    private long autoExportIntervalMinutes = 5;

    public TracingConfig() {}

    public TracingConfig(String configPath) throws IOException {
        loadFromProperties(configPath);
    }

    public static TracingConfig loadDefault() {
        TracingConfig config = new TracingConfig();
        try {
            Path configPath = Paths.get(DEFAULT_CONFIG_FILE);
            if (Files.exists(configPath)) {
                config.loadFromProperties(DEFAULT_CONFIG_FILE);
            }
        } catch (IOException e) {}
        return config;
    }

    public void loadFromProperties(String configPath) throws IOException {
        Properties props = new Properties();
        try (InputStream input = Files.newInputStream(Paths.get(configPath))) {
            props.load(input);
        }
        this.enabled = parseBoolean(props.getProperty("tracing.enabled", "true"));
        this.llmTracingEnabled = parseBoolean(props.getProperty("tracing.llm.enabled", "true"));
        this.skillTracingEnabled = parseBoolean(props.getProperty("tracing.skill.enabled", "true"));
        this.contractNetTracingEnabled = parseBoolean(props.getProperty("tracing.contractNet.enabled", "true"));
        this.jsonExportEnabled = parseBoolean(props.getProperty("export.json.enabled", "true"));
        this.csvExportEnabled = parseBoolean(props.getProperty("export.csv.enabled", "true"));
        this.exportDirectory = props.getProperty("export.directory", DEFAULT_EXPORT_DIR);
        this.retentionHours = parseLong(props.getProperty("retention.hours"), DEFAULT_RETENTION_HOURS);
        this.exportQueueSize = parseInt(props.getProperty("export.queueSize"), 10000);
        this.autoExportEnabled = parseBoolean(props.getProperty("export.auto.enabled", "true"));
        this.autoExportIntervalMinutes = parseLong(props.getProperty("export.auto.intervalMinutes"), 5);
    }

    private boolean parseBoolean(String value) {
        return value != null && (value.equalsIgnoreCase("true") || value.equals("1"));
    }

    private long parseLong(String value, long defaultValue) {
        if (value == null || value.trim().isEmpty()) return defaultValue;
        try { return Long.parseLong(value.trim()); } catch (NumberFormatException e) { return defaultValue; }
    }

    private int parseInt(String value, int defaultValue) {
        if (value == null || value.trim().isEmpty()) return defaultValue;
        try { return Integer.parseInt(value.trim()); } catch (NumberFormatException e) { return defaultValue; }
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isLLMTracingEnabled() { return llmTracingEnabled; }
    public void setLLMTracingEnabled(boolean enabled) { this.llmTracingEnabled = enabled; }
    public boolean isSkillTracingEnabled() { return skillTracingEnabled; }
    public void setSkillTracingEnabled(boolean enabled) { this.skillTracingEnabled = enabled; }
    public boolean isContractNetTracingEnabled() { return contractNetTracingEnabled; }
    public void setContractNetTracingEnabled(boolean enabled) { this.contractNetTracingEnabled = enabled; }
    public boolean isJsonExportEnabled() { return jsonExportEnabled; }
    public void setJsonExportEnabled(boolean enabled) { this.jsonExportEnabled = enabled; }
    public boolean isCsvExportEnabled() { return csvExportEnabled; }
    public void setCsvExportEnabled(boolean enabled) { this.csvExportEnabled = enabled; }
    public String getExportDirectory() { return exportDirectory; }
    public void setExportDirectory(String directory) { this.exportDirectory = directory; }
    public long getRetentionHours() { return retentionHours; }
    public void setRetentionHours(long hours) { this.retentionHours = hours; }
    public long getRetentionMillis() { return TimeUnit.HOURS.toMillis(retentionHours); }
    public int getExportQueueSize() { return exportQueueSize; }
    public void setExportQueueSize(int size) { this.exportQueueSize = size; }
    public boolean isAutoExportEnabled() { return autoExportEnabled; }
    public void setAutoExportEnabled(boolean enabled) { this.autoExportEnabled = enabled; }
    public long getAutoExportIntervalMinutes() { return autoExportIntervalMinutes; }
    public void setAutoExportIntervalMinutes(long minutes) { this.autoExportIntervalMinutes = minutes; }

    @Override
    public String toString() {
        return "TracingConfig{enabled=" + enabled + ", llmTracingEnabled=" + llmTracingEnabled +
               ", skillTracingEnabled=" + skillTracingEnabled + ", contractNetTracingEnabled=" + contractNetTracingEnabled +
               ", jsonExportEnabled=" + jsonExportEnabled + ", csvExportEnabled=" + csvExportEnabled +
               ", exportDirectory='" + exportDirectory + '\'' + ", retentionHours=" + retentionHours +
               ", exportQueueSize=" + exportQueueSize + ", autoExportEnabled=" + autoExportEnabled +
               ", autoExportIntervalMinutes=" + autoExportIntervalMinutes + '}';
    }
}
