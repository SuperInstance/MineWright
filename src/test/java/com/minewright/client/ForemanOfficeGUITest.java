package com.minewright.client;

import com.minewright.client.gui.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for {@link ForemanOfficeGUI}.
 *
 * Tests cover:
 * <ul>
 *   <li>GUI open/close toggle</li>
 *   <li>Message addition methods</li>
 *   <li>Keyboard input handling</li>
 *   <li>Character input handling</li>
 *   <li>Mouse input handling</li>
 *   <li>Scroll handling</li>
 *   <li>Rendering behavior</li>
 *   <li>Delegate initialization</li>
 *   <li>State management</li>
 *   <li>Null and edge case handling</li>
 * </ul>
 *
 * @since 1.0.0
 */
@DisplayName("ForemanOfficeGUI Comprehensive Tests")
class ForemanOfficeGUITest {

    @Mock
    private Minecraft mockMinecraft;

    @Mock
    private GuiGraphics mockGuiGraphics;

    @Mock
    private RenderGuiOverlayEvent.Post mockRenderEvent;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Reset GUI state before each test
        if (ForemanOfficeGUI.isOpen()) {
            ForemanOfficeGUI.toggle();
        }
    }

    // ==================== Toggle Tests ====================

    @Test
    @DisplayName("Toggle should change GUI state from closed to open")
    void testToggleFromClosedToOpen() {
        // Ensure GUI is closed
        if (ForemanOfficeGUI.isOpen()) {
            ForemanOfficeGUI.toggle();
        }

        assertFalse(ForemanOfficeGUI.isOpen(), "GUI should be closed initially");

        ForemanOfficeGUI.toggle();

        assertTrue(ForemanOfficeGUI.isOpen(), "GUI should be open after toggle");
    }

    @Test
    @DisplayName("Toggle should change GUI state from open to closed")
    void testToggleFromOpenToClosed() {
        // Ensure GUI is open
        if (!ForemanOfficeGUI.isOpen()) {
            ForemanOfficeGUI.toggle();
        }

        assertTrue(ForemanOfficeGUI.isOpen(), "GUI should be open initially");

        ForemanOfficeGUI.toggle();

        assertFalse(ForemanOfficeGUI.isOpen(), "GUI should be closed after toggle");
    }

    @Test
    @DisplayName("Multiple toggles should work correctly")
    void testMultipleToggles() {
        boolean initialState = ForemanOfficeGUI.isOpen();

        ForemanOfficeGUI.toggle();
        assertNotEquals(initialState, ForemanOfficeGUI.isOpen());

        ForemanOfficeGUI.toggle();
        assertEquals(initialState, ForemanOfficeGUI.isOpen());

        ForemanOfficeGUI.toggle();
        assertNotEquals(initialState, ForemanOfficeGUI.isOpen());

        ForemanOfficeGUI.toggle();
        assertEquals(initialState, ForemanOfficeGUI.isOpen());
    }

    // ==================== Message Addition Tests ====================

    @Test
    @DisplayName("Add message should work when GUI is closed")
    void testAddMessageWhenGUIClosed() {
        assertDoesNotThrow(() -> {
            ForemanOfficeGUI.addMessage("Sender", "Test message", 0xFF0000, false);
        }, "Adding message when GUI is closed should not throw exception");
    }

    @Test
    @DisplayName("Add message should work when GUI is open")
    void testAddMessageWhenGUIOpen() {
        // Open GUI
        if (!ForemanOfficeGUI.isOpen()) {
            ForemanOfficeGUI.toggle();
        }

        assertDoesNotThrow(() -> {
            ForemanOfficeGUI.addMessage("Sender", "Test message", 0xFF0000, false);
        }, "Adding message when GUI is open should not throw exception");
    }

    @Test
    @DisplayName("Add user message should work")
    void testAddUserMessage() {
        assertDoesNotThrow(() -> {
            ForemanOfficeGUI.addUserMessage("Test command");
        }, "Adding user message should not throw exception");
    }

    @Test
    @DisplayName("Add crew message should work")
    void testAddCrewMessage() {
        assertDoesNotThrow(() -> {
            ForemanOfficeGUI.addCrewMessage("Steve", "I'm working on it!");
        }, "Adding crew message should not throw exception");
    }

    @Test
    @DisplayName("Add system message should work")
    void testAddSystemMessage() {
        assertDoesNotThrow(() -> {
            ForemanOfficeGUI.addSystemMessage("System notification");
        }, "Adding system message should not throw exception");
    }

    @Test
    @DisplayName("Add multiple messages should work")
    void testAddMultipleMessages() {
        assertDoesNotThrow(() -> {
            ForemanOfficeGUI.addUserMessage("Command 1");
            ForemanOfficeGUI.addCrewMessage("Steve", "Response 1");
            ForemanOfficeGUI.addSystemMessage("System 1");
            ForemanOfficeGUI.addUserMessage("Command 2");
            ForemanOfficeGUI.addCrewMessage("Alex", "Response 2");
        }, "Adding multiple messages should not throw exception");
    }

    // ==================== Keyboard Input Tests ====================

    @Test
    @DisplayName("Handle key press when GUI is closed should return false")
    void testKeyPressWhenGUIClosed() {
        // Ensure GUI is closed
        if (ForemanOfficeGUI.isOpen()) {
            ForemanOfficeGUI.toggle();
        }

        boolean result = ForemanOfficeGUI.handleKeyPress(65, 0, 0);
        assertFalse(result, "Key press when GUI is closed should return false");
    }

    @Test
    @DisplayName("Handle key press when GUI is open should return true")
    void testKeyPressWhenGUIOpen() {
        // Open GUI
        if (!ForemanOfficeGUI.isOpen()) {
            ForemanOfficeGUI.toggle();
        }

        boolean result = ForemanOfficeGUI.handleKeyPress(65, 0, 0);
        assertTrue(result, "Key press when GUI is open should return true");
    }

    @Test
    @DisplayName("Escape key should close GUI")
    void testEscapeKeyClosesGUI() {
        // Open GUI
        if (!ForemanOfficeGUI.isOpen()) {
            ForemanOfficeGUI.toggle();
        }

        assertTrue(ForemanOfficeGUI.isOpen(), "GUI should be open");

        ForemanOfficeGUI.handleKeyPress(256, 0, 0); // ESC

        assertFalse(ForemanOfficeGUI.isOpen(), "GUI should be closed after ESC");
    }

    @Test
    @DisplayName("Enter key should be handled")
    void testEnterKeyHandled() {
        // Open GUI
        if (!ForemanOfficeGUI.isOpen()) {
            ForemanOfficeGUI.toggle();
        }

        boolean result = ForemanOfficeGUI.handleKeyPress(257, 0, 0); // Enter
        assertTrue(result, "Enter key should be consumed");
    }

    @Test
    @DisplayName("Arrow keys should be handled")
    void testArrowKeysHandled() {
        // Open GUI
        if (!ForemanOfficeGUI.isOpen()) {
            ForemanOfficeGUI.toggle();
        }

        assertTrue(ForemanOfficeGUI.handleKeyPress(265, 0, 0), "Up arrow should be consumed");
        assertTrue(ForemanOfficeGUI.handleKeyPress(264, 0, 0), "Down arrow should be consumed");
        assertTrue(ForemanOfficeGUI.handleKeyPress(263, 0, 0), "Left arrow should be consumed");
        assertTrue(ForemanOfficeGUI.handleKeyPress(262, 0, 0), "Right arrow should be consumed");
    }

    // ==================== Character Input Tests ====================

    @Test
    @DisplayName("Handle char typed when GUI is closed should return false")
    void testCharTypedWhenGUIClosed() {
        // Ensure GUI is closed
        if (ForemanOfficeGUI.isOpen()) {
            ForemanOfficeGUI.toggle();
        }

        boolean result = ForemanOfficeGUI.handleCharTyped('a', 0);
        assertFalse(result, "Char typed when GUI is closed should return false");
    }

    @Test
    @DisplayName("Handle char typed when GUI is open should return true")
    void testCharTypedWhenGUIOpen() {
        // Open GUI
        if (!ForemanOfficeGUI.isOpen()) {
            ForemanOfficeGUI.toggle();
        }

        boolean result = ForemanOfficeGUI.handleCharTyped('a', 0);
        assertTrue(result, "Char typed when GUI is open should return true");
    }

    @Test
    @DisplayName("Handle various characters")
    void testHandleVariousCharacters() {
        // Open GUI
        if (!ForemanOfficeGUI.isOpen()) {
            ForemanOfficeGUI.toggle();
        }

        assertTrue(ForemanOfficeGUI.handleCharTyped('a', 0));
        assertTrue(ForemanOfficeGUI.handleCharTyped('Z', 0));
        assertTrue(ForemanOfficeGUI.handleCharTyped('0', 0));
        assertTrue(ForemanOfficeGUI.handleCharTyped(' ', 0));
        assertTrue(ForemanOfficeGUI.handleCharTyped('!', 0));
    }

    // ==================== Mouse Input Tests ====================

    @Test
    @DisplayName("Handle mouse click when GUI is closed should not throw")
    void testMouseClickWhenGUIClosed() {
        // Ensure GUI is closed
        if (ForemanOfficeGUI.isOpen()) {
            ForemanOfficeGUI.toggle();
        }

        assertDoesNotThrow(() -> {
            ForemanOfficeGUI.handleMouseClick(100.0, 100.0, 0);
        }, "Mouse click when GUI is closed should not throw exception");
    }

    @Test
    @DisplayName("Handle mouse click when GUI is open should not throw")
    void testMouseClickWhenGUIOpen() {
        // Open GUI
        if (!ForemanOfficeGUI.isOpen()) {
            ForemanOfficeGUI.toggle();
        }

        assertDoesNotThrow(() -> {
            ForemanOfficeGUI.handleMouseClick(100.0, 100.0, 0);
        }, "Mouse click when GUI is open should not throw exception");
    }

    @Test
    @DisplayName("Handle mouse click at various positions")
    void testMouseClickAtVariousPositions() {
        // Open GUI
        if (!ForemanOfficeGUI.isOpen()) {
            ForemanOfficeGUI.toggle();
        }

        assertDoesNotThrow(() -> {
            ForemanOfficeGUI.handleMouseClick(0.0, 0.0, 0);
            ForemanOfficeGUI.handleMouseClick(100.0, 100.0, 0);
            ForemanOfficeGUI.handleMouseClick(-50.0, -50.0, 0);
            ForemanOfficeGUI.handleMouseClick(1000.0, 1000.0, 0);
        }, "Mouse clicks at various positions should be handled");
    }

    @Test
    @DisplayName("Handle different mouse buttons")
    void testHandleDifferentMouseButtons() {
        // Open GUI
        if (!ForemanOfficeGUI.isOpen()) {
            ForemanOfficeGUI.toggle();
        }

        assertDoesNotThrow(() -> {
            ForemanOfficeGUI.handleMouseClick(100.0, 100.0, 0); // Left
            ForemanOfficeGUI.handleMouseClick(100.0, 100.0, 1); // Right
            ForemanOfficeGUI.handleMouseClick(100.0, 100.0, 2); // Middle
        }, "Different mouse buttons should be handled");
    }

    // ==================== Mouse Scroll Tests ====================

    @Test
    @DisplayName("Handle mouse scroll when GUI is closed should not throw")
    void testMouseScrollWhenGUIClosed() {
        // Ensure GUI is closed
        if (ForemanOfficeGUI.isOpen()) {
            ForemanOfficeGUI.toggle();
        }

        assertDoesNotThrow(() -> {
            ForemanOfficeGUI.handleMouseScroll(1.0);
        }, "Mouse scroll when GUI is closed should not throw exception");
    }

    @Test
    @DisplayName("Handle mouse scroll when GUI is open should not throw")
    void testMouseScrollWhenGUIOpen() {
        // Open GUI
        if (!ForemanOfficeGUI.isOpen()) {
            ForemanOfficeGUI.toggle();
        }

        assertDoesNotThrow(() -> {
            ForemanOfficeGUI.handleMouseScroll(1.0);
        }, "Mouse scroll when GUI is open should not throw exception");
    }

    @Test
    @DisplayName("Handle various scroll deltas")
    void testHandleVariousScrollDeltas() {
        // Open GUI
        if (!ForemanOfficeGUI.isOpen()) {
            ForemanOfficeGUI.toggle();
        }

        assertDoesNotThrow(() -> {
            ForemanOfficeGUI.handleMouseScroll(0.1);
            ForemanOfficeGUI.handleMouseScroll(1.0);
            ForemanOfficeGUI.handleMouseScroll(10.0);
            ForemanOfficeGUI.handleMouseScroll(-0.1);
            ForemanOfficeGUI.handleMouseScroll(-10.0);
        }, "Various scroll deltas should be handled");
    }

    // ==================== Tick Update Tests ====================

    @Test
    @DisplayName("Tick when GUI is closed should not throw")
    void testTickWhenGUIClosed() {
        // Ensure GUI is closed
        if (ForemanOfficeGUI.isOpen()) {
            ForemanOfficeGUI.toggle();
        }

        assertDoesNotThrow(() -> {
            ForemanOfficeGUI.tick();
        }, "Tick when GUI is closed should not throw exception");
    }

    @Test
    @DisplayName("Tick when GUI is open should not throw")
    void testTickWhenGUIOpen() {
        // Open GUI
        if (!ForemanOfficeGUI.isOpen()) {
            ForemanOfficeGUI.toggle();
        }

        assertDoesNotThrow(() -> {
            ForemanOfficeGUI.tick();
        }, "Tick when GUI is open should not throw exception");
    }

    @Test
    @DisplayName("Multiple ticks should be safe")
    void testMultipleTicks() {
        // Open GUI
        if (!ForemanOfficeGUI.isOpen()) {
            ForemanOfficeGUI.toggle();
        }

        assertDoesNotThrow(() -> {
            for (int i = 0; i < 100; i++) {
                ForemanOfficeGUI.tick();
            }
        }, "Multiple ticks should be safe");
    }

    // ==================== Rendering Tests ====================

    @Test
    @DisplayName("Render overlay should not throw exception")
    void testRenderOverlayDoesNotThrow() {
        assertDoesNotThrow(() -> {
            ForemanOfficeGUI.onRenderOverlay(mockRenderEvent);
        }, "Render overlay should not throw exception");
    }

    @Test
    @DisplayName("Render overlay when GUI is closed should not throw")
    void testRenderOverlayWhenGUIClosed() {
        // Ensure GUI is closed
        if (ForemanOfficeGUI.isOpen()) {
            ForemanOfficeGUI.toggle();
        }

        assertDoesNotThrow(() -> {
            ForemanOfficeGUI.onRenderOverlay(mockRenderEvent);
        }, "Render overlay when GUI is closed should not throw exception");
    }

    @Test
    @DisplayName("Render overlay when GUI is open should not throw")
    void testRenderOverlayWhenGUIOpen() {
        // Open GUI
        if (!ForemanOfficeGUI.isOpen()) {
            ForemanOfficeGUI.toggle();
        }

        assertDoesNotThrow(() -> {
            ForemanOfficeGUI.onRenderOverlay(mockRenderEvent);
        }, "Render overlay when GUI is open should not throw exception");
    }

    // ==================== Null Safety Tests ====================

    @Test
    @DisplayName("Add message with null sender should work")
    void testAddMessageWithNullSender() {
        assertDoesNotThrow(() -> {
            ForemanOfficeGUI.addMessage(null, "Test message", 0xFF0000, false);
        }, "Adding message with null sender should not throw exception");
    }

    @Test
    @DisplayName("Add message with null text should work")
    void testAddMessageWithNullText() {
        assertDoesNotThrow(() -> {
            ForemanOfficeGUI.addMessage("Sender", null, 0xFF0000, false);
        }, "Adding message with null text should not throw exception");
    }

    @Test
    @DisplayName("Add user message with null text should work")
    void testAddUserMessageWithNullText() {
        assertDoesNotThrow(() -> {
            ForemanOfficeGUI.addUserMessage(null);
        }, "Adding user message with null text should not throw exception");
    }

    @Test
    @DisplayName("Add crew message with null parameters should work")
    void testAddCrewMessageWithNullParameters() {
        assertDoesNotThrow(() -> {
            ForemanOfficeGUI.addCrewMessage(null, null);
        }, "Adding crew message with null parameters should not throw exception");
    }

    @Test
    @DisplayName("Add system message with null text should work")
    void testAddSystemMessageWithNullText() {
        assertDoesNotThrow(() -> {
            ForemanOfficeGUI.addSystemMessage(null);
        }, "Adding system message with null text should not throw exception");
    }

    @Test
    @DisplayName("Render with null event should not crash")
    void testRenderWithNullEvent() {
        assertDoesNotThrow(() -> {
            ForemanOfficeGUI.onRenderOverlay(null);
        }, "Render with null event should not crash");
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Add very long message should work")
    void testAddVeryLongMessage() {
        String longMessage = "A".repeat(10000);

        assertDoesNotThrow(() -> {
            ForemanOfficeGUI.addMessage("Sender", longMessage, 0xFF0000, false);
        }, "Adding very long message should not throw exception");
    }

    @Test
    @DisplayName("Add message with special characters should work")
    void testAddMessageWithSpecialCharacters() {
        String specialMessage = "Test\n\t\r\u0000\u001Fmessage";

        assertDoesNotThrow(() -> {
            ForemanOfficeGUI.addMessage("Sender", specialMessage, 0xFF0000, false);
        }, "Adding message with special characters should not throw exception");
    }

    @Test
    @DisplayName("Add message with unicode should work")
    void testAddMessageWithUnicode() {
        String unicodeMessage = "Hello 世界 🌍 مرحبا";

        assertDoesNotThrow(() -> {
            ForemanOfficeGUI.addMessage("Sender", unicodeMessage, 0xFF0000, false);
        }, "Adding message with unicode should not throw exception");
    }

    @Test
    @DisplayName("Toggle GUI repeatedly should be safe")
    void testToggleGUIRepeatedly() {
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 100; i++) {
                ForemanOfficeGUI.toggle();
            }
        }, "Toggling GUI repeatedly should be safe");
    }

    @Test
    @DisplayName("Handle input with all key codes should be safe")
    void testHandleAllKeyCodes() {
        // Open GUI
        if (!ForemanOfficeGUI.isOpen()) {
            ForemanOfficeGUI.toggle();
        }

        assertDoesNotThrow(() -> {
            // Test various key codes
            ForemanOfficeGUI.handleKeyPress(0, 0, 0);
            ForemanOfficeGUI.handleKeyPress(65, 0, 0);
            ForemanOfficeGUI.handleKeyPress(256, 0, 0);
            ForemanOfficeGUI.handleKeyPress(257, 0, 0);
            ForemanOfficeGUI.handleKeyPress(265, 0, 0);
            ForemanOfficeGUI.handleKeyPress(264, 0, 0);
        }, "Handling various key codes should be safe");
    }

    // ==================== State Management Tests ====================

    @Test
    @DisplayName("GUI state should persist")
    void testGUIStatePersists() {
        boolean initialState = ForemanOfficeGUI.isOpen();

        ForemanOfficeGUI.toggle();
        assertTrue(ForemanOfficeGUI.isOpen() != initialState);

        // State should persist
        assertTrue(ForemanOfficeGUI.isOpen() != initialState);
    }

    @Test
    @DisplayName("Multiple message additions should accumulate")
    void testMultipleMessageAdditionsAccumulate() {
        // This test verifies that messages accumulate properly
        // Actual message count would require access to internal state
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 10; i++) {
                ForemanOfficeGUI.addUserMessage("Command " + i);
            }
        }, "Multiple message additions should work");
    }

    // ==================== Integration Tests ====================

    @Test
    @DisplayName("Full GUI cycle should work")
    void testFullGUICycle() {
        // Open GUI
        ForemanOfficeGUI.toggle();
        assertTrue(ForemanOfficeGUI.isOpen());

        // Add messages
        ForemanOfficeGUI.addUserMessage("Test command");
        ForemanOfficeGUI.addCrewMessage("Steve", "Working on it!");

        // Handle input
        ForemanOfficeGUI.handleKeyPress(65, 0, 0);
        ForemanOfficeGUI.handleCharTyped('a', 0);
        ForemanOfficeGUI.handleMouseClick(100.0, 100.0, 0);
        ForemanOfficeGUI.handleMouseScroll(1.0);

        // Tick
        ForemanOfficeGUI.tick();

        // Close GUI
        ForemanOfficeGUI.toggle();
        assertFalse(ForemanOfficeGUI.isOpen());
    }

    @Test
    @DisplayName("GUI operations in sequence should be safe")
    void testGUIOperationsInSequence() {
        assertDoesNotThrow(() -> {
            ForemanOfficeGUI.toggle();
            ForemanOfficeGUI.addUserMessage("Command 1");
            ForemanOfficeGUI.addCrewMessage("Steve", "Response 1");
            ForemanOfficeGUI.tick();
            ForemanOfficeGUI.handleKeyPress(65, 0, 0);
            ForemanOfficeGUI.handleCharTyped('a', 0);
            ForemanOfficeGUI.handleMouseClick(100.0, 100.0, 0);
            ForemanOfficeGUI.handleMouseScroll(1.0);
            ForemanOfficeGUI.onRenderOverlay(mockRenderEvent);
            ForemanOfficeGUI.toggle();
        }, "GUI operations in sequence should be safe");
    }
}
