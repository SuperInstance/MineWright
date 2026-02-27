package com.minewright.orchestration;

/**
 * Defines the role of an agent in the hierarchical orchestration system.
 *
 * <p>Agents can have different roles that determine their responsibilities
 * and capabilities in the multi-agent coordination system.</p>
 *
 * <p><b>Hierarchy:</b></p>
 * <pre>
 * HUMAN (Player)
 *    └── FOREMAN (Orchestrator MineWright)
 *          ├── WORKER (Standard MineWright agents)
 *          └── SPECIALIST (Task-specific agents)
 * </pre>
 *
 * @since 1.2.0
 */
public enum AgentRole {

    /**
     * Foreman role - the primary orchestrator that coordinates other agents.
     *
     * <p>Responsibilities:</p>
     * <ul>
     *   <li>Receives high-level commands from the human player</li>
     *   <li>Decomposes goals into distributable tasks</li>
     *   <li>Assigns tasks to workers and specialists</li>
     *   <li>Monitors progress and handles failures</li>
     *   <li>Reports status back to the human</li>
     *   <li>Can perform tasks if no workers available</li>
     * </ul>
     *
     * <p>There should typically be only ONE foreman in a team.</p>
     */
    FOREMAN("Foreman", true, true),

    /**
     * Worker role - standard agent that executes assigned tasks.
     *
     * <p>Responsibilities:</p>
     * <ul>
     *   <li>Executes tasks assigned by the foreman</li>
     *   <li>Reports progress and completion</li>
     *   <li>Requests help when stuck</li>
     *   <li>Can coordinate with other workers on collaborative tasks</li>
     * </ul>
     */
    WORKER("Worker", false, true),

    /**
     * Specialist role - agent with specific expertise.
     *
     * <p>Responsibilities:</p>
     * <ul>
     *   <li>Handles specific types of tasks (mining, building, farming, etc.)</li>
     *   <li>Can provide guidance to other agents in their specialty</li>
     *   <li>May have enhanced capabilities in their domain</li>
     * </ul>
     */
    SPECIALIST("Specialist", false, true),

    /**
     * Solo role - agent operating independently without orchestration.
     *
     * <p>Used for backward compatibility when multi-agent features are disabled.</p>
     */
    SOLO("Solo", false, false);

    private final String displayName;
    private final boolean canOrchestrate;
    private final boolean canExecuteTasks;

    AgentRole(String displayName, boolean canOrchestrate, boolean canExecuteTasks) {
        this.displayName = displayName;
        this.canOrchestrate = canOrchestrate;
        this.canExecuteTasks = canExecuteTasks;
    }

    /**
     * Returns the human-readable display name.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns true if this role can orchestrate other agents.
     */
    public boolean canOrchestrate() {
        return canOrchestrate;
    }

    /**
     * Returns true if this role can execute tasks directly.
     */
    public boolean canExecuteTasks() {
        return canExecuteTasks;
    }

    /**
     * Returns true if this role should participate in task distribution.
     */
    public boolean isPartOfTeam() {
        return this != SOLO;
    }
}
