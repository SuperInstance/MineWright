package com.minewright.client;

import com.minewright.client.gui.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Side-mounted GUI panel for MineWright agent interaction.
 * Inspired by Cursor's composer - slides in/out from the right side.
 * Now with scrollable message history and enhanced UX!
 *
 * Refactored Wave 46: Split into focused, single-responsibility classes
 */
public class ForemanOfficeGUI {
    private static final int PANEL_WIDTH = 200;
    private static final int PANEL_PADDING = 6;
    private static final int ANIMATION_SPEED = 20;

    // UI state
    private static boolean isOpen = false;
    private static float slideOffset = PANEL_WIDTH;
    private static float fadeAlpha = 0.0f;

    // Delegates (lazy initialization)
    private static MessagePanel messagePanel;
    private static InputHandler inputHandler;
    private static VoiceIntegrationPanel voicePanel;
    private static GUIRenderer renderer;

    // Quick buttons
    private static final InputHandler.QuickButton[] QUICK_BUTTONS = {
        new InputHandler.QuickButton("Spawn", "spawn Crew", "Create a new crew member", 0xFF4CAF50),
        new InputHandler.QuickButton("Follow", "follow me", "Make crew follow you", 0xFF2196F3),
        new InputHandler.QuickButton("Mic", "__VOICE_INPUT__", "Click to speak a command", 0xFFE91E63),
        new InputHandler.QuickButton("Help", "help", "Show available commands", 0xFF9C27B0)
    };

    /**
     * Initialize delegates lazily
     */
    private static void initDelegates() {
        if (messagePanel == null) {
            messagePanel = new MessagePanel();
        }
        if (inputHandler == null) {
            inputHandler = new InputHandler(messagePanel);
        }
        if (voicePanel == null) {
            voicePanel = new VoiceIntegrationPanel(messagePanel, inputHandler::sendCommand);
        }
        if (renderer == null) {
            renderer = new GUIRenderer();
        }
    }

    /**
     * Toggle GUI open/closed
     */
    public static void toggle() {
        isOpen = !isOpen;

        initDelegates();

        Minecraft mc = Minecraft.getInstance();

        if (isOpen) {
            inputHandler.initializeInputBox(mc, PANEL_WIDTH);
            mc.setScreen(new ForemanOverlayScreen());
            if (inputHandler.getInputBox() != null) {
                inputHandler.getInputBox().setFocused(true);
            }
            fadeAlpha = 0.0f;
            playUISound(SoundEvents.UI_BUTTON_CLICK.value(), 0.5f);

            // Auto-start voice input when GUI opens
            voicePanel.startVoiceInput();
        } else {
            inputHandler.cleanupInputBox();
            if (mc.screen instanceof ForemanOverlayScreen) {
                mc.setScreen(null);
            }

            // Stop voice input if active when closing
            if (voicePanel.isVoiceInputActive()) {
                voicePanel.stopVoiceInput();
            }
        }
    }

    /**
     * Check if GUI is open
     */
    public static boolean isOpen() {
        return isOpen;
    }

    /**
     * Add a message to the chat history
     */
    public static void addMessage(String sender, String text, int bubbleColor, boolean isUser) {
        initDelegates();
        messagePanel.addMessage(sender, text, bubbleColor, isUser);
    }

    /**
     * Add a user command to the history
     */
    public static void addUserMessage(String text) {
        initDelegates();
        messagePanel.addUserMessage(text);
    }

    /**
     * Add a Crew response to the history
     */
    public static void addCrewMessage(String crewName, String text) {
        initDelegates();
        messagePanel.addCrewMessage(crewName, text);
    }

    /**
     * Add a system message to the history
     */
    public static void addSystemMessage(String text) {
        initDelegates();
        messagePanel.addSystemMessage(text);
    }

    /**
     * Handle keyboard input
     */
    public static boolean handleKeyPress(int keyCode, int scanCode, int modifiers) {
        if (!isOpen) return false;

        initDelegates();

        boolean shouldClose = false;

        // Escape key - close panel
        if (keyCode == 256) { // ESC
            shouldClose = true;
        } else {
            inputHandler.handleKeyPress(keyCode, scanCode, modifiers);
        }

        if (shouldClose) {
            toggle();
            return true;
        }

        return true;
    }

    /**
     * Handle character typing
     */
    public static boolean handleCharTyped(char codePoint, int modifiers) {
        if (!isOpen) return false;

        initDelegates();
        return inputHandler.handleCharTyped(codePoint, modifiers);
    }

    /**
     * Handle mouse clicks
     */
    public static void handleMouseClick(double mouseX, double mouseY, int button) {
        if (!isOpen) return;

        initDelegates();

        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int panelX = (int) (screenWidth - PANEL_WIDTH + slideOffset);

        inputHandler.handleMouseClick(mouseX, mouseY, button, panelX, PANEL_WIDTH, screenWidth, screenHeight, QUICK_BUTTONS);

        // Check for voice input button click
        int headerHeight = 35;
        int crewPanelY = headerHeight;
        int crewPanelHeight = inputHandler.isCrewPanelExpanded() ? 180 : 30;
        int buttonAreaY = crewPanelY + crewPanelHeight + 5;

        if (mouseX >= panelX && mouseX <= panelX + PANEL_WIDTH &&
            mouseY >= buttonAreaY + 5 && mouseY <= buttonAreaY + 5 + 20) {
            int buttonCount = QUICK_BUTTONS.length;
            int buttonWidth = (PANEL_WIDTH - 12 - (buttonCount - 1) * 3) / buttonCount;

            for (int i = 0; i < buttonCount; i++) {
                int btnX = panelX + 6 + i * (buttonWidth + 3);
                if (mouseX >= btnX && mouseX <= btnX + buttonWidth) {
                    InputHandler.QuickButton btn = QUICK_BUTTONS[i];
                    if (btn.command.equals("__VOICE_INPUT__")) {
                        voicePanel.startVoiceInput();
                        playUISound(SoundEvents.UI_BUTTON_CLICK.value(), 0.4f);
                    }
                }
            }
        }
    }

    /**
     * Handle mouse scroll
     */
    public static void handleMouseScroll(double scrollDelta) {
        if (!isOpen) return;

        initDelegates();
        inputHandler.handleMouseScroll(scrollDelta);
    }

    /**
     * Update GUI state each tick
     */
    public static void tick() {
        if (isOpen) {
            initDelegates();
            inputHandler.tick();
            renderer.tick();
        }
    }

    /**
     * Render the GUI overlay
     */
    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay().id().toString().contains("hotbar")) {
            return; // Don't render over hotbar
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        initDelegates();

        // Animate slide offset
        if (isOpen && slideOffset > 0) {
            slideOffset = Math.max(0, slideOffset - ANIMATION_SPEED);
        } else if (!isOpen && slideOffset < PANEL_WIDTH) {
            slideOffset = Math.min(PANEL_WIDTH, slideOffset + ANIMATION_SPEED);
        }

        // Don't render if completely hidden
        if (slideOffset >= PANEL_WIDTH) return;

        // Animate fade-in effect
        if (isOpen && fadeAlpha < 1.0f) {
            fadeAlpha = Math.min(1.0f, fadeAlpha + 0.05f);
        } else if (!isOpen && fadeAlpha > 0.0f) {
            fadeAlpha = Math.max(0.0f, fadeAlpha - 0.1f);
        }

        GuiGraphics graphics = event.getGuiGraphics();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int panelX = (int) (screenWidth - PANEL_WIDTH + slideOffset);
        int panelY = 0;

        // Apply fade effect
        GUIRenderer.applyFadeEffect(fadeAlpha);

        // Render panel background
        renderer.renderPanelBackground(graphics, panelX, panelY, PANEL_WIDTH, screenHeight, fadeAlpha);

        // Render header
        int headerHeight = GUIRenderer.renderHeader(graphics, mc.font, panelX, panelY, PANEL_PADDING);

        // Render crew status panel
        int crewPanelY = headerHeight;
        int crewPanelHeight = inputHandler.isCrewPanelExpanded() ? 180 : 30;
        renderer.renderCrewStatusPanel(graphics, mc, panelX, crewPanelY, PANEL_WIDTH, crewPanelHeight,
            inputHandler.isCrewPanelExpanded(), 180, 30, PANEL_PADDING);

        // Render quick action buttons
        int buttonAreaY = crewPanelY + crewPanelHeight + 5;
        int buttonAreaHeight = 30;
        QuickButtonsPanel.renderQuickActionButtons(graphics, mc, panelX, buttonAreaY, PANEL_WIDTH, buttonAreaHeight,
            QUICK_BUTTONS, inputHandler);

        // Render message history
        int inputAreaY = screenHeight - 80;
        int messageAreaTop = buttonAreaY + buttonAreaHeight + 5;
        int messageAreaHeight = inputAreaY - messageAreaTop - 5;

        messagePanel.renderMessages(graphics, mc, panelX, messageAreaTop, PANEL_WIDTH, messageAreaHeight,
            renderer, PANEL_PADDING);

        // Render input area
        renderInputArea(graphics, mc, panelX, inputAreaY, screenHeight);

        GUIRenderer.disableBlendEffect();
    }

    /**
     * Render the command input area
     */
    private static void renderInputArea(GuiGraphics graphics, Minecraft mc, int x, int y, int screenHeight) {
        int fadedHeader = (GUIRenderer.HEADER_COLOR & 0x00FFFFFF) | (((int)((GUIRenderer.HEADER_COLOR >> 24) * fadeAlpha)) << 24);

        graphics.fillGradient(x, y, x + PANEL_WIDTH, screenHeight, fadedHeader, fadedHeader);
        graphics.drawString(mc.font, "§7Command:", x + PANEL_PADDING, y + 10, 0xFF888888);

        EditBox inputBox = inputHandler.getInputBox();
        if (inputBox != null && isOpen) {
            int inputBoxX = x + PANEL_PADDING;
            int inputBoxY = y + 25;
            inputBox.setX(inputBoxX);
            inputBox.setY(inputBoxY);
            inputBox.setWidth(PANEL_WIDTH - (PANEL_PADDING * 2));

            // Draw focus border if input box is focused
            if (inputBox.isFocused()) {
                renderFocusBorder(graphics, mc, inputBox, inputBoxX, inputBoxY);
            }

            inputBox.render(graphics, (int)mc.mouseHandler.xpos(), (int)mc.mouseHandler.ypos(), mc.getFrameTime());
        }

        graphics.drawString(mc.font, "§8Enter: Send | ↑↓: History | Scroll: Messages",
            x + PANEL_PADDING, screenHeight - 15, 0xFF555555);
    }

    /**
     * Render focus border around input box
     */
    private static void renderFocusBorder(GuiGraphics graphics, Minecraft mc, EditBox inputBox, int x, int y) {
        int borderThickness = 2;
        long gameTime = mc.level != null ? mc.level.getGameTime() : 0;
        int pulseAlpha = (int)(128 + 64 * Math.sin(gameTime * 0.1));
        int focusColor = (GUIRenderer.FOCUS_BORDER_COLOR & 0x00FFFFFF) | (pulseAlpha << 24);

        graphics.fill(x - borderThickness, y - borderThickness,
                    x + inputBox.getWidth() + borderThickness, y, focusColor);
        graphics.fill(x - borderThickness, y + inputBox.getHeight(),
                    x + inputBox.getWidth() + borderThickness, y + inputBox.getHeight() + borderThickness, focusColor);
        graphics.fill(x - borderThickness, y, x, y + inputBox.getHeight(), focusColor);
        graphics.fill(x + inputBox.getWidth(), y, x + inputBox.getWidth() + borderThickness,
                    y + inputBox.getHeight(), focusColor);
    }

    /**
     * Play a UI sound with controlled volume
     */
    private static void playUISound(net.minecraft.sounds.SoundEvent sound, float volume) {
        Minecraft mc = Minecraft.getInstance();
        var soundManager = mc.getSoundManager();
        if (soundManager != null) {
            soundManager.play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(sound, 1.0f, volume));
        }
    }
}
