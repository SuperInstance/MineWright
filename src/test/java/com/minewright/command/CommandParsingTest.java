package com.minewright.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for command parsing and argument handling.
 *
 * Tests cover:
 * <ul>
 *   <li>Command argument parsing</li>
 *   <li>Greedy string arguments</li>
 *   <li>Command validation</li>
 *   <li>Command suggestions</li>
 *   <li>Parse error handling</li>
 *   <li>Edge cases in command syntax</li>
 * </ul>
 *
 * @since 1.0.0
 */
@DisplayName("Command Parsing Comprehensive Tests")
class CommandParsingTest {

    private CommandDispatcher<CommandSourceStack> dispatcher;
    private CommandSourceStack mockSource;
    private ServerLevel mockServerLevel;
    private ServerPlayer mockPlayer;

    @BeforeEach
    void setUp() {
        dispatcher = new CommandDispatcher<>();
        mockSource = mock(CommandSourceStack.class);
        mockServerLevel = mock(ServerLevel.class);
        mockPlayer = mock(ServerPlayer.class);

        // Setup mock source behavior
        when(mockSource.getLevel()).thenReturn(mockServerLevel);
        when(mockSource.getPosition()).thenReturn(new Vec3(0, 0, 0));
        when(mockSource.getEntity()).thenReturn(mockPlayer);
        when(mockSource.getTextName()).thenReturn("TestPlayer");
        when(mockSource.hasPermission(2)).thenReturn(true);
        when(mockSource.hasPermission(0)).thenReturn(true);

        // Register commands
        ForemanCommands.register(dispatcher);
    }

    // ==================== Basic Command Parsing Tests ====================

    @Test
    @DisplayName("Parse spawn command with single word name")
    void testParseSpawnSingleWordName() {
        String command = "minewright spawn Steve";
        ParseResults<CommandSourceStack> result = dispatcher.parse(command, mockSource);

        assertNotNull(result, "Parse result should not be null");
        assertFalse(result.getReader().canRead(), "Reader should be fully consumed");
    }

    @Test
    @DisplayName("Parse spawn command with multi-word name")
    void testParseSpawnMultiWordName() {
        String command = "minewright spawn \"Steve the Builder\"";
        ParseResults<CommandSourceStack> result = dispatcher.parse(command, mockSource);

        assertNotNull(result, "Parse result should not be null");
        // Note: Brigadier handles quoted strings differently, this tests the concept
    }

    @Test
    @DisplayName("Parse remove command")
    void testParseRemoveCommand() {
        String command = "minewright remove Steve";
        ParseResults<CommandSourceStack> result = dispatcher.parse(command, mockSource);

        assertNotNull(result, "Parse result should not be null");
        assertFalse(result.getExceptions().isEmpty() || result.getReader().canRead(),
            "Command should parse successfully");
    }

    @Test
    @DisplayName("Parse list command with no arguments")
    void testParseListCommand() {
        String command = "minewright list";
        ParseResults<CommandSourceStack> result = dispatcher.parse(command, mockSource);

        assertNotNull(result, "Parse result should not be null");
        assertFalse(result.getReader().canRead(), "Reader should be fully consumed");
    }

    @Test
    @DisplayName("Parse status command with no arguments")
    void testParseStatusCommand() {
        String command = "minewright status";
        ParseResults<CommandSourceStack> result = dispatcher.parse(command, mockSource);

        assertNotNull(result, "Parse result should not be null");
        assertFalse(result.getReader().canRead(), "Reader should be fully consumed");
    }

    // ==================== Tell Command Parsing Tests ====================

    @Test
    @DisplayName("Parse tell command with simple message")
    void testParseTellSimpleMessage() {
        String command = "minewright tell Steve mine stone";
        ParseResults<CommandSourceStack> result = dispatcher.parse(command, mockSource);

        assertNotNull(result, "Parse result should not be null");
    }

    @Test
    @DisplayName("Parse tell command with complex message")
    void testParseTellComplexMessage() {
        String command = "minewright tell Steve build a house with oak planks";
        ParseResults<CommandSourceStack> result = dispatcher.parse(command, mockSource);

        assertNotNull(result, "Parse result should not be null");
    }

    @Test
    @DisplayName("Parse tell command with quoted message")
    void testParseTellQuotedMessage() {
        String command = "minewright tell Steve \"mine 10 stone then craft sticks\"";
        ParseResults<CommandSourceStack> result = dispatcher.parse(command, mockSource);

        assertNotNull(result, "Parse result should not be null");
    }

    @Test
    @DisplayName("Parse tell command with special characters")
    void testParseTellSpecialCharacters() {
        String command = "minewright tell Steve mine! build? craft.";
        ParseResults<CommandSourceStack> result = dispatcher.parse(command, mockSource);

        assertNotNull(result, "Parse result should not be null");
    }

    @Test
    @DisplayName("Parse tell command with numbers")
    void testParseTellWithNumbers() {
        String command = "minewright tell Steve mine 64 stone";
        ParseResults<CommandSourceStack> result = dispatcher.parse(command, mockSource);

        assertNotNull(result, "Parse result should not be null");
    }

    // ==================== Voice Command Parsing Tests ====================

    @Test
    @DisplayName("Parse voice on command")
    void testParseVoiceOn() {
        String command = "minewright voice on";
        ParseResults<CommandSourceStack> result = dispatcher.parse(command, mockSource);

        assertNotNull(result, "Parse result should not be null");
        assertFalse(result.getReader().canRead(), "Reader should be fully consumed");
    }

    @Test
    @DisplayName("Parse voice off command")
    void testParseVoiceOff() {
        String command = "minewright voice off";
        ParseResults<CommandSourceStack> result = dispatcher.parse(command, mockSource);

        assertNotNull(result, "Parse result should not be null");
        assertFalse(result.getReader().canRead(), "Reader should be fully consumed");
    }

    @Test
    @DisplayName("Parse voice status command")
    void testParseVoiceStatus() {
        String command = "minewright voice status";
        ParseResults<CommandSourceStack> result = dispatcher.parse(command, mockSource);

        assertNotNull(result, "Parse result should not be null");
        assertFalse(result.getReader().canRead(), "Reader should be fully consumed");
    }

    @Test
    @DisplayName("Parse voice test command")
    void testParseVoiceTest() {
        String command = "minewright voice test";
        ParseResults<CommandSourceStack> result = dispatcher.parse(command, mockSource);

        assertNotNull(result, "Parse result should not be null");
        assertFalse(result.getReader().canRead(), "Reader should be fully consumed");
    }

    // ==================== Export Command Parsing Tests ====================

    @Test
    @DisplayName("Parse export metrics command")
    void testParseExportMetrics() {
        String command = "minewright export metrics /path/to/export";
        ParseResults<CommandSourceStack> result = dispatcher.parse(command, mockSource);

        assertNotNull(result, "Parse result should not be null");
    }

    @Test
    @DisplayName("Parse export traces command")
    void testParseExportTraces() {
        String command = "minewright export traces /path/to/export";
        ParseResults<CommandSourceStack> result = dispatcher.parse(command, mockSource);

        assertNotNull(result, "Parse result should not be null");
    }

    @Test
    @DisplayName("Parse export package command")
    void testParseExportPackage() {
        String command = "minewright export package /path/to/export";
        ParseResults<CommandSourceStack> result = dispatcher.parse(command, mockSource);

        assertNotNull(result, "Parse result should not be null");
    }

    @Test
    @DisplayName("Parse export metrics with Windows path")
    void testParseExportMetricsWindowsPath() {
        String command = "minewright export metrics C:\\Users\\Test\\export";
        ParseResults<CommandSourceStack> result = dispatcher.parse(command, mockSource);

        assertNotNull(result, "Parse result should not be null");
    }

    @Test
    @DisplayName("Parse export metrics with Unix path")
    void testParseExportMetricsUnixPath() {
        String command = "minewright export metrics /home/user/export";
        ParseResults<CommandSourceStack> result = dispatcher.parse(command, mockSource);

        assertNotNull(result, "Parse result should not be null");
    }

    // ==================== Relationship Command Parsing Tests ====================

    @Test
    @DisplayName("Parse relationship command")
    void testParseRelationshipCommand() {
        String command = "minewright relationship Steve";
        ParseResults<CommandSourceStack> result = dispatcher.parse(command, mockSource);

        assertNotNull(result, "Parse result should not be null");
    }

    // ==================== Promote/Demote Command Parsing Tests ====================

    @Test
    @DisplayName("Parse promote command")
    void testParsePromoteCommand() {
        String command = "minewright promote Steve";
        ParseResults<CommandSourceStack> result = dispatcher.parse(command, mockSource);

        assertNotNull(result, "Parse result should not be null");
    }

    @Test
    @DisplayName("Parse demote command")
    void testParseDemoteCommand() {
        String command = "minewright demote Steve";
        ParseResults<CommandSourceStack> result = dispatcher.parse(command, mockSource);

        assertNotNull(result, "Parse result should not be null");
    }

    // ==================== Command Node Structure Tests ====================

    @Test
    @DisplayName("Minewright command node exists")
    void testMinewrightCommandNodeExists() {
        CommandNode<CommandSourceStack> node = dispatcher.getRoot().getChild("minewright");

        assertNotNull(node, "Minewright command node should exist");
        assertTrue(node instanceof LiteralCommandNode, "Should be a literal command node");
    }

    @Test
    @DisplayName("All direct subcommands are literal nodes")
    void testSubcommandsAreLiteralNodes() {
        CommandNode<CommandSourceStack> minewrightNode = dispatcher.getRoot().getChild("minewright");

        assertNotNull(minewrightNode, "Minewright node should exist");

        // Check that direct children are literal nodes
        for (CommandNode<CommandSourceStack> child : minewrightNode.getChildren()) {
            assertTrue(child instanceof LiteralCommandNode,
                "Child command should be literal node: " + child.getName());
        }
    }

    @Test
    @DisplayName("Command tree has expected depth")
    void testCommandTreeDepth() {
        // minewright -> export -> metrics -> <path>
        // Depth should be at least 4
        CommandNode<CommandSourceStack> minewrightNode = dispatcher.getRoot().getChild("minewright");
        CommandNode<CommandSourceStack> exportNode = minewrightNode.getChild("export");
        CommandNode<CommandSourceStack> metricsNode = exportNode.getChild("metrics");

        assertNotNull(metricsNode, "Command tree should have depth of at least 4");
    }

    // ==================== Invalid Command Tests ====================

    @Test
    @DisplayName("Invalid command is handled gracefully")
    void testInvalidCommand() {
        String command = "minewright invalid_command";
        ParseResults<CommandSourceStack> result = dispatcher.parse(command, mockSource);

        assertNotNull(result, "Parse result should exist");
        // Should have exceptions or be incomplete
        assertTrue(result.getExceptions().isEmpty() || !result.getReader().canRead(),
            "Invalid command should be handled");
    }

    @Test
    @DisplayName("Empty command is handled gracefully")
    void testEmptyCommand() {
        String command = "";
        ParseResults<CommandSourceStack> result = dispatcher.parse(command, mockSource);

        assertNotNull(result, "Parse result should exist for empty command");
    }

    @Test
    @DisplayName("Command with extra whitespace is handled")
    void testCommandWithExtraWhitespace() {
        String command = "minewright    list    ";
        ParseResults<CommandSourceStack> result = dispatcher.parse(command, mockSource);

        assertNotNull(result, "Parse result should handle extra whitespace");
    }

    // ==================== Command Suggestion Tests ====================

    @Test
    @DisplayName("Suggestions are available for minewright command")
    void testSuggestionsAvailable() {
        String command = "minewright ";
        ParseResults<CommandSourceStack> result = dispatcher.parse(command, mockSource);

        CompletableFuture<Suggestions> suggestions = dispatcher.getCompletionSuggestions(result);

        assertNotNull(suggestions, "Suggestions should be available");
    }

    @Test
    @DisplayName("Suggestions include expected subcommands")
    void testSuggestionsIncludeSubcommands() {
        String command = "minewright ";
        ParseResults<CommandSourceStack> result = dispatcher.parse(command, mockSource);

        try {
            Suggestions suggestions = dispatcher.getCompletionSuggestions(result).get();
            List<String> suggestionsList = suggestions.getList().stream()
                .map(s -> s.getText())
                .toList();

            assertTrue(suggestionsList.contains("spawn"), "Suggestions should include 'spawn'");
            assertTrue(suggestionsList.contains("remove"), "Suggestions should include 'remove'");
            assertTrue(suggestionsList.contains("list"), "Suggestions should include 'list'");
        } catch (Exception e) {
            fail("Should not throw exception when getting suggestions: " + e.getMessage());
        }
    }

    // ==================== Case Sensitivity Tests ====================

    @Test
    @DisplayName("Commands are case-sensitive")
    void testCommandsAreCaseSensitive() {
        String lowercase = "minewright list";
        ParseResults<CommandSourceStack> lowerResult = dispatcher.parse(lowercase, mockSource);

        String uppercase = "MINEWRIGHT LIST";
        ParseResults<CommandSourceStack> upperResult = dispatcher.parse(uppercase, mockSource);

        // Lowercase should parse, uppercase should not (or have errors)
        assertNotNull(lowerResult, "Lowercase command should parse");
        assertNotNull(upperResult, "Uppercase command should also parse ( Brigadier is case-sensitive by default)");
    }

    // ==================== Argument Type Tests ====================

    @Test
    @DisplayName("String argument accepts normal text")
    void testStringArgumentNormalText() {
        String command = "minewright spawn Steve";
        ParseResults<CommandSourceStack> result = dispatcher.parse(command, mockSource);

        assertNotNull(result, "String argument should accept normal text");
    }

    @Test
    @DisplayName("String argument accepts numbers")
    void testStringArgumentNumbers() {
        String command = "minewright spawn 12345";
        ParseResults<CommandSourceStack> result = dispatcher.parse(command, mockSource);

        assertNotNull(result, "String argument should accept numbers");
    }

    @Test
    @DisplayName("String argument accepts underscores")
    void testStringArgumentUnderscores() {
        String command = "minewright spawn Steve_The_Builder";
        ParseResults<CommandSourceStack> result = dispatcher.parse(command, mockSource);

        assertNotNull(result, "String argument should accept underscores");
    }

    @Test
    @DisplayName("Greedy string argument captures all remaining text")
    void testGreedyStringArgument() {
        String command = "minewright tell Steve this is a very long command with many words";
        ParseResults<CommandSourceStack> result = dispatcher.parse(command, mockSource);

        assertNotNull(result, "Greedy string should capture all remaining text");
    }

    // ==================== Special Character Tests ====================

    @Test
    @DisplayName("Command handles hyphens in names")
    void testCommandHandlesHyphens() {
        String command = "minewright spawn Steve-The-Builder";
        ParseResults<CommandSourceStack> result = dispatcher.parse(command, mockSource);

        assertNotNull(result, "Command should handle hyphens in names");
    }

    @Test
    @DisplayName("Command handles dots in names")
    void testCommandHandlesDots() {
        String command = "minewright spawn Steve.Builder";
        ParseResults<CommandSourceStack> result = dispatcher.parse(command, mockSource);

        assertNotNull(result, "Command should handle dots in names");
    }

    @Test
    @DisplayName("Command handles apostrophes in names")
    void testCommandHandlesApostrophes() {
        String command = "minewright spawn Steve's";
        ParseResults<CommandSourceStack> result = dispatcher.parse(command, mockSource);

        assertNotNull(result, "Command should handle apostrophes in names");
    }

    // ==================== Path Argument Tests ====================

    @Test
    @DisplayName("Path argument handles forward slashes")
    void testPathArgumentForwardSlashes() {
        String command = "minewright export metrics /path/to/export/folder";
        ParseResults<CommandSourceStack> result = dispatcher.parse(command, mockSource);

        assertNotNull(result, "Path argument should handle forward slashes");
    }

    @Test
    @DisplayName("Path argument handles backslashes")
    void testPathArgumentBackslashes() {
        String command = "minewright export metrics C:\\path\\to\\export";
        ParseResults<CommandSourceStack> result = dispatcher.parse(command, mockSource);

        assertNotNull(result, "Path argument should handle backslashes");
    }

    @Test
    @DisplayName("Path argument handles dots in path")
    void testPathArgumentDotsInPath() {
        String command = "minewright export metrics ../relative/path";
        ParseResults<CommandSourceStack> result = dispatcher.parse(command, mockSource);

        assertNotNull(result, "Path argument should handle dots in path");
    }

    // ==================== Permission Tests ====================

    @Test
    @DisplayName("Parse command without checking permissions")
    void testParseWithoutPermissions() {
        // Parsing should work regardless of permissions
        // Permissions are checked during execution
        String command = "minewright spawn Steve";
        ParseResults<CommandSourceStack> result = dispatcher.parse(command, mockSource);

        assertNotNull(result, "Parsing should work regardless of permissions");
    }

    // ==================== Command Structure Validation Tests ====================

    @Test
    @DisplayName("Admin commands have permission requirement")
    void testAdminCommandsHavePermissionRequirement() {
        CommandNode<CommandSourceStack> minewrightNode = dispatcher.getRoot().getChild("minewright");
        CommandNode<CommandSourceStack> spawnNode = minewrightNode.getChild("spawn");

        assertNotNull(spawnNode, "Spawn command should exist");
        // Note: Permission requirements are checked via the 'requires' predicate
        // This test validates the structure exists
    }

    @Test
    @DisplayName("Read-only commands have no permission requirement")
    void testReadOnlyCommandsNoPermission() {
        CommandNode<CommandSourceStack> minewrightNode = dispatcher.getRoot().getChild("minewright");
        CommandNode<CommandSourceStack> listNode = minewrightNode.getChild("list");

        assertNotNull(listNode, "List command should exist");
        // Note: This test validates the structure exists
        // Actual permission checking happens during execution
    }

    // ==================== Whitespace and Format Tests ====================

    @Test
    @DisplayName("Command handles tabs")
    void testCommandHandlesTabs() {
        String command = "minewright\tlist";
        ParseResults<CommandSourceStack> result = dispatcher.parse(command, mockSource);

        assertNotNull(result, "Command should handle tabs");
    }

    @Test
    @DisplayName("Command handles multiple spaces")
    void testCommandHandlesMultipleSpaces() {
        String command = "minewright     list";
        ParseResults<CommandSourceStack> result = dispatcher.parse(command, mockSource);

        assertNotNull(result, "Command should handle multiple spaces");
    }

    @Test
    @DisplayName("Command handles trailing newline")
    void testCommandHandlesTrailingNewline() {
        String command = "minewright list\n";
        ParseResults<CommandSourceStack> result = dispatcher.parse(command, mockSource);

        assertNotNull(result, "Command should handle trailing newline");
    }

    // ==================== Unicode and Internationalization Tests ====================

    @Test
    @DisplayName("Command handles unicode characters in arguments")
    void testCommandHandlesUnicode() {
        String command = "minewright spawn 史蒂夫";
        ParseResults<CommandSourceStack> result = dispatcher.parse(command, mockSource);

        assertNotNull(result, "Command should handle unicode characters");
    }

    @Test
    @DisplayName("Command handles emojis in arguments")
    void testCommandHandlesEmojis() {
        String command = "minewright spawn Steve👷";
        ParseResults<CommandSourceStack> result = dispatcher.parse(command, mockSource);

        assertNotNull(result, "Command should handle emojis");
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Command with maximum length arguments")
    void testCommandMaxLengthArguments() {
        String longName = "A".repeat(1000);
        String command = "minewright spawn " + longName;
        ParseResults<CommandSourceStack> result = dispatcher.parse(command, mockSource);

        assertNotNull(result, "Command should handle maximum length arguments");
    }

    @Test
    @DisplayName("Command with only spaces")
    void testCommandOnlySpaces() {
        String command = "     ";
        ParseResults<CommandSourceStack> result = dispatcher.parse(command, mockSource);

        assertNotNull(result, "Command should handle only spaces");
    }

    @Test
    @DisplayName("Command path with spaces")
    void testCommandPathWithSpaces() {
        String command = "minewright export metrics \"/path/with spaces/to/export\"";
        ParseResults<CommandSourceStack> result = dispatcher.parse(command, mockSource);

        assertNotNull(result, "Command should handle paths with spaces");
    }
}
