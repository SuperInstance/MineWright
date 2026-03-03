/**
 * Task profile system for declarative task sequences.
 *
 * <p>This package implements an Honorbuddy-style profile system where complex task sequences
 * can be defined declaratively in JSON and executed by the profile engine. Profiles enable
 * reusable, shareable automation patterns.</p>
 *
 * <h2>Key Components</h2>
 * <ul>
 *   <li>{@link com.minewright.profile.TaskProfile} - Declarative task sequence definition</li>
 *   <li>{@link com.minewright.profile.ProfileTask} - Individual task within a profile</li>
 *   <li>{@link com.minewright.profile.ProfileParser} - Parse profiles from JSON</li>
 *   <li>{@link com.minewright.profile.ProfileExecutor} - Execute profile tasks</li>
 *   <li>{@link com.minewright.profile.ProfileRegistry} - Store and retrieve profiles</li>
 *   <li>{@link com.minewright.profile.ProfileGenerator} - LLM-driven profile generation</li>
 * </ul>
 *
 * <h2>Design Philosophy</h2>
 * <p>Inspired by Honorbuddy's profile system, which allowed users to share and
* load custom behavior profiles. A profile is a sequence of tasks:</p>
 *
 * <pre>
 * {
 *   "name": "mining_stone",
 *   "tasks": [
 *     {"type": "mine", "target": "stone", "count": 64},
 *     {"type": "deposit", "container": "chest"},
 *     {"type": "return", "location": "home"}
 *   ]
 * }
 * </pre>
 *
 * <h2>Profile Execution</h2>
 * <p>Profiles are executed by the ProfileExecutor which:</p>
 * <ol>
*   <li>Loads profile from registry or generates via LLM</li>
*   <li>Validates task parameters</li>
*   <li>Executes tasks sequentially</li>
*   <li>Handles failures with retry/abort logic</li>
*   <li>Reports progress to listeners</li>
 * </ol>
 *
 * @since 1.7.0
 * @see com.minewright.profile.TaskProfile
 * @see com.minewright.profile.ProfileExecutor
 */
package com.minewright.profile;
