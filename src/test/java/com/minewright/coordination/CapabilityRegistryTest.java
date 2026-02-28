package com.minewright.coordination;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for {@link CapabilityRegistry}.
 *
 * <p>Tests cover the capability registry functionality including:</p>
 * <ul>
 *   <li>Agent registration and unregistration</li>
 *   <li>Capability lookup by ID and name</li>
 *   <li>Position and load updates</li>
 *   <li>Skill-based queries</li>
 *   <li>Proximity searches</li>
 *   <li>Availability filtering</li>
 *   <li>Best agent selection</li>
 *   <li>Listener notifications</li>
 *   <li>Cleanup operations</li>
 *   <li>Thread safety</li>
 * </ul>
 *
 * @see CapabilityRegistry
 * @see AgentCapability
 */
@DisplayName("Capability Registry Tests")
@ExtendWith(MockitoExtension.class)
class CapabilityRegistryTest {

    private CapabilityRegistry registry;
    private UUID agentId;
    private String agentName;
    private AgentCapability capability;

    @Mock
    private CapabilityRegistry.CapabilityListener listener;

    @BeforeEach
    void setUp() {
        registry = new CapabilityRegistry();
        agentId = UUID.randomUUID();
        agentName = "TestAgent";
        capability = new AgentCapability(agentId, agentName);
    }

    @Nested
    @DisplayName("Registration Tests")
    class RegistrationTests {

        @Test
        @DisplayName("Register adds capability to registry")
        void registerAddsCapability() {
            registry.register(agentId, capability);

            assertTrue(registry.isRegistered(agentId));
            assertEquals(1, registry.getAgentCount());
        }

        @Test
        @DisplayName("Register throws on duplicate agent ID")
        void registerThrowsOnDuplicate() {
            registry.register(agentId, capability);

            assertThrows(IllegalStateException.class,
                    () -> registry.register(agentId, capability),
                    "Should throw when registering same agent twice");
        }

        @Test
        @DisplayName("Register throws on null agent ID")
        void registerThrowsOnNullAgentId() {
            assertThrows(IllegalArgumentException.class,
                    () -> registry.register(null, capability),
                    "Should throw for null agent ID");
        }

        @Test
        @DisplayName("Register throws on null capability")
        void registerThrowsOnNullCapability() {
            assertThrows(IllegalArgumentException.class,
                    () -> registry.register(agentId, null),
                    "Should throw for null capability");
        }

        @Test
        @DisplayName("Register notifies listeners")
        void registerNotifiesListeners() {
            registry.addListener(listener);

            registry.register(agentId, capability);

            verify(listener, times(1)).onAgentRegistered(agentId);
        }

        @Test
        @DisplayName("Register allows lookup by name")
        void registerAllowsLookupByName() {
            registry.register(agentId, capability);

            assertTrue(registry.isRegisteredName(agentName));
            assertEquals(capability, registry.getCapabilityByName(agentName));
        }

        @Test
        @DisplayName("Register name lookup is case insensitive")
        void registerNameLookupIsCaseInsensitive() {
            registry.register(agentId, capability);

            assertTrue(registry.isRegisteredName(agentName.toLowerCase()));
            assertTrue(registry.isRegisteredName(agentName.toUpperCase()));
        }

        @Test
        @DisplayName("Register multiple agents")
        void registerMultipleAgents() {
            UUID agent2 = UUID.randomUUID();
            UUID agent3 = UUID.randomUUID();
            AgentCapability cap2 = new AgentCapability(agent2, "Agent2");
            AgentCapability cap3 = new AgentCapability(agent3, "Agent3");

            registry.register(agentId, capability);
            registry.register(agent2, cap2);
            registry.register(agent3, cap3);

            assertEquals(3, registry.getAgentCount());
            assertTrue(registry.isRegistered(agentId));
            assertTrue(registry.isRegistered(agent2));
            assertTrue(registry.isRegistered(agent3));
        }
    }

    @Nested
    @DisplayName("Unregistration Tests")
    class UnregistrationTests {

        @BeforeEach
        void setUp() {
            registry.register(agentId, capability);
        }

        @Test
        @DisplayName("Unregister removes agent from registry")
        void unregisterRemovesAgent() {
            AgentCapability removed = registry.unregister(agentId);

            assertEquals(capability, removed);
            assertFalse(registry.isRegistered(agentId));
            assertEquals(0, registry.getAgentCount());
        }

        @Test
        @DisplayName("Unregister notifies listeners")
        void unregisterNotifiesListeners() {
            registry.addListener(listener);

            registry.unregister(agentId);

            verify(listener, times(1)).onAgentUnregistered(agentId);
        }

        @Test
        @DisplayName("Unregister removes name mapping")
        void unregisterRemovesNameMapping() {
            registry.unregister(agentId);

            assertFalse(registry.isRegisteredName(agentName));
            assertNull(registry.getCapabilityByName(agentName));
        }

        @Test
        @DisplayName("Unregister non-existent agent returns null")
        void unregisterNonExistentReturnsNull() {
            UUID unknownId = UUID.randomUUID();

            AgentCapability removed = registry.unregister(unknownId);

            assertNull(removed);
        }

        @Test
        @DisplayName("Unregister by name removes agent")
        void unregisterByNameRemovesAgent() {
            AgentCapability removed = registry.unregisterByName(agentName);

            assertEquals(capability, removed);
            assertFalse(registry.isRegistered(agentId));
        }

        @Test
        @DisplayName("Unregister by name for non-existent returns null")
        void unregisterByNameForNonExistentReturnsNull() {
            registry.unregister(agentId);

            AgentCapability removed = registry.unregisterByName(agentName);

            assertNull(removed);
        }

        @Test
        @DisplayName("Unregister null returns null")
        void unregisterNullReturnsNull() {
            AgentCapability removed = registry.unregister(null);

            assertNull(removed);
        }

        @Test
        @DisplayName("Unregister by name null returns null")
        void unregisterByNameNullReturnsNull() {
            AgentCapability removed = registry.unregisterByName(null);

            assertNull(removed);
        }
    }

    @Nested
    @DisplayName("Capability Access Tests")
    class AccessTests {

        @BeforeEach
        void setUp() {
            registry.register(agentId, capability);
        }

        @Test
        @DisplayName("Get capability returns registered capability")
        void getCapabilityReturnsRegistered() {
            AgentCapability retrieved = registry.getCapability(agentId);

            assertEquals(capability, retrieved);
        }

        @Test
        @DisplayName("Get capability for non-existent returns null")
        void getCapabilityForNonExistentReturnsNull() {
            AgentCapability retrieved = registry.getCapability(UUID.randomUUID());

            assertNull(retrieved);
        }

        @Test
        @DisplayName("Get capability by name returns correct agent")
        void getCapabilityByNameReturnsCorrect() {
            AgentCapability retrieved = registry.getCapabilityByName(agentName);

            assertEquals(capability, retrieved);
        }

        @Test
        @DisplayName("Get capability by name for non-existent returns null")
        void getCapabilityByNameForNonExistentReturnsNull() {
            AgentCapability retrieved = registry.getCapabilityByName("Unknown");

            assertNull(retrieved);
        }

        @Test
        @DisplayName("Get capability by name with null returns null")
        void getCapabilityByNameNullReturnsNull() {
            AgentCapability retrieved = registry.getCapabilityByName(null);

            assertNull(retrieved);
        }

        @Test
        @DisplayName("Get all agents returns unmodifiable collection")
        void getAllAgentsReturnsUnmodifiable() {
            registry.register(UUID.randomUUID(), new AgentCapability(UUID.randomUUID(), "Agent2"));

            var agents = registry.getAllAgents();

            assertEquals(2, agents.size());
            assertThrows(UnsupportedOperationException.class,
                    () -> agents.clear(),
                    "Should not be able to modify returned collection");
        }

        @Test
        @DisplayName("Get agent count returns correct number")
        void getAgentCountReturnsCorrectNumber() {
            assertEquals(1, registry.getAgentCount());

            UUID agent2 = UUID.randomUUID();
            registry.register(agent2, new AgentCapability(agent2, "Agent2"));

            assertEquals(2, registry.getAgentCount());

            registry.unregister(agent2);

            assertEquals(1, registry.getAgentCount());
        }
    }

    @Nested
    @DisplayName("Position Update Tests")
    class PositionUpdateTests {

        @BeforeEach
        void setUp() {
            registry.register(agentId, capability);
        }

        @Test
        @DisplayName("Update position changes agent position")
        void updatePositionChangesAgentPosition() {
            BlockPos newPos = new BlockPos(10, 64, 20);

            registry.updatePosition(agentId, newPos);

            assertEquals(newPos, capability.getCurrentPosition());
        }

        @Test
        @DisplayName("Update position notifies listeners")
        void updatePositionNotifiesListeners() {
            registry.addListener(listener);
            BlockPos newPos = new BlockPos(10, 64, 20);

            registry.updatePosition(agentId, newPos);

            verify(listener, times(1)).onPositionChanged(agentId, newPos);
        }

        @Test
        @DisplayName("Update position for non-existent agent does nothing")
        void updatePositionForNonExistentDoesNothing() {
            BlockPos newPos = new BlockPos(10, 64, 20);

            assertDoesNotThrow(() -> registry.updatePosition(UUID.randomUUID(), newPos));
        }

        @Test
        @DisplayName("Update position with null does nothing")
        void updatePositionWithNullDoesNothing() {
            BlockPos original = capability.getCurrentPosition();

            assertDoesNotThrow(() -> registry.updatePosition(agentId, null));

            assertEquals(original, capability.getCurrentPosition());
        }
    }

    @Nested
    @DisplayName("Load Update Tests")
    class LoadUpdateTests {

        @BeforeEach
        void setUp() {
            registry.register(agentId, capability);
        }

        @Test
        @DisplayName("Update load changes agent load")
        void updateLoadChangesAgentLoad() {
            registry.updateLoad(agentId, 0.7);

            assertEquals(0.7, capability.getCurrentLoad());
        }

        @Test
        @DisplayName("Update load notifies listeners")
        void updateLoadNotifiesListeners() {
            registry.addListener(listener);

            registry.updateLoad(agentId, 0.5);

            verify(listener, times(1)).onLoadChanged(agentId, 0.5);
        }

        @Test
        @DisplayName("Update load for non-existent agent does nothing")
        void updateLoadForNonExistentDoesNothing() {
            assertDoesNotThrow(() -> registry.updateLoad(UUID.randomUUID(), 0.5));
        }
    }

    @Nested
    @DisplayName("Active State Update Tests")
    class ActiveStateUpdateTests {

        @BeforeEach
        void setUp() {
            registry.register(agentId, capability);
        }

        @Test
        @DisplayName("Update active changes agent state")
        void updateActiveChangesAgentState() {
            registry.updateActive(agentId, false);

            assertFalse(capability.isActive());
        }

        @Test
        @DisplayName("Update active notifies listeners")
        void updateActiveNotifiesListeners() {
            registry.addListener(listener);

            registry.updateActive(agentId, false);

            verify(listener, times(1)).onCapabilitiesUpdated(agentId);
        }

        @Test
        @DisplayName("Update active for non-existent agent does nothing")
        void updateActiveForNonExistentDoesNothing() {
            assertDoesNotThrow(() -> registry.updateActive(UUID.randomUUID(), false));
        }
    }

    @Nested
    @DisplayName("Query Tests - Find Capable Agents")
    class QueryTests {

        private UUID agent1;
        private UUID agent2;
        private UUID agent3;

        @BeforeEach
        void setUp() {
            agent1 = UUID.randomUUID();
            agent2 = UUID.randomUUID();
            agent3 = UUID.randomUUID();

            AgentCapability cap1 = new AgentCapability(agent1, "Miner")
                    .addSkill("mining")
                    .setProficiency("mining", 0.9);
            AgentCapability cap2 = new AgentCapability(agent2, "Builder")
                    .addSkill("building")
                    .setProficiency("building", 0.8);
            AgentCapability cap3 = new AgentCapability(agent3, "Multi")
                    .addSkill("mining")
                    .setProficiency("mining", 0.7)
                    .addSkill("building")
                    .setProficiency("building", 0.6);

            registry.register(agent1, cap1);
            registry.register(agent2, cap2);
            registry.register(agent3, cap3);
        }

        @Test
        @DisplayName("Find capable agents returns only those with skill")
        void findCapableAgentsReturnsOnlyCapable() {
            List<AgentCapability> miners = registry.findCapableAgents("mining");

            assertEquals(2, miners.size());
            assertTrue(miners.stream().allMatch(cap -> cap.hasSkill("mining")));
        }

        @Test
        @DisplayName("Find capable agents is case insensitive")
        void findCapableAgentsIsCaseInsensitive() {
            List<AgentCapability> miners = registry.findCapableAgents("MINING");

            assertEquals(2, miners.size());
        }

        @Test
        @DisplayName("Find capable agents returns empty for non-existent skill")
        void findCapableAgentsReturnsEmptyForNonExistentSkill() {
            List<AgentCapability> agents = registry.findCapableAgents("combat");

            assertTrue(agents.isEmpty());
        }

        @Test
        @DisplayName("Find capable agents with null returns empty")
        void findCapableAgentsWithNullReturnsEmpty() {
            List<AgentCapability> agents = registry.findCapableAgents((String) null);

            assertTrue(agents.isEmpty());
        }

        @Test
        @DisplayName("Find capable agents with blank returns empty")
        void findCapableAgentsWithBlankReturnsEmpty() {
            List<AgentCapability> agents = registry.findCapableAgents("");

            assertTrue(agents.isEmpty());
        }

        @Test
        @DisplayName("Find capable agents with skill map")
        void findCapableAgentsWithSkillMap() {
            Map<String, Double> required = Map.of("mining", 0.8, "building", 0.5);

            List<AgentCapability> agents = registry.findCapableAgents(required);

            assertEquals(1, agents.size(),
                    "Only agent3 should have both skills at required levels");
            assertEquals(agent3, agents.get(0).getAgentId());
        }

        @Test
        @DisplayName("Find capable agents with empty map returns all")
        void findCapableAgentsWithEmptyMapReturnsAll() {
            List<AgentCapability> agents = registry.findCapableAgents(Map.of());

            assertEquals(3, agents.size());
        }

        @Test
        @DisplayName("Find capable agents with null map returns all")
        void findCapableAgentsWithNullMapReturnsAll() {
            List<AgentCapability> agents = registry.findCapableAgents((Map<String, Double>) null);

            assertEquals(3, agents.size());
        }
    }

    @Nested
    @DisplayName("Query Tests - Nearby Agents")
    class NearbyAgentsTests {

        private UUID agent1;
        private UUID agent2;
        private UUID agent3;

        @BeforeEach
        void setUp() {
            agent1 = UUID.randomUUID();
            agent2 = UUID.randomUUID();
            agent3 = UUID.randomUUID();

            AgentCapability cap1 = new AgentCapability(agent1, "Agent1");
            cap1.updatePosition(new BlockPos(0, 64, 0));

            AgentCapability cap2 = new AgentCapability(agent2, "Agent2");
            cap2.updatePosition(new BlockPos(10, 64, 0));

            AgentCapability cap3 = new AgentCapability(agent3, "Agent3");
            cap3.updatePosition(new BlockPos(50, 64, 0));

            registry.register(agent1, cap1);
            registry.register(agent2, cap2);
            registry.register(agent3, cap3);
        }

        @Test
        @DisplayName("Get nearby agents respects radius")
        void getNearbyAgentsRespectsRadius() {
            BlockPos center = new BlockPos(0, 64, 0);
            List<AgentCapability> nearby = registry.getNearbyAgents(center, 15.0);

            assertEquals(2, nearby.size(),
                    "Should find agents within 15 blocks");
            assertTrue(nearby.stream().allMatch(cap ->
                    cap.distanceTo(center) <= 15.0));
        }

        @Test
        @DisplayName("Get nearby agents includes center agent")
        void getNearbyAgentsIncludesCenterAgent() {
            BlockPos center = new BlockPos(0, 64, 0);
            List<AgentCapability> nearby = registry.getNearbyAgents(center, 5.0);

            assertEquals(1, nearby.size());
            assertEquals(agent1, nearby.get(0).getAgentId());
        }

        @Test
        @DisplayName("Get nearby agents with null position returns empty")
        void getNearbyAgentsWithNullReturnsEmpty() {
            List<AgentCapability> nearby = registry.getNearbyAgents(null, 10.0);

            assertTrue(nearby.isEmpty());
        }

        @Test
        @DisplayName("Get nearby agents with zero radius returns only agents at position")
        void getNearbyAgentsWithZeroRadius() {
            BlockPos center = new BlockPos(0, 64, 0);
            List<AgentCapability> nearby = registry.getNearbyAgents(center, 0.0);

            assertEquals(1, nearby.size());
        }

        @Test
        @DisplayName("Get nearby agents uses 3D distance")
        void getNearbyAgentsUses3DDistance() {
            // Agent at (0, 64, 0)
            // Add agent at (0, 70, 0) - 6 blocks away in Y
            UUID agent4 = UUID.randomUUID();
            AgentCapability cap4 = new AgentCapability(agent4, "Agent4");
            cap4.updatePosition(new BlockPos(0, 70, 0));
            registry.register(agent4, cap4);

            BlockPos center = new BlockPos(0, 64, 0);
            List<AgentCapability> nearby = registry.getNearbyAgents(center, 6.0);

            assertEquals(2, nearby.size(),
                    "Should include agent 6 blocks away in Y");
        }
    }

    @Nested
    @DisplayName("Query Tests - Available Agents")
    class AvailableAgentsTests {

        @BeforeEach
        void setUp() {
            UUID agent1 = UUID.randomUUID();
            UUID agent2 = UUID.randomUUID();
            UUID agent3 = UUID.randomUUID();

            AgentCapability cap1 = new AgentCapability(agent1, "Agent1");
            cap1.updateLoad(0.3); // Available

            AgentCapability cap2 = new AgentCapability(agent2, "Agent2");
            cap2.updateLoad(0.9); // Not available (too high load)

            AgentCapability cap3 = new AgentCapability(agent3, "Agent3");
            cap3.updateLoad(0.5);
            cap3.setActive(false); // Not available (inactive)

            registry.register(agent1, cap1);
            registry.register(agent2, cap2);
            registry.register(agent3, cap3);
        }

        @Test
        @DisplayName("Get available agents filters by availability")
        void getAvailableAgentsFiltersByAvailability() {
            List<AgentCapability> available = registry.getAvailableAgents();

            assertEquals(1, available.size(),
                    "Only agent1 should be available");
        }

        @Test
        @DisplayName("Get available agents respects load threshold")
        void getAvailableAgentsRespectsLoadThreshold() {
            UUID agent4 = UUID.randomUUID();
            AgentCapability cap4 = new AgentCapability(agent4, "Agent4");
            cap4.updateLoad(0.79); // Just below threshold
            registry.register(agent4, cap4);

            UUID agent5 = UUID.randomUUID();
            AgentCapability cap5 = new AgentCapability(agent5, "Agent5");
            cap5.updateLoad(0.8); // At threshold
            registry.register(agent5, cap5);

            List<AgentCapability> available = registry.getAvailableAgents();

            assertTrue(available.stream().anyMatch(cap -> cap.getAgentId().equals(agent4)),
                    "Agent with load 0.79 should be available");
            assertFalse(available.stream().anyMatch(cap -> cap.getAgentId().equals(agent5)),
                    "Agent with load 0.8 should not be available");
        }

        @Test
        @DisplayName("Get available agents requires active state")
        void getAvailableAgentsRequiresActiveState() {
            UUID agent4 = UUID.randomUUID();
            AgentCapability cap4 = new AgentCapability(agent4, "Agent4");
            cap4.updateLoad(0.0);
            cap4.setActive(true);
            registry.register(agent4, cap4);

            List<AgentCapability> available = registry.getAvailableAgents();

            assertEquals(2, available.size(),
                    "Should include both active agents with low load");
        }
    }

    @Nested
    @DisplayName("Query Tests - Best Agent")
    class BestAgentTests {

        private UUID agent1;
        private UUID agent2;
        private UUID agent3;

        @BeforeEach
        void setUp() {
            agent1 = UUID.randomUUID();
            agent2 = UUID.randomUUID();
            agent3 = UUID.randomUUID();

            AgentCapability cap1 = new AgentCapability(agent1, "SkilledFar");
                cap1.addSkill("mining");
                cap1.setProficiency("mining", 0.9);
                cap1.updateLoad(0.2);
                cap1.updatePosition(new BlockPos(100, 64, 100));

            AgentCapability cap2 = new AgentCapability(agent2, "MedSkilledClose");
                cap2.addSkill("mining");
                cap2.setProficiency("mining", 0.7);
                cap2.updateLoad(0.5);
                cap2.updatePosition(new BlockPos(10, 64, 10));

            AgentCapability cap3 = new AgentCapability(agent3, "LowSkilledNear");
                cap3.addSkill("mining");
                cap3.setProficiency("mining", 0.6);
                cap3.updateLoad(0.1);
                cap3.updatePosition(new BlockPos(5, 64, 5));

            registry.register(agent1, cap1);
            registry.register(agent2, cap2);
            registry.register(agent3, cap3);
        }

        @Test
        @DisplayName("Find best agent considers skill, load, and distance")
        void findBestAgentConsidersSkillLoadAndDistance() {
            Map<String, Double> required = Map.of("mining", 0.5);
            BlockPos taskPos = new BlockPos(0, 64, 0);

            Optional<AgentCapability> best = registry.findBestAgent(required, taskPos);

            assertTrue(best.isPresent(),
                    "Should find a best agent");

            // The best agent should balance skill proficiency, low load, and proximity
            // Agent3 is closest and has lowest load, though slightly lower skill
            // Agent2 has medium skill and is somewhat close
            // Agent1 has highest skill but is far away
            // The algorithm weights: skill 50%, load 30%, distance 20%
            UUID bestId = best.get().getAgentId();

            assertTrue(bestId.equals(agent2) || bestId.equals(agent3),
                    "Best agent should be agent2 or agent3 (better balance of factors)");
        }

        @Test
        @DisplayName("Find best agent without position considers skill and load")
        void findBestAgentWithoutPositionConsidersSkillAndLoad() {
            Map<String, Double> required = Map.of("mining", 0.5);

            Optional<AgentCapability> best = registry.findBestAgent(required, null);

            assertTrue(best.isPresent());

            // Agent1 should win (highest skill, low load) when distance doesn't matter
            assertEquals(agent1, best.get().getAgentId());
        }

        @Test
        @DisplayName("Find best agent returns empty when no capable agents")
        void findBestAgentReturnsEmptyWhenNoCapable() {
            Map<String, Double> required = Map.of("combat", 0.8);

            Optional<AgentCapability> best = registry.findBestAgent(required, null);

            assertFalse(best.isPresent(),
                    "Should return empty when no agents have required skill");
        }

        @Test
        @DisplayName("Find best agent requires availability")
        void findBestAgentRequiresAvailability() {
            // Make all agents unavailable
            registry.getAllAgents().forEach(cap -> cap.updateLoad(1.0));

            Map<String, Double> required = Map.of("mining", 0.5);

            Optional<AgentCapability> best = registry.findBestAgent(required, null);

            assertFalse(best.isPresent(),
                    "Should return empty when no agents are available");
        }

        @Test
        @DisplayName("Find best agent requires active state")
        void findBestAgentRequiresActiveState() {
            // Make all agents inactive
            registry.getAllAgents().forEach(cap -> cap.setActive(false));

            Map<String, Double> required = Map.of("mining", 0.5);

            Optional<AgentCapability> best = registry.findBestAgent(required, null);

            assertFalse(best.isPresent(),
                    "Should return empty when no agents are active");
        }

        @Test
        @DisplayName("Find best agent with empty requirements returns any available")
        void findBestAgentWithEmptyRequirementsReturnsAny() {
            Optional<AgentCapability> best = registry.findBestAgent(Map.of(), null);

            assertTrue(best.isPresent(),
                    "Should find best agent even without skill requirements");
        }
    }

    @Nested
    @DisplayName("Query Tests - Agents By Availability")
    class AgentsByAvailabilityTests {

        @BeforeEach
        void setUp() {
            UUID agent1 = UUID.randomUUID();
            UUID agent2 = UUID.randomUUID();
            UUID agent3 = UUID.randomUUID();

            AgentCapability cap1 = new AgentCapability(agent1, "Agent1");
            cap1.updateLoad(0.1);

            AgentCapability cap2 = new AgentCapability(agent2, "Agent2");
            cap2.updateLoad(0.5);

            AgentCapability cap3 = new AgentCapability(agent3, "Agent3");
            cap3.updateLoad(0.9);

            registry.register(agent1, cap1);
            registry.register(agent2, cap2);
            registry.register(agent3, cap3);
        }

        @Test
        @DisplayName("Get agents by availability sorts by load")
        void getAgentsByAvailabilitySortsByLoad() {
            List<AgentCapability> sorted = registry.getAgentsByAvailability();

            assertEquals(3, sorted.size());
            assertEquals(0.1, sorted.get(0).getCurrentLoad());
            assertEquals(0.5, sorted.get(1).getCurrentLoad());
            assertEquals(0.9, sorted.get(2).getCurrentLoad());
        }

        @Test
        @DisplayName("Get agents by availability excludes inactive")
        void getAgentsByAvailabilityExcludesInactive() {
            // Make middle agent inactive
            registry.getAllAgents().stream()
                    .filter(cap -> cap.getCurrentLoad() == 0.5)
                    .findFirst()
                    .ifPresent(cap -> cap.setActive(false));

            List<AgentCapability> sorted = registry.getAgentsByAvailability();

            assertEquals(2, sorted.size(),
                    "Inactive agent should be excluded");
            assertTrue(sorted.stream().allMatch(AgentCapability::isActive));
        }
    }

    @Nested
    @DisplayName("Listener Management Tests")
    class ListenerTests {

        @Test
        @DisplayName("Add and remove listener")
        void addAndRemoveListener() {
            registry.addListener(listener);

            registry.register(agentId, capability);
            verify(listener, times(1)).onAgentRegistered(agentId);

            registry.removeListener(listener);

            registry.register(UUID.randomUUID(), new AgentCapability(UUID.randomUUID(), "Agent2"));
            // Should still be called once (from before removal)
            verify(listener, times(1)).onAgentRegistered(any());
        }

        @Test
        @DisplayName("Multiple listeners are notified")
        void multipleListenersNotified() {
            CapabilityRegistry.CapabilityListener listener2 =
                    mock(CapabilityRegistry.CapabilityListener.class);

            registry.addListener(listener);
            registry.addListener(listener2);

            registry.register(agentId, capability);

            verify(listener, times(1)).onAgentRegistered(agentId);
            verify(listener2, times(1)).onAgentRegistered(agentId);
        }

        @Test
        @DisplayName("Null listener is ignored")
        void nullListenerIgnored() {
            assertDoesNotThrow(() -> registry.addListener(null));
            assertDoesNotThrow(() -> registry.removeListener(null));
        }

        @Test
        @DisplayName("Listener exception does not stop processing")
        void listenerExceptionHandling() {
            CapabilityRegistry.CapabilityListener failingListener =
                    mock(CapabilityRegistry.CapabilityListener.class);
            CapabilityRegistry.CapabilityListener normalListener =
                    mock(CapabilityRegistry.CapabilityListener.class);

            doThrow(new RuntimeException("Test exception"))
                    .when(failingListener).onAgentRegistered(any());

            registry.addListener(failingListener);
            registry.addListener(normalListener);

            assertDoesNotThrow(() -> registry.register(agentId, capability));

            verify(failingListener, times(1)).onAgentRegistered(agentId);
            verify(normalListener, times(1)).onAgentRegistered(agentId);
        }
    }

    @Nested
    @DisplayName("Cleanup Tests")
    class CleanupTests {

        @Test
        @DisplayName("Cleanup inactive removes inactive agents")
        void cleanupInactiveRemovesInactive() {
            UUID agent1 = UUID.randomUUID();
            UUID agent2 = UUID.randomUUID();

            AgentCapability cap1 = new AgentCapability(agent1, "ActiveAgent");
            AgentCapability cap2 = new AgentCapability(agent2, "InactiveAgent");
            cap2.setActive(false);

            registry.register(agent1, cap1);
            registry.register(agent2, cap2);

            int removed = registry.cleanupInactive();

            assertEquals(1, removed);
            assertTrue(registry.isRegistered(agent1));
            assertFalse(registry.isRegistered(agent2));
        }

        @Test
        @DisplayName("Clear removes all agents")
        void clearRemovesAllAgents() {
            registry.register(agentId, capability);
            registry.register(UUID.randomUUID(), new AgentCapability(UUID.randomUUID(), "Agent2"));

            assertEquals(2, registry.getAgentCount());

            registry.clear();

            assertEquals(0, registry.getAgentCount());
            assertFalse(registry.isRegistered(agentId));
        }

        @Test
        @DisplayName("Is registered returns correct status")
        void isRegisteredReturnsCorrectStatus() {
            assertFalse(registry.isRegistered(agentId));

            registry.register(agentId, capability);

            assertTrue(registry.isRegistered(agentId));
        }

        @Test
        @DisplayName("Is registered name returns correct status")
        void isRegisteredNameReturnsCorrectStatus() {
            assertFalse(registry.isRegisteredName(agentName));

            registry.register(agentId, capability);

            assertTrue(registry.isRegisteredName(agentName));
        }

        @Test
        @DisplayName("Is registered name with null returns false")
        void isRegisteredNameWithNullReturnsFalse() {
            assertFalse(registry.isRegisteredName(null));
        }
    }

    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {

        @Test
        @DisplayName("ToString contains registry information")
        void toStringContainsRegistryInformation() {
            registry.register(agentId, capability);

            UUID agent2 = UUID.randomUUID();
            AgentCapability cap2 = new AgentCapability(agent2, "Agent2");
            cap2.setActive(false);
            registry.register(agent2, cap2);

            String str = registry.toString();

            assertTrue(str.contains("agents=2"));
            assertTrue(str.contains("active=1"));
        }
    }

    @Nested
    @DisplayName("Concurrent Access Tests")
    class ConcurrencyTests {

        @Test
        @DisplayName("Concurrent registrations are handled safely")
        void concurrentRegistrations() throws InterruptedException {
            int threadCount = 10;
            Thread[] threads = new Thread[threadCount];
            UUID[] ids = new UUID[threadCount];

            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                ids[i] = UUID.randomUUID();
                threads[i] = new Thread(() -> {
                    AgentCapability cap = new AgentCapability(ids[index], "Agent" + index);
                    registry.register(ids[index], cap);
                });
            }

            for (Thread thread : threads) {
                thread.start();
            }

            for (Thread thread : threads) {
                thread.join();
            }

            assertEquals(threadCount, registry.getAgentCount(),
                    "All concurrent registrations should succeed");
        }

        @Test
        @DisplayName("Concurrent lookups are handled safely")
        void concurrentLookups() throws InterruptedException {
            registry.register(agentId, capability);

            int threadCount = 20;
            Thread[] threads = new Thread[threadCount];
            AtomicInteger successCount = new AtomicInteger(0);

            for (int i = 0; i < threadCount; i++) {
                threads[i] = new Thread(() -> {
                    if (registry.getCapability(agentId) != null) {
                        successCount.incrementAndGet();
                    }
                });
            }

            for (Thread thread : threads) {
                thread.start();
            }

            for (Thread thread : threads) {
                thread.join();
            }

            assertEquals(threadCount, successCount.get(),
                    "All lookups should succeed");
        }
    }
}
