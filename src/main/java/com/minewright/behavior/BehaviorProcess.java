package com.minewright.behavior;

/**
 * Interface for behavior processes in the process-based arbitration system.
 *
 * <p>Inspired by Baritone's process-based architecture and Honorbuddy's behavior management,
 * this interface defines independent behavior processes that request control rather than
 * seizing it. The ProcessManager arbitrates which process gets control based on priority
 * and canRun() conditions.</p>
 *
 * <p><b>Key Principles:</b></p>
 * <ul>
 *   <li><b>Request Control, Don't Seize:</b> Processes indicate desire to run via canRun(),
 *       ProcessManager selects highest priority process</li>
 *   <li><b>Conflict Prevention:</b> Only one process active at a time prevents behavior conflicts</li>
 *   <li><b>Clear Transitions:</b> onActivate/onDeactivate hooks for clean state management</li>
 *   <li><b>Priority-Based:</b> Higher priority processes interrupt lower priority ones</li>
 * </ul>
 *
 * <p><b>Process Lifecycle:</b></p>
 * <pre>
 *   Registered → (canRun() && selected) → onActivate() → tick() → (no longer canRun())
 *                                                            ↓
 *                                                       onDeactivate()
 * </pre>
 *
 * <p><b>Example Priority Hierarchy:</b></p>
 * <ul>
 *   <li>100 - Survival: Flee danger, eat food, escape lava (interrupts all)</li>
 *   <li>50 - TaskExecution: Execute assigned tasks (normal operation)</li>
 *   <li>25 - Follow: Follow player or another entity</li>
 *   <li>10 - Idle: Look around, wander, chat (fallback behavior)</li>
 * </ul>
 *
 * @see ProcessManager
 * @see com.minewright.behavior.processes.SurvivalProcess
 * @see com.minewright.behavior.processes.TaskExecutionProcess
 * @see com.minewright.behavior.processes.IdleProcess
 *
 * @since 1.2.0
 */
public interface BehaviorProcess {

    /**
     * Returns the unique name of this process.
     *
     * <p>Used for logging, debugging, and process identification.
     * Should be descriptive and unique across all processes.</p>
     *
     * @return Process name (e.g., "Survival", "TaskExecution", "Idle")
     */
    String getName();

    /**
     * Returns the priority of this process.
     *
     * <p>Higher values indicate higher priority. The ProcessManager will select
     * the process with the highest priority among those that canRun().</p>
     *
     * <p><b>Priority Guidelines:</b></p>
     * <ul>
     *   <li>100+ - Critical (survival, emergency responses)</li>
     *   <li>50-99 - High (important tasks, combat)</li>
     *   <li>25-49 - Medium (following, routine tasks)</li>
     *   <li>1-24 - Low (idle, wandering)</li>
     * </ul>
     *
     * @return Priority value (higher = more important)
     */
    int getPriority();

    /**
     * Checks if this process is currently the active process.
     *
     * <p>A process is active if it has been selected by ProcessManager
     * and onActivate() has been called, but onDeactivate() has not yet been called.</p>
     *
     * @return true if this process is currently active
     */
    boolean isActive();

    /**
     * Checks if this process can run given current conditions.
     *
     * <p>This is the primary arbitration mechanism. Processes should return true
     * only when they have valid work to do and preconditions are met.</p>
     *
     * <p><b>Important:</b> This method should be fast and side-effect free.
     * It's called every tick during process selection. Actual work should
     * happen in tick().</p>
     *
     * <p><b>Examples:</b></p>
     * <ul>
     *   <li>SurvivalProcess: health < 30% OR in lava</li>
     *   <li>TaskExecutionProcess: taskQueue not empty</li>
     *   <li>IdleProcess: no other process canRun()</li>
     * </ul>
     *
     * @return true if this process wants control and conditions are met
     */
    boolean canRun();

    /**
     * Called every tick when this process is active.
     *
     * <p>This is where the process performs its actual work. The process
     * should make progress toward its goal and return quickly to maintain
     * server performance (target: < 5ms per tick).</p>
     *
     * <p><b>Thread Safety:</b> Called on the main server thread only.</p>
     *
     * @see com.minewright.util.TickProfiler
     */
    void tick();

    /**
     * Called when this process is granted control by ProcessManager.
     *
     * <p>Processes should initialize their state, acquire resources, and
     * prepare to execute. This is called before the first tick() call.</p>
     *
     * <p>Processes can assume no other process is active during onActivate().</p>
     *
     * <p><b>Typical Actions:</b></p>
     * <ul>
     *   <li>Set initial state variables</li>
     *   <li>Start animations or sounds</li>
     *   <li>Acquire locks or resources</li>
     *   <li>Log activation for debugging</li>
     * </ul>
     */
    void onActivate();

    /**
     * Called when this process loses control to another process.
     *
     * <p>Processes should clean up their state, release resources, and
     * prepare to be deactivated. No further tick() calls will occur until
     * onActivate() is called again.</p>
     *
     * <p><b>Typical Actions:</b></p>
     * <ul>
     *   <li>Cancel ongoing operations</li>
     *   <li>Release locks or resources</li>
     *   <li>Save state for resumption</li>
     *   <li>Log deactivation for debugging</li>
     * </ul>
     */
    void onDeactivate();
}
