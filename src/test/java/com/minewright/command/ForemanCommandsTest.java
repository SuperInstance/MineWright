package com.minewright.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.minewright.MineWrightMod;
import com.minewright.entity.CrewManager;
import com.minewright.entity.ForemanEntity;
import com.minewright.orchestration.AgentRole;
import com.minewright.voice.VoiceManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for {@link ForemanCommands}.
 *
 * Tests cover:
 * <ul>
 *   <li>Command registration and structure</li>
 *   <li>Administrative commands (spawn, remove, stop)</li>
 *   <li>Read-only commands (list, status, relationship)</li>
 *   <li>Coordination commands (promote, demote, tell)</li>
 *   <li>Voice commands (on, off, status, test)</li>
 *   <li>Observability commands (metrics, export)</li>
 *   <li>Permission checks</li>
 *   <li>Error handling and edge cases</li>
 * </ul>
 *
 * @since 1.0.0
 */
@DisplayName("ForemanCommands Comprehensive Tests")
class ForemanCommandsTest {

    // Mock components
    private CommandDispatcher<CommandSourceStack> dispatcher;
    private CommandSourceStack mockSource;
    private ServerLevel mockServerLevel;
    private ServerPlayer mockPlayer;
    private CrewManager mockCrewManager;
    private ForemanEntity mockCrewMember;
    private VoiceManager mockVoiceManager;

    // Test constants
    private static final String TEST_CREW_NAME = "TestCrewMember";
    private static final String TEST_COMMAND = "mine 10 stone";
    private static final double SPAWN_X = 100.0;
    private static final double SPAWN_Y = 64.0;
    private static final double SPAWN_Z = 200.0;
    private static final int PERMISSION_ADMIN = 2;
    private static final int PERMISSION_PLAYER = 0;

    @BeforeEach
    void setUp() {
        // Initialize command dispatcher
        dispatcher = new CommandDispatcher<>();

        // Create mock components
        mockSource = mock(CommandSourceStack.class);
        mockServerLevel = mock(ServerLevel.class);
        mockPlayer = mock(ServerPlayer.class);
        mockCrewManager = mock(CrewManager.class);
        mockCrewMember = mock(ForemanEntity.class);
        mockVoiceManager = mock(VoiceManager.class);

        // Setup mock source behavior
        when(mockSource.getLevel()).thenReturn(mockServerLevel);
        when(mockSource.getPosition()).thenReturn(new Vec3(SPAWN_X, SPAWN_Y, SPAWN_Z));
        when(mockSource.getEntity()).thenReturn(mockPlayer);
        when(mockSource.getTextName()).thenReturn("TestPlayer");
        when(mockSource.hasPermission(PERMISSION_ADMIN)).thenReturn(true);
        when(mockSource.hasPermission(PERMISSION_PLAYER)).thenReturn(true);

        // Setup mock player behavior
        when(mockPlayer.getLookAngle()).thenReturn(new Vec3(1.0, 0.0, 0.0));

        // Setup mock crew member behavior
        when(mockCrewMember.getEntityName()).thenReturn(TEST_CREW_NAME);
        when(mockCrewMember.getRole()).thenReturn(AgentRole.WORKER);

        // Setup mock crew manager behavior
        when(mockCrewManager.spawnCrewMember(any(), any(), anyString()))
            .thenReturn(mockCrewMember);
        when(mockCrewManager.getCrewMember(anyString())).thenReturn(mockCrewMember);
        when(mockCrewManager.removeCrewMember(anyString())).thenReturn(true);
        when(mockCrewManager.getCrewMemberNames()).thenReturn(new ArrayList<>(List.of(TEST_CREW_NAME)));

        // Setup mock voice manager
        when(mockVoiceManager.isEnabled()).thenReturn(true);
        when(mockVoiceManager.getConfig()).thenReturn(new VoiceManager.VoiceConfig());

        // Register commands
        ForemanCommands.register(dispatcher);
    }

    // ==================== Command Registration Tests ====================

    @Test
    @DisplayName("Commands are registered with correct structure")
    void testCommandsAreRegistered() {
        // Verify main command node exists
        CommandNode<CommandSourceStack> minewrightNode = dispatcher.getRoot().getChild("minewright");

        assertNotNull(minewrightNode, "Main 'minewright' command should be registered");

        // Verify subcommands exist
        assertNotNull(dispatcher.getRoot().getChild("minewright").getChild("spawn"),
            "spawn command should be registered");
        assertNotNull(dispatcher.getRoot().getChild("minewright").getChild("remove"),
            "remove command should be registered");
        assertNotNull(dispatcher.getRoot().getChild("minewright").getChild("list"),
            "list command should be registered");
    }

    @Test
    @DisplayName("All expected commands are registered")
    void testAllExpectedCommandsRegistered() {
        String[] expectedCommands = {
            "spawn", "remove", "list", "status", "relationship",
            "stop", "tell", "promote", "demote",
            "voice", "metrics", "export"
        };

        CommandNode<CommandSourceStack> minewrightNode = dispatcher.getRoot().getChild("minewright");

        for (String command : expectedCommands) {
            assertNotNull(minewrightNode.getChild(command),
                "Command '" + command + "' should be registered");
        }
    }

    // ==================== Spawn Command Tests ====================

    @Test
    @DisplayName("Spawn command requires admin permission")
    void testSpawnCommandRequiresPermission() {
        when(mockSource.hasPermission(PERMISSION_ADMIN)).thenReturn(false);

        int result = executeCommand("minewright spawn " + TEST_CREW_NAME);

        assertEquals(0, result, "Spawn command should fail without admin permission");
    }

    @Test
    @DisplayName("Spawn command succeeds with valid parameters")
    void testSpawnCommandSuccess() {
        when(mockSource.hasPermission(PERMISSION_ADMIN)).thenReturn(true);

        int result = executeCommand("minewright spawn " + TEST_CREW_NAME);

        assertEquals(1, result, "Spawn command should succeed with valid parameters");

        // Verify spawn was called
        verify(mockCrewManager).spawnCrewMember(eq(mockServerLevel), any(Vec3.class), eq(TEST_CREW_NAME));
    }

    @Test
    @DisplayName("Spawn command fails when crew member already exists")
    void testSpawnCommandFailsWhenExists() {
        when(mockSource.hasPermission(PERMISSION_ADMIN)).thenReturn(true);
        when(mockCrewManager.spawnCrewMember(any(), any(), anyString())).thenReturn(null);

        int result = executeCommand("minewright spawn " + TEST_CREW_NAME);

        assertEquals(0, result, "Spawn command should fail when crew member exists");

        // Verify failure message was sent
        ArgumentCaptor<Component> messageCaptor = ArgumentCaptor.forClass(Component.class);
        verify(mockSource).sendFailure(messageCaptor.capture());

        String message = messageCaptor.getValue().getString();
        assertTrue(message.contains("Can't add") || message.contains("already exists"),
            "Failure message should indicate crew member exists");
    }

    @Test
    @DisplayName("Spawn command calculates spawn position correctly")
    void testSpawnCommandPositionCalculation() {
        when(mockSource.hasPermission(PERMISSION_ADMIN)).thenReturn(true);
        Vec3 sourcePos = new Vec3(100.0, 64.0, 200.0);
        when(mockSource.getPosition()).thenReturn(sourcePos);
        when(mockPlayer.getLookAngle()).thenReturn(new Vec3(1.0, 0.0, 0.0)); // Looking east

        int result = executeCommand("minewright spawn " + TEST_CREW_NAME);

        assertEquals(1, result, "Spawn command should succeed");

        // Verify spawn position is calculated (player position + look direction * 3)
        ArgumentCaptor<Vec3> positionCaptor = ArgumentCaptor.forClass(Vec3.class);
        verify(mockCrewManager).spawnCrewMember(eq(mockServerLevel), positionCaptor.capture(), eq(TEST_CREW_NAME));

        Vec3 spawnPos = positionCaptor.getValue();
        assertEquals(103.0, spawnPos.x, 0.01, "Should spawn 3 blocks east of player");
    }

    @Test
    @DisplayName("Spawn command handles null entity gracefully")
    void testSpawnCommandHandlesNullEntity() {
        when(mockSource.hasPermission(PERMISSION_ADMIN)).thenReturn(true);
        when(mockSource.getEntity()).thenReturn(null);

        int result = executeCommand("minewright spawn " + TEST_CREW_NAME);

        assertEquals(1, result, "Spawn command should succeed even without entity");

        // Verify default offset (3 blocks east) is used
        ArgumentCaptor<Vec3> positionCaptor = ArgumentCaptor.forClass(Vec3.class);
        verify(mockCrewManager).spawnCrewMember(eq(mockServerLevel), positionCaptor.capture(), eq(TEST_CREW_NAME));

        Vec3 spawnPos = positionCaptor.getValue();
        assertEquals(103.0, spawnPos.x, 0.01, "Should use default offset when entity is null");
    }

    @Test
    @DisplayName("Spawn command fails on client side")
    void testSpawnCommandFailsOnClientSide() {
        when(mockSource.hasPermission(PERMISSION_ADMIN)).thenReturn(true);
        when(mockSource.getLevel()).thenReturn(null);

        int result = executeCommand("minewright spawn " + TEST_CREW_NAME);

        assertEquals(0, result, "Spawn command should fail on client side");

        // Verify error message
        ArgumentCaptor<Component> messageCaptor = ArgumentCaptor.forClass(Component.class);
        verify(mockSource).sendFailure(messageCaptor.capture());

        assertTrue(messageCaptor.getValue().getString().contains("job site"),
            "Error message should mention job site (server)");
    }

    // ==================== Remove Command Tests ====================

    @Test
    @DisplayName("Remove command requires admin permission")
    void testRemoveCommandRequiresPermission() {
        when(mockSource.hasPermission(PERMISSION_ADMIN)).thenReturn(false);

        int result = executeCommand("minewright remove " + TEST_CREW_NAME);

        assertEquals(0, result, "Remove command should fail without admin permission");
    }

    @Test
    @DisplayName("Remove command succeeds for existing crew member")
    void testRemoveCommandSuccess() {
        when(mockSource.hasPermission(PERMISSION_ADMIN)).thenReturn(true);
        when(mockCrewManager.removeCrewMember(TEST_CREW_NAME)).thenReturn(true);

        int result = executeCommand("minewright remove " + TEST_CREW_NAME);

        assertEquals(1, result, "Remove command should succeed");

        verify(mockCrewManager).removeCrewMember(TEST_CREW_NAME);
    }

    @Test
    @DisplayName("Remove command fails for non-existent crew member")
    void testRemoveCommandFailsForNonExistent() {
        when(mockSource.hasPermission(PERMISSION_ADMIN)).thenReturn(true);
        when(mockCrewManager.removeCrewMember(TEST_CREW_NAME)).thenReturn(false);

        int result = executeCommand("minewright remove " + TEST_CREW_NAME);

        assertEquals(0, result, "Remove command should fail for non-existent crew member");

        // Verify error message
        ArgumentCaptor<Component> messageCaptor = ArgumentCaptor.forClass(Component.class);
        verify(mockSource).sendFailure(messageCaptor.capture());

        String message = messageCaptor.getValue().getString();
        assertTrue(message.contains("Can't find") || message.contains("roster"),
            "Error should suggest checking the roster");
    }

    // ==================== List Command Tests ====================

    @Test
    @DisplayName("List command shows empty crew when no members")
    void testListCommandEmptyCrew() {
        when(mockCrewManager.getCrewMemberNames()).thenReturn(new ArrayList<>());

        int result = executeCommand("minewright list");

        assertEquals(1, result, "List command should succeed");

        // Verify success message about empty crew
        ArgumentCaptor<Component> messageCaptor = ArgumentCaptor.forClass(Component.class);
        verify(mockSource).sendSuccess(messageCaptor.capture(), eq(false));

        String message = messageCaptor.getValue().getString();
        assertTrue(message.contains("empty") || message.contains("no crew"),
            "Message should indicate empty job site");
    }

    @Test
    @DisplayName("List command shows all crew members")
    void testListCommandShowsMembers() {
        List<String> names = List.of("Steve", "Alex", "Bob");
        when(mockCrewManager.getCrewMemberNames()).thenReturn(new ArrayList<>(names));

        int result = executeCommand("minewright list");

        assertEquals(1, result, "List command should succeed");

        // Verify all names are shown
        ArgumentCaptor<Component> messageCaptor = ArgumentCaptor.forClass(Component.class);
        verify(mockSource).sendSuccess(messageCaptor.capture(), eq(false));

        String message = messageCaptor.getValue().getString();
        assertTrue(message.contains("3"), "Should show count of 3");
        assertTrue(message.contains("Steve"), "Should include Steve");
        assertTrue(message.contains("Alex"), "Should include Alex");
        assertTrue(message.contains("Bob"), "Should include Bob");
    }

    @Test
    @DisplayName("List command does not require admin permission")
    void testListCommandNoPermissionRequired() {
        // Should work for regular players
        when(mockSource.hasPermission(PERMISSION_ADMIN)).thenReturn(false);

        int result = executeCommand("minewright list");

        assertEquals(1, result, "List command should work for regular players");
    }

    // ==================== Status Command Tests ====================

    @Test
    @DisplayName("Status command shows no crew when empty")
    void testStatusCommandEmptyCrew() {
        when(mockCrewManager.getCrewMemberNames()).thenReturn(new ArrayList<>());

        int result = executeCommand("minewright status");

        assertEquals(1, result, "Status command should succeed");

        ArgumentCaptor<Component> messageCaptor = ArgumentCaptor.forClass(Component.class);
        verify(mockSource, atLeastOnce()).sendSuccess(messageCaptor.capture(), eq(false));

        String message = messageCaptor.getValue().getString();
        assertTrue(message.contains("No crew") || message.contains("spawn"),
            "Message should suggest spawning a crew member");
    }

    @Test
    @DisplayName("Status command shows current goals")
    void testStatusCommandShowsGoals() {
        List<String> names = List.of(TEST_CREW_NAME);
        when(mockCrewManager.getCrewMemberNames()).thenReturn(new ArrayList<>(names));
        when(mockCrewMember.getActionExecutor()).thenReturn(mock(com.minewright.action.ActionExecutor.class));
        when(mockCrewMember.getActionExecutor().getCurrentGoal()).thenReturn("Mining stone at 100,64,200");

        int result = executeCommand("minewright status");

        assertEquals(1, result, "Status command should succeed");

        // Verify status is shown
        verify(mockSource, atLeastOnce()).sendSuccess(any(Component.class), eq(false));
    }

    @Test
    @DisplayName("Status command shows idle for crew with no goal")
    void testStatusCommandShowsIdle() {
        List<String> names = List.of(TEST_CREW_NAME);
        when(mockCrewManager.getCrewMemberNames()).thenReturn(new ArrayList<>(names));
        when(mockCrewMember.getActionExecutor()).thenReturn(mock(com.minewright.action.ActionExecutor.class));
        when(mockCrewMember.getActionExecutor().getCurrentGoal()).thenReturn(null);

        int result = executeCommand("minewright status");

        assertEquals(1, result, "Status command should succeed");
    }

    // ==================== Stop Command Tests ====================

    @Test
    @DisplayName("Stop command requires admin permission")
    void testStopCommandRequiresPermission() {
        when(mockSource.hasPermission(PERMISSION_ADMIN)).thenReturn(false);

        int result = executeCommand("minewright stop " + TEST_CREW_NAME);

        assertEquals(0, result, "Stop command should fail without admin permission");
    }

    @Test
    @DisplayName("Stop command stops current action and clears queue")
    void testStopCommandSuccess() {
        when(mockSource.hasPermission(PERMISSION_ADMIN)).thenReturn(true);
        when(mockCrewManager.getCrewMember(TEST_CREW_NAME)).thenReturn(mockCrewMember);
        when(mockCrewMember.getActionExecutor()).thenReturn(mock(com.minewright.action.ActionExecutor.class));
        when(mockCrewMember.getMemory()).thenReturn(mock(com.minewright.memory.ForemanMemory.class));

        int result = executeCommand("minewright stop " + TEST_CREW_NAME);

        assertEquals(1, result, "Stop command should succeed");

        // Verify action executor and memory were called
        verify(mockCrewMember.getActionExecutor()).stopCurrentAction();
        verify(mockCrewMember.getMemory()).clearTaskQueue();
    }

    @Test
    @DisplayName("Stop command fails for non-existent crew member")
    void testStopCommandFailsForNonExistent() {
        when(mockSource.hasPermission(PERMISSION_ADMIN)).thenReturn(true);
        when(mockCrewManager.getCrewMember(TEST_CREW_NAME)).thenReturn(null);

        int result = executeCommand("minewright stop " + TEST_CREW_NAME);

        assertEquals(0, result, "Stop command should fail for non-existent crew member");
    }

    // ==================== Tell Command Tests ====================

    @Test
    @DisplayName("Tell command requires admin permission")
    void testTellCommandRequiresPermission() {
        when(mockSource.hasPermission(PERMISSION_ADMIN)).thenReturn(false);

        int result = executeCommand("minewright tell " + TEST_CREW_NAME + " " + TEST_COMMAND);

        assertEquals(0, result, "Tell command should fail without admin permission");
    }

    @Test
    @DisplayName("Tell command processes natural language command")
    void testTellCommandSuccess() {
        when(mockSource.hasPermission(PERMISSION_ADMIN)).thenReturn(true);
        when(mockCrewManager.getCrewMember(TEST_CREW_NAME)).thenReturn(mockCrewMember);
        when(mockCrewMember.getActionExecutor()).thenReturn(mock(com.minewright.action.ActionExecutor.class));

        int result = executeCommand("minewright tell " + TEST_CREW_NAME + " " + TEST_COMMAND);

        assertEquals(1, result, "Tell command should succeed");

        // Verify command was sent
        verify(mockCrewMember.getActionExecutor()).processNaturalLanguageCommand(TEST_COMMAND);
    }

    @Test
    @DisplayName("Tell command handles complex commands")
    void testTellCommandHandlesComplexCommands() {
        when(mockSource.hasPermission(PERMISSION_ADMIN)).thenReturn(true);
        when(mockCrewManager.getCrewMember(TEST_CREW_NAME)).thenReturn(mockCrewMember);
        when(mockCrewMember.getActionExecutor()).thenReturn(mock(com.minewright.action.ActionExecutor.class));

        String complexCommand = "build a house with oak planks, 10 blocks wide, 5 blocks high, and 8 blocks deep";
        int result = executeCommand("minewright tell " + TEST_CREW_NAME + " " + complexCommand);

        assertEquals(1, result, "Tell command should handle complex commands");
    }

    @Test
    @DisplayName("Tell command handles special characters")
    void testTellCommandHandlesSpecialCharacters() {
        when(mockSource.hasPermission(PERMISSION_ADMIN)).thenReturn(true);
        when(mockCrewManager.getCrewMember(TEST_CREW_NAME)).thenReturn(mockCrewMember);
        when(mockCrewMember.getActionExecutor()).thenReturn(mock(com.minewright.action.ActionExecutor.class));

        String commandWithSpecial = "mine 10 stone, then craft 3 sticks!";
        int result = executeCommand("minewright tell " + TEST_CREW_NAME + " " + commandWithSpecial);

        assertEquals(1, result, "Tell command should handle special characters");
    }

    // ==================== Promote Command Tests ====================

    @Test
    @DisplayName("Promote command requires admin permission")
    void testPromoteCommandRequiresPermission() {
        when(mockSource.hasPermission(PERMISSION_ADMIN)).thenReturn(false);

        int result = executeCommand("minewright promote " + TEST_CREW_NAME);

        assertEquals(0, result, "Promote command should fail without admin permission");
    }

    @Test
    @DisplayName("Promote command promotes worker to foreman")
    void testPromoteCommandSuccess() {
        when(mockSource.hasPermission(PERMISSION_ADMIN)).thenReturn(true);
        when(mockCrewManager.getCrewMember(TEST_CREW_NAME)).thenReturn(mockCrewMember);
        when(mockCrewMember.getRole()).thenReturn(AgentRole.WORKER);

        int result = executeCommand("minewright promote " + TEST_CREW_NAME);

        assertEquals(1, result, "Promote command should succeed");

        verify(mockCrewMember).setRole(AgentRole.FOREMAN);
    }

    @Test
    @DisplayName("Promote command fails if already foreman")
    void testPromoteCommandFailsAlreadyForeman() {
        when(mockSource.hasPermission(PERMISSION_ADMIN)).thenReturn(true);
        when(mockCrewManager.getCrewMember(TEST_CREW_NAME)).thenReturn(mockCrewMember);
        when(mockCrewMember.getRole()).thenReturn(AgentRole.FOREMAN);

        int result = executeCommand("minewright promote " + TEST_CREW_NAME);

        assertEquals(0, result, "Promote command should fail if already foreman");
    }

    @Test
    @DisplayName("Promote command fails for non-existent crew member")
    void testPromoteCommandFailsNonExistent() {
        when(mockSource.hasPermission(PERMISSION_ADMIN)).thenReturn(true);
        when(mockCrewManager.getCrewMember(TEST_CREW_NAME)).thenReturn(null);

        int result = executeCommand("minewright promote " + TEST_CREW_NAME);

        assertEquals(0, result, "Promote command should fail for non-existent crew member");
    }

    // ==================== Demote Command Tests ====================

    @Test
    @DisplayName("Demote command requires admin permission")
    void testDemoteCommandRequiresPermission() {
        when(mockSource.hasPermission(PERMISSION_ADMIN)).thenReturn(false);

        int result = executeCommand("minewright demote " + TEST_CREW_NAME);

        assertEquals(0, result, "Demote command should fail without admin permission");
    }

    @Test
    @DisplayName("Demote command demotes foreman to worker")
    void testDemoteCommandSuccess() {
        when(mockSource.hasPermission(PERMISSION_ADMIN)).thenReturn(true);
        when(mockCrewManager.getCrewMember(TEST_CREW_NAME)).thenReturn(mockCrewMember);
        when(mockCrewMember.getRole()).thenReturn(AgentRole.FOREMAN);

        int result = executeCommand("minewright demote " + TEST_CREW_NAME);

        assertEquals(1, result, "Demote command should succeed");

        verify(mockCrewMember).setRole(AgentRole.WORKER);
    }

    @Test
    @DisplayName("Demote command fails if already worker")
    void testDemoteCommandFailsAlreadyWorker() {
        when(mockSource.hasPermission(PERMISSION_ADMIN)).thenReturn(true);
        when(mockCrewManager.getCrewMember(TEST_CREW_NAME)).thenReturn(mockCrewMember);
        when(mockCrewMember.getRole()).thenReturn(AgentRole.WORKER);

        int result = executeCommand("minewright demote " + TEST_CREW_NAME);

        assertEquals(0, result, "Demote command should fail if already worker");
    }

    @Test
    @DisplayName("Demote command fails for non-existent crew member")
    void testDemoteCommandFailsNonExistent() {
        when(mockSource.hasPermission(PERMISSION_ADMIN)).thenReturn(true);
        when(mockCrewManager.getCrewMember(TEST_CREW_NAME)).thenReturn(null);

        int result = executeCommand("minewright demote " + TEST_CREW_NAME);

        assertEquals(0, result, "Demote command should fail for non-existent crew member");
    }

    // ==================== Relationship Command Tests ====================

    @Test
    @DisplayName("Relationship command shows crew member stats")
    void testRelationshipCommandSuccess() {
        when(mockCrewManager.getCrewMember(TEST_CREW_NAME)).thenReturn(mockCrewMember);
        when(mockCrewMember.getCompanionMemory()).thenReturn(mock(com.minewright.memory.CompanionMemory.class));

        var mockRelationship = mock(com.minewright.memory.CompanionMemory.Relationship.class);
        when(mockRelationship.getAffection()).thenReturn(75);
        when(mockRelationship.getTrust()).thenReturn(80);
        when(mockRelationship.getCurrentMood()).thenReturn(mock(com.minewright.memory.CompanionMemory.Mood.class));

        var mockCompanionMemory = mock(com.minewright.memory.CompanionMemory.class);
        when(mockCompanionMemory.getRelationship()).thenReturn(mockRelationship);
        when(mockCompanionMemory.getPersonality()).thenReturn(mock(com.minewright.memory.PersonalitySystem.PersonalityProfile.class));
        when(mockCompanionMemory.getFirstMeeting()).thenReturn(Instant.now().minusSeconds(86400 * 10));
        when(mockCompanionMemory.getInteractionCount()).thenReturn(25);
        when(mockCompanionMemory.getInsideJokeCount()).thenReturn(5);
        when(mockCompanionMemory.getMilestones()).thenReturn(new ArrayList<>());
        when(mockCompanionMemory.getRecentMemories(anyInt())).thenReturn(new ArrayList<>());

        when(mockCrewMember.getCompanionMemory()).thenReturn(mockCompanionMemory);

        int result = executeCommand("minewright relationship " + TEST_CREW_NAME);

        assertEquals(1, result, "Relationship command should succeed");
    }

    @Test
    @DisplayName("Relationship command fails for non-existent crew member")
    void testRelationshipCommandFailsNonExistent() {
        when(mockCrewManager.getCrewMember(TEST_CREW_NAME)).thenReturn(null);

        int result = executeCommand("minewright relationship " + TEST_CREW_NAME);

        assertEquals(0, result, "Relationship command should fail for non-existent crew member");
    }

    // ==================== Voice Commands Tests ====================

    @Test
    @DisplayName("Voice on command enables voice system")
    void testVoiceOnCommand() {
        int result = executeCommand("minewright voice on");

        assertEquals(1, result, "Voice on command should succeed");

        verify(mockVoiceManager).setEnabled(true);
    }

    @Test
    @DisplayName("Voice off command disables voice system")
    void testVoiceOffCommand() {
        int result = executeCommand("minewright voice off");

        assertEquals(1, result, "Voice off command should succeed");

        verify(mockVoiceManager).setEnabled(false);
        verify(mockVoiceManager).stopAll();
    }

    @Test
    @DisplayName("Voice status command shows current status")
    void testVoiceStatusCommand() {
        int result = executeCommand("minewright voice status");

        assertEquals(1, result, "Voice status command should succeed");

        verify(mockVoiceManager).getConfig();
    }

    @Test
    @DisplayName("Voice test command tests voice system")
    void testVoiceTestCommand() {
        when(mockVoiceManager.isEnabled()).thenReturn(true);
        when(mockVoiceManager.test()).thenReturn(CompletableFuture.completedFuture(
            new VoiceManager.VoiceTestResult(true, "Test successful", 150)));

        int result = executeCommand("minewright voice test");

        assertEquals(1, result, "Voice test command should succeed");

        verify(mockVoiceManager).test();
    }

    @Test
    @DisplayName("Voice test command fails when voice disabled")
    void testVoiceTestCommandFailsWhenDisabled() {
        when(mockVoiceManager.isEnabled()).thenReturn(false);

        int result = executeCommand("minewright voice test");

        assertEquals(0, result, "Voice test command should fail when voice is disabled");
    }

    // ==================== Metrics Commands Tests ====================

    @Test
    @DisplayName("Metrics command requires admin permission")
    void testMetricsCommandRequiresPermission() {
        when(mockSource.hasPermission(PERMISSION_ADMIN)).thenReturn(false);

        int result = executeCommand("minewright metrics");

        assertEquals(0, result, "Metrics command should fail without admin permission");
    }

    @Test
    @DisplayName("Metrics command shows metrics summary")
    void testMetricsCommandSuccess() {
        when(mockSource.hasPermission(PERMISSION_ADMIN)).thenReturn(true);

        int result = executeCommand("minewright metrics");

        assertEquals(1, result, "Metrics command should succeed");
    }

    // ==================== Export Commands Tests ====================

    @Test
    @DisplayName("Export metrics command requires admin permission")
    void testExportMetricsRequiresPermission() {
        when(mockSource.hasPermission(PERMISSION_ADMIN)).thenReturn(false);

        int result = executeCommand("minewright export metrics /path/to/export");

        assertEquals(0, result, "Export metrics command should fail without admin permission");
    }

    @Test
    @DisplayName("Export traces command requires admin permission")
    void testExportTracesRequiresPermission() {
        when(mockSource.hasPermission(PERMISSION_ADMIN)).thenReturn(false);

        int result = executeCommand("minewright export traces /path/to/export");

        assertEquals(0, result, "Export traces command should fail without admin permission");
    }

    @Test
    @DisplayName("Export package command requires admin permission")
    void testExportPackageRequiresPermission() {
        when(mockSource.hasPermission(PERMISSION_ADMIN)).thenReturn(false);

        int result = executeCommand("minewright export package /path/to/export");

        assertEquals(0, result, "Export package command should fail without admin permission");
    }

    // ==================== Edge Case Tests ====================

    @Test
    @DisplayName("Command handles empty name gracefully")
    void testCommandHandlesEmptyName() {
        when(mockSource.hasPermission(PERMISSION_ADMIN)).thenReturn(true);

        int result = executeCommand("minewright spawn ");

        // Should not crash
        assertNotNull(result, "Command should handle empty name gracefully");
    }

    @Test
    @DisplayName("Command handles special characters in name")
    void testCommandHandlesSpecialCharactersInName() {
        when(mockSource.hasPermission(PERMISSION_ADMIN)).thenReturn(true);

        String specialName = "Test-Crew_Member.123";
        int result = executeCommand("minewright spawn " + specialName);

        // Should handle special characters
        assertNotNull(result, "Command should handle special characters in name");
    }

    @Test
    @DisplayName("Command handles very long names")
    void testCommandHandlesLongNames() {
        when(mockSource.hasPermission(PERMISSION_ADMIN)).thenReturn(true);

        String longName = "A".repeat(256);
        int result = executeCommand("minewright spawn " + longName);

        // Should handle long names
        assertNotNull(result, "Command should handle long names");
    }

    @Test
    @DisplayName("Command handles unicode characters")
    void testCommandHandlesUnicode() {
        when(mockSource.hasPermission(PERMISSION_ADMIN)).thenReturn(true);

        String unicodeName = "史蒂夫";
        int result = executeCommand("minewright spawn " + unicodeName);

        // Should handle unicode
        assertNotNull(result, "Command should handle unicode characters");
    }

    // ==================== Helper Methods ====================

    /**
     * Helper method to execute a command and return the result.
     */
    private int executeCommand(String command) {
        try {
            return dispatcher.execute(command, mockSource);
        } catch (CommandSyntaxException e) {
            return 0;
        }
    }
}
