package com.minewright.client.gui;

import com.minewright.MineWrightMod;
import com.minewright.entity.ForemanEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles all user input for the Foreman Office GUI.
 * Manages keyboard input, mouse clicks, scrolling, and command history.
 */
public class InputHandler {
    private final List<String> commandHistory = new ArrayList<>();
    private int historyIndex = -1;
    private EditBox inputBox;
    private final MessagePanel messagePanel;

    // Crew panel state
    private boolean crewPanelExpanded = true;
    private String targetedCrewMember = null;

    // Hover state tracking
    private int hoveredCrewIndex = -1;
    private int hoveredButtonIndex = -1;
    private float hoverScale = 1.0f;
    private long hoverStartTime = 0;

    // Command flash animation
    private int commandFlashTimer = 0;
    private float commandFlashIntensity = 0.0f;

    public InputHandler(MessagePanel messagePanel) {
        this.messagePanel = messagePanel;
    }

    /**
     * Initialize the input box
     */
    public void initializeInputBox(Minecraft mc, int panelWidth) {
        if (inputBox == null) {
            inputBox = new EditBox(mc.font, 0, 0, panelWidth - 20, 20,
                Component.literal("Command"));
            inputBox.setMaxLength(256);
            inputBox.setHint(Component.literal("Tell Foreman what to do..."));
            inputBox.setFocused(true);
        }
    }

    /**
     * Clean up input box
     */
    public void cleanupInputBox() {
        inputBox = null;
    }

    /**
     * Get the input box instance
     */
    public EditBox getInputBox() {
        return inputBox;
    }

    /**
     * Handle keyboard input
     */
    public boolean handleKeyPress(int keyCode, int scanCode, int modifiers) {
        if (inputBox == null) return false;

        // Escape key - close panel
        if (keyCode == 256) { // ESC
            return true; // Signal to close
        }

        // Enter key - send command
        if (keyCode == 257) {
            String command = inputBox.getValue().trim();
            if (!command.isEmpty()) {
                sendCommand(command);
                inputBox.setValue("");
                historyIndex = -1;
                playUISound(SoundEvents.UI_BUTTON_CLICK.value(), 0.3f);
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

    /**
     * Handle character typing
     */
    public boolean handleCharTyped(char codePoint, int modifiers) {
        if (inputBox != null) {
            inputBox.charTyped(codePoint, modifiers);
            return true;
        }
        return false;
    }

    /**
     * Handle mouse clicks
     */
    public void handleMouseClick(double mouseX, double mouseY, int button, int panelX, int panelWidth,
                                 int screenWidth, int screenHeight, QuickButton[] quickButtons) {
        int headerHeight = 35;
        int crewPanelY = headerHeight;
        int crewPanelHeight = crewPanelExpanded ? 180 : 30;
        int buttonAreaY = crewPanelY + crewPanelHeight + 5;

        // Check if clicking on quick action buttons
        if (mouseX >= panelX && mouseX <= panelX + panelWidth &&
            mouseY >= buttonAreaY + 5 && mouseY <= buttonAreaY + 5 + 20) {
            int buttonCount = quickButtons.length;
            int buttonWidth = (panelWidth - 12 - (buttonCount - 1) * 3) / buttonCount;

            for (int i = 0; i < buttonCount; i++) {
                int btnX = panelX + 6 + i * (buttonWidth + 3);
                if (mouseX >= btnX && mouseX <= btnX + buttonWidth) {
                    QuickButton btn = quickButtons[i];
                    if (btn.command.equals("__VOICE_INPUT__")) {
                        // Special handling for voice input button
                        // Signal to start voice input
                    } else {
                        sendCommand(btn.command);
                    }
                    playUISound(SoundEvents.UI_BUTTON_CLICK.value(), 0.4f);
                    return;
                }
            }
        }

        // Check if clicking on crew status panel header (for toggle expand/collapse)
        if (mouseX >= panelX && mouseX <= panelX + panelWidth &&
            mouseY >= crewPanelY && mouseY <= crewPanelY + 30) {
            crewPanelExpanded = !crewPanelExpanded;
            playUISound(SoundEvents.UI_BUTTON_CLICK.value(), 0.25f);
            return;
        }

        // Check if clicking on a crew member to target them
        if (crewPanelExpanded && mouseY >= crewPanelY + 25 && mouseY <= crewPanelY + crewPanelHeight) {
            var crewMembers = MineWrightMod.getCrewManager().getAllCrewMembers();
            int crewY = crewPanelY + 25;

            for (ForemanEntity crew : crewMembers) {
                if (mouseY >= crewY && mouseY <= crewY + 24) {
                    // Toggle targeting
                    if (crew.getEntityName().equals(targetedCrewMember)) {
                        targetedCrewMember = null; // Untarget
                        messagePanel.addSystemMessage("Removed " + crew.getEntityName() + " from command targeting");
                        playUISound(SoundEvents.UI_BUTTON_CLICK.value(), 0.2f);
                    } else {
                        targetedCrewMember = crew.getEntityName(); // Target
                        messagePanel.addSystemMessage("Orders will be sent to " + crew.getEntityName());
                        playUISound(SoundEvents.UI_BUTTON_CLICK.value(), 0.35f);
                    }
                    return;
                }
                crewY += 25;
            }
        }

        // Handle input box focus
        if (inputBox != null) {
            int inputAreaY = screenHeight - 80;
            if (mouseY >= inputAreaY + 25 && mouseY <= inputAreaY + 45) {
                inputBox.setFocused(true);
            } else {
                inputBox.setFocused(false);
            }
        }
    }

    /**
     * Handle mouse scroll
     */
    public void handleMouseScroll(double scrollDelta) {
        messagePanel.handleScroll(scrollDelta);
    }

    /**
     * Send a command to crew members
     */
    public void sendCommand(String command) {
        Minecraft mc = Minecraft.getInstance();

        commandHistory.add(command);
        if (commandHistory.size() > 50) {
            commandHistory.remove(0);
        }

        messagePanel.addUserMessage(command);

        // Trigger command flash animation for visual feedback
        commandFlashTimer = 20;
        commandFlashIntensity = 1.0f;

        if (command.toLowerCase().startsWith("spawn ")) {
            String name = command.substring(6).trim();
            if (name.isEmpty()) name = "Foreman";
            if (mc.player != null) {
                mc.player.connection.sendCommand("minewright spawn " + name);
                messagePanel.addSystemMessage("New crew member " + name + " reporting for duty!");
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
                messagePanel.addSystemMessage("Job site is empty! Use 'spawn <name>' to hire some crew members.");
                return;
            }
        }

        // Send command to all targeted crew members
        if (mc.player != null) {
            for (String crewName : targetCrew) {
                mc.player.connection.sendCommand("minewright tell " + crewName + " " + command);
            }

            if (targetCrew.size() > 1) {
                messagePanel.addSystemMessage("→ " + String.join(", ", targetCrew) + ": " + command);
            } else {
                messagePanel.addSystemMessage("→ " + targetCrew.get(0) + ": " + command);
            }
        }
    }

    /**
     * Parse target crew from command
     */
    private List<String> parseTargetCrew(String command) {
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

    /**
     * Update hover states
     */
    public void updateHoverState(double mouseX, double mouseY, int panelX, int panelWidth,
                                  int buttonAreaY, QuickButton[] quickButtons) {
        // Reset hover states
        hoveredButtonIndex = -1;

        int buttonCount = quickButtons.length;
        int buttonWidth = (panelWidth - 12 - (buttonCount - 1) * 3) / buttonCount;
        int buttonY = buttonAreaY + 5;

        for (int i = 0; i < buttonCount; i++) {
            int buttonX = panelX + 6 + i * (buttonWidth + 3);

            boolean isHovered = mouseX >= buttonX && mouseX <= buttonX + buttonWidth &&
                              mouseY >= buttonY && mouseY <= buttonY + 20;

            if (isHovered) {
                hoveredButtonIndex = i;

                if (hoverStartTime == 0) {
                    hoverStartTime = System.currentTimeMillis();
                }
                long hoverDuration = System.currentTimeMillis() - hoverStartTime;
                float targetScale = 1.05f;
                hoverScale = 1.0f + (targetScale - 1.0f) * Math.min(1.0f, hoverDuration / 100.0f);
            }
        }

        if (hoveredButtonIndex == -1) {
            hoverStartTime = 0;
            hoverScale = 1.0f;
        }
    }

    /**
     * Get hover state for button
     */
    public HoverState getButtonHoverState(int buttonIndex) {
        boolean isHovered = hoveredButtonIndex == buttonIndex;
        long hoverDuration = hoverStartTime > 0 ? System.currentTimeMillis() - hoverStartTime : 0;
        return new HoverState(isHovered, hoverScale, hoverDuration);
    }

    /**
     * Get crew panel state
     */
    public boolean isCrewPanelExpanded() {
        return crewPanelExpanded;
    }

    /**
     * Get targeted crew member
     */
    public String getTargetedCrewMember() {
        return targetedCrewMember;
    }

    /**
     * Update animation state
     */
    public void tick() {
        if (inputBox != null) {
            inputBox.tick();
            // Auto-focus input box
            if (!inputBox.isFocused()) {
                inputBox.setFocused(true);
            }
        }

        // Update command flash timer
        if (commandFlashTimer > 0) {
            commandFlashTimer--;
            commandFlashIntensity = commandFlashTimer / 20.0f;
        }

        // Reset crew hover state each tick
        hoveredCrewIndex = -1;
    }

    /**
     * Play a UI sound with controlled volume
     */
    private void playUISound(net.minecraft.sounds.SoundEvent sound, float volume) {
        Minecraft mc = Minecraft.getInstance();
        var soundManager = mc.getSoundManager();
        if (soundManager != null) {
            soundManager.play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(sound, 1.0f, volume));
        }
    }

    /**
     * Hover state data class
     */
    public static class HoverState {
        public final boolean isHovered;
        public final float scale;
        public final long hoverDuration;

        public HoverState(boolean isHovered, float scale, long hoverDuration) {
            this.isHovered = isHovered;
            this.scale = scale;
            this.hoverDuration = hoverDuration;
        }
    }

    /**
     * Quick button definition
     */
    public static class QuickButton {
        public final String label;
        public final String command;
        public final String tooltip;
        public final int color;

        public QuickButton(String label, String command, String tooltip, int color) {
            this.label = label;
            this.command = command;
            this.tooltip = tooltip;
            this.color = color;
        }
    }
}
