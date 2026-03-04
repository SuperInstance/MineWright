package com.minewright.client.gui;

import com.minewright.MineWrightMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages and renders the message history panel.
 * Handles message storage, scrolling, text wrapping, and bubble rendering.
 */
public class MessagePanel {
    private static final int MAX_MESSAGES = 500;
    private static final int MESSAGE_HEIGHT = 12;

    // Message bubble colors
    private static final int USER_BUBBLE_COLOR = 0xC04CAF50; // Green bubble for user
    private static final int CREW_BUBBLE_COLOR = 0xC02196F3; // Blue bubble for Crew
    private static final int SYSTEM_BUBBLE_COLOR = 0xC0FF9800; // Orange bubble for system

    private final List<ChatMessage> messages = new ArrayList<>();
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private float targetScrollOffset = 0.0f;

    /**
     * Chat message data structure
     */
    public static class ChatMessage {
        public final String sender;
        public final String text;
        public final int bubbleColor;
        public final boolean isUser;

        public ChatMessage(String sender, String text, int bubbleColor, boolean isUser) {
            this.sender = sender;
            this.text = text;
            this.bubbleColor = bubbleColor;
            this.isUser = isUser;
        }
    }

    /**
     * Add a message to the chat history
     */
    public void addMessage(String sender, String text, int bubbleColor, boolean isUser) {
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
    public void addUserMessage(String text) {
        addMessage("You", text, USER_BUBBLE_COLOR, true);
    }

    /**
     * Add a Crew response to the history
     */
    public void addCrewMessage(String crewName, String text) {
        addMessage(crewName, text, CREW_BUBBLE_COLOR, false);
    }

    /**
     * Add a system message to the history
     */
    public void addSystemMessage(String text) {
        addMessage("System", text, SYSTEM_BUBBLE_COLOR, false);
    }

    /**
     * Clear all messages
     */
    public void clear() {
        messages.clear();
        scrollOffset = 0;
        maxScroll = 0;
    }

    /**
     * Get message count
     */
    public int getMessageCount() {
        return messages.size();
    }

    /**
     * Get messages list (read-only)
     */
    public List<ChatMessage> getMessages() {
        return new ArrayList<>(messages);
    }

    /**
     * Renders the message history area
     */
    public void renderMessages(GuiGraphics graphics, Minecraft mc, int x, int y, int width, int height,
                               GUIRenderer renderer, int panelPadding) {
        // Calculate total message height
        int totalMessageHeight = 0;
        for (ChatMessage msg : messages) {
            int maxBubbleWidth = width - (panelPadding * 3);
            int bubbleHeight = calculateBubbleHeight(mc.font, msg.text, maxBubbleWidth - 10);
            totalMessageHeight += bubbleHeight + 5 + 12; // message + spacing + name
        }
        maxScroll = Math.max(0, totalMessageHeight - height);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        // Animate smooth scrolling
        float scrollLerp = 0.15f;
        targetScrollOffset = scrollOffset;
        float currentScrollFloat = scrollOffset;
        float smoothedScroll = currentScrollFloat + (targetScrollOffset - currentScrollFloat) * scrollLerp;
        scrollOffset = (int) smoothedScroll;

        int messageAreaBottom = y + height;

        // Clip rendering to message area
        graphics.enableScissor(x, y, x + width, messageAreaBottom);

        if (messages.isEmpty()) {
            graphics.drawString(mc.font, "§7No messages yet...",
                x + panelPadding, y + 5, 0xFF666666);
            graphics.drawString(mc.font, "§7Type a command below!",
                x + panelPadding, y + 17, 0xFF555555);
        } else {
            // Check if any crew is planning and show typing indicator at bottom
            boolean isAnyPlanning = MineWrightMod.getCrewManager().getAllCrewMembers().stream()
                .anyMatch(crew -> crew.getActionExecutor() != null && crew.getActionExecutor().isPlanning());

            if (isAnyPlanning) {
                renderTypingIndicator(graphics, mc, x, messageAreaBottom, panelPadding, renderer);
            }

            int currentY = messageAreaBottom - 5; // Start from bottom

            for (int i = messages.size() - 1; i >= 0; i--) {
                ChatMessage msg = messages.get(i);

                int maxBubbleWidth = width - (panelPadding * 3);
                List<String> wrappedLines = wrapText(mc.font, msg.text, maxBubbleWidth - 10);
                int bubbleWidth = calculateBubbleWidth(mc.font, msg.text, maxBubbleWidth - 10);
                int bubbleHeight = calculateBubbleHeight(mc.font, msg.text, maxBubbleWidth - 10);

                int msgY = currentY - bubbleHeight + scrollOffset;

                if (msgY + bubbleHeight < y - 20 || msgY > messageAreaBottom + 20) {
                    currentY -= bubbleHeight + 5;
                    continue;
                }

                // Render message bubble
                renderMessageBubble(graphics, mc.font, msg, wrappedLines, bubbleWidth, bubbleHeight,
                    x + width, msgY, panelPadding);

                currentY -= bubbleHeight + 5 + 12;
            }
        }

        graphics.disableScissor();

        // Render scroll bar if needed
        if (maxScroll > 0) {
            renderScrollBar(graphics, x, y, width, height);
        }
    }

    /**
     * Renders typing indicator for planning crew
     */
    private void renderTypingIndicator(GuiGraphics graphics, Minecraft mc, int x, int bottomY,
                                       int panelPadding, GUIRenderer renderer) {
        int typingIndicatorY = bottomY - 15;
        String typingText = "§7Crew is thinking" + renderer.getThinkingDots();
        int textWidth = mc.font.width(typingText);
        graphics.drawString(mc.font, typingText, x + panelPadding, typingIndicatorY, 0xFFAAAAAA);

        // Animated progress bar for typing indicator
        int typingBarWidth = textWidth + 10;
        int frame = renderer.getThinkingAnimationFrame();
        float typingProgress = ((frame % 30) / 30.0f);
        int typingFillWidth = (int)(typingBarWidth * typingProgress);
        graphics.fill(x + panelPadding, typingIndicatorY + 12,
                    x + panelPadding + typingFillWidth, typingIndicatorY + 14,
                    0x802196F3);
    }

    /**
     * Renders a single message bubble
     */
    private void renderMessageBubble(GuiGraphics graphics, Font font, ChatMessage msg,
                                     List<String> wrappedLines, int bubbleWidth, int bubbleHeight,
                                     int screenWidth, int msgY, int panelPadding) {
        if (msg.isUser) {
            int bubbleX = screenWidth - bubbleWidth - panelPadding - 5;

            // Draw bubble background with gradient for alpha support
            graphics.fillGradient(bubbleX - 3, msgY - 3, bubbleX + bubbleWidth + 3, msgY + bubbleHeight,
                msg.bubbleColor, msg.bubbleColor);

            // Draw sender name (small, above bubble)
            graphics.drawString(font, "§7" + msg.sender, bubbleX, msgY - 12, 0xFFCCCCCC);

            // Draw each line of the message
            int lineY = msgY + 5;
            for (String line : wrappedLines) {
                graphics.drawString(font, line, bubbleX + 5, lineY, 0xFFFFFFFF);
                lineY += MESSAGE_HEIGHT;
            }
        } else {
            int bubbleX = panelPadding + 6;

            // Draw bubble background with gradient for alpha support
            graphics.fillGradient(bubbleX - 3, msgY - 3, bubbleX + bubbleWidth + 3, msgY + bubbleHeight,
                msg.bubbleColor, msg.bubbleColor);

            // Draw sender name (small, above bubble)
            graphics.drawString(font, "§l" + msg.sender, bubbleX, msgY - 12, 0xFFFFFFFF);

            // Draw each line of the message
            int lineY = msgY + 5;
            for (String line : wrappedLines) {
                graphics.drawString(font, line, bubbleX + 5, lineY, 0xFFFFFFFF);
                lineY += MESSAGE_HEIGHT;
            }
        }
    }

    /**
     * Renders scroll bar
     */
    private void renderScrollBar(GuiGraphics graphics, int x, int y, int width, int height) {
        int scrollBarHeight = Math.max(20, (height * height) / (maxScroll + height));
        int scrollBarY = y + (int)((height - scrollBarHeight) * (1.0f - (float)scrollOffset / maxScroll));
        graphics.fill(x + width - 4, scrollBarY, x + width - 2, scrollBarY + scrollBarHeight, 0xFF888888);
    }

    /**
     * Proper word wrap for text that splits at word boundaries
     */
    private List<String> wrapText(Font font, String text, int maxWidth) {
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
     */
    private String breakLongWord(Font font, String word, int maxWidth) {
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
     */
    private int calculateBubbleHeight(Font font, String text, int maxWidth) {
        List<String> lines = wrapText(font, text, maxWidth);
        int lineHeight = MESSAGE_HEIGHT;
        int padding = 10;
        return (lines.size() * lineHeight) + padding;
    }

    /**
     * Calculate the width needed for a message bubble based on wrapped text
     */
    private int calculateBubbleWidth(Font font, String text, int maxWidth) {
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
     * Handle mouse scroll for messages
     */
    public void handleScroll(double scrollDelta) {
        // Smooth scrolling - adjust target instead of direct offset
        int scrollAmount = (int)(scrollDelta * 4 * MESSAGE_HEIGHT);
        targetScrollOffset = scrollOffset - scrollAmount;
        targetScrollOffset = Math.max(0, Math.min(targetScrollOffset, (float)maxScroll));
        scrollOffset = (int) targetScrollOffset;
    }

    /**
     * Get current scroll offset
     */
    public int getScrollOffset() {
        return scrollOffset;
    }

    /**
     * Get maximum scroll offset
     */
    public int getMaxScroll() {
        return maxScroll;
    }
}
