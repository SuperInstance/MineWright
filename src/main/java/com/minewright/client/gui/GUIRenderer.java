package com.minewright.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.minewright.MineWrightMod;
import com.minewright.action.ActionExecutor;
import com.minewright.action.actions.BuildStructureAction;
import com.minewright.entity.ForemanEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;

import java.util.List;

/**
 * Handles all rendering logic for the Foreman Office GUI.
 * This class is responsible for drawing the panel, buttons, messages, and crew status.
 */
public class GUIRenderer {
    // Color constants
    public static final int BACKGROUND_COLOR = 0x15202020;
    public static final int BORDER_COLOR = 0x40404040;
    public static final int HEADER_COLOR = 0x25252525;
    public static final int TEXT_COLOR = 0xFFFFFFFF;
    public static final int FOCUS_BORDER_COLOR = 0xFF2196F3;

    // Status colors
    public static final int STATUS_IDLE_COLOR = 0xFF4CAF50;
    public static final int STATUS_WORKING_COLOR = 0xFF2196F3;
    public static final int STATUS_PLANNING_COLOR = 0xFFFF9800;
    public static final int STATUS_COMBAT_COLOR = 0xFFF44336;

    // Progress indicator colors
    public static final int PROGRESS_BAR_BG = 0x80000000;
    public static final int PROGRESS_BAR_FILL = 0xFF4CAF50;
    public static final int PROGRESS_BAR_PLANNING = 0xFFFF9800;

    private int thinkingAnimationFrame = 0;

    /**
     * Update animation state for rendering
     */
    public void tick() {
        thinkingAnimationFrame++;
    }

    public int getThinkingAnimationFrame() {
        return thinkingAnimationFrame;
    }

    /**
     * Get animated thinking dots for planning state
     */
    public String getThinkingDots() {
        int frame = (thinkingAnimationFrame / 10) % 4;
        return switch (frame) {
            case 0 -> ".";
            case 1 -> "..";
            case 2 -> "...";
            default -> "";
        };
    }

    /**
     * Renders the main panel background
     */
    public void renderPanelBackground(GuiGraphics graphics, int x, int y, int width, int height, float fadeAlpha) {
        int fadedBackground = (BACKGROUND_COLOR & 0x00FFFFFF) | (((int)((BACKGROUND_COLOR >> 24) * fadeAlpha)) << 24);
        int fadedBorder = (BORDER_COLOR & 0x00FFFFFF) | (((int)((BORDER_COLOR >> 24) * fadeAlpha)) << 24);
        int fadedHeader = (HEADER_COLOR & 0x00FFFFFF) | (((int)((HEADER_COLOR >> 24) * fadeAlpha)) << 24);

        graphics.fillGradient(x, y, x + width, y + height, fadedBackground, fadedBackground);
        graphics.fillGradient(x - 2, y, x, y + height, fadedBorder, fadedBorder);

        int headerHeight = 35;
        graphics.fillGradient(x, y, x + width, y + headerHeight, fadedHeader, fadedHeader);
    }

    /**
     * Renders the panel header
     */
    public static int renderHeader(GuiGraphics graphics, Font font, int x, int y, int panelPadding) {
        int headerHeight = 35;
        graphics.drawString(font, "§lMineWright", x + panelPadding, y + 8, TEXT_COLOR);
        graphics.drawString(font, "§7Press K to close", x + panelPadding, y + 20, 0xFF888888);
        return headerHeight;
    }

    /**
     * Renders the crew status panel showing all active crew members
     */
    public void renderCrewStatusPanel(GuiGraphics graphics, Minecraft mc, int x, int y, int width, int height,
                                       boolean crewPanelExpanded, int crewPanelHeight, int crewPanelCollapsedHeight,
                                       int panelPadding) {
        // Panel background
        graphics.fillGradient(x, y, x + width, y + height, 0x20181818, 0x20181818);
        graphics.fillGradient(x, y, x + width, y + 1, 0x40404040, 0x40404040); // Top border
        graphics.fillGradient(x, y + height - 1, x + width, y + height, 0x40404040, 0x40404040); // Bottom border

        // Panel header (always visible)
        int headerY = y + 5;
        graphics.drawString(mc.font, "§lCrew Status", x + panelPadding, headerY, TEXT_COLOR);

        // Collapse/expand indicator
        String expandIndicator = crewPanelExpanded ? "▼" : "▶";
        graphics.drawString(mc.font, "§7" + expandIndicator, x + width - 20, headerY, 0xFF888888);

        // Crew count
        var crewMembers = MineWrightMod.getCrewManager().getAllCrewMembers();
        int crewCount = crewMembers.size();
        graphics.drawString(mc.font, "§7" + crewCount + " member" + (crewCount != 1 ? "s" : ""),
            x + panelPadding + 90, headerY, 0xFF888888);

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
                    x + panelPadding, crewY, 0xFF666666);
            }
        }
    }

    /**
     * Renders a single crew member entry in the status panel
     */
    private void renderCrewMember(GuiGraphics graphics, Minecraft mc, ForemanEntity crew, int x, int y, int width, int index) {
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

        String crewName = crew.getEntityName();

        // Status indicator dot
        int statusColor = getStatusColor(crew);
        int dotX = x + 6;
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
        int statusX = x + 6;
        graphics.drawString(mc.font, "§7" + statusText, statusX, y + 15, 0xFFCCCCCC);

        // Animated thinking dots
        if (isPlanning) {
            String thinkingDots = getThinkingDots();
            int dotsX = statusX + mc.font.width(statusText);
            graphics.drawString(mc.font, thinkingDots, dotsX, y + 15, STATUS_PLANNING_COLOR);
        }

        // Progress bar for executing tasks
        if (isExecuting && !isPlanning) {
            renderProgressBar(graphics, mc, executor, x, y + 24, width);
        } else if (isPlanning) {
            renderPlanningBar(graphics, mc.font, x, y + 24, width);
        }

        // Role badge
        String roleBadge = getRoleBadge(crew);
        int roleX = x + width - roleBadge.length() * 6 - 6;
        graphics.drawString(mc.font, roleBadge, roleX, y + 3, getRoleColor(crew));
    }

    /**
     * Renders progress bar for executing tasks
     */
    private void renderProgressBar(GuiGraphics graphics, Minecraft mc, ActionExecutor executor, int x, int y, int width) {
        int progressBarWidth = width - 12;
        int progressBarHeight = 8;

        // Progress bar background
        graphics.fill(x + 6, y, x + 6 + progressBarWidth, y + progressBarHeight, PROGRESS_BAR_BG);

        // Get progress percentage
        int progress = executor.getCurrentActionProgress();

        // Progress bar fill
        int fillWidth = (progressBarWidth * progress) / 100;
        graphics.fill(x + 6, y, x + 6 + fillWidth, y + progressBarHeight, PROGRESS_BAR_FILL);

        // Progress percentage text
        String progressText = progress + "%";
        graphics.drawString(mc.font, progressText, x + 6, y + 10, 0xFFCCCCCC);

        // For build tasks, show blocks placed
        if (executor.getCurrentAction() instanceof BuildStructureAction) {
            BuildStructureAction buildAction = (BuildStructureAction) executor.getCurrentAction();
            String buildInfo = getBuildProgressText(buildAction);
            if (buildInfo != null) {
                graphics.drawString(mc.font, buildInfo, x + 46, y + 10, 0xFFAAAAAA);
            }
        }
    }

    /**
     * Renders animated planning progress bar
     */
    private void renderPlanningBar(GuiGraphics graphics, Font font, int x, int y, int width) {
        int progressBarWidth = width - 12;
        int progressBarHeight = 8;

        // Progress bar background
        graphics.fill(x + 6, y, x + 6 + progressBarWidth, y + progressBarHeight, PROGRESS_BAR_BG);

        // Animated planning progress (sweeping animation)
        float planningProgress = ((thinkingAnimationFrame % 60) / 60.0f);
        int fillWidth = (int)(progressBarWidth * planningProgress);
        graphics.fill(x + 6, y, x + 6 + fillWidth, y + progressBarHeight, PROGRESS_BAR_PLANNING);

        graphics.drawString(font, "Planning...", x + 6, y + 10, 0xFFCCCCCC);
    }

    /**
     * Get build progress text showing blocks placed
     */
    private String getBuildProgressText(BuildStructureAction buildAction) {
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
    private int getStatusColor(ForemanEntity crew) {
        if (crew.getActionExecutor().isPlanning()) {
            return STATUS_PLANNING_COLOR;
        } else if (crew.getActionExecutor().isExecuting()) {
            String currentGoal = crew.getActionExecutor().getCurrentGoal();
            // Check if in combat (goal contains combat-related keywords)
            if (currentGoal != null && (currentGoal.toLowerCase().contains("attack") ||
                currentGoal.toLowerCase().contains("fight") || currentGoal.toLowerCase().contains("combat"))) {
                return STATUS_COMBAT_COLOR;
            }
            return STATUS_WORKING_COLOR;
        }
        return STATUS_IDLE_COLOR;
    }

    /**
     * Gets the status text for a crew member
     */
    private String getStatusText(ForemanEntity crew) {
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
    private String getRoleBadge(ForemanEntity crew) {
        return switch (crew.getRole()) {
            case FOREMAN -> "[F]";
            case WORKER -> "[W]";
            default -> "[S]";
        };
    }

    /**
     * Gets the color for a crew member's role badge
     */
    private int getRoleColor(ForemanEntity crew) {
        return switch (crew.getRole()) {
            case FOREMAN -> 0xFFFF9800; // Orange
            case WORKER -> 0xFF2196F3; // Blue
            default -> 0xFF888888; // Gray
        };
    }

    /**
     * Applies fade effect to rendering
     */
    public static void applyFadeEffect(float fadeAlpha) {
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, fadeAlpha);
        RenderSystem.blendFuncSeparate(
            com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
            com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            com.mojang.blaze3d.platform.GlStateManager.SourceFactor.ONE,
            com.mojang.blaze3d.platform.GlStateManager.DestFactor.ZERO
        );
    }

    /**
     * Disables blend effect after rendering
     */
    public static void disableBlendEffect() {
        RenderSystem.disableBlend();
    }
}
