package com.minewright.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.minewright.MineWrightMod;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = MineWrightMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class KeyBindings {

    public static final String KEY_CATEGORY = "key.categories.minewright";

    public static KeyMapping TOGGLE_GUI;
    public static KeyMapping VOICE_PUSH_TO_TALK;

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        TOGGLE_GUI = new KeyMapping(
            "key.minewright.toggle_gui",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K, // K key
            KEY_CATEGORY
        );

        VOICE_PUSH_TO_TALK = new KeyMapping(
            "key.minewright.voice_push_to_talk",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V, // V key
            KEY_CATEGORY
        );

        event.register(TOGGLE_GUI);
        event.register(VOICE_PUSH_TO_TALK);
    }
}

