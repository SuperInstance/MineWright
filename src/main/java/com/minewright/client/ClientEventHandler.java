package com.minewright.client;

import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;
import com.minewright.MineWrightMod;
import com.minewright.voice.VoiceManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.NarratorStatus;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles client-side events, including disabling the narrator and checking key presses
 */
@Mod.EventBusSubscriber(modid = MineWrightMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEventHandler {
    private static final Logger LOGGER = TestLogger.getLogger(ClientEventHandler.class);

    private static boolean narratorDisabled = false;
    private static boolean voiceListeningActive = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();

        if (!narratorDisabled && mc.options != null) {
            mc.options.narrator().set(NarratorStatus.OFF);
            mc.options.save();
            narratorDisabled = true;
        }

        if (KeyBindings.TOGGLE_GUI != null && KeyBindings.TOGGLE_GUI.consumeClick()) {
            ForemanOfficeGUI.toggle();
        }

        // Handle push-to-talk voice input
        if (KeyBindings.VOICE_PUSH_TO_TALK != null) {
            boolean isPressed = KeyBindings.VOICE_PUSH_TO_TALK.isDown();

            if (isPressed && !voiceListeningActive) {
                // Key just pressed - start listening
                startVoiceListening();
                voiceListeningActive = true;
            } else if (!isPressed && voiceListeningActive) {
                // Key just released - stop listening
                stopVoiceListening();
                voiceListeningActive = false;
            }
        }
    }

    /**
     * Starts listening for voice input when push-to-talk is pressed.
     */
    private static void startVoiceListening() {
        VoiceManager voice = VoiceManager.getInstance();

        if (!voice.isEnabled()) {
            LOGGER.debug("Voice system is disabled - ignoring push-to-talk");
            return;
        }

        try {
            LOGGER.info("Push-to-talk activated - starting voice input");
            voice.listenForCommand().thenAccept(transcribedText -> {
                LOGGER.info("Voice input transcribed: {}", transcribedText);

                // Send to the most recently used ForemanOfficeGUI or default GUI
                // This integration point will need to be connected to the active Foreman entity
                ForemanOfficeGUI.addSystemMessage("[VOICE] Input: " + transcribedText);

                // Note: Voice command routing to Foreman entity will be implemented when
                // the active entity selection system is fully integrated.
            }).exceptionally(e -> {
                LOGGER.error("Voice input failed", e);
                ForemanOfficeGUI.addSystemMessage("[VOICE] Error: Failed to capture voice input");
                return null;
            });
        } catch (Exception e) {
            LOGGER.error("Failed to start voice listening", e);
        }
    }

    /**
     * Stops listening for voice input when push-to-talk is released.
     */
    private static void stopVoiceListening() {
        VoiceManager voice = VoiceManager.getInstance();

        if (!voice.isEnabled()) {
            return;
        }

        LOGGER.info("Push-to-talk released - stopping voice input");
        voice.stopAll();
    }
}
