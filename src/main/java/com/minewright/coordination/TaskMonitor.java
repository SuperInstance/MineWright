package com.minewright.coordination;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.minewright.coordination.TaskRebalancingManager.*;

/**
 * Monitors tasks for rebalancing conditions.
 * <p>
 * Responsible for:
 * <ul>
 *   <li>Managing monitored task lifecycle</li>
 *   <li>Scheduling periodic health checks</li>
 *   <li>Tracking task progress</li>
 *   <li>Cleaning up completed/cancelled monitoring</li>
 * </ul>
 *
 * @since 1.4.0
 */
public class TaskMonitor {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskMonitor.class);

    private final Map<String, MonitoredTask> monitoredTasks;
    private final Map<String, ScheduledFuture<?>> monitoringFutures;
    private final ScheduledExecutorService scheduler;
    private final TaskRebalancingAssessor assessor;

    private volatile long defaultMonitoringInterval = 10000; // Check every 10 seconds

    public TaskMonitor(ScheduledExecutorService scheduler, TaskRebalancingAssessor assessor) {
        this.monitoredTasks = new ConcurrentHashMap<>();
        this.monitoringFutures = new ConcurrentHashMap<>();
        this.scheduler = scheduler;
        this.assessor = assessor;
    }

    /**
     * Starts monitoring a task for rebalancing.
     *
     * @param task The task to monitor
     * @param listener Optional listener for monitoring events
     * @return true if monitoring started successfully
     */
    public boolean startMonitoring(MonitoredTask task, RebalancingListener listener) {
        if (task == null) {
            LOGGER.warn("Cannot monitor task: null task");
            return false;
        }

        if (monitoredTasks.containsKey(task.getTaskId())) {
            LOGGER.warn("Task {} is already being monitored", task.getTaskId());
            return false;
        }

        monitoredTasks.put(task.getTaskId(), task);

        // Schedule periodic health checks
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
            () -> assessor.assessTask(task, listener),
            task.getMonitoringInterval(),
            task.getMonitoringInterval(),
            TimeUnit.MILLISECONDS
        );

        monitoringFutures.put(task.getTaskId(), future);

        LOGGER.info("Started monitoring task {} assigned to {} (estimated: {}ms, interval: {}ms)",
            task.getTaskId(), task.getAssignedAgent().toString().substring(0, 8),
            task.getEstimatedDuration(), task.getMonitoringInterval());

        // Notify listeners
        if (listener != null) {
            try {
                listener.onMonitoringStarted(task.getTaskId(), task.getAssignedAgent());
            } catch (Exception e) {
                LOGGER.warn("Listener error in onMonitoringStarted", e);
            }
        }

        return true;
    }

    /**
     * Updates task progress.
     *
     * @param taskId Task identifier
     * @param progress Progress percentage (0.0-1.0)
     */
    public void updateProgress(String taskId, double progress) {
        MonitoredTask task = monitoredTasks.get(taskId);
        if (task != null) {
            task.updateProgress(progress);
            LOGGER.debug("Updated progress for task {} to {:.1f}%", taskId, progress * 100);
        }
    }

    /**
     * Stops monitoring a task.
     *
     * @param taskId Task identifier
     * @param listener Optional listener for monitoring events
     * @return true if monitoring was stopped
     */
    public boolean stopMonitoring(String taskId, RebalancingListener listener) {
        MonitoredTask removed = monitoredTasks.remove(taskId);
        if (removed == null) {
            return false;
        }

        ScheduledFuture<?> future = monitoringFutures.remove(taskId);
        if (future != null) {
            future.cancel(false);
        }

        LOGGER.info("Stopped monitoring task {}", taskId);

        // Notify listeners
        if (listener != null) {
            try {
                listener.onMonitoringStopped(taskId);
            } catch (Exception e) {
                LOGGER.warn("Listener error in onMonitoringStopped", e);
            }
        }

        return true;
    }

    /**
     * Gets a monitored task by ID.
     *
     * @param taskId Task identifier
     * @return The monitored task, or null if not found
     */
    public MonitoredTask getMonitoredTask(String taskId) {
        return monitoredTasks.get(taskId);
    }

    /**
     * Gets the number of monitored tasks.
     *
     * @return Count of monitored tasks
     */
    public int getMonitoredTaskCount() {
        return monitoredTasks.size();
    }

    /**
     * Cancels all monitoring futures (for shutdown).
     */
    public void cancelAllFutures() {
        monitoringFutures.values().forEach(future -> future.cancel(false));
        monitoringFutures.clear();
        monitoredTasks.clear();
    }
}
