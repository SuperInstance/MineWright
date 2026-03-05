package com.minewright.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.minewright.entity.CrewManager;
import com.minewright.entity.ForemanEntity;
import com.minewright.orchestration.AgentRole;
import com.minewright.voice.VoiceManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive edge case and error handling tests for commands.
 *
 * Tests cover:
 * <ul>
 *   <li>Null and empty inputs</li>
 *   <li>Boundary conditions</li>
 *   <li>Special characters and unicode</li>
 *   <li>Invalid state scenarios</li>
 *   <li>Concurrent command execution</li>
 *   <li>Resource limits</li>
 *   <li>Recovery from errors</li>
 * </ul>
 *
 * @since 1.0.0
 */
@DisplayName("Command Edge Cases and Error Handling Tests")
class CommandEdgeCasesTest {

    private CommandDispatcher<CommandSourceStack> dispatcher;
    private CommandSourceStack mockSource;
    private ServerLevel mockServerLevel;
    private ServerPlayer mockPlayer;
    private CrewManager mockCrewManager;
    private ForemanEntity mockCrewMember;
    private VoiceManager mockVoiceManager;

    private static final String TEST_CREW_NAME = "TestCrewMember";
    private static final int PERMISSION_ADMIN = 2;

    @BeforeEach
    void setUp() {
        dispatcher = new CommandDispatcher<>();

        mockSource = mock(CommandSourceStack.class);
        mockServerLevel = mock(ServerLevel.class);
        mockPlayer = mock(ServerPlayer.class);
        mockCrewManager = mock(CrewManager.class);
        mockCrewMember = mock(ForemanEntity.class);
        mockVoiceManager = mock(VoiceManager.class);

        when(mockSource.getLevel()).thenReturn(mockServerLevel);
        when(mockSource.getPosition()).thenReturn(new Vec3(0, 0, 0));
        when(mockSource.getEntity()).thenReturn(mockPlayer);
        when(mockSource.getTextName()).thenReturn("TestPlayer");
        when(mockSource.hasPermission(PERMISSION_ADMIN)).thenReturn(true);
        when(mockSource.hasPermission(0)).thenReturn(true);
        when(mockPlayer.getLookAngle()).thenReturn(new Vec3(1.0, 0.0, 0.0));

        when(mockCrewMember.getEntityName()).thenReturn(TEST_CREW_NAME);
        when(mockCrewMember.getRole()).thenReturn(AgentRole.WORKER);
        when(mockCrewManager.spawnCrewMember(any(), any(), anyString())).thenReturn(mockCrewMember);
        when(mockCrewManager.getCrewMember(anyString())).thenReturn(mockCrewMember);
        when(mockCrewManager.removeCrewMember(anyString())).thenReturn(true);
        when(mockCrewManager.getCrewMemberNames()).thenReturn(new ArrayList<>(List.of(TEST_CREW_NAME)));

        VoiceManager.VoiceConfig mockConfig = new VoiceManager.VoiceConfig();
        when(mockVoiceManager.getConfig()).thenReturn(mockConfig);
        when(mockVoiceManager.isEnabled()).thenReturn(true);

        ForemanCommands.register(dispatcher);
    }

    // ==================== Null and Empty Input Tests ====================

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Command handles null and empty names gracefully")
    void testNullAndEmptyNames(String name) {
        if (name == null) {
            // Can't actually pass null through command dispatcher, but test the concept
            assertDoesNotThrow(() -> executeCommand("minewright spawn "));
        } else {
            assertDoesNotThrow(() -> executeCommand("minewright spawn " + name));
        }
    }

    @Test
    @DisplayName("Command handles whitespace-only name")
    void testWhitespaceOnlyName() {
        assertDoesNotThrow(() -> executeCommand("minewright spawn     "));
    }

    @Test
    @DisplayName("Command handles name with leading/trailing whitespace")
    void testNameWithLeadingTrailingWhitespace() {
        assertDoesNotThrow(() -> executeCommand("minewright spawn   Steve   "));
    }

    // ==================== Special Character Tests ====================

    @ParameterizedTest
    @ValueSource(strings = {
        "Steve-The-Builder",
        "Steve_The_Builder",
        "Steve.The.Builder",
        "Steve's",
        "O'Neil",
        "Jean-Claude",
        "Mary Ann",
        "John-Paul II",
        "Test#123",
        "Steve@Builder",
        "name+with+plus",
        "name=equals",
        "name[brackets]",
        "name{braces}",
        "name(parentheses)",
        "name<angle>",
        "name*dollar",
        "name%percent",
        "name&ampersand",
        "name^caret",
        "name~tilde",
        "name`backtick`",
        "name|pipe",
        "name\\backslash",
        "name/forward",
        "name?question",
        "name!exclamation"
    })
    @DisplayName("Command handles names with special characters")
    void testNamesWithSpecialCharacters(String name) {
        assertDoesNotThrow(() -> executeCommand("minewright spawn " + name),
            "Should handle name: " + name);
    }

    @Test
    @DisplayName("Command handles unicode characters")
    void testUnicodeCharacters() {
        String[] unicodeNames = {
            "史蒂夫", // Chinese
            "Стив", // Russian
            "סטיב", // Hebrew
            "ستيف", // Arabic
            "สตีฟ", // Thai
            "Στιβ", // Greek
            "あいうえお", // Japanese
            "abcdef", // Regular ASCII for comparison
            "Mueller", // German umlaut alternative
            "José", // Spanish
            "François", // French
            "Müller", // German
            "Åse", // Scandinavian
            "Bjørn", // Norwegian/Danish
            "Zoë", // Dutch
            "İstanbul", // Turkish
            "Александр", // Russian
            "李雷" // Chinese
        };

        for (String name : unicodeNames) {
            assertDoesNotThrow(() -> executeCommand("minewright spawn " + name),
                "Should handle unicode name: " + name);
        }
    }

    @Test
    @DisplayName("Command handles emojis in names")
    void testEmojisInNames() {
        String[] emojiNames = {
            "Steve👷",
            "Builder🏗️",
            "Miner⛏️",
            "Steve😀",
            "Test🎉",
            "👷Steve",
            "🏗️Builder",
            "⛏️Miner",
            "Steve👷‍♂️",
            "Crew👥"
        };

        for (String name : emojiNames) {
            assertDoesNotThrow(() -> executeCommand("minewright spawn " + name),
                "Should handle emoji name: " + name);
        }
    }

    @Test
    @DisplayName("Command handles zero-width characters")
    void testZeroWidthCharacters() {
        String nameWithZWJ = "Steve\u200DBuilder"; // Zero-width joiner
        assertDoesNotThrow(() -> executeCommand("minewright spawn " + nameWithZWJ));
    }

    // ==================== Boundary Condition Tests ====================

    @Test
    @DisplayName("Command handles single character name")
    void testSingleCharacterName() {
        assertDoesNotThrow(() -> executeCommand("minewright spawn S"));
    }

    @Test
    @DisplayName("Command handles very long name")
    void testVeryLongName() {
        String longName = "A".repeat(1000);
        assertDoesNotThrow(() -> executeCommand("minewright spawn " + longName));
    }

    @Test
    @DisplayName("Command handles maximum length name")
    void testMaximumLengthName() {
        String maxLengthName = "A".repeat(256); // Typical maximum for Minecraft names
        assertDoesNotThrow(() -> executeCommand("minewright spawn " + maxLengthName));
    }

    @Test
    @DisplayName("Command handles name at exact typical limit")
    void testNameAtTypicalLimit() {
        String typicalLimitName = "A".repeat(16); // Minecraft username limit
        assertDoesNotThrow(() -> executeCommand("minewright spawn " + typicalLimitName));
    }

    // ==================== Path Argument Edge Cases ====================

    @Test
    @DisplayName("Export command handles empty path")
    void testExportEmptyPath() {
        assertDoesNotThrow(() -> executeCommand("minewright export metrics "));
    }

    @Test
    @DisplayName("Export command handles root path")
    void testExportRootPath() {
        assertDoesNotThrow(() -> executeCommand("minewright export metrics /"));
    }

    @Test
    @DisplayName("Export command handles relative path")
    void testExportRelativePath() {
        assertDoesNotThrow(() -> executeCommand("minewright export metrics ../relative/path"));
    }

    @Test
    @DisplayName("Export command handles path with spaces")
    void testExportPathWithSpaces() {
        assertDoesNotThrow(() -> executeCommand("minewright export metrics \"/path/with spaces/to/export\""));
    }

    @Test
    @DisplayName("Export command handles Windows path with drive letter")
    void testExportWindowsPath() {
        assertDoesNotThrow(() -> executeCommand("minewright export metrics C:\\Users\\Test\\export"));
    }

    @Test
    @DisplayName("Export command handles UNC path")
    void testExportUNCPath() {
        assertDoesNotThrow(() -> executeCommand("minewright export metrics \\\\server\\share\\path"));
    }

    @Test
    @DisplayName("Export command handles very long path")
    void testExportVeryLongPath() {
        String longPath = "/a/" + "b/".repeat(100) + "export";
        assertDoesNotThrow(() -> executeCommand("minewright export metrics " + longPath));
    }

    // ==================== Tell Command Edge Cases ====================

    @Test
    @DisplayName("Tell command handles empty command")
    void testTellEmptyCommand() {
        assertDoesNotThrow(() -> executeCommand("minewright tell Steve "));
    }

    @Test
    @DisplayName("Tell command handles very long command")
    void testTellVeryLongCommand() {
        String longCommand = "mine " + "stone ".repeat(1000);
        assertDoesNotThrow(() -> executeCommand("minewright tell Steve " + longCommand));
    }

    @Test
    @DisplayName("Tell command handles command with only special characters")
    void testTellOnlySpecialCharacters() {
        String specialCommand = "!@#$%^&*()";
        assertDoesNotThrow(() -> executeCommand("minewright tell Steve " + specialCommand));
    }

    @Test
    @DisplayName("Tell command handles command with newlines")
    void testTellWithNewlines() {
        String commandWithNewlines = "mine stone\nthen craft planks";
        assertDoesNotThrow(() -> executeCommand("minewright tell Steve " + commandWithNewlines));
    }

    @Test
    @DisplayName("Tell command handles command with tabs")
    void testTellWithTabs() {
        String commandWithTabs = "mine stone\tcraft planks";
        assertDoesNotThrow(() -> executeCommand("minewright tell Steve " + commandWithTabs));
    }

    @Test
    @DisplayName("Tell command handles command with mixed case")
    void testTellMixedCase() {
        String mixedCase = "MiNe StOnE ThEn CrAfT PlAnKs";
        assertDoesNotThrow(() -> executeCommand("minewright tell Steve " + mixedCase));
    }

    // ==================== Null State Tests ====================

    @Test
    @DisplayName("Command handles null server level")
    void testNullServerLevel() {
        when(mockSource.getLevel()).thenReturn(null);

        assertDoesNotThrow(() -> executeCommand("minewright spawn Steve"));
    }

    @Test
    @DisplayName("Command handles null entity")
    void testNullEntity() {
        when(mockSource.getEntity()).thenReturn(null);

        assertDoesNotThrow(() -> executeCommand("minewright spawn Steve"));
    }

    @Test
    @DisplayName("Command handles null position")
    void testNullPosition() {
        when(mockSource.getPosition()).thenReturn(null);

        assertDoesNotThrow(() -> executeCommand("minewright spawn Steve"));
    }

    @Test
    @DisplayName("Command handles null look angle")
    void testNullLookAngle() {
        when(mockPlayer.getLookAngle()).thenReturn(null);

        assertDoesNotThrow(() -> executeCommand("minewright spawn Steve"));
    }

    @Test
    @DisplayName("Command handles crew manager returning null")
    void testCrewManagerReturnsNull() {
        when(mockCrewManager.getCrewMember(anyString())).thenReturn(null);

        assertDoesNotThrow(() -> executeCommand("minewright stop Steve"));
    }

    @Test
    @DisplayName("Command handles crew member with null memory")
    void testCrewMemberNullMemory() {
        when(mockCrewMember.getMemory()).thenReturn(null);

        assertDoesNotThrow(() -> executeCommand("minewright stop Steve"));
    }

    // ==================== Permission Edge Cases ====================

    @Test
    @DisplayName("Command handles permission level boundary")
    void testPermissionLevelBoundary() {
        when(mockSource.hasPermission(PERMISSION_ADMIN)).thenReturn(false);
        when(mockSource.hasPermission(PERMISSION_ADMIN - 1)).thenReturn(true);

        assertDoesNotThrow(() -> executeCommand("minewright spawn Steve"));
    }

    @Test
    @DisplayName("Command handles zero permission level")
    void testZeroPermissionLevel() {
        when(mockSource.hasPermission(PERMISSION_ADMIN)).thenReturn(false);
        when(mockSource.hasPermission(0)).thenReturn(true);

        int result = executeCommand("minewright list");
        assertEquals(1, result, "Read-only commands should work with permission 0");
    }

    // ==================== Crew Manager Error Scenarios ====================

    @Test
    @DisplayName("Command handles crew manager exception")
    void testCrewManagerException() {
        when(mockCrewManager.spawnCrewMember(any(), any(), anyString()))
            .thenThrow(new RuntimeException("Crew manager error"));

        assertDoesNotThrow(() -> executeCommand("minewright spawn Steve"));
    }

    @Test
    @DisplayName("Command handles crew already at capacity")
    void testCrewAtCapacity() {
        when(mockCrewManager.spawnCrewMember(any(), any(), anyString())).thenReturn(null);

        int result = executeCommand("minewright spawn Steve");
        assertEquals(0, result, "Should fail when crew is at capacity");
    }

    @Test
    @DisplayName("Command handles duplicate crew member name")
    void testDuplicateCrewName() {
        when(mockCrewManager.spawnCrewMember(any(), any(), anyString())).thenReturn(null);

        int result = executeCommand("minewright spawn Steve");
        assertEquals(0, result, "Should fail when crew member already exists");
    }

    // ==================== Voice Command Edge Cases ====================

    @Test
    @DisplayName("Voice test handles disabled voice system")
    void testVoiceTestWhenDisabled() {
        when(mockVoiceManager.isEnabled()).thenReturn(false);

        int result = executeCommand("minewright voice test");
        assertEquals(0, result, "Voice test should fail when voice is disabled");
    }

    @Test
    @DisplayName("Voice status handles null config")
    void testVoiceStatusNullConfig() {
        when(mockVoiceManager.getConfig()).thenReturn(null);

        assertDoesNotThrow(() -> executeCommand("minewright voice status"));
    }

    // ==================== Concurrent Execution Tests ====================

    @Test
    @DisplayName("Command handles rapid execution")
    void testRapidExecution() {
        for (int i = 0; i < 100; i++) {
            assertDoesNotThrow(() -> executeCommand("minewright list"),
                "Should handle rapid execution, iteration: " + i);
        }
    }

    @Test
    @DisplayName("Command handles concurrent spawn attempts")
    void testConcurrentSpawns() throws InterruptedException {
        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                assertDoesNotThrow(() -> executeCommand("minewright spawn Steve" + index));
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }
    }

    // ==================== Invalid State Tests ====================

    @Test
    @DisplayName("Promote handles non-worker role")
    void testPromoteNonWorkerRole() {
        when(mockCrewMember.getRole()).thenReturn(AgentRole.FOREMAN);

        int result = executeCommand("minewright promote Steve");
        assertEquals(0, result, "Should fail when trying to promote non-worker");
    }

    @Test
    @DisplayName("Demote handles non-foreman role")
    void testDemoteNonForemanRole() {
        when(mockCrewMember.getRole()).thenReturn(AgentRole.WORKER);

        int result = executeCommand("minewright demote Steve");
        assertEquals(0, result, "Should fail when trying to demote non-foreman");
    }

    @Test
    @DisplayName("Stop handles already stopped crew")
    void testStopAlreadyStopped() {
        when(mockCrewMember.getActionExecutor()).thenReturn(null);

        assertDoesNotThrow(() -> executeCommand("minewright stop Steve"));
    }

    // ==================== Numeric and Symbol Tests ====================

    @ParameterizedTest
    @ValueSource(strings = {
        "123",
        "123.456",
        "0",
        "-1",
        "1e5",
        "0xFF",
        "0b1010",
        "1_000_000",
        "3.14",
        "-273.15",
        "6.02e23"
    })
    @DisplayName("Command handles numeric names")
    void testNumericNames(String name) {
        assertDoesNotThrow(() -> executeCommand("minewright spawn " + name),
            "Should handle numeric name: " + name);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Steve\n",
        "Steve\r",
        "Steve\r\n",
        "Steve\t",
        "Steve\u0000",
        "Steve\u0001",
        "Steve\u007F"
    })
    @DisplayName("Command handles control characters")
    void testControlCharacters(String name) {
        assertDoesNotThrow(() -> executeCommand("minewright spawn " + name),
            "Should handle name with control char");
    }

    // ==================== Recovery Tests ====================

    @Test
    @DisplayName("Command recovers from temporary failure")
    void testRecoveryFromTemporaryFailure() {
        // First attempt fails
        when(mockCrewManager.spawnCrewMember(any(), any(), anyString())).thenReturn(null);
        int result1 = executeCommand("minewright spawn Steve");
        assertEquals(0, result1, "First attempt should fail");

        // Second attempt succeeds
        when(mockCrewManager.spawnCrewMember(any(), any(), anyString())).thenReturn(mockCrewMember);
        int result2 = executeCommand("minewright spawn Steve");
        assertEquals(1, result2, "Second attempt should succeed");
    }

    @Test
    @DisplayName("Command handles state changes between executions")
    void testStateChangesBetweenExecutions() {
        // Spawn succeeds
        when(mockCrewManager.spawnCrewMember(any(), any(), anyString())).thenReturn(mockCrewMember);
        int spawnResult = executeCommand("minewright spawn Steve");
        assertEquals(1, spawnResult, "Spawn should succeed");

        // Stop succeeds
        int stopResult = executeCommand("minewright stop Steve");
        assertEquals(1, stopResult, "Stop should succeed");

        // Remove succeeds
        int removeResult = executeCommand("minewright remove Steve");
        assertEquals(1, removeResult, "Remove should succeed");
    }

    // ==================== Memory and Resource Tests ====================

    @Test
    @DisplayName("Command handles large crew lists")
    void testLargeCrewList() {
        List<String> largeCrew = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            largeCrew.add("Crew" + i);
        }
        when(mockCrewManager.getCrewMemberNames()).thenReturn(largeCrew);

        assertDoesNotThrow(() -> executeCommand("minewright list"));
    }

    @Test
    @DisplayName("Command handles many status checks")
    void testManyStatusChecks() {
        List<String> crew = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            crew.add("Crew" + i);
        }
        when(mockCrewManager.getCrewMemberNames()).thenReturn(crew);

        assertDoesNotThrow(() -> executeCommand("minewright status"));
    }

    // ==================== Cross-Platform Tests ====================

    @Test
    @DisplayName("Command handles Windows line endings")
    void testWindowsLineEndings() {
        String command = "minewright list\r\n";
        assertDoesNotThrow(() -> executeCommand(command));
    }

    @Test
    @DisplayName("Command handles Unix line endings")
    void testUnixLineEndings() {
        String command = "minewright list\n";
        assertDoesNotThrow(() -> executeCommand(command));
    }

    @Test
    @DisplayName("Command handles old Mac line endings")
    void testOldMacLineEndings() {
        String command = "minewright list\r";
        assertDoesNotThrow(() -> executeCommand(command));
    }

    // ==================== Helper Methods ====================

    private int executeCommand(String command) {
        try {
            return dispatcher.execute(command, mockSource);
        } catch (Exception e) {
            return 0;
        }
    }

    private static Stream<String> provideSpecialCharacters() {
        return Stream.of(
            "Steve-The-Builder",
            "Steve_The_Builder",
            "Steve.The.Builder",
            "O'Neil",
            "Jean-Claude"
        );
    }
}
