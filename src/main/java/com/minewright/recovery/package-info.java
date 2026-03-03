/**
 * Stuck detection and recovery system for resilient agent behavior.
 *
 * <p>This package provides comprehensive stuck detection and recovery mechanisms
 * to handle situations where agents get stuck or fail to make progress. Inspired by
 * research from Baritone and other game automation projects.</p>
 *
 * <h2>Key Components</h2>
 * <ul>
 *   <li>{@link com.minewright.recovery.StuckDetector} - Detects position/progress/state/path stuck conditions</li>
 *   <li>{@link com.minewright.recovery.StuckType} - Categorizes stuck conditions</li>
 *   <li>{@link com.minewright.recovery.RecoveryManager} - Coordinates recovery attempts</li>
*   <li>{@link com.minewright.recovery.RecoveryResult} - Recovery outcome tracking</li>
* </ul>
 *
 * <h3>Recovery Strategies</h3>
 * <p>Located in {@code com.minewright.recovery.strategies}:</p>
 * <ul>
*   <li>{@link com.minewright.recovery.strategies.RepathStrategy} - Try recalculating path</li>
*   <li>{@link com.minewright.recovery.strategies.TeleportStrategy} - Emergency teleport to safety</li>
*   <li>{@link com.minewright.recovery.strategies.AbortStrategy} - Give up and report failure</li>
* </ul>
 *
 * <h2>Stuck Types</h2>
 * <p>The system detects multiple categories of stuck conditions:</p>
 * <ul>
*   <li><b>POSITION_STUCK</b> - Agent hasn't moved for too long</li>
*   <li><b>PROGRESS_STUCK</b> - Task progress has stalled</li>
*   <li><b>STATE_STUCK</b> - Agent is in same state too long</li>
*   <li><b>PATH_STUCK</b> - Pathfinding continuously fails</li>
* </ul>
 *
 * <h2>Recovery Flow</h2>
 * <p>When stuck is detected:</p>
* <ol>
*   <li>StuckDetector identifies stuck type</li>
*   <li>RecoveryManager selects appropriate strategy</li>
*   <li>Strategy executes recovery attempt</li>
*   <li>If recovery fails, escalates to next strategy</li>
*   <li>Tracks success rates for adaptive strategy selection</li>
* </ol>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Check if stuck
* StuckType stuckType = stuckDetector.checkStuck();
* if (stuckType != null) {
*     // Attempt recovery
*     RecoveryResult result = recoveryManager.attemptRecovery(stuckType);
*     if (result.isSuccess()) {
*         LOGGER.info("Recovered from stuck: {}", result.getMessage());
*     }
* }
* }</pre>
 *
 * @since 1.7.0
 * @see com.minewright.recovery.StuckDetector
 * @see com.minewright.recovery.RecoveryManager
 */
package com.minewright.recovery;
