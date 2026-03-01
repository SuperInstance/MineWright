package com.minewright.coordination;

import com.minewright.testutil.TestLogger;

import net.minecraft.core.BlockPos;
import org.slf4j.Logger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks the capabilities and state of an agent for task allocation decisions.
 *
 * <p><b>Capability Dimensions:</b></p>
 * <ul>
 *   <li><b>Skills</b> - Set of capabilities (mining, building, combat, farming, etc.)</li>
 *   <li><b>Proficiencies</b> - Skill level (0.0-1.0) for each skill</li>
 *   <li><b>Tools</b> - Available tools/equipment</li>
 *   <li><b>Position</b> - Current location for distance calculations</li>
 *   <li><b>Load</b> - Current busyness (0.0=idle, 1.0=fully loaded)</li>
 * </ul>
 *
 * <p><b>Bid Scoring:</b></p>
 * <p>The calculateBidScore method computes a score based on:
 * <ul>
 *   <li>Skill matching and proficiency levels</li>
 *   <li>Distance to task location</li>
 *   <li>Current load factor</li>
 *   <li>Tool availability</li>
 * </ul>
 *
 * @since 1.3.0
 */
public class AgentCapability {
    /**
     * Logger for this class.
     * Uses TestLogger to avoid triggering MineWrightMod initialization during tests.
     */
    private static final Logger LOGGER = TestLogger.getLogger(AgentCapability.class);

    /**
     * Standard skill types for MineWright agents.
     */
    public static final class Skills {
        public static final String MINING = "mining";
        public static final String BUILDING = "building";
        public static final String COMBAT = "combat";
        public static final String FARMING = "farming";
        public static final String CRAFTING = "crafting";
        public static final String GATHERING = "gathering";
        public static final String PATHFINDING = "pathfinding";
        public static final String EXPLORATION = "exploration";

        private Skills() {} // Utility class
    }

    private final UUID agentId;
    private final String agentName;
    private final Set<String> skills;
    private final Map<String, Double> proficiencies;
    private final Set<String> availableTools;
    private BlockPos currentPosition;
    private volatile double currentLoad;
    private volatile boolean isActive;

    // Task history for learning/adaptation
    private final Map<String, Integer> completedTaskCounts;
    private final Map<String, Long> lastTaskCompletionTimes;

    /**
     * Creates a new agent capability profile.
     *
     * @param agentId Unique identifier for the agent
     * @param agentName Display name of the agent
     */
    public AgentCapability(UUID agentId, String agentName) {
        if (agentId == null) {
            throw new IllegalArgumentException("Agent ID cannot be null");
        }
        if (agentName == null || agentName.isBlank()) {
            throw new IllegalArgumentException("Agent name cannot be null or blank");
        }

        this.agentId = agentId;
        this.agentName = agentName;
        this.skills = ConcurrentHashMap.newKeySet();
        this.proficiencies = new ConcurrentHashMap<>();
        this.availableTools = ConcurrentHashMap.newKeySet();
        this.currentPosition = BlockPos.ZERO;
        this.currentLoad = 0.0;
        this.isActive = true;
        this.completedTaskCounts = new ConcurrentHashMap<>();
        this.lastTaskCompletionTimes = new ConcurrentHashMap<>();
    }

    // ========== Identity ==========

    public UUID getAgentId() {
        return agentId;
    }

    public String getAgentName() {
        return agentName;
    }

    // ========== Skills ==========

    /**
     * Adds a skill to this agent's capabilities.
     *
     * @param skill The skill to add
     * @return this for chaining
     */
    public AgentCapability addSkill(String skill) {
        if (skill != null && !skill.isBlank()) {
            skills.add(skill.toLowerCase());
            // Set default proficiency if not set
            proficiencies.putIfAbsent(skill.toLowerCase(), 0.5);
        }
        return this;
    }

    /**
     * Adds multiple skills.
     *
     * @param newSkills Collection of skills to add
     * @return this for chaining
     */
    public AgentCapability addSkills(Collection<String> newSkills) {
        if (newSkills != null) {
            newSkills.stream()
                .filter(s -> s != null && !s.isBlank())
                .forEach(this::addSkill);
        }
        return this;
    }

    /**
     * Removes a skill from this agent's capabilities.
     *
     * @param skill The skill to remove
     * @return true if the skill was removed
     */
    public boolean removeSkill(String skill) {
        if (skill == null) return false;
        String key = skill.toLowerCase();
        proficiencies.remove(key);
        return skills.remove(key);
    }

    /**
     * Checks if the agent has a specific skill.
     *
     * @param skill The skill to check
     * @return true if the agent has this skill
     */
    public boolean hasSkill(String skill) {
        return skill != null && skills.contains(skill.toLowerCase());
    }

    /**
     * Gets all skills.
     *
     * @return Unmodifiable set of skills
     */
    public Set<String> getSkills() {
        return Collections.unmodifiableSet(skills);
    }

    // ========== Proficiencies ==========

    /**
     * Sets the proficiency level for a skill.
     *
     * @param skill The skill
     * @param level Proficiency (0.0-1.0)
     * @return this for chaining
     */
    public AgentCapability setProficiency(String skill, double level) {
        if (skill == null || skill.isBlank()) {
            return this;
        }
        if (level < 0.0 || level > 1.0) {
            throw new IllegalArgumentException("Proficiency must be between 0.0 and 1.0");
        }
        String key = skill.toLowerCase();
        proficiencies.put(key, level);
        skills.add(key); // Ensure skill is added
        return this;
    }

    /**
     * Gets the proficiency level for a skill.
     *
     * @param skill The skill to check
     * @return Proficiency (0.0-1.0), or 0.0 if not proficient
     */
    public double getProficiency(String skill) {
        if (skill == null) return 0.0;
        return proficiencies.getOrDefault(skill.toLowerCase(), 0.0);
    }

    /**
     * Gets all proficiencies.
     *
     * @return Unmodifiable map of skill to proficiency
     */
    public Map<String, Double> getProficiencies() {
        return Collections.unmodifiableMap(proficiencies);
    }

    // ========== Tools ==========

    /**
     * Adds a tool to the agent's available tools.
     *
     * @param tool The tool to add
     * @return this for chaining
     */
    public AgentCapability addTool(String tool) {
        if (tool != null && !tool.isBlank()) {
            availableTools.add(tool.toLowerCase());
        }
        return this;
    }

    /**
     * Adds multiple tools.
     *
     * @param newTools Collection of tools to add
     * @return this for chaining
     */
    public AgentCapability addTools(Collection<String> newTools) {
        if (newTools != null) {
            newTools.stream()
                .filter(t -> t != null && !t.isBlank())
                .forEach(this::addTool);
        }
        return this;
    }

    /**
     * Removes a tool.
     *
     * @param tool The tool to remove
     * @return true if removed
     */
    public boolean removeTool(String tool) {
        return tool != null && availableTools.remove(tool.toLowerCase());
    }

    /**
     * Checks if the agent has a specific tool.
     *
     * @param tool The tool to check
     * @return true if available
     */
    public boolean hasTool(String tool) {
        return tool != null && availableTools.contains(tool.toLowerCase());
    }

    /**
     * Gets all available tools.
     *
     * @return Unmodifiable set of tools
     */
    public Set<String> getAvailableTools() {
        return Collections.unmodifiableSet(availableTools);
    }

    /**
     * Checks if the agent has all required tools.
     *
     * @param requiredTools Set of required tools
     * @return true if all are available
     */
    @SuppressWarnings("unchecked")
    public boolean hasTools(Set<String> requiredTools) {
        if (requiredTools == null || requiredTools.isEmpty()) {
            return true;
        }
        return requiredTools.stream()
            .allMatch(this::hasTool);
    }

    // ========== Position ==========

    /**
     * Updates the agent's current position.
     *
     * @param pos New position
     */
    public void updatePosition(BlockPos pos) {
        if (pos != null) {
            this.currentPosition = pos;
        }
    }

    /**
     * Gets the current position.
     *
     * @return Current position
     */
    public BlockPos getCurrentPosition() {
        return currentPosition;
    }

    /**
     * Calculates distance to a target position.
     *
     * @param target Target position
     * @return Euclidean distance in blocks
     */
    public double distanceTo(BlockPos target) {
        if (target == null) {
            return Double.MAX_VALUE;
        }
        double dx = currentPosition.getX() - target.getX();
        double dy = currentPosition.getY() - target.getY();
        double dz = currentPosition.getZ() - target.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    // ========== Load ==========

    /**
     * Updates the agent's current load.
     *
     * @param load Load factor (0.0=idle, 1.0=fully loaded)
     */
    public void updateLoad(double load) {
        this.currentLoad = Math.max(0.0, Math.min(1.0, load));
    }

    /**
     * Gets the current load.
     *
     * @return Load factor (0.0-1.0)
     */
    public double getCurrentLoad() {
        return currentLoad;
    }

    /**
     * Checks if the agent is available for new tasks.
     *
     * @return true if load is below threshold (0.8)
     */
    public boolean isAvailable() {
        return currentLoad < 0.8 && isActive;
    }

    // ========== Active State ==========

    /**
     * Sets whether the agent is active.
     *
     * @param active true if active
     */
    public void setActive(boolean active) {
        this.isActive = active;
    }

    /**
     * Checks if the agent is active.
     *
     * @return true if active
     */
    public boolean isActive() {
        return isActive;
    }

    // ========== Task History ==========

    /**
     * Records a completed task.
     *
     * @param taskType Type of task completed
     */
    public void recordTaskCompletion(String taskType) {
        if (taskType == null || taskType.isBlank()) {
            return;
        }
        completedTaskCounts.merge(taskType, 1, Integer::sum);
        lastTaskCompletionTimes.put(taskType, System.currentTimeMillis());
    }

    /**
     * Gets the count of completed tasks by type.
     *
     * @param taskType Task type to check
     * @return Count of completed tasks
     */
    public int getCompletedTaskCount(String taskType) {
        return completedTaskCounts.getOrDefault(taskType, 0);
    }

    /**
     * Gets the last completion time for a task type.
     *
     * @param taskType Task type to check
     * @return Timestamp of last completion, or 0 if never completed
     */
    public long getLastTaskCompletionTime(String taskType) {
        return lastTaskCompletionTimes.getOrDefault(taskType, 0L);
    }

    // ========== Bid Calculation ==========

    /**
     * Calculates a bid score for a task announcement.
     *
     * <p>The score considers:</p>
     * <ul>
     *   <li>Skill match and proficiency (40% weight)</li>
     *   <li>Distance to task (30% weight)</li>
     *   <li>Current load (20% weight)</li>
     *   <li>Tool availability (10% weight)</li>
     * </ul>
     *
     * @param announcement The task announcement
     * @return Bid score (0.0-1.0)
     */
    public double calculateBidScore(TaskAnnouncement announcement) {
        if (announcement == null || !isActive) {
            return 0.0;
        }

        double skillScore = calculateSkillScore(announcement);
        double distanceScore = calculateDistanceScore(announcement);
        double loadScore = 1.0 - currentLoad;
        double toolScore = calculateToolScore(announcement);

        // Weighted average
        double score = (skillScore * 0.40) +
                       (distanceScore * 0.30) +
                       (loadScore * 0.20) +
                       (toolScore * 0.10);

        LOGGER.debug("Bid score for {}: skill={:.2f}, dist={:.2f}, load={:.2f}, tool={:.2f}, total={:.2f}",
            agentName, skillScore, distanceScore, loadScore, toolScore, score);

        return Math.max(0.0, Math.min(1.0, score));
    }

    /**
     * Calculates the skill match score.
     */
    @SuppressWarnings("unchecked")
    private double calculateSkillScore(TaskAnnouncement announcement) {
        Object requiredSkillsObj = announcement.requirements().get("skills");

        if (!(requiredSkillsObj instanceof Iterable<?> requiredSkills)) {
            // No skill requirements - return average proficiency
            return proficiencies.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.5);
        }

        double totalProficiency = 0.0;
        int skillCount = 0;

        for (Object skillObj : requiredSkills) {
            if (skillObj instanceof String skill) {
                double prof = getProficiency(skill);
                double minProf = announcement.getMinProficiency();

                // If below minimum, significantly penalize
                if (prof < minProf) {
                    prof = prof * 0.3; // 70% penalty for below minimum
                }

                totalProficiency += prof;
                skillCount++;
            }
        }

        return skillCount > 0 ? totalProficiency / skillCount : 0.0;
    }

    /**
     * Calculates the distance score.
     */
    private double calculateDistanceScore(TaskAnnouncement announcement) {
        double maxDistance = announcement.getMaxDistance();
        // NOTE: Task position extraction requires TaskAnnouncement to include target position
        // Currently defaults to 0.0 (no distance penalty)
        double distance = 0.0;

        if (maxDistance == Double.MAX_VALUE) {
            return 1.0; // No distance constraint
        }

        if (distance > maxDistance) {
            return 0.0; // Too far
        }

        // Linear falloff: closer is better
        return 1.0 - (distance / maxDistance);
    }

    /**
     * Calculates the tool availability score.
     */
    @SuppressWarnings("unchecked")
    private double calculateToolScore(TaskAnnouncement announcement) {
        Object requiredToolsObj = announcement.requirements().get("tools");

        if (!(requiredToolsObj instanceof Iterable<?> requiredTools)) {
            return 1.0; // No tool requirements
        }

        int availableCount = 0;
        int totalCount = 0;

        for (Object toolObj : requiredTools) {
            if (toolObj instanceof String tool) {
                totalCount++;
                if (hasTool(tool)) {
                    availableCount++;
                }
            }
        }

        return totalCount > 0 ? (double) availableCount / totalCount : 1.0;
    }

    /**
     * Creates a task bid based on this agent's capabilities.
     *
     * @param announcement The task announcement
     * @param estimatedTime Estimated completion time (milliseconds)
     * @param confidence Confidence in success (0.0-1.0)
     * @return The constructed task bid
     */
    public TaskBid createBid(TaskAnnouncement announcement, long estimatedTime, double confidence) {
        double score = calculateBidScore(announcement);

        Map<String, Object> capabilities = new HashMap<>();
        capabilities.put("proficiencies", new HashMap<>(proficiencies));
        capabilities.put("tools", new HashSet<>(availableTools));
        // NOTE: Distance calculation requires agent position vs task position
        // Currently set to 0.0 as distance is handled in calculateDistanceScore()
        capabilities.put("distance", 0.0);
        capabilities.put("currentLoad", currentLoad);

        return TaskBid.builder()
            .announcementId(announcement.announcementId())
            .bidderId(agentId)
            .score(score)
            .estimatedTime(estimatedTime)
            .confidence(confidence)
            .capabilities(capabilities)
            .build();
    }

    // ========== Object Methods ==========

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AgentCapability that = (AgentCapability) o;
        return agentId.equals(that.agentId);
    }

    @Override
    public int hashCode() {
        return agentId.hashCode();
    }

    @Override
    public String toString() {
        return String.format("AgentCapability[%s (%s), skills=%d, load=%.2f, active=%s]",
            agentName, agentId.toString().substring(0, 8), skills.size(), currentLoad, isActive);
    }

    /**
     * Creates a builder for agent capabilities.
     *
     * @param agentId Agent UUID
     * @param agentName Agent display name
     * @return New builder
     */
    public static Builder builder(UUID agentId, String agentName) {
        return new Builder(agentId, agentName);
    }

    /**
     * Builder for creating AgentCapability instances.
     */
    public static class Builder {
        private final AgentCapability capability;

        private Builder(UUID agentId, String agentName) {
            this.capability = new AgentCapability(agentId, agentName);
        }

        public Builder skill(String skill, double proficiency) {
            capability.addSkill(skill);
            capability.setProficiency(skill, proficiency);
            return this;
        }

        public Builder skills(Map<String, Double> skills) {
            skills.forEach((skill, prof) -> {
                capability.addSkill(skill);
                capability.setProficiency(skill, prof);
            });
            return this;
        }

        public Builder tool(String tool) {
            capability.addTool(tool);
            return this;
        }

        public Builder tools(Collection<String> tools) {
            capability.addTools(tools);
            return this;
        }

        public Builder position(BlockPos pos) {
            capability.updatePosition(pos);
            return this;
        }

        public Builder load(double load) {
            capability.updateLoad(load);
            return this;
        }

        public Builder active(boolean active) {
            capability.setActive(active);
            return this;
        }

        public AgentCapability build() {
            return capability;
        }
    }
}
