package com.minewright.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.minewright.MineWrightMod;
import com.minewright.entity.CrewManager;
import com.minewright.entity.ForemanEntity;
import com.minewright.orchestration.AgentRole;
import com.minewright.action.ActionExecutor;
import com.minewright.memory.ForemanMemory;
import com.minewright.voice.VoiceManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for command execution flow and integration.
 *
 * Tests cover:
 * <ul>
 *   <li>Full command execution lifecycle</li>
 *   <li>Integration with CrewManager</li>
 *   <li>Integration with VoiceManager</li>
 *   <li>Integration with ActionExecutor</li>
 *   <li>Message feedback to players</li>
 *   <li>Async command execution</li>
 *   <li>Error propagation</li>
 *   <li>State changes after commands</li>
 * </ul>
 *
 * @since 1.0.0
 */
@DisplayName("Command Execution Integration Tests")
class CommandExecutionTest {

    private CommandDispatcher<CommandSourceStack> dispatcher;
    private CommandSourceStack mockSource;
    private ServerLevel mockServerLevel;
    private ServerPlayer mockPlayer;
    private CrewManager mockCrewManager;
    private ForemanEntity mockCrewMember;
    private ActionExecutor mockActionExecutor;
    private ForemanMemory mockMemory;
    private VoiceManager mockVoiceManager;

    private static final String TEST_CREW_NAME = "TestCrewMember";
    private static final String TEST_PLAYER_NAME = "TestPlayer";
    private static final int PERMISSION_ADMIN = 2;

    @BeforeEach
    void setUp() {
        dispatcher = new CommandDispatcher<>();

        // Create mocks
        mockSource = mock(CommandSourceStack.class);
        mockServerLevel = mock(ServerLevel.class);
        mockPlayer = mock(ServerPlayer.class);
        mockCrewManager = mock(CrewManager.class);
        mockCrewMember = mock(ForemanEntity.class);
        mockActionExecutor = mock(ActionExecutor.class);
        mockMemory = mock(ForemanMemory.class);
        mockVoiceManager = mock(VoiceManager.class);

        // Setup mock source
        when(mockSource.getLevel()).thenReturn(mockServerLevel);
        when(mockSource.getPosition()).thenReturn(new Vec3(100.0, 64.0, 200.0));
        when(mockSource.getEntity()).thenReturn(mockPlayer);
        when(mockSource.getTextName()).thenReturn(TEST_PLAYER_NAME);
        when(mockSource.hasPermission(PERMISSION_ADMIN)).thenReturn(true);
        when(mockSource.hasPermission(0)).thenReturn(true);

        // Setup mock player
        when(mockPlayer.getLookAngle()).thenReturn(new Vec3(1.0, 0.0, 0.0));

        // Setup mock crew member
        when(mockCrewMember.getEntityName()).thenReturn(TEST_CREW_NAME);
        when(mockCrewMember.getRole()).thenReturn(AgentRole.WORKER);
        when(mockCrewMember.getActionExecutor()).thenReturn(mockActionExecutor);
        when(mockCrewMember.getMemory()).thenReturn(mockMemory);

        // Setup mock crew manager
        when(mockCrewManager.spawnCrewMember(any(), any(), anyString())).thenReturn(mockCrewMember);
        when(mockCrewManager.getCrewMember(anyString())).thenReturn(mockCrewMember);
        when(mockCrewManager.removeCrewMember(anyString())).thenReturn(true);
        when(mockCrewManager.getCrewMemberNames()).thenReturn(new ArrayList<>(List.of(TEST_CREW_NAME)));

        // Setup mock voice manager
        VoiceManager.VoiceConfig mockConfig = new VoiceManager.VoiceConfig();
        when(mockVoiceManager.getConfig()).thenReturn(mockConfig);
        when(mockVoiceManager.isEnabled()).thenReturn(true);
        when(mockVoiceManager.test()).thenReturn(CompletableFuture.completedFuture(
            new VoiceManager.VoiceTestResult(true, "Test successful", 150)));

        // Register commands
        ForemanCommands.register(dispatcher);
    }

    // ==================== Spawn Command Execution Tests ====================

    @Test
    @DisplayName("Spawn command execution flow: success case")
    void testSpawnExecutionFlowSuccess() {
        int result = executeCommand("minewright spawn " + TEST_CREW_NAME);

        assertEquals(1, result, "Spawn command should return success");

        // Verify crew manager was called
        verify(mockCrewManager).spawnCrewMember(
            eq(mockServerLevel),
            any(Vec3.class),
            eq(TEST_CREW_NAME)
        );

        // Verify success message sent
        ArgumentCaptor<Component> successCaptor = ArgumentCaptor.forClass(Component.class);
        verify(mockSource).sendSuccess(successCaptor.capture(), eq(true));

        String message = successCaptor.getValue().getString();
        assertTrue(message.contains(TEST_CREW_NAME), "Success message should include crew name");
        assertTrue(message.contains("reported for duty") || message.contains("crew"),
            "Success message should confirm spawn");
    }

    @Test
    @DisplayName("Spawn command execution flow: failure when exists")
    void testSpawnExecutionFlowFailureExists() {
        when(mockCrewManager.spawnCrewMember(any(), any(), anyString())).thenReturn(null);

        int result = executeCommand("minewright spawn " + TEST_CREW_NAME);

        assertEquals(0, result, "Spawn command should return failure");

        // Verify failure message
        ArgumentCaptor<Component> failureCaptor = ArgumentCaptor.forClass(Component.class);
        verify(mockSource).sendFailure(failureCaptor.capture());

        String message = failureCaptor.getValue().getString();
        assertTrue(message.contains("Can't add") || message.contains("already"),
            "Failure message should explain why spawn failed");
    }

    @Test
    @DisplayName("Spawn command updates crew state")
    void testSpawnUpdatesCrewState() {
        executeCommand("minewright spawn " + TEST_CREW_NAME);

        // Verify spawn was called with correct parameters
        ArgumentCaptor<Vec3> positionCaptor = ArgumentCaptor.forClass(Vec3.class);
        verify(mockCrewManager).spawnCrewMember(
            eq(mockServerLevel),
            positionCaptor.capture(),
            eq(TEST_CREW_NAME)
        );

        Vec3 spawnPos = positionCaptor.getValue();
        assertNotNull(spawnPos, "Spawn position should be set");
        assertTrue(spawnPos.x > 100.0, "Should spawn in front of player");
    }

    // ==================== Remove Command Execution Tests ====================

    @Test
    @DisplayName("Remove command execution flow: success case")
    void testRemoveExecutionFlowSuccess() {
        when(mockCrewManager.removeCrewMember(TEST_CREW_NAME)).thenReturn(true);

        int result = executeCommand("minewright remove " + TEST_CREW_NAME);

        assertEquals(1, result, "Remove command should return success");

        verify(mockCrewManager).removeCrewMember(TEST_CREW_NAME);

        // Verify success message
        ArgumentCaptor<Component> successCaptor = ArgumentCaptor.forClass(Component.class);
        verify(mockSource).sendSuccess(successCaptor.capture(), eq(true));

        String message = successCaptor.getValue().getString();
        assertTrue(message.contains(TEST_CREW_NAME), "Message should include crew name");
        assertTrue(message.contains("clocked out") || message.contains("left"),
            "Message should confirm removal");
    }

    @Test
    @DisplayName("Remove command execution flow: failure when not found")
    void testRemoveExecutionFlowFailureNotFound() {
        when(mockCrewManager.removeCrewMember(TEST_CREW_NAME)).thenReturn(false);

        int result = executeCommand("minewright remove " + TEST_CREW_NAME);

        assertEquals(0, result, "Remove command should return failure");

        // Verify error message
        ArgumentCaptor<Component> failureCaptor = ArgumentCaptor.forClass(Component.class);
        verify(mockSource).sendFailure(failureCaptor.capture());

        String message = failureCaptor.getValue().getString();
        assertTrue(message.contains("Can't find") || message.contains("roster"),
            "Error should suggest checking roster");
    }

    // ==================== List Command Execution Tests ====================

    @Test
    @DisplayName("List command shows crew members")
    void testListExecutionFlow() {
        List<String> names = List.of("Steve", "Alex", "Bob");
        when(mockCrewManager.getCrewMemberNames()).thenReturn(new ArrayList<>(names));

        int result = executeCommand("minewright list");

        assertEquals(1, result, "List command should succeed");

        // Verify list message
        ArgumentCaptor<Component> messageCaptor = ArgumentCaptor.forClass(Component.class);
        verify(mockSource).sendSuccess(messageCaptor.capture(), eq(false));

        String message = messageCaptor.getValue().getString();
        assertTrue(message.contains("3"), "Should show count");
        assertTrue(message.contains("Steve"), "Should include Steve");
        assertTrue(message.contains("Alex"), "Should include Alex");
        assertTrue(message.contains("Bob"), "Should include Bob");
    }

    @Test
    @DisplayName("List command handles empty crew")
    void testListEmptyCrew() {
        when(mockCrewManager.getCrewMemberNames()).thenReturn(new ArrayList<>());

        int result = executeCommand("minewright list");

        assertEquals(1, result, "List command should succeed");

        ArgumentCaptor<Component> messageCaptor = ArgumentCaptor.forClass(Component.class);
        verify(mockSource).sendSuccess(messageCaptor.capture(), eq(false));

        String message = messageCaptor.getValue().getString();
        assertTrue(message.contains("empty") || message.contains("no crew"),
            "Should indicate empty job site");
    }

    // ==================== Status Command Execution Tests ====================

    @Test
    @DisplayName("Status command shows crew status")
    void testStatusExecutionFlow() {
        List<String> names = List.of(TEST_CREW_NAME);
        when(mockCrewManager.getCrewMemberNames()).thenReturn(new ArrayList<>(names));
        when(mockActionExecutor.getCurrentGoal()).thenReturn("Mining stone at 100,64,200");

        int result = executeCommand("minewright status");

        assertEquals(1, result, "Status command should succeed");

        // Verify status messages sent
        verify(mockSource, atLeastOnce()).sendSuccess(any(Component.class), eq(false));
    }

    @Test
    @DisplayName("Status command shows idle for crew with no goal")
    void testStatusIdleCrew() {
        List<String> names = List.of(TEST_CREW_NAME);
        when(mockCrewManager.getCrewMemberNames()).thenReturn(new ArrayList<>(names));
        when(mockActionExecutor.getCurrentGoal()).thenReturn(null);

        int result = executeCommand("minewright status");

        assertEquals(1, result, "Status command should succeed");

        verify(mockSource, atLeastOnce()).sendSuccess(any(Component.class), eq(false));
    }

    // ==================== Stop Command Execution Tests ====================

    @Test
    @DisplayName("Stop command stops action and clears queue")
    void testStopExecutionFlow() {
        int result = executeCommand("minewright stop " + TEST_CREW_NAME);

        assertEquals(1, result, "Stop command should succeed");

        // Verify stop was called
        verify(mockActionExecutor).stopCurrentAction();
        verify(mockMemory).clearTaskQueue();

        // Verify success message
        ArgumentCaptor<Component> successCaptor = ArgumentCaptor.forClass(Component.class);
        verify(mockSource).sendSuccess(successCaptor.capture(), eq(true));

        String message = successCaptor.getValue().getString();
        assertTrue(message.contains(TEST_CREW_NAME), "Message should include crew name");
        assertTrue(message.contains("stopped") || message.contains("cleared"),
            "Message should confirm stop");
    }

    @Test
    @DisplayName("Stop command fails for non-existent crew")
    void testStopNonExistentCrew() {
        when(mockCrewManager.getCrewMember(TEST_CREW_NAME)).thenReturn(null);

        int result = executeCommand("minewright stop " + TEST_CREW_NAME);

        assertEquals(0, result, "Stop command should fail");

        // Verify no action executor methods were called
        verify(mockActionExecutor, never()).stopCurrentAction();
        verify(mockMemory, never()).clearTaskQueue();
    }

    // ==================== Tell Command Execution Tests ====================

    @Test
    @DisplayName("Tell command processes natural language command")
    void testTellExecutionFlow() {
        String command = "mine 10 stone";
        int result = executeCommand("minewright tell " + TEST_CREW_NAME + " " + command);

        assertEquals(1, result, "Tell command should succeed");

        // Verify command was sent to action executor
        verify(mockActionExecutor).processNaturalLanguageCommand(command);

        // Verify feedback message
        ArgumentCaptor<Component> successCaptor = ArgumentCaptor.forClass(Component.class);
        verify(mockSource).sendSuccess(successCaptor.capture(), eq(true));

        String message = successCaptor.getValue().getString();
        assertTrue(message.contains(TEST_CREW_NAME), "Message should include crew name");
        assertTrue(message.contains(command), "Message should include the command");
    }

    @Test
    @DisplayName("Tell command handles complex multi-word commands")
    void testTellComplexCommand() {
        String complexCommand = "build a house with oak planks, 10 blocks wide";
        int result = executeCommand("minewright tell " + TEST_CREW_NAME + " " + complexCommand);

        assertEquals(1, result, "Tell command should succeed");

        verify(mockActionExecutor).processNaturalLanguageCommand(complexCommand);
    }

    // ==================== Promote/Demote Command Execution Tests ====================

    @Test
    @DisplayName("Promote command changes role to foreman")
    void testPromoteExecutionFlow() {
        when(mockCrewMember.getRole()).thenReturn(AgentRole.WORKER);

        int result = executeCommand("minewright promote " + TEST_CREW_NAME);

        assertEquals(1, result, "Promote command should succeed");

        verify(mockCrewMember).setRole(AgentRole.FOREMAN);

        // Verify success message
        ArgumentCaptor<Component> successCaptor = ArgumentCaptor.forClass(Component.class);
        verify(mockSource).sendSuccess(successCaptor.capture(), eq(true));

        String message = successCaptor.getValue().getString();
        assertTrue(message.contains("Promoted") || message.contains("Foreman"),
            "Message should confirm promotion");
    }

    @Test
    @DisplayName("Demote command changes role to worker")
    void testDemoteExecutionFlow() {
        when(mockCrewMember.getRole()).thenReturn(AgentRole.FOREMAN);

        int result = executeCommand("minewright demote " + TEST_CREW_NAME);

        assertEquals(1, result, "Demote command should succeed");

        verify(mockCrewMember).setRole(AgentRole.WORKER);

        // Verify success message
        ArgumentCaptor<Component> successCaptor = ArgumentCaptor.forClass(Component.class);
        verify(mockSource).sendSuccess(successCaptor.capture(), eq(true));

        String message = successCaptor.getValue().getString();
        assertTrue(message.contains("Demoted") || message.contains("Worker"),
            "Message should confirm demotion");
    }

    // ==================== Voice Command Execution Tests ====================

    @Test
    @DisplayName("Voice on command enables voice system")
    void testVoiceOnExecutionFlow() {
        int result = executeCommand("minewright voice on");

        assertEquals(1, result, "Voice on command should succeed");

        verify(mockVoiceManager).setEnabled(true);

        // Verify success message
        ArgumentCaptor<Component> successCaptor = ArgumentCaptor.forClass(Component.class);
        verify(mockSource).sendSuccess(successCaptor.capture(), eq(true));

        String message = successCaptor.getValue().getString();
        assertTrue(message.contains("ON") || message.contains("radio"),
            "Message should confirm voice is on");
    }

    @Test
    @DisplayName("Voice off command disables voice system")
    void testVoiceOffExecutionFlow() {
        int result = executeCommand("minewright voice off");

        assertEquals(1, result, "Voice off command should succeed");

        verify(mockVoiceManager).setEnabled(false);
        verify(mockVoiceManager).stopAll();

        // Verify success message
        ArgumentCaptor<Component> successCaptor = ArgumentCaptor.forClass(Component.class);
        verify(mockSource).sendSuccess(successCaptor.capture(), eq(true));

        String message = successCaptor.getValue().getString();
        assertTrue(message.contains("OFF") || message.contains("manual"),
            "Message should confirm voice is off");
    }

    @Test
    @DisplayName("Voice status command shows configuration")
    void testVoiceStatusExecutionFlow() {
        int result = executeCommand("minewright voice status");

        assertEquals(1, result, "Voice status command should succeed");

        verify(mockVoiceManager).getConfig();

        // Verify status message
        ArgumentCaptor<Component> messageCaptor = ArgumentCaptor.forClass(Component.class);
        verify(mockSource).sendSuccess(messageCaptor.capture(), eq(false));

        String message = messageCaptor.getValue().getString();
        assertTrue(message.contains("Voice") || message.contains("Status"),
            "Message should show voice status");
    }

    // ==================== Permission Enforcement Tests ====================

    @Test
    @DisplayName("Admin command fails without permission")
    void testAdminCommandFailsWithoutPermission() {
        when(mockSource.hasPermission(PERMISSION_ADMIN)).thenReturn(false);

        int result = executeCommand("minewright spawn " + TEST_CREW_NAME);

        assertEquals(0, result, "Admin command should fail without permission");

        // Verify no spawn was attempted
        verify(mockCrewManager, never()).spawnCrewMember(any(), any(), any());
    }

    @Test
    @DisplayName("Read-only command works without admin permission")
    void testReadOnlyCommandWorksWithoutPermission() {
        when(mockSource.hasPermission(PERMISSION_ADMIN)).thenReturn(false);

        List<String> names = List.of(TEST_CREW_NAME);
        when(mockCrewManager.getCrewMemberNames()).thenReturn(new ArrayList<>(names));

        int result = executeCommand("minewright list");

        assertEquals(1, result, "Read-only command should work without admin permission");

        // Verify crew manager was called
        verify(mockCrewManager).getCrewMemberNames();
    }

    // ==================== Error Handling Tests ====================

    @Test
    @DisplayName("Command handles crew manager errors gracefully")
    void testCommandHandlesCrewManagerErrors() {
        when(mockCrewManager.spawnCrewMember(any(), any(), anyString()))
            .thenThrow(new RuntimeException("Crew manager error"));

        // Should not throw exception
        assertDoesNotThrow(() -> executeCommand("minewright spawn " + TEST_CREW_NAME));
    }

    @Test
    @DisplayName("Command handles null crew member gracefully")
    void testCommandHandlesNullCrewMember() {
        when(mockCrewManager.getCrewMember(TEST_CREW_NAME)).thenReturn(null);

        int result = executeCommand("minewright stop " + TEST_CREW_NAME);

        assertEquals(0, result, "Command should handle null crew member");

        // Verify error message sent
        verify(mockSource).sendFailure(any(Component.class));
    }

    // ==================== Message Formatting Tests ====================

    @Test
    @DisplayName("Success messages are properly formatted")
    void testSuccessMessageFormatting() {
        int result = executeCommand("minewright spawn " + TEST_CREW_NAME);

        assertEquals(1, result);

        ArgumentCaptor<Component> messageCaptor = ArgumentCaptor.forClass(Component.class);
        verify(mockSource).sendSuccess(messageCaptor.capture(), eq(true));

        Component message = messageCaptor.getValue();
        assertNotNull(message, "Success message should not be null");
        assertFalse(message.getString().isEmpty(), "Success message should not be empty");
    }

    @Test
    @DisplayName("Error messages are properly formatted")
    void testErrorMessageFormatting() {
        when(mockCrewManager.removeCrewMember(TEST_CREW_NAME)).thenReturn(false);

        int result = executeCommand("minewright remove " + TEST_CREW_NAME);

        assertEquals(0, result);

        ArgumentCaptor<Component> messageCaptor = ArgumentCaptor.forClass(Component.class);
        verify(mockSource).sendFailure(messageCaptor.capture());

        Component message = messageCaptor.getValue();
        assertNotNull(message, "Error message should not be null");
        assertFalse(message.getString().isEmpty(), "Error message should not be empty");
    }

    // ==================== Async Execution Tests ====================

    @Test
    @DisplayName("Tell command executes asynchronously")
    void testTellAsyncExecution() throws Exception {
        String command = "mine 10 stone";
        int result = executeCommand("minewright tell " + TEST_CREW_NAME + " " + command);

        assertEquals(1, result, "Tell command should return immediately");

        // Give async execution time to complete
        TimeUnit.MILLISECONDS.sleep(100);

        // Verify command was processed
        verify(mockActionExecutor, timeout(1000)).processNaturalLanguageCommand(command);
    }

    // ==================== State Change Tests ====================

    @Test
    @DisplayName("Spawn command adds crew to manager")
    void testSpawnAddsCrewToManager() {
        executeCommand("minewright spawn " + TEST_CREW_NAME);

        verify(mockCrewManager).spawnCrewMember(
            eq(mockServerLevel),
            any(Vec3.class),
            eq(TEST_CREW_NAME)
        );
    }

    @Test
    @DisplayName("Remove command removes crew from manager")
    void testRemoveRemovesCrewFromManager() {
        executeCommand("minewright remove " + TEST_CREW_NAME);

        verify(mockCrewManager).removeCrewMember(TEST_CREW_NAME);
    }

    // ==================== Multiple Execution Tests ====================

    @Test
    @DisplayName("Multiple spawn commands are handled")
    void testMultipleSpawnCommands() {
        executeCommand("minewright spawn Steve");
        executeCommand("minewright spawn Alex");
        executeCommand("minewright spawn Bob");

        verify(mockCrewManager, times(3)).spawnCrewMember(
            eq(mockServerLevel),
            any(Vec3.class),
            anyString()
        );
    }

    @Test
    @DisplayName("Multiple tell commands are processed")
    void testMultipleTellCommands() {
        executeCommand("minewright tell Steve mine stone");
        executeCommand("minewright tell Steve craft planks");
        executeCommand("minewright tell Steve build house");

        verify(mockActionExecutor, atLeast(3)).processNaturalLanguageCommand(anyString());
    }

    // ==================== Helper Methods ====================

    private int executeCommand(String command) {
        try {
            return dispatcher.execute(command, mockSource);
        } catch (Exception e) {
            return 0;
        }
    }
}
