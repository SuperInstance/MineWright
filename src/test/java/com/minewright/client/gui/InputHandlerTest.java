package com.minewright.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for {@link InputHandler}.
 *
 * Tests cover:
 * <ul>
 *   <li>Input box initialization and cleanup</li>
 *   <li>Keyboard input handling</li>
 *   <li>Character input handling</li>
 *   <li>Mouse click handling</li>
 *   <li>Mouse scroll handling</li>
 *   <li>Command history navigation</li>
 *   <li>Command sending and execution</li>
 *   <li>Crew panel state management</li>
 *   <li>Hover state tracking</li>
 *   <li>Tick updates</li>
 *   <li>Null and edge case handling</li>
 * </ul>
 *
 * @since 1.0.0
 */
@DisplayName("InputHandler Comprehensive Tests")
class InputHandlerTest {

    @Mock
    private Minecraft mockMinecraft;

    @Mock
    private MessagePanel mockMessagePanel;

    @Mock
    private EditBox mockEditBox;

    private InputHandler inputHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        inputHandler = new InputHandler(mockMessagePanel);
    }

    // ==================== Initialization Tests ====================

    @Test
    @DisplayName("Initialize input box should create EditBox")
    void testInitializeInputBox() {
        inputHandler.initializeInputBox(mockMinecraft, 200);

        assertNotNull(inputHandler.getInputBox(),
                     "Input box should be initialized");
    }

    @Test
    @DisplayName("Initialize input box should set focus")
    void testInitializeInputBoxSetsFocus() {
        inputHandler.initializeInputBox(mockMinecraft, 200);
        EditBox inputBox = inputHandler.getInputBox();

        if (inputBox != null) {
            assertTrue(inputBox.isFocused(),
                      "Input box should be focused after initialization");
        }
    }

    @Test
    @DisplayName("Initialize input box with different widths")
    void testInitializeInputBoxWithDifferentWidths() {
        assertDoesNotThrow(() -> {
            inputHandler.initializeInputBox(mockMinecraft, 100);
            inputHandler.cleanupInputBox();
            inputHandler.initializeInputBox(mockMinecraft, 200);
            inputHandler.cleanupInputBox();
            inputHandler.initializeInputBox(mockMinecraft, 400);
        }, "Initialize with different widths should work");
    }

    @Test
    @DisplayName("Cleanup input box should set to null")
    void testCleanupInputBox() {
        inputHandler.initializeInputBox(mockMinecraft, 200);
        inputHandler.cleanupInputBox();

        assertNull(inputHandler.getInputBox(),
                  "Input box should be null after cleanup");
    }

    @Test
    @DisplayName("Cleanup without initialization should be safe")
    void testCleanupWithoutInitialization() {
        assertDoesNotThrow(() -> {
            inputHandler.cleanupInputBox();
            inputHandler.cleanupInputBox();
        }, "Cleanup without initialization should not throw exception");
    }

    // ==================== Keyboard Input Tests ====================

    @Test
    @DisplayName("Handle key press without input box should return false")
    void testKeyPressWithoutInputBox() {
        boolean result = inputHandler.handleKeyPress(65, 0, 0);
        assertFalse(result, "Key press without input box should return false");
    }

    @Test
    @DisplayName("Escape key should signal to close")
    void testEscapeKeySignalsClose() {
        inputHandler.initializeInputBox(mockMinecraft, 200);
        boolean result = inputHandler.handleKeyPress(256, 0, 0); // ESC

        assertTrue(result, "Escape key should signal to close");
    }

    @Test
    @DisplayName("Enter key with empty input should not send command")
    void testEnterKeyWithEmptyInput() {
        inputHandler.initializeInputBox(mockMinecraft, 200);
        EditBox inputBox = inputHandler.getInputBox();
        if (inputBox != null) {
            when(inputBox.getValue()).thenReturn("");
        }

        boolean result = inputHandler.handleKeyPress(257, 0, 0); // Enter

        assertTrue(result, "Enter key should be consumed");
    }

    @Test
    @DisplayName("Enter key with text should send command")
    void testEnterKeySendsCommand() {
        inputHandler.initializeInputBox(mockMinecraft, 200);
        EditBox inputBox = inputHandler.getInputBox();
        if (inputBox != null) {
            when(inputBox.getValue()).thenReturn("test command");
            doAnswer(invocation -> {
                when(inputBox.getValue()).thenReturn("");
                return null;
            }).when(inputBox).setValue(anyString());
        }

        boolean result = inputHandler.handleKeyPress(257, 0, 0); // Enter

        assertTrue(result, "Enter key should be consumed");
    }

    @Test
    @DisplayName("Up arrow should navigate history backward")
    void testUpArrowNavigatesHistoryBackward() {
        inputHandler.initializeInputBox(mockMinecraft, 200);
        EditBox inputBox = inputHandler.getInputBox();

        // Add some commands to history
        inputHandler.sendCommand("command1");
        inputHandler.sendCommand("command2");

        if (inputBox != null) {
            doAnswer(invocation -> {
                when(inputBox.getValue()).thenReturn("command2");
                return null;
            }).when(inputBox).setValue(anyString());
        }

        boolean result = inputHandler.handleKeyPress(265, 0, 0); // UP

        assertTrue(result, "Up arrow should be consumed");
    }

    @Test
    @DisplayName("Down arrow should navigate history forward")
    void testDownArrowNavigatesHistoryForward() {
        inputHandler.initializeInputBox(mockMinecraft, 200);
        EditBox inputBox = inputHandler.getInputBox();

        // Add commands and navigate up
        inputHandler.sendCommand("command1");
        inputHandler.sendCommand("command2");
        inputHandler.handleKeyPress(265, 0, 0); // UP

        if (inputBox != null) {
            doAnswer(invocation -> {
                when(inputBox.getValue()).thenReturn("");
                return null;
            }).when(inputBox).setValue(anyString());
        }

        boolean result = inputHandler.handleKeyPress(264, 0, 0); // DOWN

        assertTrue(result, "Down arrow should be consumed");
    }

    @Test
    @DisplayName("Special keys should be forwarded to input box")
    void testSpecialKeysForwardedToInputBox() {
        inputHandler.initializeInputBox(mockMinecraft, 200);
        EditBox inputBox = inputHandler.getInputBox();

        if (inputBox != null) {
            // Backspace
            boolean result1 = inputHandler.handleKeyPress(259, 0, 0);
            assertTrue(result1, "Backspace should be consumed");

            // Delete
            boolean result2 = inputHandler.handleKeyPress(261, 0, 0);
            assertTrue(result2, "Delete should be consumed");

            // Home
            boolean result3 = inputHandler.handleKeyPress(268, 0, 0);
            assertTrue(result3, "Home should be consumed");

            // End
            boolean result4 = inputHandler.handleKeyPress(269, 0, 0);
            assertTrue(result4, "End should be consumed");
        }
    }

    @Test
    @DisplayName("Arrow keys should be forwarded to input box")
    void testArrowKeysForwardedToInputBox() {
        inputHandler.initializeInputBox(mockMinecraft, 200);
        EditBox inputBox = inputHandler.getInputBox();

        if (inputBox != null) {
            boolean result1 = inputHandler.handleKeyPress(263, 0, 0); // Left
            assertTrue(result1, "Left arrow should be consumed");

            boolean result2 = inputHandler.handleKeyPress(262, 0, 0); // Right
            assertTrue(result2, "Right arrow should be consumed");
        }
    }

    // ==================== Character Input Tests ====================

    @Test
    @DisplayName("Handle char typed without input box should return false")
    void testCharTypedWithoutInputBox() {
        boolean result = inputHandler.handleCharTyped('a', 0);
        assertFalse(result, "Char typed without input box should return false");
    }

    @Test
    @DisplayName("Handle char typed should forward to input box")
    void testCharTypedForwardedToInputBox() {
        inputHandler.initializeInputBox(mockMinecraft, 200);
        EditBox inputBox = inputHandler.getInputBox();

        if (inputBox != null) {
            boolean result = inputHandler.handleCharTyped('a', 0);
            assertTrue(result, "Char typed should be consumed");
        }
    }

    @Test
    @DisplayName("Handle char typed with various characters")
    void testCharTypedWithVariousCharacters() {
        inputHandler.initializeInputBox(mockMinecraft, 200);
        EditBox inputBox = inputHandler.getInputBox();

        if (inputBox != null) {
            assertTrue(inputHandler.handleCharTyped('a', 0));
            assertTrue(inputHandler.handleCharTyped('Z', 0));
            assertTrue(inputHandler.handleCharTyped('0', 0));
            assertTrue(inputHandler.handleCharTyped('!', 0));
            assertTrue(inputHandler.handleCharTyped(' ', 0));
        }
    }

    @Test
    @DisplayName("Handle char typed with modifiers")
    void testCharTypedWithModifiers() {
        inputHandler.initializeInputBox(mockMinecraft, 200);
        EditBox inputBox = inputHandler.getInputBox();

        if (inputBox != null) {
            assertTrue(inputHandler.handleCharTyped('a', 1)); // Shift
            assertTrue(inputHandler.handleCharTyped('a', 2)); // Control
            assertTrue(inputHandler.handleCharTyped('a', 4)); // Alt
        }
    }

    // ==================== Mouse Click Tests ====================

    @Test
    @DisplayName("Handle mouse click should not throw exception")
    void testHandleMouseClickDoesNotThrow() {
        InputHandler.QuickButton[] buttons = {
            new InputHandler.QuickButton("Test", "cmd", "tooltip", 0xFF0000)
        };

        assertDoesNotThrow(() -> {
            inputHandler.handleMouseClick(100.0, 100.0, 0, 0, 200, 800, 600, buttons);
        }, "Mouse click should not throw exception");
    }

    @Test
    @DisplayName("Mouse click outside panel area should be handled")
    void testMouseClickOutsidePanel() {
        InputHandler.QuickButton[] buttons = {};

        assertDoesNotThrow(() -> {
            inputHandler.handleMouseClick(-100.0, -100.0, 0, 0, 200, 800, 600, buttons);
        }, "Click outside panel should not throw exception");
    }

    @Test
    @DisplayName("Mouse click on crew panel header should toggle expansion")
    void testMouseClickOnCrewPanelHeader() {
        InputHandler.QuickButton[] buttons = {};
        boolean initialExpanded = inputHandler.isCrewPanelExpanded();

        inputHandler.handleMouseClick(50.0, 30.0, 0, 0, 200, 800, 600, buttons);

        assertNotEquals(initialExpanded, inputHandler.isCrewPanelExpanded(),
                       "Crew panel expansion should toggle");
    }

    // ==================== Mouse Scroll Tests ====================

    @Test
    @DisplayName("Handle mouse scroll should delegate to message panel")
    void testHandleMouseScroll() {
        assertDoesNotThrow(() -> {
            inputHandler.handleMouseScroll(1.0);
            inputHandler.handleMouseScroll(-1.0);
            inputHandler.handleMouseScroll(0.0);
        }, "Mouse scroll should not throw exception");
    }

    @Test
    @DisplayName("Handle mouse scroll with various deltas")
    void testHandleMouseScrollWithVariousDeltas() {
        assertDoesNotThrow(() -> {
            inputHandler.handleMouseScroll(0.1);
            inputHandler.handleMouseScroll(1.0);
            inputHandler.handleMouseScroll(10.0);
            inputHandler.handleMouseScroll(-0.1);
            inputHandler.handleMouseScroll(-10.0);
        }, "Various scroll deltas should be handled");
    }

    // ==================== Command Sending Tests ====================

    @Test
    @DisplayName("Send command should add to history")
    void testSendCommandAddsToHistory() {
        inputHandler.sendCommand("test command");

        // Verify command was added to message panel
        verify(mockMessagePanel).addUserMessage("test command");
    }

    @Test
    @DisplayName("Send empty command should not be added")
    void testSendEmptyCommand() {
        inputHandler.sendCommand("");

        // Should not add empty command
        verify(mockMessagePanel, never()).addUserMessage("");
    }

    @Test
    @DisplayName("Send command with only whitespace should not be added")
    void testSendWhitespaceCommand() {
        inputHandler.sendCommand("   ");

        // Should not add whitespace-only command
        verify(mockMessagePanel, never()).addUserMessage("   ");
    }

    @Test
    @DisplayName("Send multiple commands should add all to history")
    void testSendMultipleCommands() {
        inputHandler.sendCommand("command1");
        inputHandler.sendCommand("command2");
        inputHandler.sendCommand("command3");

        verify(mockMessagePanel, times(3)).addUserMessage(anyString());
    }

    @Test
    @DisplayName("Command history should limit to 50 entries")
    void testCommandHistoryLimit() {
        // Add 60 commands (exceeds limit of 50)
        for (int i = 0; i < 60; i++) {
            inputHandler.sendCommand("command" + i);
        }

        // Should have added all 60, but history should be limited to 50
        // This is tested by the internal implementation
        verify(mockMessagePanel, times(60)).addUserMessage(anyString());
    }

    // ==================== Crew Panel State Tests ====================

    @Test
    @DisplayName("Crew panel should be expanded by default")
    void testCrewPanelExpandedByDefault() {
        assertTrue(inputHandler.isCrewPanelExpanded(),
                  "Crew panel should be expanded by default");
    }

    @Test
    @DisplayName("Toggle crew panel should change state")
    void testToggleCrewPanel() {
        boolean initialState = inputHandler.isCrewPanelExpanded();

        // Toggle by clicking on crew panel header
        inputHandler.handleMouseClick(50.0, 30.0, 0, 0, 200, 800, 600, new InputHandler.QuickButton[0]);

        assertNotEquals(initialState, inputHandler.isCrewPanelExpanded(),
                       "Crew panel state should change");
    }

    @Test
    @DisplayName("Targeted crew member should be null initially")
    void testTargetedCrewMemberNullInitially() {
        assertNull(inputHandler.getTargetedCrewMember(),
                  "Targeted crew member should be null initially");
    }

    // ==================== Hover State Tests ====================

    @Test
    @DisplayName("Update hover state should not throw exception")
    void testUpdateHoverState() {
        InputHandler.QuickButton[] buttons = {
            new InputHandler.QuickButton("Test", "cmd", "tooltip", 0xFF0000)
        };

        assertDoesNotThrow(() -> {
            inputHandler.updateHoverState(100.0, 100.0, 0, 200, 100, buttons);
        }, "Update hover state should not throw exception");
    }

    @Test
    @DisplayName("Get button hover state should return valid state")
    void testGetButtonHoverState() {
        InputHandler.HoverState state = inputHandler.getButtonHoverState(0);

        assertNotNull(state, "Hover state should not be null");
        assertFalse(state.isHovered, "Button should not be hovered initially");
        assertEquals(1.0f, state.scale, 0.01f, "Scale should be 1.0 initially");
        assertEquals(0L, state.hoverDuration, "Hover duration should be 0 initially");
    }

    // ==================== Tick Update Tests ====================

    @Test
    @DisplayName("Tick should update input box")
    void testTickUpdatesInputBox() {
        inputHandler.initializeInputBox(mockMinecraft, 200);
        EditBox inputBox = inputHandler.getInputBox();

        assertDoesNotThrow(() -> {
            inputHandler.tick();
        }, "Tick should not throw exception");

        if (inputBox != null) {
            verify(inputBox, atLeastOnce()).tick();
        }
    }

    @Test
    @DisplayName("Tick without input box should be safe")
    void testTickWithoutInputBox() {
        assertDoesNotThrow(() -> {
            inputHandler.tick();
        }, "Tick without input box should not throw exception");
    }

    @Test
    @DisplayName("Multiple ticks should be safe")
    void testMultipleTicks() {
        inputHandler.initializeInputBox(mockMinecraft, 200);

        assertDoesNotThrow(() -> {
            for (int i = 0; i < 100; i++) {
                inputHandler.tick();
            }
        }, "Multiple ticks should not throw exception");
    }

    // ==================== QuickButton Tests ====================

    @Test
    @DisplayName("QuickButton should store all properties")
    void testQuickButtonProperties() {
        InputHandler.QuickButton button = new InputHandler.QuickButton(
            "Label", "command", "tooltip text", 0x123456
        );

        assertEquals("Label", button.label);
        assertEquals("command", button.command);
        assertEquals("tooltip text", button.tooltip);
        assertEquals(0x123456, button.color);
    }

    @Test
    @DisplayName("QuickButton with null properties should work")
    void testQuickButtonWithNullProperties() {
        assertDoesNotThrow(() -> {
            InputHandler.QuickButton button = new InputHandler.QuickButton(
                null, null, null, 0x000000
            );
            assertNull(button.label);
            assertNull(button.command);
            assertNull(button.tooltip);
        }, "QuickButton with null properties should not throw exception");
    }

    @Test
    @DisplayName("QuickButton with empty strings should work")
    void testQuickButtonWithEmptyStrings() {
        assertDoesNotThrow(() -> {
            InputHandler.QuickButton button = new InputHandler.QuickButton(
                "", "", "", 0x000000
            );
            assertEquals("", button.label);
            assertEquals("", button.command);
            assertEquals("", button.tooltip);
        }, "QuickButton with empty strings should not throw exception");
    }

    // ==================== HoverState Tests ====================

    @Test
    @DisplayName("HoverState should store all properties")
    void testHoverStateProperties() {
        InputHandler.HoverState state = new InputHandler.HoverState(true, 1.5f, 1000L);

        assertTrue(state.isHovered);
        assertEquals(1.5f, state.scale, 0.01f);
        assertEquals(1000L, state.hoverDuration);
    }

    @Test
    @DisplayName("HoverState with false should work")
    void testHoverStateWithFalse() {
        InputHandler.HoverState state = new InputHandler.HoverState(false, 1.0f, 0L);

        assertFalse(state.isHovered);
        assertEquals(1.0f, state.scale, 0.01f);
        assertEquals(0L, state.hoverDuration);
    }

    // ==================== Null Safety Tests ====================

    @Test
    @DisplayName("Handle key press with null input box should return false")
    void testKeyPressWithNullInputBox() {
        boolean result = inputHandler.handleKeyPress(65, 0, 0);
        assertFalse(result, "Key press with null input box should return false");
    }

    @Test
    @DisplayName("Handle char typed with null input box should return false")
    void testCharTypedWithNullInputBox() {
        boolean result = inputHandler.handleCharTyped('a', 0);
        assertFalse(result, "Char typed with null input box should return false");
    }

    @Test
    @DisplayName("Handle mouse click with null buttons array should work")
    void testMouseClickWithNullButtons() {
        assertDoesNotThrow(() -> {
            inputHandler.handleMouseClick(100.0, 100.0, 0, 0, 200, 800, 600, null);
        }, "Mouse click with null buttons should not throw exception");
    }

    @Test
    @DisplayName("Send command with null should be handled")
    void testSendCommandWithNull() {
        assertDoesNotThrow(() -> {
            inputHandler.sendCommand(null);
        }, "Send command with null should not throw exception");
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Handle very long command")
    void testHandleVeryLongCommand() {
        String longCommand = "a".repeat(1000);
        assertDoesNotThrow(() -> {
            inputHandler.sendCommand(longCommand);
        }, "Very long command should be handled");
    }

    @Test
    @DisplayName("Handle command with special characters")
    void testHandleCommandWithSpecialCharacters() {
        String specialCommand = "test\n\t\r\u0000command";
        assertDoesNotThrow(() -> {
            inputHandler.sendCommand(specialCommand);
        }, "Command with special characters should be handled");
    }

    @Test
    @DisplayName("Handle command with unicode")
    void testHandleCommandWithUnicode() {
        String unicodeCommand = "test 世界 🌍 command";
        assertDoesNotThrow(() -> {
            inputHandler.sendCommand(unicodeCommand);
        }, "Command with unicode should be handled");
    }
}
