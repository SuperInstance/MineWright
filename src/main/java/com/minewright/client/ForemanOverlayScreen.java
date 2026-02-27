package com.minewright.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Invisible overlay screen that captures input for the MineWright GUI
 * This prevents game controls from activating while typing
 */
public class ForemanOverlayScreen extends Screen {

    public ForemanOverlayScreen() {
        super(Component.literal("MineWright"));
    }

    @Override
    public boolean isPauseScreen() {
        return false; // Don't pause the game
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Don't render anything - the ForemanOfficeGUI renders via overlay
        // This screen is just to capture input
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // K key to close
        if (keyCode == 75 && !hasShiftDown() && !hasControlDown() && !hasAltDown()) { // K
            ForemanOfficeGUI.toggle();
            if (minecraft != null) {
                minecraft.setScreen(null);
            }
            return true;
        }

        return ForemanOfficeGUI.handleKeyPress(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        // Pass character input to ForemanOfficeGUI
        return ForemanOfficeGUI.handleCharTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        ForemanOfficeGUI.handleMouseClick(mouseX, mouseY, button);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        ForemanOfficeGUI.handleMouseScroll(scrollDelta);
        return true;
    }

    @Override
    public void removed() {
        // Clean up when screen is closed
        if (ForemanOfficeGUI.isOpen()) {
            ForemanOfficeGUI.toggle();
        }
    }
}

