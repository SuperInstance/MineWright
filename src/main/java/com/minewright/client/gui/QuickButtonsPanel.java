package com.minewright.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Renders quick action buttons with hover effects and tooltips.
 */
public class QuickButtonsPanel {
    private static final int PANEL_PADDING = 6;

    /**
     * Renders quick action buttons with hover effects and tooltips
     */
    public static void renderQuickActionButtons(GuiGraphics graphics, Minecraft mc, int x, int y, int width, int height,
                                                 InputHandler.QuickButton[] quickButtons, InputHandler inputHandler) {
        int buttonCount = quickButtons.length;
        int buttonWidth = (width - (PANEL_PADDING * 2) - (buttonCount - 1) * 3) / buttonCount;
        int buttonHeight = 20;
        int buttonY = y + 5;

        // Get mouse position for hover detection
        double mouseX = mc.mouseHandler.xpos();
        double mouseY = mc.mouseHandler.ypos();

        // Update hover state in input handler
        inputHandler.updateHoverState(mouseX, mouseY, x, width, y, quickButtons);

        for (int i = 0; i < buttonCount; i++) {
            InputHandler.QuickButton button = quickButtons[i];
            int buttonX = x + PANEL_PADDING + i * (buttonWidth + 3);

            // Get hover state from input handler
            InputHandler.HoverState hoverState = inputHandler.getButtonHoverState(i);

            if (hoverState.isHovered) {
                // Draw hover effect (lighter background)
                int hoverColor = 0x40FFFFFF;
                graphics.fill(buttonX, buttonY, buttonX + buttonWidth, buttonY + buttonHeight, hoverColor);

                // Draw tooltip after delay
                if (hoverState.hoverDuration > 500) {
                    renderTooltip(graphics, mc, button.tooltip, buttonX, buttonY, buttonWidth, buttonHeight, x, width);
                }
            }

            // Apply scale effect to button text
            float scale = hoverState.isHovered ? hoverState.scale : 1.0f;
            int scaledWidth = (int)(buttonWidth * scale);
            int scaledHeight = (int)(buttonHeight * scale);
            int scaledX = buttonX + (buttonWidth - scaledWidth) / 2;
            int scaledY = buttonY + (buttonHeight - scaledHeight) / 2;

            // Draw button background
            int buttonBgColor = hoverState.isHovered ? (button.color | 0xFF000000) : (button.color & 0x80FFFFFF);
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
    }

    /**
     * Renders a tooltip for a button
     */
    private static void renderTooltip(GuiGraphics graphics, Minecraft mc, String tooltip,
                                      int buttonX, int buttonY, int buttonWidth, int buttonHeight,
                                      int panelX, int panelWidth) {
        int tooltipY = buttonY + buttonHeight + 5;
        int tooltipWidth = mc.font.width(tooltip) + 10;
        int tooltipX = Math.max(panelX, Math.min(buttonX + buttonWidth / 2 - tooltipWidth / 2, panelX + panelWidth - tooltipWidth));

        // Tooltip background
        graphics.fill(tooltipX, tooltipY, tooltipX + tooltipWidth, tooltipY + 14, 0xD0000000);
        graphics.fill(tooltipX, tooltipY, tooltipX + tooltipWidth, tooltipY + 1, 0xFF888888);
        graphics.fill(tooltipX, tooltipY + 13, tooltipX + tooltipWidth, tooltipY + 14, 0xFF888888);
        graphics.fill(tooltipX, tooltipY, tooltipX + 1, tooltipY + 14, 0xFF888888);
        graphics.fill(tooltipX + tooltipWidth - 1, tooltipY, tooltipX + tooltipWidth, tooltipY + 14, 0xFF888888);

        // Tooltip text
        graphics.drawString(mc.font, tooltip, tooltipX + 5, tooltipY + 3, 0xFFFFFFFF);
    }
}
