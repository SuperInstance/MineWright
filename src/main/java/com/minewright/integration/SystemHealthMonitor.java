package com.minewright.integration;

import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;
import com.minewright.blackboard.Blackboard;
import com.minewright.blackboard.KnowledgeArea;
import com.minewright.communication.CommunicationBus;
import com.minewright.coordination.ContractNetManager;
import com.minewright.decision.TaskPrioritizer;
import com.minewright.llm.TaskPlanner;
import com.minewright.llm.async.LLMCache;
import com.minewright.llm.cascade.CascadeRouter;
import com.minewright.llm.cascade.LLMTier;
import com.minewright.skill.SkillLibrary;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Monitors the health and performance of all MineWright systems.
 *
 * <p><b>Purpose:</b></p>
 * <p>SystemHealthMonitor provides real-time visibility into the status of
 * all subsystems. It tracks metrics, detects issues, and provides diagnostics
 * for troubleshooting.</p>
 *
 * <p><b>Monitored Systems:</b></p>
 * <ul>
 *   <li><b>Skill Library:</b> Skill count, success rates, learning progress</li>
 *   <li><b>LLM Cache:</b> Hit rate, size, efficiency</li>
 *   <li><b>Cascade Router:</b> Tier usage, costs, fallbacks, failures</li>
 *   <li><b>Task Planner:</b> Request count, latency, provider health</li>
 *   <li><b>Contract Net:</b> Active negotiations, bid counts, awards</li>
 *   <li><b>Blackboard:</b> Entry counts, subscriber counts, staleness</li>
 *   <li><b>Communication:</b> Message throughput, queue sizes, errors</li>
 * </ul>
 *
 * <p><b>Usage:</b></p>
 * <pre>
 * SystemHealthMonitor monitor = new SystemHealthMonitor(orchestrator);
 *
 * // Check overall health
 * SystemHealth health = monitor.checkHealth();
 * if (!health.isHealthy()) {
 *     System.out.println("Issues detected: " + health.getIssues());
 * }
 *
 * // Get detailed report
 * String report = monitor.generateHealthReport();
 * System.out.println(report);
 *
 * // Run periodic checks
 * monitor.startPeriodicChecks(60000); // Every minute
 * </pre>
 *
 * @since 1.6.0
 */
public class SystemHealthMonitor {
    private static final Logger LOGGER = TestLogger.getLogger(SystemHealthMonitor.class);

    // ------------------------------------------------------------------------
    // Dependencies
    // ------------------------------------------------------------------------

    private final SteveOrchestrator orchestrator;
    private final SkillLibrary skillLibrary;
    private final CascadeRouter cascadeRouter;
    private final TaskPlanner taskPlanner;
    private final TaskPrioritizer taskPrioritizer;
    private final ContractNetManager contractNet;
    private final Blackboard blackboard;
    private final CommunicationBus commBus;

    // ------------------------------------------------------------------------
    // Health History
    // ------------------------------------------------------------------------

    private final List<SystemHealth> healthHistory;
    private final Map<String, HealthMetric> metricHistory;
    private final int maxHistorySize = 100;

    // ------------------------------------------------------------------------
    // Periodic Monitoring
    // ------------------------------------------------------------------------

    private volatile boolean monitoringActive = false;
    private Thread monitoringThread;
    private long monitoringIntervalMs = 60000; // 1 minute default

    /**
     * Creates a health monitor for the given orchestrator.
     *
     * @param orchestrator The orchestrator to monitor
     */
    public SystemHealthMonitor(SteveOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
        this.skillLibrary = orchestrator.getSkillLibrary();
        this.cascadeRouter = orchestrator.getCascadeRouter();
        this.taskPlanner = orchestrator.getTaskPlanner();
        this.taskPrioritizer = orchestrator.getPrioritizer();
        this.contractNet = orchestrator.getContractNet();
        this.blackboard = orchestrator.getBlackboard();
        this.commBus = orchestrator.getCommunicationBus();

        this.healthHistory = new ArrayList<>();
        this.metricHistory = new ConcurrentHashMap<>();

        LOGGER.info("SystemHealthMonitor initialized");
    }

    // ------------------------------------------------------------------------
    // Health Checks
    // ------------------------------------------------------------------------

    /**
     * Performs a comprehensive health check of all systems.
     *
     * <p>This method checks each subsystem and returns an overall health
     * assessment with detailed metrics and any detected issues.</p>
     *
     * @return System health summary
     */
    public SystemHealth checkHealth() {
        LOGGER.debug("Performing system health check...");

        List<HealthIssue> issues = new ArrayList<>();
        Map<String, HealthMetric> metrics = new HashMap<>();

        // Check Skill Library
        checkSkillLibrary(issues, metrics);

        // Check LLM Cache
        checkLLMCache(issues, metrics);

        // Check Cascade Router
        checkCascadeRouter(issues, metrics);

        // Check Task Planner
        checkTaskPlanner(issues, metrics);

        // Check Contract Net
        checkContractNet(issues, metrics);

        // Check Blackboard
        checkBlackboard(issues, metrics);

        // Check Communication Bus
        checkCommunicationBus(issues, metrics);

        // Determine overall health
        boolean healthy = issues.stream().noneMatch(i -> i.severity() == HealthSeverity.CRITICAL);

        SystemHealth health = new SystemHealth(healthy, issues, metrics, System.currentTimeMillis());

        // Record in history
        recordHealth(health);

        LOGGER.info("Health check complete: {} - {} metrics, {} issues",
            healthy ? "HEALTHY" : "UNHEALTHY", metrics.size(), issues.size());

        return health;
    }

    /**
     * Checks the health of the Skill Library.
     */
    private void checkSkillLibrary(List<HealthIssue> issues, Map<String, HealthMetric> metrics) {
        if (skillLibrary == null) {
            issues.add(new HealthIssue(
                HealthComponent.SKILL_LIBRARY,
                HealthSeverity.WARNING,
                "SkillLibrary is null"
            ));
            return;
        }

        int skillCount = skillLibrary.getSkillCount();
        metrics.put("skill_library.count", new HealthMetric(
            "Skill Count",
            skillCount,
            skillCount > 0 ? HealthStatus.GOOD : HealthStatus.WARNING
        ));

        Map<String, Integer> stats = skillLibrary.getStatistics();
        int totalExecutions = stats.getOrDefault("totalExecutions", 0);

        metrics.put("skill_library.executions", new HealthMetric(
            "Total Executions",
            totalExecutions,
            HealthStatus.GOOD
        ));

        if (skillCount == 0) {
            issues.add(new HealthIssue(
                HealthComponent.SKILL_LIBRARY,
                HealthSeverity.INFO,
                "No skills in library - learning may not be active"
            ));
        }
    }

    /**
     * Checks the health of the LLM Cache.
     */
    private void checkLLMCache(List<HealthIssue> issues, Map<String, HealthMetric> metrics) {
        LLMCache cache = taskPlanner.getLLMCache();
        if (cache == null) {
            issues.add(new HealthIssue(
                HealthComponent.LLM_CACHE,
                HealthSeverity.WARNING,
                "LLMCache is null"
            ));
            return;
        }

        // Get cache statistics
        LLMCache.CacheStatsSnapshot stats = cache.getStats();
        double hitRate = stats.hitRate;
        int size = (int) cache.size();

        metrics.put("llm_cache.hit_rate", new HealthMetric(
            "Cache Hit Rate",
            hitRate * 100,
            hitRate >= 0.3 ? HealthStatus.GOOD : HealthStatus.WARNING,
            "%"
        ));

        metrics.put("llm_cache.size", new HealthMetric(
            "Cache Size",
            size,
            HealthStatus.GOOD
        ));

        if (hitRate < 0.1 && size > 100) {
            issues.add(new HealthIssue(
                HealthComponent.LLM_CACHE,
                HealthSeverity.WARNING,
                String.format("Low cache hit rate: %.1f%% (size: %d)", hitRate * 100, size)
            ));
        }
    }

    /**
     * Checks the health of the Cascade Router.
     */
    private void checkCascadeRouter(List<HealthIssue> issues, Map<String, HealthMetric> metrics) {
        if (cascadeRouter == null) {
            issues.add(new HealthIssue(
                HealthComponent.CASCADE_ROUTER,
                HealthSeverity.INFO,
                "CascadeRouter is null (not enabled)"
            ));
            return;
        }

        long totalRequests = cascadeRouter.getTotalRequests();
        double cacheHitRate = cascadeRouter.getCacheHitRate();
        long fallbacks = cascadeRouter.getFallbacks();
        long failures = cascadeRouter.getFailures();
        double totalCost = cascadeRouter.getTotalCost();

        metrics.put("cascade.requests", new HealthMetric(
            "Total Requests",
            totalRequests,
            HealthStatus.GOOD
        ));

        metrics.put("cascade.cache_hit_rate", new HealthMetric(
            "Cache Hit Rate",
            cacheHitRate * 100,
            HealthStatus.GOOD,
            "%"
        ));

        metrics.put("cascade.fallbacks", new HealthMetric(
            "Fallbacks",
            fallbacks,
            HealthStatus.GOOD
        ));

        metrics.put("cascade.failures", new HealthMetric(
            "Failures",
            failures,
            failures > 0 ? HealthStatus.WARNING : HealthStatus.GOOD
        ));

        metrics.put("cascade.total_cost", new HealthMetric(
            "Total Cost",
            totalCost,
            HealthStatus.GOOD,
            "$"
        ));

        // Check tier availability
        for (LLMTier tier : LLMTier.values()) {
            if (tier.isAvailable()) {
                long usage = cascadeRouter.getTierUsage(tier);
                double cost = cascadeRouter.getTierCost(tier);

                metrics.put("cascade." + tier.name().toLowerCase() + ".usage",
                    new HealthMetric(tier.name() + " Usage", usage, HealthStatus.GOOD));

                metrics.put("cascade." + tier.name().toLowerCase() + ".cost",
                    new HealthMetric(tier.name() + " Cost", cost, HealthStatus.GOOD, "$"));
            }
        }

        // High failure rate warning
        if (totalRequests > 10 && (double) failures / totalRequests > 0.1) {
            issues.add(new HealthIssue(
                HealthComponent.CASCADE_ROUTER,
                HealthSeverity.WARNING,
                String.format("High failure rate: %.1f%% (%d/%d)",
                    (double) failures / totalRequests * 100, failures, totalRequests)
            ));
        }
    }

    /**
     * Checks the health of the Task Planner.
     */
    private void checkTaskPlanner(List<HealthIssue> issues, Map<String, HealthMetric> metrics) {
        if (taskPlanner == null) {
            issues.add(new HealthIssue(
                HealthComponent.TASK_PLANNER,
                HealthSeverity.CRITICAL,
                "TaskPlanner is null"
            ));
            return;
        }

        // Check provider health
        boolean openaiHealthy = taskPlanner.isProviderHealthy("openai");
        boolean groqHealthy = taskPlanner.isProviderHealthy("groq");
        boolean geminiHealthy = taskPlanner.isProviderHealthy("gemini");

        metrics.put("planner.openai_healthy", new HealthMetric(
            "OpenAI Healthy",
            openaiHealthy ? 1 : 0,
            openaiHealthy ? HealthStatus.GOOD : HealthStatus.WARNING
        ));

        metrics.put("planner.groq_healthy", new HealthMetric(
            "Groq Healthy",
            groqHealthy ? 1 : 0,
            groqHealthy ? HealthStatus.GOOD : HealthStatus.WARNING
        ));

        metrics.put("planner.gemini_healthy", new HealthMetric(
            "Gemini Healthy",
            geminiHealthy ? 1 : 0,
            geminiHealthy ? HealthStatus.GOOD : HealthStatus.WARNING
        ));

        if (!openaiHealthy && !groqHealthy && !geminiHealthy) {
            issues.add(new HealthIssue(
                HealthComponent.TASK_PLANNER,
                HealthSeverity.CRITICAL,
                "All LLM providers are unhealthy"
            ));
        }
    }

    /**
     * Checks the health of the Contract Net Manager.
     */
    private void checkContractNet(List<HealthIssue> issues, Map<String, HealthMetric> metrics) {
        if (contractNet == null) {
            issues.add(new HealthIssue(
                HealthComponent.CONTRACT_NET,
                HealthSeverity.INFO,
                "ContractNetManager is null (not enabled)"
            ));
            return;
        }

        int activeCount = contractNet.getActiveCount();

        metrics.put("contract_net.active_negotiations", new HealthMetric(
            "Active Negotiations",
            activeCount,
            HealthStatus.GOOD
        ));

        // Check for stalled negotiations (active for too long)
        if (activeCount > 10) {
            issues.add(new HealthIssue(
                HealthComponent.CONTRACT_NET,
                HealthSeverity.WARNING,
                "High number of active negotiations: " + activeCount
            ));
        }
    }

    /**
     * Checks the health of the Blackboard.
     */
    private void checkBlackboard(List<HealthIssue> issues, Map<String, HealthMetric> metrics) {
        if (blackboard == null) {
            issues.add(new HealthIssue(
                HealthComponent.BLACKBOARD,
                HealthSeverity.INFO,
                "Blackboard is null (not enabled)"
            ));
            return;
        }

        int totalEntries = blackboard.getTotalEntryCount();

        metrics.put("blackboard.total_entries", new HealthMetric(
            "Total Entries",
            totalEntries,
            HealthStatus.GOOD
        ));

        // Check each knowledge area
        for (KnowledgeArea area : KnowledgeArea.values()) {
            int count = blackboard.getEntryCount(area);
            metrics.put("blackboard." + area.getId() + ".count",
                new HealthMetric(area.getId() + " Entries", count, HealthStatus.GOOD));
        }

        // Warn if too many entries (memory leak risk)
        if (totalEntries > 10000) {
            issues.add(new HealthIssue(
                HealthComponent.BLACKBOARD,
                HealthSeverity.WARNING,
                "High entry count: " + totalEntries + " (consider evicting stale entries)"
            ));
        }
    }

    /**
     * Checks the health of the Communication Bus.
     */
    private void checkCommunicationBus(List<HealthIssue> issues, Map<String, HealthMetric> metrics) {
        if (commBus == null) {
            issues.add(new HealthIssue(
                HealthComponent.COMMUNICATION_BUS,
                HealthSeverity.INFO,
                "CommunicationBus is null (not enabled)"
            ));
            return;
        }

        CommunicationBus.MessageStats stats = commBus.getStats();

        metrics.put("comm.sent", new HealthMetric(
            "Messages Sent",
            stats.getSent(),
            HealthStatus.GOOD
        ));

        metrics.put("comm.delivered", new HealthMetric(
            "Messages Delivered",
            stats.getDelivered(),
            HealthStatus.GOOD
        ));

        metrics.put("comm.dropped", new HealthMetric(
            "Messages Dropped",
            stats.getDropped(),
            stats.getDropped() > 0 ? HealthStatus.WARNING : HealthStatus.GOOD
        ));

        metrics.put("comm.failed", new HealthMetric(
            "Messages Failed",
            stats.getFailed(),
            stats.getFailed() > 0 ? HealthStatus.WARNING : HealthStatus.GOOD
        ));

        int registeredAgents = commBus.getRegisteredAgents().size();
        metrics.put("comm.registered_agents", new HealthMetric(
            "Registered Agents",
            registeredAgents,
            HealthStatus.GOOD
        ));

        // High drop rate warning
        if (stats.getSent() > 100) {
            double dropRate = (double) stats.getDropped() / stats.getSent();
            if (dropRate > 0.05) {
                issues.add(new HealthIssue(
                    HealthComponent.COMMUNICATION_BUS,
                    HealthSeverity.WARNING,
                    String.format("High message drop rate: %.1f%%", dropRate * 100)
                ));
            }
        }
    }

    // ------------------------------------------------------------------------
    // Health Reports
    // ------------------------------------------------------------------------

    /**
     * Generates a detailed health report.
     *
     * @return Formatted health report string
     */
    public String generateHealthReport() {
        SystemHealth health = checkHealth();

        StringBuilder report = new StringBuilder();
        report.append("=== MineWright System Health Report ===\n");
        report.append(String.format("Generated: %s\n", new Date(health.timestamp())));
        report.append(String.format("Overall Status: %s\n\n", health.healthy() ? "HEALTHY" : "UNHEALTHY"));

        // Metrics by component
        report.append("Metrics:\n");
        health.metrics().entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                HealthMetric metric = entry.getValue();
                String statusIcon = switch (metric.status()) {
                    case GOOD -> "✓";
                    case WARNING -> "⚠";
                    case ERROR -> "✗";
                };
                report.append(String.format("  [%s] %s: %.2f %s\n",
                    statusIcon, metric.name(), metric.value(),
                    metric.unit() != null ? metric.unit() : ""));
            });

        // Issues
        if (!health.issues().isEmpty()) {
            report.append("\nIssues:\n");
            for (HealthIssue issue : health.issues()) {
                String severityIcon = switch (issue.severity()) {
                    case INFO -> "ℹ";
                    case WARNING -> "⚠";
                    case CRITICAL -> "✖";
                };
                report.append(String.format("  [%s] %s: %s\n",
                    severityIcon, issue.component(), issue.message()));
            }
        } else {
            report.append("\nNo issues detected.\n");
        }

        return report.toString();
    }

    // ------------------------------------------------------------------------
    // Periodic Monitoring
    // ------------------------------------------------------------------------

    /**
     * Starts periodic health checks.
     *
     * @param intervalMs Check interval in milliseconds
     */
    public void startPeriodicChecks(long intervalMs) {
        if (monitoringActive) {
            LOGGER.warn("Periodic monitoring already active");
            return;
        }

        this.monitoringIntervalMs = intervalMs;
        this.monitoringActive = true;

        monitoringThread = new Thread(() -> {
            LOGGER.info("Starting periodic health checks (interval: {}ms)", intervalMs);

            while (monitoringActive) {
                try {
                    Thread.sleep(intervalMs);
                    if (monitoringActive) {
                        checkHealth();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            LOGGER.info("Periodic health checks stopped");
        }, "HealthMonitor");

        monitoringThread.setDaemon(true);
        monitoringThread.start();

        LOGGER.info("Periodic health monitoring started");
    }

    /**
     * Stops periodic health checks.
     */
    public void stopPeriodicChecks() {
        monitoringActive = false;
        if (monitoringThread != null) {
            monitoringThread.interrupt();
            monitoringThread = null;
        }
        LOGGER.info("Periodic health monitoring stopped");
    }

    // ------------------------------------------------------------------------
    // Health History
    // ------------------------------------------------------------------------

    /**
     * Records a health check in history.
     */
    private void recordHealth(SystemHealth health) {
        synchronized (healthHistory) {
            healthHistory.add(health);
            while (healthHistory.size() > maxHistorySize) {
                healthHistory.remove(0);
            }
        }

        // Update metric history
        for (Map.Entry<String, HealthMetric> entry : health.metrics().entrySet()) {
            metricHistory.put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Gets recent health history.
     *
     * @param count Maximum number of records
     * @return List of recent health checks
     */
    public List<SystemHealth> getHealthHistory(int count) {
        synchronized (healthHistory) {
            int start = Math.max(0, healthHistory.size() - count);
            return new ArrayList<>(healthHistory.subList(start, healthHistory.size()));
        }
    }

    /**
     * Gets the history for a specific metric.
     *
     * @param metricName Metric name
     * @return Current metric value, or null
     */
    public HealthMetric getMetric(String metricName) {
        return metricHistory.get(metricName);
    }
}

// ------------------------------------------------------------------------
// Health Data Classes
// ------------------------------------------------------------------------

/**
 * Overall system health summary.
 */
record SystemHealth(
    boolean healthy,
    List<HealthIssue> issues,
    Map<String, HealthMetric> metrics,
    long timestamp
) {}

/**
 * A health issue detected during monitoring.
 */
record HealthIssue(
    HealthComponent component,
    HealthSeverity severity,
    String message
) {}

/**
 * A health metric with value and status.
 */
record HealthMetric(
    String name,
    double value,
    HealthStatus status,
    String unit
) {
    public HealthMetric(String name, double value, HealthStatus status) {
        this(name, value, status, null);
    }
}

/**
 * System components that can be monitored.
 */
enum HealthComponent {
    SKILL_LIBRARY,
    LLM_CACHE,
    CASCADE_ROUTER,
    TASK_PLANNER,
    CONTRACT_NET,
    BLACKBOARD,
    COMMUNICATION_BUS
}

/**
 * Severity levels for health issues.
 */
enum HealthSeverity {
    INFO,
    WARNING,
    CRITICAL
}

/**
 * Status indicators for health metrics.
 */
enum HealthStatus {
    GOOD,
    WARNING,
    ERROR
}
