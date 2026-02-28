package com.minewright.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.minewright.MineWrightMod;
import com.minewright.entity.ForemanEntity;
import com.minewright.action.ActionExecutor;
import com.minewright.action.actions.BuildStructureAction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Side-mounted GUI panel for MineWright agent interaction.
 * Inspired by Cursor's composer - slides in/out from the right side.
 * Now with scrollable message history and enhanced UX!
 */
public class ForemanOfficeGUI {
    private static final int PANEL_WIDTH = 200;
    private static final int PANEL_PADDING = 6;
    private static final int ANIMATION_SPEED = 20;
    private static final int MESSAGE_HEIGHT = 12;
    private static final int MAX_MESSAGES = 500;

    private static boolean isOpen = false;
    private static float slideOffset = PANEL_WIDTH; // Start fully hidden
    private static float fadeAlpha = 0.0f; // Fade-in animation alpha
    private static EditBox inputBox;
    private static List<String> commandHistory = new ArrayList<>();
    private static int historyIndex = -1;

    // Message history and scrolling
    private static List<ChatMessage> messages = new ArrayList<>();
    private static int scrollOffset = 0;
    private static int maxScroll = 0;
    private static float targetScrollOffset = 0.0f; // For smooth scrolling
    private static final int BACKGROUND_COLOR = 0x15202020; // Ultra transparent (15 = ~8% opacity)
    private static final int BORDER_COLOR = 0x40404040; // More transparent border
    private static final int HEADER_COLOR = 0x25252525; // More transparent header (~15% opacity)
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int FOCUS_BORDER_COLOR = 0xFF2196F3; // Blue for focused input

    // Message bubble colors
    private static final int USER_BUBBLE_COLOR = 0xC04CAF50; // Green bubble for user
    private static final int CREW_BUBBLE_COLOR = 0xC02196F3; // Blue bubble for Crew
    private static final int SYSTEM_BUBBLE_COLOR = 0xC0FF9800; // Orange bubble for system

    // UX Enhancement: Hover state tracking
    private static int hoveredCrewIndex = -1;
    private static int hoveredButtonIndex = -1;
    private static float hoverScale = 1.0f;
    private static long hoverStartTime = 0;

    // UX Enhancement: Button definitions
    private static final class QuickButton {
        final String label;
        final String command;
        final String tooltip;
        final int color;

        QuickButton(String label, String command, String tooltip, int color) {
            this.label = label;
            this.command = command;
            this.tooltip = tooltip;
            this.color = color;
        }
    }

    private static final List<QuickButton> QUICK_BUTTONS = List.of(
        new QuickButton("Spawn", "spawn Crew", "Create a new crew member", 0xFF4CAF50),
        new QuickButton("Follow", "follow me", "Make crew follow you", 0xFF2196F3),
        new QuickButton("Mic", "__VOICE_INPUT__", "Click to speak a command", 0xFFE91E63),
        new QuickButton("Help", "help", "Show available commands", 0xFF9C27B0)
    );

    // Voice input state
    private static boolean isVoiceInputActive = false;

    private static class ChatMessage {
        String sender; // "You", "Foreman", "Crew", "System", etc.
        String text;
        int bubbleColor;
        boolean isUser; // true if message from user

        ChatMessage(String sender, String text, int bubbleColor, boolean isUser) {
            this.sender = sender;
            this.text = text;
            this.bubbleColor = bubbleColor;
            this.isUser = isUser;
        }
    }

    // Crew status panel state
    private static boolean crewPanelExpanded = true;
    private static final int CREW_PANEL_HEIGHT = 180; // Increased to accommodate progress bars
    private static final int CREW_PANEL_COLLAPSED_HEIGHT = 30;
    private static String targetedCrewMember = null; // Currently selected crew for commands

    // Status colors
    private static final int STATUS_IDLE_COLOR = 0xFF4CAF50;    // Green
    private static final int STATUS_WORKING_COLOR = 0xFF2196F3;  // Blue
    private static final int STATUS_PLANNING_COLOR = 0xFFFF9800; // Orange
    private static final int STATUS_COMBAT_COLOR = 0xFFF44336;   // Red

    // Progress indicator colors
    private static final int PROGRESS_BAR_BG = 0x80000000;      // Semi-transparent black
    private static final int PROGRESS_BAR_FILL = 0xFF4CAF50;    // Green progress fill
    private static final int PROGRESS_BAR_PLANNING = 0xFFFF9800; // Orange for planning

    // Animation states
    private static int thinkingAnimationFrame = 0;
    private static int commandFlashTimer = 0; // For visual feedback when command is sent
    private static float commandFlashIntensity = 0.0f;

    public static void toggle() {
        isOpen = !isOpen;

        Minecraft mc = Minecraft.getInstance();

        if (isOpen) {
            initializeInputBox();
            mc.setScreen(new ForemanOverlayScreen());
            if (inputBox != null) {
                inputBox.setFocused(true);
            }
            // Reset fade animation
            fadeAlpha = 0.0f;
            // Play subtle open sound
            playUISound(SoundEvents.UI_BUTTON_CLICK.value(), 0.5f);
        } else {
            if (inputBox != null) {
                inputBox = null;
            }
            if (mc.screen instanceof ForemanOverlayScreen) {
                mc.setScreen(null);
            }
            // Reset hover states
            hoveredCrewIndex = -1;
            hoveredButtonIndex = -1;
            hoverScale = 1.0f;
        }
    }

    /**
     * Play a UI sound with controlled volume
     */
    private static void playUISound(net.minecraft.sounds.SoundEvent sound, float volume) {
        Minecraft mc = Minecraft.getInstance();
        SoundManager soundManager = mc.getSoundManager();
        if (soundManager != null) {
            soundManager.play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(sound, 1.0f, volume));
        }
    }

    public static boolean isOpen() {
        return isOpen;
    }

    private static void initializeInputBox() {
        Minecraft mc = Minecraft.getInstance();
        if (inputBox == null) {
            inputBox = new EditBox(mc.font, 0, 0, PANEL_WIDTH - 20, 20, 
                Component.literal("Command"));
            inputBox.setMaxLength(256);
            inputBox.setHint(Component.literal("Tell Foreman what to do..."));
            inputBox.setFocused(true);
        }
    }

    /**
     * Add a message to the chat history
     */
    public static void addMessage(String sender, String text, int bubbleColor, boolean isUser) {
        messages.add(new ChatMessage(sender, text, bubbleColor, isUser));
        if (messages.size() > MAX_MESSAGES) {
            messages.remove(0);
        }
        // Auto-scroll to bottom on new message
        scrollOffset = 0;
    }

    /**
     * Add a user command to the history
     */
    public static void addUserMessage(String text) {
        addMessage("You", text, USER_BUBBLE_COLOR, true);
    }

    /**
     * Add a Crew response to the history
     */
    public static void addCrewMessage(String crewName, String text) {
        addMessage(crewName, text, CREW_BUBBLE_COLOR, false);
    }

    /**
     * Add a system message to the history
     */
    public static void addSystemMessage(String text) {
        addMessage("System", text, SYSTEM_BUBBLE_COLOR, false);
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay().id().toString().contains("hotbar")) {
            return; // Don't render over hotbar
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

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

        // Animate smooth scrolling
        float scrollLerp = 0.15f; // Smooth interpolation factor
        targetScrollOffset = scrollOffset;
        float currentScrollFloat = scrollOffset;
        float smoothedScroll = currentScrollFloat + (targetScrollOffset - currentScrollFloat) * scrollLerp;
        scrollOffset = (int) smoothedScroll;

        GuiGraphics graphics = event.getGuiGraphics();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int panelX = (int) (screenWidth - PANEL_WIDTH + slideOffset);
        int panelY = 0;
        int panelHeight = screenHeight;

        // Apply fade effect to alpha
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, fadeAlpha);
        RenderSystem.blendFuncSeparate(
            com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
            com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            com.mojang.blaze3d.platform.GlStateManager.SourceFactor.ONE,
            com.mojang.blaze3d.platform.GlStateManager.DestFactor.ZERO
        );

        // Draw background with fade alpha
        int fadedBackground = (BACKGROUND_COLOR & 0x00FFFFFF) | (((int)((BACKGROUND_COLOR >> 24) * fadeAlpha)) << 24);
        int fadedBorder = (BORDER_COLOR & 0x00FFFFFF) | (((int)((BORDER_COLOR >> 24) * fadeAlpha)) << 24);
        int fadedHeader = (HEADER_COLOR & 0x00FFFFFF) | (((int)((HEADER_COLOR >> 24) * fadeAlpha)) << 24);

        graphics.fillGradient(panelX, panelY, screenWidth, panelHeight, fadedBackground, fadedBackground);
        graphics.fillGradient(panelX - 2, panelY, panelX, panelHeight, fadedBorder, fadedBorder);

        int headerHeight = 35;
        graphics.fillGradient(panelX, panelY, screenWidth, headerHeight, fadedHeader, fadedHeader);
        graphics.drawString(mc.font, "§lMineWright", panelX + PANEL_PADDING, panelY + 8, TEXT_COLOR);
        graphics.drawString(mc.font, "§7Press K to close", panelX + PANEL_PADDING, panelY + 20, 0xFF888888);

        // Render crew status panel (collapsible)
        int crewPanelY = headerHeight;
        int crewPanelCurrentHeight = crewPanelExpanded ? CREW_PANEL_HEIGHT : CREW_PANEL_COLLAPSED_HEIGHT;
        renderCrewStatusPanel(graphics, mc, panelX, crewPanelY, screenWidth, crewPanelCurrentHeight);

        // Quick action buttons
        int buttonAreaY = crewPanelY + crewPanelCurrentHeight + 5;
        int buttonAreaHeight = 30;
        renderQuickActionButtons(graphics, mc, panelX, buttonAreaY, screenWidth, buttonAreaHeight);

        // Message history area
        int inputAreaY = screenHeight - 80;
        int messageAreaTop = buttonAreaY + buttonAreaHeight + 5;
        int messageAreaHeight = inputAreaY - messageAreaTop - 5;
        int messageAreaBottom = messageAreaTop + messageAreaHeight;

        int totalMessageHeight = 0;
        for (ChatMessage msg : messages) {
            int maxBubbleWidth = PANEL_WIDTH - (PANEL_PADDING * 3);
            int bubbleHeight = calculateBubbleHeight(mc.font, msg.text, maxBubbleWidth - 10);
            totalMessageHeight += bubbleHeight + 5 + 12; // message + spacing + name
        }
        maxScroll = Math.max(0, totalMessageHeight - messageAreaHeight);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        // Render messages (scrollable)
        int yPos = messageAreaTop + 5;
        
        // Clip rendering to message area
        graphics.enableScissor(panelX, messageAreaTop, screenWidth, messageAreaBottom);
        
        if (messages.isEmpty()) {
            graphics.drawString(mc.font, "§7No messages yet...",
                panelX + PANEL_PADDING, yPos, 0xFF666666);
            graphics.drawString(mc.font, "§7Type a command below!",
                panelX + PANEL_PADDING, yPos + 12, 0xFF555555);
        } else {
            // Check if any crew is planning and show typing indicator at bottom
            boolean isAnyPlanning = MineWrightMod.getCrewManager().getAllCrewMembers().stream()
                .anyMatch(crew -> crew.getActionExecutor() != null && crew.getActionExecutor().isPlanning());

            if (isAnyPlanning) {
                int typingIndicatorY = messageAreaBottom - 15;
                String typingText = "§7Crew is thinking" + getThinkingDots();
                int textWidth = mc.font.width(typingText);
                graphics.drawString(mc.font, typingText, panelX + PANEL_PADDING, typingIndicatorY, 0xFFAAAAAA);

                // Animated progress bar for typing indicator
                int typingBarWidth = textWidth + 10;
                float typingProgress = ((thinkingAnimationFrame % 30) / 30.0f);
                int typingFillWidth = (int)(typingBarWidth * typingProgress);
                graphics.fill(panelX + PANEL_PADDING, typingIndicatorY + 12,
                            panelX + PANEL_PADDING + typingFillWidth, typingIndicatorY + 14,
                            0x802196F3);
            }
            int currentY = messageAreaBottom - 5; // Start from bottom
            
            for (int i = messages.size() - 1; i >= 0; i--) {
                ChatMessage msg = messages.get(i);

                int maxBubbleWidth = PANEL_WIDTH - (PANEL_PADDING * 3); // Leave space on sides
                List<String> wrappedLines = wrapText(mc.font, msg.text, maxBubbleWidth - 10);
                int bubbleWidth = calculateBubbleWidth(mc.font, msg.text, maxBubbleWidth - 10);
                int bubbleHeight = calculateBubbleHeight(mc.font, msg.text, maxBubbleWidth - 10);

                int msgY = currentY - bubbleHeight + scrollOffset;

                if (msgY + bubbleHeight < messageAreaTop - 20 || msgY > messageAreaBottom + 20) {
                    currentY -= bubbleHeight + 5;
                    continue;
                }

                // Render message bubble based on sender
                if (msg.isUser) {
                    int bubbleX = screenWidth - bubbleWidth - PANEL_PADDING - 5;

                    // Draw bubble background with gradient for alpha support
                    graphics.fillGradient(bubbleX - 3, msgY - 3, bubbleX + bubbleWidth + 3, msgY + bubbleHeight, msg.bubbleColor, msg.bubbleColor);

                    // Draw sender name (small, above bubble)
                    graphics.drawString(mc.font, "§7" + msg.sender, bubbleX, msgY - 12, 0xFFCCCCCC);

                    // Draw each line of the message
                    int lineY = msgY + 5;
                    for (String line : wrappedLines) {
                        graphics.drawString(mc.font, line, bubbleX + 5, lineY, 0xFFFFFFFF);
                        lineY += MESSAGE_HEIGHT;
                    }

                } else {
                    int bubbleX = panelX + PANEL_PADDING;

                    // Draw bubble background with gradient for alpha support
                    graphics.fillGradient(bubbleX - 3, msgY - 3, bubbleX + bubbleWidth + 3, msgY + bubbleHeight, msg.bubbleColor, msg.bubbleColor);

                    // Draw sender name (small, above bubble)
                    graphics.drawString(mc.font, "§l" + msg.sender, bubbleX, msgY - 12, TEXT_COLOR);

                    // Draw each line of the message
                    int lineY = msgY + 5;
                    for (String line : wrappedLines) {
                        graphics.drawString(mc.font, line, bubbleX + 5, lineY, 0xFFFFFFFF);
                        lineY += MESSAGE_HEIGHT;
                    }
                }

                currentY -= bubbleHeight + 5 + 12; // Extra space for sender name
            }
        }
        
        graphics.disableScissor();
        
        if (maxScroll > 0) {
            int scrollBarHeight = Math.max(20, (messageAreaHeight * messageAreaHeight) / (maxScroll + messageAreaHeight));
            int scrollBarY = messageAreaTop + (int)((messageAreaHeight - scrollBarHeight) * (1.0f - (float)scrollOffset / maxScroll));
            graphics.fill(screenWidth - 4, scrollBarY, screenWidth - 2, scrollBarY + scrollBarHeight, 0xFF888888);
        }

        // Command input area (bottom) with gradient for alpha support
        graphics.fillGradient(panelX, inputAreaY, screenWidth, screenHeight, fadedHeader, fadedHeader);
        graphics.drawString(mc.font, "§7Command:", panelX + PANEL_PADDING, inputAreaY + 10, 0xFF888888);

        if (inputBox != null && isOpen) {
            int inputBoxX = panelX + PANEL_PADDING;
            int inputBoxY = inputAreaY + 25;
            inputBox.setX(inputBoxX);
            inputBox.setY(inputBoxY);
            inputBox.setWidth(PANEL_WIDTH - (PANEL_PADDING * 2));

            // Draw focus border if input box is focused
            if (inputBox.isFocused()) {
                int borderThickness = 2;
                long gameTime = mc.level != null ? mc.level.getGameTime() : 0;
                int pulseAlpha = (int)(128 + 64 * Math.sin(gameTime * 0.1));
                int focusColor = (FOCUS_BORDER_COLOR & 0x00FFFFFF) | (pulseAlpha << 24);
                graphics.fill(inputBoxX - borderThickness, inputBoxY - borderThickness,
                            inputBoxX + inputBox.getWidth() + borderThickness, inputBoxY,
                            focusColor);
                graphics.fill(inputBoxX - borderThickness, inputBoxY + inputBox.getHeight(),
                            inputBoxX + inputBox.getWidth() + borderThickness, inputBoxY + inputBox.getHeight() + borderThickness,
                            focusColor);
                graphics.fill(inputBoxX - borderThickness, inputBoxY, inputBoxX, inputBoxY + inputBox.getHeight(),
                            focusColor);
                graphics.fill(inputBoxX + inputBox.getWidth(), inputBoxY, inputBoxX + inputBox.getWidth() + borderThickness,
                            inputBoxY + inputBox.getHeight(), focusColor);
            }

            inputBox.render(graphics, (int)mc.mouseHandler.xpos(), (int)mc.mouseHandler.ypos(), mc.getFrameTime());
        }

        graphics.drawString(mc.font, "§8Enter: Send | ↑↓: History | Scroll: Messages", 
            panelX + PANEL_PADDING, screenHeight - 15, 0xFF555555);
        
        RenderSystem.disableBlend();
    }

    /**
     * Proper word wrap for text that splits at word boundaries
     * @param font Minecraft font for width calculation
     * @param text Text to wrap
     * @param maxWidth Maximum width per line
     * @return List of wrapped lines
     */
    private static List<String> wrapText(net.minecraft.client.gui.Font font, String text, int maxWidth) {
        List<String> lines = new ArrayList<>();

        // Handle empty or null text
        if (text == null || text.isEmpty()) {
            lines.add("");
            return lines;
        }

        // If text fits in one line, return it
        if (font.width(text) <= maxWidth) {
            lines.add(text);
            return lines;
        }

        // Split into words, preserving spaces
        String[] words = text.split("(?<=\\s)");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            // Try adding the word to the current line
            String testLine = currentLine + word;

            if (font.width(testLine) <= maxWidth) {
                // Word fits, add it
                currentLine.append(word);
            } else {
                // Word doesn't fit
                if (currentLine.length() == 0) {
                    // Single word is too long, break it up
                    String broken = breakLongWord(font, word, maxWidth);
                    String[] brokenLines = broken.split("\n");
                    for (int i = 0; i < brokenLines.length; i++) {
                        if (i > 0) {
                            lines.add(currentLine.toString().trim());
                            currentLine = new StringBuilder();
                        }
                        currentLine.append(brokenLines[i]);
                    }
                } else {
                    // Start a new line
                    lines.add(currentLine.toString().trim());
                    currentLine = new StringBuilder();

                    // Try fitting the word on the new line
                    testLine = currentLine + word;
                    if (font.width(testLine) <= maxWidth) {
                        currentLine.append(word);
                    } else {
                        // Word is still too long, break it
                        String broken = breakLongWord(font, word.trim(), maxWidth);
                        String[] brokenLines = broken.split("\n");
                        currentLine.append(brokenLines[0]);
                        for (int i = 1; i < brokenLines.length; i++) {
                            lines.add(currentLine.toString());
                            currentLine = new StringBuilder(brokenLines[i]);
                        }
                    }
                }
            }
        }

        // Add the last line if there's anything left
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString().trim());
        }

        return lines;
    }

    /**
     * Break a long word that doesn't fit on one line
     * @param font Minecraft font for width calculation
     * @param word Word to break
     * @param maxWidth Maximum width per line
     * @return String with newlines inserted at break points
     */
    private static String breakLongWord(net.minecraft.client.gui.Font font, String word, int maxWidth) {
        StringBuilder result = new StringBuilder();
        StringBuilder currentLine = new StringBuilder();

        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            String testLine = currentLine.toString() + c;

            if (font.width(testLine) <= maxWidth) {
                currentLine.append(c);
            } else {
                if (currentLine.length() > 0) {
                    result.append(currentLine.toString());
                    result.append("\n");
                    currentLine = new StringBuilder();
                }
                currentLine.append(c);
            }
        }

        if (currentLine.length() > 0) {
            result.append(currentLine.toString());
        }

        return result.toString();
    }

    /**
     * Calculate the height needed for a message bubble based on wrapped text
     * @param font Minecraft font for width calculation
     * @param text Text to measure
     * @param maxWidth Maximum width per line
     * @return Total height for all lines
     */
    private static int calculateBubbleHeight(net.minecraft.client.gui.Font font, String text, int maxWidth) {
        List<String> lines = wrapText(font, text, maxWidth);
        int lineHeight = MESSAGE_HEIGHT;
        int padding = 10;
        return (lines.size() * lineHeight) + padding;
    }

    /**
     * Calculate the width needed for a message bubble based on wrapped text
     * @param font Minecraft font for width calculation
     * @param text Text to measure
     * @param maxWidth Maximum width per line
     * @return Actual width needed (width of widest line)
     */
    private static int calculateBubbleWidth(net.minecraft.client.gui.Font font, String text, int maxWidth) {
        List<String> lines = wrapText(font, text, maxWidth);
        int maxWidthNeeded = 0;

        for (String line : lines) {
            int lineWidth = font.width(line);
            if (lineWidth > maxWidthNeeded) {
                maxWidthNeeded = lineWidth;
            }
        }

        // Add padding
        return maxWidthNeeded + 10;
    }

    /**
     * Renders quick action buttons with hover effects and tooltips
     */
    private static void renderQuickActionButtons(GuiGraphics graphics, Minecraft mc, int x, int y, int width, int height) {
        int buttonCount = QUICK_BUTTONS.size();
        int buttonWidth = (width - (PANEL_PADDING * 2) - (buttonCount - 1) * 3) / buttonCount;
        int buttonHeight = 20;
        int buttonY = y + 5;

        // Get mouse position for hover detection
        double mouseX = mc.mouseHandler.xpos();
        double mouseY = mc.mouseHandler.ypos();

        hoveredButtonIndex = -1; // Reset hover state

        for (int i = 0; i < buttonCount; i++) {
            QuickButton button = QUICK_BUTTONS.get(i);
            int buttonX = x + PANEL_PADDING + i * (buttonWidth + 3);

            // Check if mouse is hovering over this button
            boolean isHovered = mouseX >= buttonX && mouseX <= buttonX + buttonWidth &&
                              mouseY >= buttonY && mouseY <= buttonY + buttonHeight;

            if (isHovered) {
                hoveredButtonIndex = i;

                // Animate hover scale
                if (hoverStartTime == 0) {
                    hoverStartTime = System.currentTimeMillis();
                }
                long hoverDuration = System.currentTimeMillis() - hoverStartTime;
                float targetScale = 1.05f;
                hoverScale = 1.0f + (targetScale - 1.0f) * Math.min(1.0f, hoverDuration / 100.0f);

                // Draw hover effect (lighter background)
                int hoverColor = 0x40FFFFFF;
                graphics.fill(buttonX, buttonY, buttonX + buttonWidth, buttonY + buttonHeight, hoverColor);

                // Draw tooltip after delay
                if (hoverDuration > 500) {
                    int tooltipY = buttonY + buttonHeight + 5;
                    int tooltipWidth = mc.font.width(button.tooltip) + 10;
                    int tooltipX = Math.max(x, Math.min(buttonX + buttonWidth / 2 - tooltipWidth / 2, x + width - tooltipWidth));

                    // Tooltip background
                    graphics.fill(tooltipX, tooltipY, tooltipX + tooltipWidth, tooltipY + 14, 0xD0000000);
                    graphics.fill(tooltipX, tooltipY, tooltipX + tooltipWidth, tooltipY + 1, 0xFF888888);
                    graphics.fill(tooltipX, tooltipY + 13, tooltipX + tooltipWidth, tooltipY + 14, 0xFF888888);
                    graphics.fill(tooltipX, tooltipY, tooltipX + 1, tooltipY + 14, 0xFF888888);
                    graphics.fill(tooltipX + tooltipWidth - 1, tooltipY, tooltipX + tooltipWidth, tooltipY + 14, 0xFF888888);

                    // Tooltip text
                    graphics.drawString(mc.font, button.tooltip, tooltipX + 5, tooltipY + 3, 0xFFFFFFFF);
                }
            }

            // Apply scale effect to button text
            float scale = isHovered ? hoverScale : 1.0f;
            int scaledWidth = (int)(buttonWidth * scale);
            int scaledHeight = (int)(buttonHeight * scale);
            int scaledX = buttonX + (buttonWidth - scaledWidth) / 2;
            int scaledY = buttonY + (buttonHeight - scaledHeight) / 2;

            // Draw button background
            int buttonBgColor = isHovered ? (button.color | 0xFF000000) : (button.color & 0x80FFFFFF);
            graphics.fill(scaledX, scaledY, scaledX + scaledWidth, scaledY + scaledHeight, buttonBgColor);

            // Draw button border
            graphics.fill(scaledX, scaledY, scaledX + scaledWidth, scaledY + 1, 0xFF000000);
            graphics.fill(scaledX, scaledY + scaledHeight - 1, scaledX + scaledWidth, scaledY + scaledHeight, 0xFF000000);
            graphics.fill(scaledX, scaledY, scaledX + 1, scaledY + scaledHeight, 0xFF000000);
            graphics.fill(scaledX + scaledWidth - 1, scaledY, scaledX + scaledWidth, scaledY + scaledHeight, 0xFF000000);

            // Draw button label (centered)
            String label = button.label;
            int labelX = scaledX + (scaledWidth - mc.font.width(label)) / 2;
            int labelY = scaledY + (scaledHeight - mc.font.lineHeight) / 2;
            graphics.drawString(mc.font, label, labelX, labelY, 0xFFFFFFFF);
        }

        // Reset hover start time if no buttons are hovered
        if (hoveredButtonIndex == -1) {
            hoverStartTime = 0;
            hoverScale = 1.0f;
        }
    }

    /**
     * Renders the crew status panel showing all active crew members
     * @param graphics GuiGraphics instance
     * @param mc Minecraft instance
     * @param x X position of panel
     * @param y Y position of panel
     * @param width Width of panel
     * @param height Current height (depends on expanded/collapsed state)
     */
    private static void renderCrewStatusPanel(GuiGraphics graphics, Minecraft mc, int x, int y, int width, int height) {
        // Panel background
        graphics.fillGradient(x, y, x + width, y + height, 0x20181818, 0x20181818);
        graphics.fillGradient(x, y, x + width, y + 1, 0x40404040, 0x40404040); // Top border
        graphics.fillGradient(x, y + height - 1, x + width, y + height, 0x40404040, 0x40404040); // Bottom border

        // Panel header (always visible)
        int headerY = y + 5;
        graphics.drawString(mc.font, "§lCrew Status", x + PANEL_PADDING, headerY, TEXT_COLOR);

        // Collapse/expand indicator
        String expandIndicator = crewPanelExpanded ? "▼" : "▶";
        graphics.drawString(mc.font, "§7" + expandIndicator, x + width - 20, headerY, 0xFF888888);

        // Crew count
        var crewMembers = MineWrightMod.getCrewManager().getAllCrewMembers();
        int crewCount = crewMembers.size();
        graphics.drawString(mc.font, "§7" + crewCount + " member" + (crewCount != 1 ? "s" : ""),
            x + PANEL_PADDING + 90, headerY, 0xFF888888);

        // Render crew member details if expanded
        if (crewPanelExpanded) {
            int crewY = y + 25;
            int crewIndex = 0;

            for (ForemanEntity crew : crewMembers) {
                if (crewY + 25 > y + height) break; // Don't render beyond panel

                renderCrewMember(graphics, mc, crew, x, crewY, width, crewIndex);
                crewY += 25;
                crewIndex++;
            }

            // Show message if no crew members
            if (crewCount == 0) {
                graphics.drawString(mc.font, "§7No crew members. Use 'spawn <name>' to create one!",
                    x + PANEL_PADDING, crewY, 0xFF666666);
            }
        }
    }

    /**
     * Renders a single crew member entry in the status panel
     * @param graphics GuiGraphics instance
     * @param mc Minecraft instance
     * @param crew The crew member entity
     * @param x X position
     * @param y Y position
     * @param width Panel width
     * @param index Index of this crew member (for background striping)
     */
    private static void renderCrewMember(GuiGraphics graphics, Minecraft mc, ForemanEntity crew, int x, int y, int width, int index) {
        ActionExecutor executor = crew.getActionExecutor();
        boolean isPlanning = executor != null && executor.isPlanning();
        boolean isExecuting = executor != null && executor.isExecuting();

        // Increase height for progress bar
        int itemHeight = (isPlanning || isExecuting) ? 40 : 24;

        // Check for hover effect
        double mouseX = mc.mouseHandler.xpos();
        double mouseY = mc.mouseHandler.ypos();
        boolean isHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + itemHeight;

        // Alternating background for readability
        if (index % 2 == 0) {
            int bgColor = isHovered ? 0x25252525 : 0x15151515;
            graphics.fillGradient(x + 2, y, x + width - 2, y + itemHeight, bgColor, bgColor);
        } else if (isHovered) {
            graphics.fillGradient(x + 2, y, x + width - 2, y + itemHeight, 0x25252525, 0x25252525);
        }

        // Highlight if targeted
        if (crew.getEntityName().equals(targetedCrewMember)) {
            int targetColor = isHovered ? 0x502196F3 : 0x402196F3;
            graphics.fillGradient(x + 2, y, x + width - 2, y + itemHeight, targetColor, targetColor);
        }

        // Update hover index for mouse handling
        if (isHovered) {
            hoveredCrewIndex = index;
        }

        String crewName = crew.getEntityName();

        // Status indicator dot
        int statusColor = getStatusColor(crew);
        int dotX = x + PANEL_PADDING;
        int dotY = y + 8;
        graphics.fill(dotX, dotY, dotX + 6, dotY + 6, statusColor);
        graphics.fill(dotX + 1, dotY + 1, dotX + 5, dotY + 5, statusColor);

        // Crew name
        int nameX = dotX + 10;
        graphics.drawString(mc.font, "§l" + crewName, nameX, y + 3, TEXT_COLOR);

        // Health bar (only if not showing progress bar)
        if (!isPlanning && !isExecuting) {
            float healthPercent = crew.getHealth() / crew.getMaxHealth();
            int healthBarWidth = 50;
            int healthBarX = nameX + mc.font.width(crewName) + 10;
            int healthBarY = y + 6;

            // Health bar background
            graphics.fill(healthBarX, healthBarY, healthBarX + healthBarWidth, healthBarY + 6, 0xFF333333);

            // Health bar fill (green to red gradient based on health)
            int healthFillWidth = (int)(healthBarWidth * healthPercent);
            int healthColor = healthPercent > 0.5f ? 0xFF4CAF50 : (healthPercent > 0.25f ? 0xFFFFC107 : 0xFFF44336);
            graphics.fill(healthBarX, healthBarY, healthBarX + healthFillWidth, healthBarY + 6, healthColor);
        }

        // Current action/status with animated thinking indicator
        String statusText = getStatusText(crew);
        int statusX = x + PANEL_PADDING;
        graphics.drawString(mc.font, "§7" + statusText, statusX, y + 15, 0xFFCCCCCC);

        // Animated thinking dots
        if (isPlanning) {
            String thinkingDots = getThinkingDots();
            int dotsX = statusX + mc.font.width(statusText);
            graphics.drawString(mc.font, thinkingDots, dotsX, y + 15, STATUS_PLANNING_COLOR);
        }

        // Progress bar for executing tasks
        if (isExecuting && !isPlanning) {
            int progressBarY = y + 24;
            int progressBarWidth = width - (PANEL_PADDING * 2);
            int progressBarHeight = 8;

            // Progress bar background
            graphics.fill(x + PANEL_PADDING, progressBarY,
                         x + PANEL_PADDING + progressBarWidth, progressBarY + progressBarHeight,
                         PROGRESS_BAR_BG);

            // Get progress percentage
            int progress = executor.getCurrentActionProgress();

            // Progress bar fill
            int fillWidth = (progressBarWidth * progress) / 100;
            graphics.fill(x + PANEL_PADDING, progressBarY,
                         x + PANEL_PADDING + fillWidth, progressBarY + progressBarHeight,
                         PROGRESS_BAR_FILL);

            // Progress percentage text
            String progressText = progress + "%";
            graphics.drawString(mc.font, progressText, x + PANEL_PADDING, progressBarY + 10, 0xFFCCCCCC);

            // For build tasks, show blocks placed
            if (executor.getCurrentAction() instanceof BuildStructureAction) {
                BuildStructureAction buildAction = (BuildStructureAction) executor.getCurrentAction();
                String buildInfo = getBuildProgressText(buildAction);
                if (buildInfo != null) {
                    graphics.drawString(mc.font, buildInfo, x + PANEL_PADDING + 40, progressBarY + 10, 0xFFAAAAAA);
                }
            }
        } else if (isPlanning) {
            // Animated planning progress bar
            int progressBarY = y + 24;
            int progressBarWidth = width - (PANEL_PADDING * 2);
            int progressBarHeight = 8;

            // Progress bar background
            graphics.fill(x + PANEL_PADDING, progressBarY,
                         x + PANEL_PADDING + progressBarWidth, progressBarY + progressBarHeight,
                         PROGRESS_BAR_BG);

            // Animated planning progress (sweeping animation)
            float planningProgress = ((thinkingAnimationFrame % 60) / 60.0f);
            int fillWidth = (int)(progressBarWidth * planningProgress);
            graphics.fill(x + PANEL_PADDING, progressBarY,
                         x + PANEL_PADDING + fillWidth, progressBarY + progressBarHeight,
                         PROGRESS_BAR_PLANNING);

            graphics.drawString(mc.font, "Planning...", x + PANEL_PADDING, progressBarY + 10, 0xFFCCCCCC);
        }

        // Role badge
        String roleBadge = getRoleBadge(crew);
        int roleX = x + width - roleBadge.length() * 6 - PANEL_PADDING;
        graphics.drawString(mc.font, roleBadge, roleX, y + 3, getRoleColor(crew));
    }

    /**
     * Get animated thinking dots for planning state
     */
    private static String getThinkingDots() {
        int frame = (thinkingAnimationFrame / 10) % 4;
        return switch (frame) {
            case 0 -> ".";
            case 1 -> "..";
            case 2 -> "...";
            default -> "";
        };
    }

    /**
     * Get build progress text showing blocks placed
     */
    private static String getBuildProgressText(BuildStructureAction buildAction) {
        try {
            // Use reflection to access collaborative build info
            var collaborativeBuildField = BuildStructureAction.class.getDeclaredField("collaborativeBuild");
            collaborativeBuildField.setAccessible(true);
            var collaborativeBuild = collaborativeBuildField.get(buildAction);

            if (collaborativeBuild != null) {
                var getBlocksPlaced = collaborativeBuild.getClass().getMethod("getBlocksPlaced");
                var getTotalBlocks = collaborativeBuild.getClass().getMethod("getTotalBlocks");

                int placed = (Integer) getBlocksPlaced.invoke(collaborativeBuild);
                int total = (Integer) getTotalBlocks.invoke(collaborativeBuild);

                return placed + "/" + total + " blocks";
            }
        } catch (Exception e) {
            // Fallback to description parsing
            String desc = buildAction.getDescription();
            if (desc.contains("(") && desc.contains("/")) {
                int start = desc.lastIndexOf("(");
                int end = desc.lastIndexOf("/");
                if (start > 0 && end > start) {
                    String current = desc.substring(start + 1, end).trim();
                    String totalStr = desc.substring(end + 1, desc.length() - 1).trim();
                    try {
                        int currentNum = Integer.parseInt(current);
                        int totalNum = Integer.parseInt(totalStr);
                        return currentNum + "/" + totalNum + " blocks";
                    } catch (NumberFormatException ex) {
                        // Ignore parse errors
                    }
                }
            }
        }
        return null;
    }

    /**
     * Gets the status color for a crew member based on their current state
     */
    private static int getStatusColor(ForemanEntity crew) {
        if (crew.getActionExecutor().isPlanning()) {
            return STATUS_PLANNING_COLOR; // Orange - planning
        } else if (crew.getActionExecutor().isExecuting()) {
            String currentGoal = crew.getActionExecutor().getCurrentGoal();
            // Check if in combat (goal contains combat-related keywords)
            if (currentGoal != null && (currentGoal.toLowerCase().contains("attack") ||
                currentGoal.toLowerCase().contains("fight") || currentGoal.toLowerCase().contains("combat"))) {
                return STATUS_COMBAT_COLOR; // Red - combat
            }
            return STATUS_WORKING_COLOR; // Blue - working
        }
        return STATUS_IDLE_COLOR; // Green - idle
    }

    /**
     * Gets the status text for a crew member
     */
    private static String getStatusText(ForemanEntity crew) {
        if (crew.getActionExecutor().isPlanning()) {
            return "Planning...";
        }

        if (crew.getActionExecutor().isExecuting()) {
            String currentGoal = crew.getActionExecutor().getCurrentGoal();
            if (currentGoal != null && currentGoal.length() > 30) {
                return currentGoal.substring(0, 27) + "...";
            }
            return currentGoal != null ? currentGoal : "Working";
        }

        return "Idle";
    }

    /**
     * Gets the role badge text for a crew member
     */
    private static String getRoleBadge(ForemanEntity crew) {
        switch (crew.getRole()) {
            case FOREMAN:
                return "[F]";
            case WORKER:
                return "[W]";
            case SOLO:
            default:
                return "[S]";
        }
    }

    /**
     * Gets the color for a crew member's role badge
     */
    private static int getRoleColor(ForemanEntity crew) {
        switch (crew.getRole()) {
            case FOREMAN:
                return 0xFFFF9800; // Orange
            case WORKER:
                return 0xFF2196F3; // Blue
            case SOLO:
            default:
                return 0xFF888888; // Gray
        }
    }

    public static boolean handleKeyPress(int keyCode, int scanCode, int modifiers) {
        if (!isOpen || inputBox == null) return false;

        Minecraft mc = Minecraft.getInstance();
        
        // Escape key - close panel
        if (keyCode == 256) { // ESC
            toggle();
            return true;
        }
        
        // Enter key - send command
        if (keyCode == 257) {
            String command = inputBox.getValue().trim();
            if (!command.isEmpty()) {
                sendCommand(command);
                inputBox.setValue("");
                historyIndex = -1;
                playUISound(SoundEvents.UI_BUTTON_CLICK.value(), 0.3f); // Subtle click sound
            }
            return true;
        }

        // Arrow up - previous command
        if (keyCode == 265 && !commandHistory.isEmpty()) { // UP
            if (historyIndex < commandHistory.size() - 1) {
                historyIndex++;
                inputBox.setValue(commandHistory.get(commandHistory.size() - 1 - historyIndex));
            }
            return true;
        }

        // Arrow down - next command
        if (keyCode == 264) { // DOWN
            if (historyIndex > 0) {
                historyIndex--;
                inputBox.setValue(commandHistory.get(commandHistory.size() - 1 - historyIndex));
            } else if (historyIndex == 0) {
                historyIndex = -1;
                inputBox.setValue("");
            }
            return true;
        }

        // Backspace, Delete, Home, End, Left, Right - pass to input box
        if (keyCode == 259 || keyCode == 261 || keyCode == 268 || keyCode == 269 || 
            keyCode == 263 || keyCode == 262) {
            inputBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }

        return true; // Consume all keys to prevent game controls
    }

    public static boolean handleCharTyped(char codePoint, int modifiers) {
        if (isOpen && inputBox != null) {
            inputBox.charTyped(codePoint, modifiers);
            return true; // Consumed
        }
        return false;
    }

    public static void handleMouseClick(double mouseX, double mouseY, int button) {
        if (!isOpen) return;

        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int panelX = (int) (screenWidth - PANEL_WIDTH + slideOffset);

        // Check if clicking on quick action buttons
        int headerHeight = 35;
        int crewPanelY = headerHeight;
        int crewPanelCurrentHeight = crewPanelExpanded ? CREW_PANEL_HEIGHT : CREW_PANEL_COLLAPSED_HEIGHT;
        int buttonAreaY = crewPanelY + crewPanelCurrentHeight + 5;
        int buttonHeight = 30;

        if (mouseX >= panelX && mouseX <= panelX + PANEL_WIDTH &&
            mouseY >= buttonAreaY + 5 && mouseY <= buttonAreaY + 5 + 20) {
            // Determine which button was clicked
            int buttonCount = QUICK_BUTTONS.size();
            int buttonWidth = (PANEL_WIDTH - (PANEL_PADDING * 2) - (buttonCount - 1) * 3) / buttonCount;

            for (int i = 0; i < buttonCount; i++) {
                int btnX = panelX + PANEL_PADDING + i * (buttonWidth + 3);
                if (mouseX >= btnX && mouseX <= btnX + buttonWidth) {
                    QuickButton btn = QUICK_BUTTONS.get(i);
                    if (btn.command.equals("__VOICE_INPUT__")) {
                        // Special handling for voice input button
                        startVoiceInput();
                    } else {
                        sendCommand(btn.command);
                    }
                    playUISound(SoundEvents.UI_BUTTON_CLICK.value(), 0.4f);
                    return;
                }
            }
        }

        // Check if clicking on crew status panel header (for toggle expand/collapse)
        if (mouseX >= panelX && mouseX <= panelX + PANEL_WIDTH &&
            mouseY >= crewPanelY && mouseY <= crewPanelY + CREW_PANEL_COLLAPSED_HEIGHT) {
            crewPanelExpanded = !crewPanelExpanded;
            playUISound(SoundEvents.UI_BUTTON_CLICK.value(), 0.25f);
            return;
        }

        // Check if clicking on a crew member to target them
        if (crewPanelExpanded && mouseY >= crewPanelY + 25 && mouseY <= crewPanelY + crewPanelCurrentHeight) {
            var crewMembers = MineWrightMod.getCrewManager().getAllCrewMembers();
            int crewY = crewPanelY + 25;
            int crewIndex = 0;

            for (ForemanEntity crew : crewMembers) {
                if (mouseY >= crewY && mouseY <= crewY + 24) {
                    // Toggle targeting
                    if (crew.getEntityName().equals(targetedCrewMember)) {
                        targetedCrewMember = null; // Untarget
                        addSystemMessage("Removed " + crew.getEntityName() + " from command targeting");
                        playUISound(SoundEvents.UI_BUTTON_CLICK.value(), 0.2f);
                    } else {
                        targetedCrewMember = crew.getEntityName(); // Target
                        addSystemMessage("Orders will be sent to " + crew.getEntityName());
                        playUISound(SoundEvents.UI_BUTTON_CLICK.value(), 0.35f);
                    }
                    return;
                }
                crewY += 25;
                crewIndex++;
            }
        }

        if (inputBox != null) {
            int inputAreaY = screenHeight - 80;
            if (mouseY >= inputAreaY + 25 && mouseY <= inputAreaY + 45) {
                inputBox.setFocused(true);
            } else {
                inputBox.setFocused(false);
            }
        }
    }

    public static void handleMouseScroll(double scrollDelta) {
        if (!isOpen) return;

        // Smooth scrolling - adjust target instead of direct offset
        int scrollAmount = (int)(scrollDelta * 4 * MESSAGE_HEIGHT); // Increased multiplier for faster scroll
        targetScrollOffset = scrollOffset - scrollAmount;
        targetScrollOffset = Math.max(0, Math.min(targetScrollOffset, (float)maxScroll));
        scrollOffset = (int) targetScrollOffset; // Update immediately for responsiveness
    }

    private static void sendCommand(String command) {
        Minecraft mc = Minecraft.getInstance();

        commandHistory.add(command);
        if (commandHistory.size() > 50) {
            commandHistory.remove(0);
        }

        addUserMessage(command);

        // Trigger command flash animation for visual feedback
        commandFlashTimer = 20; // 20 ticks = 1 second
        commandFlashIntensity = 1.0f;

        if (command.toLowerCase().startsWith("spawn ")) {
            String name = command.substring(6).trim();
            if (name.isEmpty()) name = "Foreman";
            if (mc.player != null) {
                mc.player.connection.sendCommand("minewright spawn " + name);
                addSystemMessage("New crew member " + name + " reporting for duty!");
            }
            return;
        }

        List<String> targetCrew = parseTargetCrew(command);

        // If no explicit targets and a crew member is targeted, use that
        if (targetCrew.isEmpty() && targetedCrewMember != null) {
            targetCrew.add(targetedCrewMember);
        }

        if (targetCrew.isEmpty()) {
            var crew = MineWrightMod.getCrewManager().getAllCrewMembers();
            if (!crew.isEmpty()) {
                targetCrew.add(crew.iterator().next().getEntityName());
            } else {
                // No crew members available
                addSystemMessage("Job site is empty! Use 'spawn <name>' to hire some crew members.");
                return;
            }
        }

        // Send command to all targeted crew members
        if (mc.player != null) {
            for (String crewName : targetCrew) {
                mc.player.connection.sendCommand("minewright tell " + crewName + " " + command);
            }

            if (targetCrew.size() > 1) {
                addSystemMessage("→ " + String.join(", ", targetCrew) + ": " + command);
            } else {
                addSystemMessage("→ " + targetCrew.get(0) + ": " + command);
            }
        }
    }

    private static List<String> parseTargetCrew(String command) {
        List<String> targets = new ArrayList<>();
        String commandLower = command.toLowerCase();

        if (commandLower.startsWith("all crew ") || commandLower.startsWith("all ") ||
            commandLower.startsWith("everyone ") || commandLower.startsWith("everybody ")) {
            var allCrew = MineWrightMod.getCrewManager().getAllCrewMembers();
            for (ForemanEntity crew : allCrew) {
                targets.add(crew.getEntityName());
            }
            return targets;
        }

        var allCrew = MineWrightMod.getCrewManager().getAllCrewMembers();
        List<String> availableNames = new ArrayList<>();
        for (ForemanEntity crew : allCrew) {
            availableNames.add(crew.getEntityName().toLowerCase());
        }

        String[] parts = command.split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            String firstWord = trimmed.split(" ")[0].toLowerCase();

            if (availableNames.contains(firstWord)) {
                for (ForemanEntity crew : allCrew) {
                    if (crew.getEntityName().equalsIgnoreCase(firstWord)) {
                        targets.add(crew.getEntityName());
                        break;
                    }
                }
            }
        }

        return targets;
    }

    public static void tick() {
        if (isOpen && inputBox != null) {
            inputBox.tick();
            // Auto-focus input box when panel is open
            if (!inputBox.isFocused()) {
                inputBox.setFocused(true);
            }
        }

        // Update thinking animation frame
        thinkingAnimationFrame++;

        // Update command flash timer
        if (commandFlashTimer > 0) {
            commandFlashTimer--;
            commandFlashIntensity = commandFlashTimer / 20.0f;
        }

        // Reset crew hover state each tick (will be recalculated during render)
        hoveredCrewIndex = -1;
    }

    /**
     * Starts voice input for command recognition.
     */
    private static void startVoiceInput() {
        if (isVoiceInputActive) {
            addSystemMessage("Already listening...");
            return;
        }

        com.minewright.voice.VoiceManager voice = com.minewright.voice.VoiceManager.getInstance();

        if (!voice.isEnabled()) {
            addSystemMessage("Voice input is disabled. Enable it in config.");
            return;
        }

        isVoiceInputActive = true;
        addSystemMessage("Listening... Speak your command");

        try {
            voice.listenForCommand().thenAccept(transcribedText -> {
                isVoiceInputActive = false;
                if (transcribedText != null && !transcribedText.isEmpty()) {
                    // Run on main thread
                    net.minecraft.client.Minecraft.getInstance().execute(() -> {
                        addSystemMessage("Heard: \"" + transcribedText + "\"");
                        // Auto-send the command
                        sendCommand(transcribedText);
                    });
                } else {
                    net.minecraft.client.Minecraft.getInstance().execute(() -> {
                        addSystemMessage("No speech detected");
                    });
                }
            }).exceptionally(e -> {
                isVoiceInputActive = false;
                net.minecraft.client.Minecraft.getInstance().execute(() -> {
                    addSystemMessage("Voice error: " + e.getMessage());
                });
                return null;
            });
        } catch (Exception e) {
            isVoiceInputActive = false;
            addSystemMessage("Failed to start voice input: " + e.getMessage());
        }
    }
}
