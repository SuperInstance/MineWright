package com.minewright.client.gui;

import com.minewright.voice.VoiceManager;

import java.util.function.Consumer;

/**
 * Handles voice input integration for the Foreman Office GUI.
 * Manages voice recognition state and provides callbacks for voice commands.
 */
public class VoiceIntegrationPanel {
    private boolean isVoiceInputActive = false;
    private final MessagePanel messagePanel;
    private final Consumer<String> commandSender;

    public VoiceIntegrationPanel(MessagePanel messagePanel, Consumer<String> commandSender) {
        this.messagePanel = messagePanel;
        this.commandSender = commandSender;
    }

    /**
     * Start voice input for command recognition
     */
    public void startVoiceInput() {
        if (isVoiceInputActive) {
            messagePanel.addSystemMessage("Already listening...");
            return;
        }

        VoiceManager voice = VoiceManager.getInstance();

        if (!voice.isEnabled()) {
            messagePanel.addSystemMessage("Voice input is disabled. Enable it in config.");
            return;
        }

        isVoiceInputActive = true;
        messagePanel.addSystemMessage("Listening... Speak your command");

        try {
            voice.listenForCommand().thenAccept(transcribedText -> {
                isVoiceInputActive = false;
                if (transcribedText != null && !transcribedText.isEmpty()) {
                    // Run on main thread
                    net.minecraft.client.Minecraft.getInstance().execute(() -> {
                        messagePanel.addSystemMessage("Heard: \"" + transcribedText + "\"");
                        // Send the command through the callback
                        commandSender.accept(transcribedText);
                    });
                } else {
                    net.minecraft.client.Minecraft.getInstance().execute(() -> {
                        messagePanel.addSystemMessage("No speech detected");
                    });
                }
            }).exceptionally(e -> {
                isVoiceInputActive = false;
                net.minecraft.client.Minecraft.getInstance().execute(() -> {
                    messagePanel.addSystemMessage("Voice error: " + e.getMessage());
                });
                return null;
            });
        } catch (Exception e) {
            isVoiceInputActive = false;
            messagePanel.addSystemMessage("Failed to start voice input: " + e.getMessage());
        }
    }

    /**
     * Stop voice input if currently active
     */
    public void stopVoiceInput() {
        if (isVoiceInputActive) {
            isVoiceInputActive = false;
            messagePanel.addSystemMessage("Voice input stopped");
        }
    }

    /**
     * Check if voice input is currently active
     */
    public boolean isVoiceInputActive() {
        return isVoiceInputActive;
    }

    /**
     * Clean up resources
     */
    public void cleanup() {
        stopVoiceInput();
    }
}
