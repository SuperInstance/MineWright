package com.minewright.coordination;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for {@link AgentCapability}.
 *
 * <p>Tests cover agent capability tracking including:</p>
 * <ul>
 *   <li>Skill management (add, remove, check)</li>
 *   <li>Proficiency tracking</li>
 *   <li>Tool availability</li>
 *   <li>Position updates and distance calculation</li>
 *   <li>Load management</li>
 *   <li>Active state tracking</li>
 *   <li>Task history recording</li>
 *   <li>Bid score calculation</li>
 *   <li>Builder pattern</li>
 * </ul>
 *
 * @see AgentCapability
 */
@DisplayName("Agent Capability Tests")
class AgentCapabilityTest {

    private UUID agentId;
    private String agentName;
    private AgentCapability capability;

    @BeforeEach
    void setUp() {
        agentId = UUID.randomUUID();
        agentName = "TestAgent";
        capability = new AgentCapability(agentId, agentName);
    }

    @Nested
    @DisplayName("Constructor and Identity Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Constructor creates valid capability")
        void constructorCreatesValidCapability() {
            assertNotNull(capability.getAgentId());
            assertEquals(agentId, capability.getAgentId());
            assertEquals(agentName, capability.getAgentName());
            assertTrue(capability.getSkills().isEmpty());
            assertTrue(capability.getProficiencies().isEmpty());
            assertTrue(capability.getAvailableTools().isEmpty());
            assertEquals(BlockPos.ZERO, capability.getCurrentPosition());
            assertEquals(0.0, capability.getCurrentLoad());
            assertTrue(capability.isActive());
        }

        @Test
        @DisplayName("Constructor throws on null agent ID")
        void constructorThrowsOnNullAgentId() {
            assertThrows(IllegalArgumentException.class,
                    () -> new AgentCapability(null, "TestAgent"),
                    "Should throw IllegalArgumentException for null agent ID");
        }

        @Test
        @DisplayName("Constructor throws on blank agent name")
        void constructorThrowsOnBlankAgentName() {
            assertThrows(IllegalArgumentException.class,
                    () -> new AgentCapability(agentId, ""),
                    "Should throw IllegalArgumentException for blank name");

            assertThrows(IllegalArgumentException.class,
                    () -> new AgentCapability(agentId, "   "),
                    "Should throw IllegalArgumentException for whitespace name");
        }

        @Test
        @DisplayName("Constructor throws on null agent name")
        void constructorThrowsOnNullAgentName() {
            assertThrows(IllegalArgumentException.class,
                    () -> new AgentCapability(agentId, null),
                    "Should throw IllegalArgumentException for null name");
        }
    }

    @Nested
    @DisplayName("Skill Management Tests")
    class SkillTests {

        @Test
        @DisplayName("Add skill adds to skill set")
        void addSkillAddsToSkillSet() {
            capability.addSkill("mining");

            assertTrue(capability.hasSkill("mining"));
            assertEquals(1, capability.getSkills().size());
        }

        @Test
        @DisplayName("Add skill is case insensitive")
        void addSkillIsCaseInsensitive() {
            capability.addSkill("Mining");

            assertTrue(capability.hasSkill("mining"));
            assertTrue(capability.hasSkill("Mining"));
            assertTrue(capability.hasSkill("MINING"));
        }

        @Test
        @DisplayName("Add skill sets default proficiency")
        void addSkillSetsDefaultProficiency() {
            capability.addSkill("building");

            assertEquals(0.5, capability.getProficiency("building"),
                    "Default proficiency should be 0.5");
        }

        @Test
        @DisplayName("Add skills from collection")
        void addSkillsFromCollection() {
            capability.addSkills(Set.of("mining", "building", "combat"));

            assertEquals(3, capability.getSkills().size());
            assertTrue(capability.hasSkill("mining"));
            assertTrue(capability.hasSkill("building"));
            assertTrue(capability.hasSkill("combat"));
        }

        @Test
        @DisplayName("Add skills filters null and blank")
        void addSkillsFiltersNullAndBlank() {
            capability.addSkills(Set.of("mining", null, "", "building", "   "));

            assertEquals(2, capability.getSkills().size(),
                    "Should only add valid skills");
            assertTrue(capability.hasSkill("mining"));
            assertTrue(capability.hasSkill("building"));
        }

        @Test
        @DisplayName("Remove skill removes from skill set")
        void removeSkillRemovesFromSkillSet() {
            capability.addSkill("mining");
            assertTrue(capability.hasSkill("mining"));

            boolean removed = capability.removeSkill("mining");

            assertTrue(removed);
            assertFalse(capability.hasSkill("mining"));
        }

        @Test
        @DisplayName("Remove skill also removes proficiency")
        void removeSkillAlsoRemovesProficiency() {
            capability.addSkill("mining");
            capability.setProficiency("mining", 0.9);
            assertEquals(0.9, capability.getProficiency("mining"));

            capability.removeSkill("mining");

            assertEquals(0.0, capability.getProficiency("mining"),
                    "Proficiency should be removed with skill");
        }

        @Test
        @DisplayName("Remove non-existent skill returns false")
        void removeNonExistentSkill() {
            boolean removed = capability.removeSkill("mining");

            assertFalse(removed);
        }

        @Test
        @DisplayName("Remove skill is case insensitive")
        void removeSkillIsCaseInsensitive() {
            capability.addSkill("Mining");

            assertTrue(capability.removeSkill("mining"));
            assertFalse(capability.hasSkill("Mining"));
        }

        @Test
        @DisplayName("Remove null skill returns false")
        void removeNullSkill() {
            capability.addSkill("mining");

            boolean removed = capability.removeSkill(null);

            assertFalse(removed);
            assertTrue(capability.hasSkill("mining"));
        }

        @Test
        @DisplayName("Has skill returns correct result")
        void hasSkillReturnsCorrectResult() {
            capability.addSkill("mining");

            assertTrue(capability.hasSkill("mining"));
            assertFalse(capability.hasSkill("building"));
        }

        @Test
        @DisplayName("Has skill is case insensitive")
        void hasSkillIsCaseInsensitive() {
            capability.addSkill("Mining");

            assertTrue(capability.hasSkill("mining"));
            assertTrue(capability.hasSkill("MINING"));
        }

        @Test
        @DisplayName("Has skill returns false for null")
        void hasSkillReturnsFalseForNull() {
            capability.addSkill("mining");

            assertFalse(capability.hasSkill(null));
        }

        @Test
        @DisplayName("Get skills returns unmodifiable set")
        void getSkillsReturnsUnmodifiableSet() {
            capability.addSkill("mining");

            Set<String> skills = capability.getSkills();

            assertThrows(UnsupportedOperationException.class,
                    () -> skills.add("building"),
                    "Should not be able to modify returned set");
        }
    }

    @Nested
    @DisplayName("Proficiency Tests")
    class ProficiencyTests {

        @Test
        @DisplayName("Set proficiency updates value")
        void setProficiencyUpdatesValue() {
            capability.addSkill("mining");
            capability.setProficiency("mining", 0.8);

            assertEquals(0.8, capability.getProficiency("mining"));
        }

        @Test
        @DisplayName("Set proficiency is case insensitive")
        void setProficiencyIsCaseInsensitive() {
            capability.addSkill("Mining");
            capability.setProficiency("mining", 0.7);

            assertEquals(0.7, capability.getProficiency("Mining"));
        }

        @Test
        @DisplayName("Set proficiency adds skill if not present")
        void setProficiencyAddsSkillIfNotPresent() {
            capability.setProficiency("building", 0.9);

            assertTrue(capability.hasSkill("building"));
            assertEquals(0.9, capability.getProficiency("building"));
        }

        @Test
        @DisplayName("Set proficiency throws on invalid value")
        void setProficiencyThrowsOnInvalidValue() {
            capability.addSkill("mining");

            assertThrows(IllegalArgumentException.class,
                    () -> capability.setProficiency("mining", -0.1),
                    "Should throw for negative proficiency");

            assertThrows(IllegalArgumentException.class,
                    () -> capability.setProficiency("mining", 1.1),
                    "Should throw for proficiency > 1.0");
        }

        @Test
        @DisplayName("Set proficiency allows boundary values")
        void setProficiencyAllowsBoundaryValues() {
            capability.addSkill("mining");

            assertDoesNotThrow(() -> capability.setProficiency("mining", 0.0));
            assertEquals(0.0, capability.getProficiency("mining"));

            assertDoesNotThrow(() -> capability.setProficiency("mining", 1.0));
            assertEquals(1.0, capability.getProficiency("mining"));
        }

        @Test
        @DisplayName("Set proficiency ignores null or blank skill")
        void setProficiencyIgnoresNullOrBlank() {
            assertDoesNotThrow(() -> capability.setProficiency(null, 0.5));
            assertDoesNotThrow(() -> capability.setProficiency("", 0.5));
            assertDoesNotThrow(() -> capability.setProficiency("   ", 0.5));
        }

        @Test
        @DisplayName("Get proficiency returns 0.0 for non-existent skill")
        void getProficiencyReturnsZeroForNonExistent() {
            assertEquals(0.0, capability.getProficiency("mining"));
        }

        @Test
        @DisplayName("Get proficiency returns 0.0 for null")
        void getProficiencyReturnsZeroForNull() {
            assertEquals(0.0, capability.getProficiency(null));
        }

        @Test
        @DisplayName("Get proficiencies returns unmodifiable map")
        void getProficienciesReturnsUnmodifiableMap() {
            capability.addSkill("mining");

            Map<String, Double> proficiencies = capability.getProficiencies();

            assertThrows(UnsupportedOperationException.class,
                    () -> proficiencies.put("building", 0.5),
                    "Should not be able to modify returned map");
        }
    }

    @Nested
    @DisplayName("Tool Management Tests")
    class ToolTests {

        @Test
        @DisplayName("Add tool adds to tool set")
        void addToolAddsToToolSet() {
            capability.addTool("pickaxe");

            assertTrue(capability.hasTool("pickaxe"));
            assertEquals(1, capability.getAvailableTools().size());
        }

        @Test
        @DisplayName("Add tool is case insensitive")
        void addToolIsCaseInsensitive() {
            capability.addTool("Pickaxe");

            assertTrue(capability.hasTool("pickaxe"));
            assertTrue(capability.hasTool("PICKAXE"));
        }

        @Test
        @DisplayName("Add tools from collection")
        void addToolsFromCollection() {
            capability.addTools(Set.of("pickaxe", "shovel", "axe"));

            assertEquals(3, capability.getAvailableTools().size());
            assertTrue(capability.hasTool("pickaxe"));
            assertTrue(capability.hasTool("shovel"));
            assertTrue(capability.hasTool("axe"));
        }

        @Test
        @DisplayName("Remove tool removes from tool set")
        void removeToolRemovesFromToolSet() {
            capability.addTool("pickaxe");
            assertTrue(capability.hasTool("pickaxe"));

            boolean removed = capability.removeTool("pickaxe");

            assertTrue(removed);
            assertFalse(capability.hasTool("pickaxe"));
        }

        @Test
        @DisplayName("Remove non-existent tool returns false")
        void removeNonExistentTool() {
            boolean removed = capability.removeTool("pickaxe");

            assertFalse(removed);
        }

        @Test
        @DisplayName("Remove tool is case insensitive")
        void removeToolIsCaseInsensitive() {
            capability.addTool("Pickaxe");

            assertTrue(capability.removeTool("pickaxe"));
            assertFalse(capability.hasTool("Pickaxe"));
        }

        @Test
        @DisplayName("Remove null tool returns false")
        void removeNullTool() {
            capability.addTool("pickaxe");

            boolean removed = capability.removeTool(null);

            assertFalse(removed);
            assertTrue(capability.hasTool("pickaxe"));
        }

        @Test
        @DisplayName("Has tool returns correct result")
        void hasToolReturnsCorrectResult() {
            capability.addTool("pickaxe");

            assertTrue(capability.hasTool("pickaxe"));
            assertFalse(capability.hasTool("shovel"));
        }

        @Test
        @DisplayName("Has tool returns false for null")
        void hasToolReturnsFalseForNull() {
            capability.addTool("pickaxe");

            assertFalse(capability.hasTool(null));
        }

        @Test
        @DisplayName("Has tools returns true when all tools available")
        void hasToolsReturnsTrueWhenAllAvailable() {
            capability.addTools(Set.of("pickaxe", "shovel", "axe"));

            assertTrue(capability.hasTools(Set.of("pickaxe", "shovel")));
        }

        @Test
        @DisplayName("Has tools returns false when some tools missing")
        void hasToolsReturnsFalseWhenSomeMissing() {
            capability.addTool("pickaxe");

            assertFalse(capability.hasTools(Set.of("pickaxe", "shovel")));
        }

        @Test
        @DisplayName("Has tools returns true for empty set")
        void hasToolsReturnsTrueForEmptySet() {
            assertTrue(capability.hasTools(Set.of()));
        }

        @Test
        @DisplayName("Has tools returns true for null")
        void hasToolsReturnsTrueForNull() {
            assertTrue(capability.hasTools(null));
        }

        @Test
        @DisplayName("Get tools returns unmodifiable set")
        void getToolsReturnsUnmodifiableSet() {
            capability.addTool("pickaxe");

            Set<String> tools = capability.getAvailableTools();

            assertThrows(UnsupportedOperationException.class,
                    () -> tools.add("shovel"),
                    "Should not be able to modify returned set");
        }
    }

    @Nested
    @DisplayName("Position and Distance Tests")
    class PositionTests {

        @Test
        @DisplayName("Update position changes location")
        void updatePositionChangesLocation() {
            BlockPos pos = new BlockPos(10, 64, 20);

            capability.updatePosition(pos);

            assertEquals(pos, capability.getCurrentPosition());
        }

        @Test
        @DisplayName("Update position ignores null")
        void updatePositionIgnoresNull() {
            BlockPos original = capability.getCurrentPosition();

            capability.updatePosition(null);

            assertEquals(original, capability.getCurrentPosition());
        }

        @Test
        @DisplayName("Distance to calculates correctly")
        void distanceToCalculatesCorrectly() {
            capability.updatePosition(new BlockPos(0, 0, 0));
            BlockPos target = new BlockPos(3, 4, 0);

            double distance = capability.distanceTo(target);

            // Distance = sqrt(3^2 + 4^2 + 0^2) = 5
            assertEquals(5.0, distance, 0.001);
        }

        @Test
        @DisplayName("Distance to handles negative coordinates")
        void distanceToHandlesNegativeCoordinates() {
            capability.updatePosition(new BlockPos(-10, -20, -30));
            BlockPos target = new BlockPos(-7, -16, -26);

            double distance = capability.distanceTo(target);

            // Distance = sqrt(3^2 + 4^2 + 4^2) = sqrt(41) ≈ 6.403
            assertEquals(6.403, distance, 0.001);
        }

        @Test
        @DisplayName("Distance to returns max for null target")
        void distanceToReturnsMaxForNullTarget() {
            capability.updatePosition(new BlockPos(0, 0, 0));

            double distance = capability.distanceTo(null);

            assertEquals(Double.MAX_VALUE, distance);
        }

        @Test
        @DisplayName("Distance to same position is zero")
        void distanceToSamePositionIsZero() {
            BlockPos pos = new BlockPos(10, 20, 30);
            capability.updatePosition(pos);

            double distance = capability.distanceTo(pos);

            assertEquals(0.0, distance, 0.001);
        }

        @Test
        @DisplayName("Distance calculation is accurate")
        void distanceCalculationIsAccurate() {
            capability.updatePosition(new BlockPos(0, 0, 0));

            // Test various distances
            assertEquals(10.0, capability.distanceTo(new BlockPos(10, 0, 0)), 0.001);
            assertEquals(17.3205, capability.distanceTo(new BlockPos(10, 10, 10)), 0.001);
            assertEquals(387.298, capability.distanceTo(new BlockPos(100, 200, 300)), 0.001);
        }
    }

    @Nested
    @DisplayName("Load Management Tests")
    class LoadTests {

        @Test
        @DisplayName("Update load changes value")
        void updateLoadChangesValue() {
            capability.updateLoad(0.7);

            assertEquals(0.7, capability.getCurrentLoad());
        }

        @Test
        @DisplayName("Update load clamps to valid range")
        void updateLoadClampsToValidRange() {
            capability.updateLoad(-0.5);
            assertEquals(0.0, capability.getCurrentLoad(),
                    "Load should be clamped to minimum 0.0");

            capability.updateLoad(1.5);
            assertEquals(1.0, capability.getCurrentLoad(),
                    "Load should be clamped to maximum 1.0");
        }

        @Test
        @DisplayName("Update load allows boundary values")
        void updateLoadAllowsBoundaryValues() {
            capability.updateLoad(0.0);
            assertEquals(0.0, capability.getCurrentLoad());

            capability.updateLoad(1.0);
            assertEquals(1.0, capability.getCurrentLoad());
        }

        @Test
        @DisplayName("Is available returns true when load is low")
        void isAvailableReturnsTrueWhenLoadIsLow() {
            capability.updateLoad(0.5);

            assertTrue(capability.isAvailable());
        }

        @Test
        @DisplayName("Is available returns false when load is high")
        void isAvailableReturnsFalseWhenLoadIsHigh() {
            capability.updateLoad(0.9);

            assertFalse(capability.isAvailable());
        }

        @Test
        @DisplayName("Is available respects threshold")
        void isAvailableRespectsThreshold() {
            capability.updateLoad(0.79);
            assertTrue(capability.isAvailable(),
                    "Load below 0.8 should be available");

            capability.updateLoad(0.8);
            assertFalse(capability.isAvailable(),
                    "Load at or above 0.8 should not be available");
        }

        @Test
        @DisplayName("Is available requires active state")
        void isAvailableRequiresActiveState() {
            capability.updateLoad(0.0);
            capability.setActive(false);

            assertFalse(capability.isAvailable(),
                    "Inactive agent should not be available");
        }
    }

    @Nested
    @DisplayName("Active State Tests")
    class ActiveStateTests {

        @Test
        @DisplayName("Set active changes state")
        void setActiveChangesState() {
            capability.setActive(false);

            assertFalse(capability.isActive());
        }

        @Test
        @DisplayName("Active state defaults to true")
        void activeStateDefaultsToTrue() {
            assertTrue(capability.isActive());
        }
    }

    @Nested
    @DisplayName("Task History Tests")
    class TaskHistoryTests {

        @Test
        @DisplayName("Record task completion increments count")
        void recordTaskCompletionIncrementsCount() {
            capability.recordTaskCompletion("mining");

            assertEquals(1, capability.getCompletedTaskCount("mining"));
        }

        @Test
        @DisplayName("Record multiple completions increments count")
        void recordMultipleCompletionsIncrementsCount() {
            capability.recordTaskCompletion("mining");
            capability.recordTaskCompletion("mining");
            capability.recordTaskCompletion("mining");

            assertEquals(3, capability.getCompletedTaskCount("mining"));
        }

        @Test
        @DisplayName("Record different task types separately")
        void recordDifferentTaskTypesSeparately() {
            capability.recordTaskCompletion("mining");
            capability.recordTaskCompletion("building");
            capability.recordTaskCompletion("mining");

            assertEquals(2, capability.getCompletedTaskCount("mining"));
            assertEquals(1, capability.getCompletedTaskCount("building"));
        }

        @Test
        @DisplayName("Get completed task count returns 0 for unknown task")
        void getCompletedTaskCountReturnsZeroForUnknown() {
            assertEquals(0, capability.getCompletedTaskCount("mining"));
        }

        @Test
        @DisplayName("Record task completion ignores null")
        void recordTaskCompletionIgnoresNull() {
            capability.recordTaskCompletion(null);

            assertEquals(0, capability.getCompletedTaskCount(null));
        }

        @Test
        @DisplayName("Record task completion ignores blank")
        void recordTaskCompletionIgnoresBlank() {
            capability.recordTaskCompletion("");
            capability.recordTaskCompletion("   ");

            assertEquals(0, capability.getCompletedTaskCount(""));
        }

        @Test
        @DisplayName("Record task completion updates timestamp")
        void recordTaskCompletionUpdatesTimestamp() {
            long before = System.currentTimeMillis();
            capability.recordTaskCompletion("mining");
            long after = System.currentTimeMillis();

            long timestamp = capability.getLastTaskCompletionTime("mining");

            assertTrue(timestamp >= before && timestamp <= after,
                    "Timestamp should be between before and after");
        }

        @Test
        @DisplayName("Get last completion time returns 0 for unknown task")
        void getLastTaskCompletionTimeReturnsZeroForUnknown() {
            assertEquals(0, capability.getLastTaskCompletionTime("mining"));
        }
    }

    @Nested
    @DisplayName("Bid Score Calculation Tests")
    class BidScoreTests {

        private TaskAnnouncement announcement;

        @BeforeEach
        void createAnnouncement() {
            announcement = TaskAnnouncement.builder()
                    .task(new com.minewright.action.Task("mine", Map.of()))
                    .requesterId(UUID.randomUUID())
                    .deadlineAfter(30000)
                    .requireSkill("mining")
                    .minProficiency(0.5)
                    .build();
        }

        @Test
        @DisplayName("Calculate bid score uses skills")
        void calculateBidScoreUsesSkills() {
            capability.addSkill("mining");
            capability.setProficiency("mining", 0.9);

            double score = capability.calculateBidScore(announcement);

            assertTrue(score > 0.0,
                    "Agent with matching skill should have positive score");
        }

        @Test
        @DisplayName("Calculate bid score returns 0 for inactive agent")
        void calculateBidScoreReturnsZeroForInactive() {
            capability.addSkill("mining");
            capability.setActive(false);

            double score = capability.calculateBidScore(announcement);

            assertEquals(0.0, score);
        }

        @Test
        @DisplayName("Calculate bid score returns 0 for null announcement")
        void calculateBidScoreReturnsZeroForNull() {
            double score = capability.calculateBidScore(null);

            assertEquals(0.0, score);
        }

        @Test
        @DisplayName("Calculate bid score penalizes low proficiency")
        void calculateBidScorePenalizesLowProficiency() {
            // Agent with proficiency below minimum
            AgentCapability lowProf = new AgentCapability(agentId, "LowProf");
            lowProf.addSkill("mining");
            lowProf.setProficiency("mining", 0.3); // Below 0.5 minimum

            // Agent with proficiency at minimum
            AgentCapability minProf = new AgentCapability(UUID.randomUUID(), "MinProf");
            minProf.addSkill("mining");
            minProf.setProficiency("mining", 0.5);

            double lowScore = lowProf.calculateBidScore(announcement);
            double minScore = minProf.calculateBidScore(announcement);

            assertTrue(lowScore < minScore,
                    "Low proficiency should be penalized");
        }

        @Test
        @DisplayName("Calculate bid score considers load")
        void calculateBidScoreConsidersLoad() {
            capability.addSkill("mining");
            capability.setProficiency("mining", 0.8);

            capability.updateLoad(0.0);
            double lowLoadScore = capability.calculateBidScore(announcement);

            capability.updateLoad(0.8);
            double highLoadScore = capability.calculateBidScore(announcement);

            assertTrue(lowLoadScore > highLoadScore,
                    "Lower load should result in higher score");
        }

        @Test
        @DisplayName("Calculate bid score is clamped to valid range")
        void calculateBidScoreIsClampedToValidRange() {
            capability.addSkill("mining");
            capability.setProficiency("mining", 1.0);
            capability.updateLoad(0.0);

            double score = capability.calculateBidScore(announcement);

            assertTrue(score >= 0.0 && score <= 1.0,
                    "Score should be clamped to [0.0, 1.0]");
        }
    }

    @Nested
    @DisplayName("Create Bid Tests")
    class CreateBidTests {

        private TaskAnnouncement announcement;

        @BeforeEach
        void createAnnouncement() {
            announcement = TaskAnnouncement.builder()
                    .task(new com.minewright.action.Task("mine", Map.of()))
                    .requesterId(UUID.randomUUID())
                    .deadlineAfter(30000)
                    .requireSkill("mining")
                    .build();
        }

        @Test
        @DisplayName("Create bid builds valid bid")
        void createBidBuildsValidBid() {
            capability.addSkill("mining");

            TaskBid bid = capability.createBid(announcement, 30000, 0.9);

            assertNotNull(bid);
            assertEquals(agentId, bid.bidderId());
            assertEquals(announcement.announcementId(), bid.announcementId());
            assertEquals(30000, bid.estimatedTime());
            assertEquals(0.9, bid.confidence());
        }

        @Test
        @DisplayName("Create bid includes capabilities")
        void createBidIncludesCapabilities() {
            capability.addSkill("mining");
            capability.setProficiency("mining", 0.8);
            capability.addTool("pickaxe");
            capability.updateLoad(0.3);

            TaskBid bid = capability.createBid(announcement, 30000, 0.9);

            Map<String, Object> capabilities = bid.capabilities();
            assertTrue(capabilities.containsKey("proficiencies"));
            assertTrue(capabilities.containsKey("tools"));
            assertTrue(capabilities.containsKey("distance"));
            assertTrue(capabilities.containsKey("currentLoad"));
        }

        @Test
        @DisplayName("Create bid score matches calculated score")
        void createBidScoreMatchesCalculatedScore() {
            capability.addSkill("mining");
            capability.setProficiency("mining", 0.9);

            double calculatedScore = capability.calculateBidScore(announcement);
            TaskBid bid = capability.createBid(announcement, 30000, 0.9);

            assertEquals(calculatedScore, bid.score(), 0.001,
                    "Bid score should match calculated score");
        }
    }

    @Nested
    @DisplayName("Builder Pattern Tests")
    class BuilderTests {

        @Test
        @DisplayName("Builder creates valid capability")
        void builderCreatesValidCapability() {
            AgentCapability built = AgentCapability.builder(agentId, agentName)
                    .skill("mining", 0.8)
                    .tool("pickaxe")
                    .position(new BlockPos(10, 20, 30))
                    .load(0.5)
                    .active(true)
                    .build();

            assertEquals(agentId, built.getAgentId());
            assertEquals(agentName, built.getAgentName());
            assertTrue(built.hasSkill("mining"));
            assertEquals(0.8, built.getProficiency("mining"));
            assertTrue(built.hasTool("pickaxe"));
            assertEquals(new BlockPos(10, 20, 30), built.getCurrentPosition());
            assertEquals(0.5, built.getCurrentLoad());
            assertTrue(built.isActive());
        }

        @Test
        @DisplayName("Builder with skills map")
        void builderWithSkillsMap() {
            AgentCapability built = AgentCapability.builder(agentId, agentName)
                    .skills(Map.of(
                            "mining", 0.9,
                            "building", 0.7,
                            "combat", 0.5
                    ))
                    .build();

            assertEquals(3, built.getSkills().size());
            assertEquals(0.9, built.getProficiency("mining"));
            assertEquals(0.7, built.getProficiency("building"));
            assertEquals(0.5, built.getProficiency("combat"));
        }

        @Test
        @DisplayName("Builder with tools collection")
        void builderWithToolsCollection() {
            AgentCapability built = AgentCapability.builder(agentId, agentName)
                    .tools(Set.of("pickaxe", "shovel", "axe"))
                    .build();

            assertEquals(3, built.getAvailableTools().size());
            assertTrue(built.hasTool("pickaxe"));
            assertTrue(built.hasTool("shovel"));
            assertTrue(built.hasTool("axe"));
        }

        @Test
        @DisplayName("Builder methods can be chained")
        void builderMethodsCanBeChained() {
            AgentCapability built = AgentCapability.builder(agentId, agentName)
                    .skill("mining", 0.9)
                    .skill("building", 0.7)
                    .tool("pickaxe")
                    .tool("shovel")
                    .position(new BlockPos(1, 2, 3))
                    .load(0.3)
                    .build();

            assertTrue(built.hasSkill("mining") && built.hasSkill("building"));
            assertTrue(built.hasTool("pickaxe") && built.hasTool("shovel"));
            assertEquals(new BlockPos(1, 2, 3), built.getCurrentPosition());
            assertEquals(0.3, built.getCurrentLoad());
        }
    }

    @Nested
    @DisplayName("Equals and HashCode Tests")
    class EqualityTests {

        @Test
        @DisplayName("Same agent ID are equal")
        void sameAgentIdAreEqual() {
            AgentCapability cap1 = new AgentCapability(agentId, "Agent1");
            AgentCapability cap2 = new AgentCapability(agentId, "Agent2");

            assertEquals(cap1, cap2);
            assertEquals(cap1.hashCode(), cap2.hashCode());
        }

        @Test
        @DisplayName("Different agent ID are not equal")
        void differentAgentIdAreNotEqual() {
            AgentCapability cap1 = new AgentCapability(UUID.randomUUID(), "Agent");
            AgentCapability cap2 = new AgentCapability(UUID.randomUUID(), "Agent");

            assertNotEquals(cap1, cap2);
        }

        @Test
        @DisplayName("Equal to same instance")
        void equalToSameInstance() {
            assertEquals(capability, capability);
        }

        @Test
        @DisplayName("Not equal to null")
        void notEqualToNull() {
            assertNotEquals(capability, null);
        }

        @Test
        @DisplayName("Not equal to different type")
        void notEqualToDifferentType() {
            assertNotEquals(capability, "not a capability");
        }
    }

    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {

        @Test
        @DisplayName("ToString contains agent information")
        void toStringContainsAgentInformation() {
            capability.addSkill("mining");
            capability.updateLoad(0.5);

            String str = capability.toString();

            assertTrue(str.contains(agentName));
            assertTrue(str.contains(agentId.toString().substring(0, 8)));
            assertTrue(str.contains("skills=1"));
            assertTrue(str.contains("load=0.50"));
            assertTrue(str.contains("active=true"));
        }
    }
}
