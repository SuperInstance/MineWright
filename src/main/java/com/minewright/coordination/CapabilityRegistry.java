package com.minewright.coordination;

import com.minewright.testutil.TestLogger;

import net.minecraft.core.BlockPos;
import org.slf4j.Logger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Central registry for tracking agent capabilities across the system.
 *
 * <p><b>Purpose:</b></p>
 * <p>Maintains a real-time view of all agents' capabilities, positions,
 * and availability. This enables efficient task allocation decisions
 * through the Contract Net Protocol.</p>
 *
 * <p><b>Thread Safety:</b></p>
 * <ul>
 *   <li>Uses ConcurrentHashMap for thread-safe agent storage</li>
 *   <li>All operations are atomic and thread-safe</li>
 *   <li>Safe for concurrent access from multiple agents</li>
 * </ul>
 *
 * @see AgentCapability
 * @see ContractNetManager
 * @since 1.3.0
 */
public class CapabilityRegistry {
    /**
     * Logger for this class.
     * Uses TestLogger to avoid triggering MineWrightMod initialization during tests.
     */
    private static final Logger LOGGER = TestLogger.getLogger(CapabilityRegistry.class);

    /**
     * Listener for capability changes.
     */
    public interface CapabilityListener {
        /**
         * Called when an agent is registered.
         */
        default void onAgentRegistered(UUID agentId) {}

        /**
         * Called when an agent is unregistered.
         */
        default void onAgentUnregistered(UUID agentId) {}

        /**
         * Called when an agent's capabilities are updated.
         */
        default void onCapabilitiesUpdated(UUID agentId) {}

        /**
         * Called when an agent's position changes.
         */
        default void onPositionChanged(UUID agentId, BlockPos newPos) {}

        /**
         * Called when an agent's load changes.
         */
        default void onLoadChanged(UUID agentId, double newLoad) {}
    }

    private final Map<UUID, AgentCapability> agents;
    private final Map<String, UUID> nameToId;
    private final List<CapabilityListener> listeners;

    /**
     * Creates a new capability registry.
     */
    public CapabilityRegistry() {
        this.agents = new ConcurrentHashMap<>();
        this.nameToId = new ConcurrentHashMap<>();
        this.listeners = new ArrayList<>();
    }

    // ========== Registration ==========

    /**
     * Registers an agent with the given capabilities.
     *
     * @param agentId Agent UUID
     * @param capability Agent capabilities
     * @throws IllegalStateException if agent already registered
     */
    public void register(UUID agentId, AgentCapability capability) {
        if (agentId == null) {
            throw new IllegalArgumentException("Agent ID cannot be null");
        }
        if (capability == null) {
            throw new IllegalArgumentException("Capability cannot be null");
        }
        if (agents.containsKey(agentId)) {
            throw new IllegalStateException("Agent already registered: " + agentId);
        }

        agents.put(agentId, capability);
        nameToId.put(capability.getAgentName().toLowerCase(), agentId);

        LOGGER.info("Registered agent capability: {} ({}) with {} skills",
            capability.getAgentName(), agentId.toString().substring(0, 8),
            capability.getSkills().size());

        // Notify listeners
        listeners.forEach(l -> {
            try {
                l.onAgentRegistered(agentId);
            } catch (Exception e) {
                LOGGER.warn("Listener error in onAgentRegistered", e);
            }
        });
    }

    /**
     * Unregisters an agent from the registry.
     *
     * @param agentId Agent UUID
     * @return The removed capability, or null if not found
     */
    public AgentCapability unregister(UUID agentId) {
        if (agentId == null) {
            return null;
        }

        AgentCapability removed = agents.remove(agentId);

        if (removed != null) {
            nameToId.remove(removed.getAgentName().toLowerCase());

            LOGGER.info("Unregistered agent capability: {} ({})",
                removed.getAgentName(), agentId.toString().substring(0, 8));

            // Notify listeners
            listeners.forEach(l -> {
                try {
                    l.onAgentUnregistered(agentId);
                } catch (Exception e) {
                    LOGGER.warn("Listener error in onAgentUnregistered", e);
                }
            });
        }

        return removed;
    }

    /**
     * Unregisters an agent by name.
     *
     * @param agentName Agent name
     * @return The removed capability, or null if not found
     */
    public AgentCapability unregisterByName(String agentName) {
        if (agentName == null) {
            return null;
        }
        UUID agentId = nameToId.get(agentName.toLowerCase());
        return agentId != null ? unregister(agentId) : null;
    }

    /**
     * Checks if an agent is registered.
     *
     * @param agentId Agent UUID
     * @return true if registered
     */
    public boolean isRegistered(UUID agentId) {
        return agents.containsKey(agentId);
    }

    /**
     * Checks if an agent name is registered.
     *
     * @param agentName Agent name
     * @return true if registered
     */
    public boolean isRegisteredName(String agentName) {
        return agentName != null && nameToId.containsKey(agentName.toLowerCase());
    }

    // ========== Access ==========

    /**
     * Gets an agent's capabilities by ID.
     *
     * @param agentId Agent UUID
     * @return The capability, or null if not found
     */
    public AgentCapability getCapability(UUID agentId) {
        return agents.get(agentId);
    }

    /**
     * Gets an agent's capabilities by name.
     *
     * @param agentName Agent name
     * @return The capability, or null if not found
     */
    public AgentCapability getCapabilityByName(String agentName) {
        if (agentName == null) {
            return null;
        }
        UUID agentId = nameToId.get(agentName.toLowerCase());
        return agentId != null ? agents.get(agentId) : null;
    }

    /**
     * Gets all registered agents.
     *
     * @return Unmodifiable collection of capabilities
     */
    public Collection<AgentCapability> getAllAgents() {
        return Collections.unmodifiableCollection(agents.values());
    }

    /**
     * Gets the count of registered agents.
     *
     * @return Agent count
     */
    public int getAgentCount() {
        return agents.size();
    }

    // ========== Updates ==========

    /**
     * Updates an agent's position.
     *
     * @param agentId Agent UUID
     * @param pos New position
     */
    public void updatePosition(UUID agentId, BlockPos pos) {
        AgentCapability capability = agents.get(agentId);
        if (capability != null) {
            capability.updatePosition(pos);

            // Notify listeners
            listeners.forEach(l -> {
                try {
                    l.onPositionChanged(agentId, pos);
                } catch (Exception e) {
                    LOGGER.warn("Listener error in onPositionChanged", e);
                }
            });
        }
    }

    /**
     * Updates an agent's load.
     *
     * @param agentId Agent UUID
     * @param load New load factor (0.0-1.0)
     */
    public void updateLoad(UUID agentId, double load) {
        AgentCapability capability = agents.get(agentId);
        if (capability != null) {
            capability.updateLoad(load);

            // Notify listeners
            listeners.forEach(l -> {
                try {
                    l.onLoadChanged(agentId, load);
                } catch (Exception e) {
                    LOGGER.warn("Listener error in onLoadChanged", e);
                }
            });
        }
    }

    /**
     * Marks an agent as active/inactive.
     *
     * @param agentId Agent UUID
     * @param active Active state
     */
    public void updateActive(UUID agentId, boolean active) {
        AgentCapability capability = agents.get(agentId);
        if (capability != null) {
            capability.setActive(active);

            // Notify listeners
            listeners.forEach(l -> {
                try {
                    l.onCapabilitiesUpdated(agentId);
                } catch (Exception e) {
                    LOGGER.warn("Listener error in onCapabilitiesUpdated", e);
                }
            });
        }
    }

    // ========== Queries ==========

    /**
     * Finds all agents with a specific skill.
     *
     * @param skill Skill to search for
     * @return List of agents with the skill
     */
    public List<AgentCapability> findCapableAgents(String skill) {
        if (skill == null || skill.isBlank()) {
            return List.of();
        }

        return agents.values().stream()
            .filter(cap -> cap.hasSkill(skill))
            .collect(Collectors.toList());
    }

    /**
     * Finds all agents with required skills meeting minimum proficiency.
     *
     * @param requiredSkills Map of skill to minimum proficiency
     * @return List of capable agents
     */
    public List<AgentCapability> findCapableAgents(Map<String, Double> requiredSkills) {
        if (requiredSkills == null || requiredSkills.isEmpty()) {
            return new ArrayList<>(agents.values());
        }

        return agents.values().stream()
            .filter(cap -> requiredSkills.entrySet().stream()
                .allMatch(entry -> {
                    double prof = cap.getProficiency(entry.getKey());
                    return prof >= entry.getValue();
                }))
            .collect(Collectors.toList());
    }

    /**
     * Gets agents nearby a position.
     *
     * @param pos Center position
     * @param radius Search radius in blocks
     * @return List of agents within radius
     */
    public List<AgentCapability> getNearbyAgents(BlockPos pos, double radius) {
        if (pos == null) {
            return List.of();
        }

        return agents.values().stream()
            .filter(cap -> {
                double distance = cap.distanceTo(pos);
                return distance <= radius;
            })
            .collect(Collectors.toList());
    }

    /**
     * Gets available agents (not at full capacity).
     *
     * @return List of available agents
     */
    public List<AgentCapability> getAvailableAgents() {
        return agents.values().stream()
            .filter(AgentCapability::isActive)
            .filter(AgentCapability::isAvailable)
            .collect(Collectors.toList());
    }

    /**
     * Gets the best agent for a task based on capabilities.
     *
     * @param requiredSkills Map of skill to minimum proficiency
     * @param taskPosition Task location (or null)
     * @return Best agent, or empty if none suitable
     */
    public Optional<AgentCapability> findBestAgent(
        Map<String, Double> requiredSkills,
        BlockPos taskPosition
    ) {
        List<AgentCapability> candidates = findCapableAgents(requiredSkills).stream()
            .filter(AgentCapability::isActive)
            .filter(AgentCapability::isAvailable)
            .collect(Collectors.toList());

        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        // Score each candidate
        return candidates.stream()
            .max(Comparator.comparingDouble(cap -> {
                double skillScore = requiredSkills.entrySet().stream()
                    .mapToDouble(entry -> {
                        double prof = cap.getProficiency(entry.getKey());
                        return prof / entry.getValue(); // Ratio of proficiency to requirement
                    })
                    .average()
                    .orElse(0.5);

                double loadScore = 1.0 - cap.getCurrentLoad();
                double distanceScore = 1.0;

                if (taskPosition != null) {
                    double distance = cap.distanceTo(taskPosition);
                    distanceScore = 1.0 / (1.0 + distance / 100.0); // Decay with distance
                }

                return (skillScore * 0.5) + (loadScore * 0.3) + (distanceScore * 0.2);
            }));
    }

    /**
     * Gets agents sorted by availability (lowest load first).
     *
     * @return List of agents sorted by load
     */
    public List<AgentCapability> getAgentsByAvailability() {
        return agents.values().stream()
            .filter(AgentCapability::isActive)
            .sorted(Comparator.comparingDouble(AgentCapability::getCurrentLoad))
            .collect(Collectors.toList());
    }

    // ========== Listeners ==========

    /**
     * Adds a capability change listener.
     *
     * @param listener The listener to add
     */
    public void addListener(CapabilityListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /**
     * Removes a capability change listener.
     *
     * @param listener The listener to remove
     */
    public void removeListener(CapabilityListener listener) {
        listeners.remove(listener);
    }

    // ========== Cleanup ==========

    /**
     * Removes inactive agents.
     *
     * @return Number of agents removed
     */
    public int cleanupInactive() {
        int before = agents.size();

        Iterator<Map.Entry<UUID, AgentCapability>> it = agents.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, AgentCapability> entry = it.next();
            if (!entry.getValue().isActive()) {
                it.remove();
                nameToId.remove(entry.getValue().getAgentName().toLowerCase());
            }
        }

        int removed = before - agents.size();
        if (removed > 0) {
            LOGGER.info("Cleaned up {} inactive agents", removed);
        }

        return removed;
    }

    /**
     * Clears all registered agents.
     */
    public void clear() {
        int count = agents.size();
        agents.clear();
        nameToId.clear();

        if (count > 0) {
            LOGGER.info("Cleared {} agents from capability registry", count);
        }
    }

    @Override
    public String toString() {
        return String.format("CapabilityRegistry[agents=%d, active=%d]",
            agents.size(),
            (int) agents.values().stream().filter(AgentCapability::isActive).count()
        );
    }
}
