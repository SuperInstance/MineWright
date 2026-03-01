package com.minewright.behavior;

import com.minewright.entity.ForemanEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Manages behavior process arbitration using priority-based selection.
 *
 * <p>Inspired by Baritone's PathingControlManager and Honorbuddy's behavior management,
 * this class prevents behavior conflicts by ensuring only one process is active at a time.
 * Processes request control via canRun(), and the manager selects the highest priority
 * process that can run.</p>
 *
 * <p><b>Arbitration Logic:</b></p>
 * <ol>
 *   <li>Each tick, evaluate all registered processes via canRun()</li>
 *   <li>Among processes that can run, select the one with highest priority</li>
 *   <li>If selected process differs from current, handle transition</li>
 *   <li>Call tick() on the active process</li>
 * </ol>
 *
 * <p><b>Process Transitions:</b></p>
 * <ul>
 *   <li><b>Activation:</b> onActivate() called when process gains control</li>
 *   <li><b>Deactivation:</b> onDeactivate() called when process loses control</li>
 *   <li><b>Preemption:</b> Higher priority process can interrupt lower priority</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b> This class is not thread-safe. All methods should be called
 * from the main server thread (during entity tick).</p>
 *
 * @see BehaviorProcess
 * @see com.minewright.behavior.processes.SurvivalProcess
 * @see com.minewright.behavior.processes.TaskExecutionProcess
 *
 * @since 1.2.0
 */
public class ProcessManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessManager.class);

    /**
     * The foreman entity this manager is controlling behavior for.
     */
    private final ForemanEntity foreman;

    /**
     * All registered behavior processes.
     * Processes are evaluated every tick for canRun() condition.
     */
    private final List<BehaviorProcess> processes;

    /**
     * The currently active process, or null if no process is active.
     * Only one process is active at a time to prevent conflicts.
     */
    private BehaviorProcess activeProcess;

    /**
     * Tick counter for debug logging.
     */
    private long tickCount = 0;

    /**
     * Creates a new ProcessManager for the given foreman entity.
     *
     * @param foreman The foreman entity to manage behavior for
     */
    public ProcessManager(ForemanEntity foreman) {
        this.foreman = foreman;
        this.processes = new ArrayList<>();
        this.activeProcess = null;

        LOGGER.debug("ProcessManager initialized for Foreman '{}'", foreman.getEntityName());
    }

    /**
     * Registers a behavior process with this manager.
     *
     * <p>Processes should be registered during initialization in priority order.
     * The same process cannot be registered twice.</p>
     *
     * @param process The process to register
     * @throws IllegalArgumentException if process is already registered
     */
    public void registerProcess(BehaviorProcess process) {
        if (process == null) {
            throw new IllegalArgumentException("Cannot register null process");
        }

        if (processes.contains(process)) {
            throw new IllegalArgumentException("Process already registered: " + process.getName());
        }

        processes.add(process);
        LOGGER.debug("Registered process '{}' (priority: {}) for Foreman '{}'",
            process.getName(), process.getPriority(), foreman.getEntityName());
    }

    /**
     * Main update loop called every game tick (20 times per second).
     *
     * <p>This method:
     * <ol>
     *   <li>Evaluates all processes to find which can run</li>
     *   <li>Selects the highest priority process that can run</li>
     *   <li>Handles process transitions (activate/deactivate)</li>
     *   <li>Calls tick() on the active process</li>
     * </ol>
     *
     * <p><b>Priority Calculation:</b></p>
     * <pre>
     * selectedProcess = processes.stream()
     *     .filter(BehaviorProcess::canRun)
     *     .max(Comparator.comparingInt(BehaviorProcess::getPriority))
     *     .orElse(null)
     * </pre>
     *
     * <p><b>Transition Handling:</b></p>
     * <ul>
     *   <li>If selected != current: Deactivate current, activate selected</li>
     *   <li>If selected == null: Deactivate current, go idle</li>
     *   <li>If selected == current: Continue with current process</li>
     * </ul>
     */
    public void tick() {
        tickCount++;

        // Find the highest priority process that can run
        BehaviorProcess selectedProcess = selectProcess();

        // Handle process transition if needed
        if (selectedProcess != activeProcess) {
            transitionTo(selectedProcess);
        }

        // Tick the active process
        if (activeProcess != null) {
            try {
                activeProcess.tick();
            } catch (Exception e) {
                LOGGER.error("[{}] Error ticking process '{}': {}",
                    foreman.getEntityName(), activeProcess.getName(), e);

                // Deactivate the failing process to prevent error loops
                if (activeProcess == selectedProcess) {
                    selectedProcess = null;
                    transitionTo(null);
                }
            }
        }

        // Debug logging every 100 ticks
        if (tickCount % 100 == 0 && activeProcess != null) {
            LOGGER.debug("[{}] Active process: '{}' (priority: {}, canRun: {})",
                foreman.getEntityName(),
                activeProcess.getName(),
                activeProcess.getPriority(),
                activeProcess.canRun());
        }
    }

    /**
     * Selects the highest priority process that can run.
     *
     * <p>Evaluates all registered processes and returns the one with the highest
     * priority among those that return true for canRun(). If no process can run,
     * returns null (idle state).</p>
     *
     * @return The selected process, or null if none can run
     */
    private BehaviorProcess selectProcess() {
        // Find highest priority process that can run
        Optional<BehaviorProcess> selected = processes.stream()
            .filter(BehaviorProcess::canRun)
            .max(Comparator.comparingInt(BehaviorProcess::getPriority));

        return selected.orElse(null);
    }

    /**
     * Transitions from the current process to a new process.
     *
     * <p>Handles clean activation/deactivation:
     * <ol>
     *   <li>If current process exists, call onDeactivate()</li>
     *   <li>If new process exists, call onActivate()</li>
     *   <li>Update activeProcess reference</li>
     *   <li>Log transition for debugging</li>
     * </ol>
     *
     * @param newProcess The process to transition to (null for idle)
     */
    private void transitionTo(BehaviorProcess newProcess) {
        String oldName = activeProcess != null ? activeProcess.getName() : "IDLE";
        String newName = newProcess != null ? newProcess.getName() : "IDLE";

        // Deactivate current process
        if (activeProcess != null) {
            try {
                LOGGER.debug("[{}] Deactivating process '{}'", foreman.getEntityName(), oldName);
                activeProcess.onDeactivate();
            } catch (Exception e) {
                LOGGER.error("[{}] Error deactivating process '{}': {}",
                    foreman.getEntityName(), oldName, e);
            }
        }

        // Activate new process
        if (newProcess != null) {
            try {
                LOGGER.debug("[{}] Activating process '{}' (priority: {})",
                    foreman.getEntityName(), newName, newProcess.getPriority());
                newProcess.onActivate();
            } catch (Exception e) {
                LOGGER.error("[{}] Error activating process '{}': {}",
                    foreman.getEntityName(), newName, e);
                // If activation fails, don't set it as active
                activeProcess = null;
                return;
            }
        }

        // Update active process
        activeProcess = newProcess;

        // Log transition
        if (!oldName.equals(newName)) {
            LOGGER.info("[{}] Process transition: {} → {}",
                foreman.getEntityName(), oldName, newName);
        }
    }

    /**
     * Returns the currently active process.
     *
     * @return The active process, or null if no process is active
     */
    public BehaviorProcess getActiveProcess() {
        return activeProcess;
    }

    /**
     * Returns the name of the currently active process.
     *
     * @return Active process name, or "IDLE" if no process is active
     */
    public String getActiveProcessName() {
        return activeProcess != null ? activeProcess.getName() : "IDLE";
    }

    /**
     * Returns all registered processes.
     *
     * @return List of registered processes
     */
    public List<BehaviorProcess> getProcesses() {
        return new ArrayList<>(processes);
    }

    /**
     * Forces deactivation of the current process.
     *
     * <p>Useful for emergency stops or when external conditions require
     * immediate behavior change. The process will be deactivated but
     * can be reactivated if canRun() returns true on next tick.</p>
     */
    public void forceDeactivate() {
        if (activeProcess != null) {
            LOGGER.info("[{}] Force deactivating process '{}'",
                foreman.getEntityName(), activeProcess.getName());
            transitionTo(null);
        }
    }

    /**
     * Checks if a specific process is currently active.
     *
     * @param processName The name of the process to check
     * @return true if the named process is active
     */
    public boolean isProcessActive(String processName) {
        return activeProcess != null && activeProcess.getName().equals(processName);
    }

    /**
     * Gets the number of registered processes.
     *
     * @return Process count
     */
    public int getProcessCount() {
        return processes.size();
    }

    /**
     * Clears all registered processes.
     *
     * <p>The current process will be deactivated before clearing.</p>
     */
    public void clearProcesses() {
        LOGGER.info("[{}] Clearing all processes ({} processes registered)",
            foreman.getEntityName(), processes.size());

        transitionTo(null);
        processes.clear();
    }
}
