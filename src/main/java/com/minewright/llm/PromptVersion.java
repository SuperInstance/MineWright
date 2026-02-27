package com.minewright.llm;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Manages prompt versioning for A/B testing and gradual rollouts.
 *
 * <p>This class enables:</p>
 * <ul>
 *   <li>Multiple prompt versions to coexist</li>
 *   <li>Percentage-based traffic splitting (A/B testing)</li>
 *   <li>Version-specific metrics tracking</li>
 *   <li>Easy rollback to previous versions</li>
 * </ul>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>
 * PromptVersion.register("v1", () -> PromptBuilder.buildSystemPrompt());
 * PromptVersion.register("v2", () -> PromptBuilder.buildSystemPromptV2());
 * PromptVersion.setActiveVersion("v2", 0.1); // 10% traffic to v2
 * String prompt = PromptVersion.getActivePrompt();
 * </pre>
 *
 * @since 1.3.0
 */
public class PromptVersion {

    private static final Map<String, Supplier<String>> versions = new HashMap<>();
    private static final Map<String, VersionMetrics> metrics = new HashMap<>();

    private static volatile String defaultVersion = "default";
    private static volatile TestConfig activeTest = null;

    /**
     * Registers a prompt version with a unique identifier.
     *
     * @param versionId    Unique version identifier (e.g., "v1", "v2-experimental")
     * @param promptSupplier Supplier that generates the prompt content
     */
    public static synchronized void register(String versionId, Supplier<String> promptSupplier) {
        versions.put(versionId, promptSupplier);
        metrics.putIfAbsent(versionId, new VersionMetrics(versionId));
    }

    /**
     * Sets up an A/B test with percentage-based traffic splitting.
     *
     * @param testVersion  The version to test (e.g., "v2")
     * @param percentage   Percentage of traffic to direct to test version (0.0-1.0)
     */
    public static synchronized void setActiveVersion(String testVersion, double percentage) {
        if (percentage < 0.0 || percentage > 1.0) {
            throw new IllegalArgumentException("Percentage must be between 0.0 and 1.0");
        }
        if (!versions.containsKey(testVersion)) {
            throw new IllegalArgumentException("Unknown version: " + testVersion);
        }

        activeTest = new TestConfig(defaultVersion, testVersion, percentage);
    }

    /**
     * Disables A/B testing and returns to default version.
     */
    public static synchronized void disableTest() {
        activeTest = null;
    }

    /**
     * Gets the active prompt based on current A/B test configuration.
     *
     * @return The prompt content for the appropriate version
     */
    public static String getActivePrompt() {
        if (activeTest == null) {
            return getPromptForVersion(defaultVersion);
        }

        String selectedVersion = activeTest.selectVersion();
        String prompt = getPromptForVersion(selectedVersion);

        // Track usage
        VersionMetrics versionMetrics = metrics.get(selectedVersion);
        if (versionMetrics != null) {
            versionMetrics.recordUsage();
        }

        return prompt;
    }

    /**
     * Gets prompt for a specific version.
     *
     * @param versionId Version identifier
     * @return The prompt content, or default if version not found
     */
    public static String getPromptForVersion(String versionId) {
        Supplier<String> supplier = versions.get(versionId);
        if (supplier == null) {
            return versions.getOrDefault(defaultVersion, () -> "").get();
        }
        return supplier.get();
    }

    /**
     * Records a successful response for a version.
     * Used for tracking effectiveness.
     *
     * @param versionId Version that generated the response
     * @param success   true if the response was successful/parsed correctly
     */
    public static void recordResponse(String versionId, boolean success) {
        VersionMetrics versionMetrics = metrics.get(versionId);
        if (versionMetrics != null) {
            versionMetrics.recordResponse(success);
        }
    }

    /**
     * Gets metrics for all versions.
     */
    public static synchronized Map<String, VersionMetrics> getAllMetrics() {
        return new HashMap<>(metrics);
    }

    /**
     * Gets metrics for a specific version.
     */
    public static VersionMetrics getMetrics(String versionId) {
        return metrics.get(versionId);
    }

    /**
     * Resets all metrics.
     */
    public static synchronized void resetMetrics() {
        metrics.keySet().forEach(id -> metrics.put(id, new VersionMetrics(id)));
    }

    /**
     * Sets the default version (used when no test is active).
     */
    public static void setDefaultVersion(String versionId) {
        if (!versions.containsKey(versionId)) {
            throw new IllegalArgumentException("Unknown version: " + versionId);
        }
        defaultVersion = versionId;
    }

    /**
     * Gets the current A/B test configuration.
     */
    public static TestConfig getActiveTest() {
        return activeTest;
    }

    /**
     * Configuration for an A/B test.
     */
    public static class TestConfig {
        private final String controlVersion;
        private final String testVersion;
        private final double testPercentage;

        public TestConfig(String controlVersion, String testVersion, double testPercentage) {
            this.controlVersion = controlVersion;
            this.testVersion = testVersion;
            this.testPercentage = testPercentage;
        }

        /**
         * Selects a version based on traffic split.
         * Uses thread-safe random selection.
         */
        public String selectVersion() {
            return Math.random() < testPercentage ? testVersion : controlVersion;
        }

        public String getControlVersion() {
            return controlVersion;
        }

        public String getTestVersion() {
            return testVersion;
        }

        public double getTestPercentage() {
            return testPercentage;
        }

        @Override
        public String toString() {
            return String.format("TestConfig{control=%s, test=%s, split=%.1f%%}",
                controlVersion, testVersion, testPercentage * 100);
        }
    }

    /**
     * Metrics for a specific prompt version.
     */
    public static class VersionMetrics {
        private final String versionId;
        private volatile long usageCount = 0;
        private volatile long successCount = 0;
        private volatile long failureCount = 0;

        public VersionMetrics(String versionId) {
            this.versionId = versionId;
        }

        private synchronized void recordUsage() {
            usageCount++;
        }

        private synchronized void recordResponse(boolean success) {
            if (success) {
                successCount++;
            } else {
                failureCount++;
            }
        }

        public String getVersionId() {
            return versionId;
        }

        public long getUsageCount() {
            return usageCount;
        }

        public long getSuccessCount() {
            return successCount;
        }

        public long getFailureCount() {
            return failureCount;
        }

        public double getSuccessRate() {
            long total = successCount + failureCount;
            return total > 0 ? (double) successCount / total : 0.0;
        }

        @Override
        public String toString() {
            return String.format(
                "VersionMetrics{id=%s, usage=%d, success=%.1f%% (%d/%d)}",
                versionId,
                usageCount,
                getSuccessRate() * 100,
                successCount,
                successCount + failureCount
            );
        }
    }
}
