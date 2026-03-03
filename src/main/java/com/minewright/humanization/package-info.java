/**
 * Human-like behavior utilities for MineWright agents.
 *
 * <p>This package provides utilities to make AI agents behave more naturally
 * and human-like, reducing the robotic feel of automated actions. These utilities
 * were inspired by game automation research from projects like WoW Glider and
 * Honorbuddy, which found that adding small imperfections and delays makes
 * automated behavior feel more natural.</p>
 *
 * <h2>Key Components</h2>
 * <ul>
 *   <li>{@link com.minewright.humanization.HumanizationUtils} - Gaussian jitter, reaction times,
 *       coordinate noise generation</li>
 *   <li>{@link com.minewright.humanization.MistakeSimulator} - Probabilistic mistake triggering
 *       for realistic error simulation</li>
 *   <li>{@link com.minewright.humanization.IdleBehaviorController} - Natural idle actions
 *       (looking around, stretching, fidgeting)</li>
 *   <li>{@link com.minewright.humanization.SessionManager} - Play session tracking
 *       for fatigue and break simulation</li>
 * </ul>
 *
 * <h2>Design Philosophy</h2>
 * <p>Research from game automation communities has shown that perfect, instantaneous actions
 * are easily detectable as bot behavior. By introducing:</p>
 * <ul>
 *   <li>Variable reaction times (100-500ms with Gaussian distribution)</li>
 *   <li>Occasional mistakes (1-5% error rate depending on task complexity)</li>
 *   <li>Natural idle behaviors during downtime</li>
 *   <li>Session-based fatigue simulation</li>
 * </ul>
 *
 * <p>Agents appear more like human players and less like automated scripts.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Add human-like delay before action
 * int delay = HumanizationUtils.getReactionTimeMs();
 * Thread.sleep(delay);
 *
 * // Possibly introduce a mistake
 * if (MistakeSimulator.shouldMakeMistake(taskComplexity)) {
 *     // Simulate a minor error
 *     position = HumanizationUtils.addJitter(position, 0.5);
 * }
 *
 * // Trigger idle behavior when bored
 * idleController.tick(); // May look around, stretch, etc.
 * }</pre>
 *
 * @since 1.7.0
 * @see com.minewright.humanization.HumanizationUtils
 * @see com.minewright.humanization.MistakeSimulator
 */
package com.minewright.humanization;
