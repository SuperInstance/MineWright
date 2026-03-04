package com.minewright.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Manages the task queue for action execution.
 *
 * <p><b>Responsibilities:</b></p>
 * <ul>
 *   <li>Thread-safe task queuing and dequeuing</li>
 *   <li>Queue state queries (empty, size, pending)</li>
 *   <li>Queue operations (clear, add, poll)</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b> Uses {@link BlockingQueue} for thread-safe operations.
 * This allows tasks to be queued from any thread (e.g., orchestration thread)
 * while being consumed on the game thread.</p>
 *
 * @since 1.0.0
 */
public class ActionQueue {
    private static final Logger LOGGER = LoggerFactory.getLogger(ActionQueue.class);

    /**
     * Thread-safe queue of tasks pending execution.
     * Uses LinkedBlockingQueue for thread-safe concurrent access.
     */
    private final BlockingQueue<com.minewright.action.Task> taskQueue;

    /**
     * Creates a new action queue.
     */
    public ActionQueue() {
        this.taskQueue = new LinkedBlockingQueue<>();
    }

    /**
     * Queues a task for execution.
     *
     * <p><b>Thread Safety:</b> This method is thread-safe and can be called from
     * any thread (e.g., orchestration thread). Uses {@link BlockingQueue#offer}
     * for non-blocking insertion.</p>
     *
     * @param task Task to queue
     * @return true if task was queued successfully, false if queue is full
     */
    public boolean queueTask(com.minewright.action.Task task) {
        if (task == null) {
            LOGGER.warn("Attempted to queue null task");
            return false;
        }

        // Use offer() for thread-safe, non-blocking insertion
        boolean queued = taskQueue.offer(task);
        if (queued) {
            LOGGER.debug("Task queued: {}", task.getAction());
        } else {
            LOGGER.warn("Failed to queue task (queue full): {}", task.getAction());
        }
        return queued;
    }

    /**
     * Queues multiple tasks for execution.
     *
     * @param tasks List of tasks to queue
     */
    public void queueAll(List<com.minewright.action.Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        taskQueue.addAll(tasks);
        LOGGER.debug("Queued {} tasks", tasks.size());
    }

    /**
     * Clears all pending tasks from the queue.
     */
    public void clear() {
        int size = taskQueue.size();
        taskQueue.clear();
        LOGGER.debug("Cleared {} pending tasks", size);
    }

    /**
     * Retrieves and removes the next task, or null if queue is empty.
     *
     * @return Next task, or null if empty
     */
    public com.minewright.action.Task poll() {
        return taskQueue.poll();
    }

    /**
     * Checks if the queue is empty.
     *
     * @return true if no tasks are pending
     */
    public boolean isEmpty() {
        return taskQueue.isEmpty();
    }

    /**
     * Gets the number of pending tasks.
     *
     * @return Queue size
     */
    public int size() {
        return taskQueue.size();
    }

    /**
     * Gets all pending tasks as a list for inspection.
     *
     * @return List of pending tasks
     */
    public List<com.minewright.action.Task> getPendingTasks() {
        return new ArrayList<>(taskQueue);
    }

    /**
     * Gets a snapshot of the next task without removing it.
     *
     * @return Next task, or null if empty
     */
    public com.minewright.action.Task peek() {
        return taskQueue.peek();
    }
}
