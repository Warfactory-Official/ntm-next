// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuMachineGasCent;
import com.hbm.inventory.recipes.GasCentrifugeRecipe;
import com.hbm.inventory.recipes.GasCentrifugeRecipes;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.BlockEntityMachineGasCent.StageTank;
import com.hbm.tileentity.machine.BlockEntityMachineGasCent;
import com.hbm.util.I18nUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;

public class ScreenMachineGasCent extends ScreenInfoContainer<MenuMachineGasCent> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/processing/gui_centrifuge_gas.png");

    private static final int SHEET = 256;

    private static final int POWER_X = 182, POWER_BOTTOM = 69, POWER_W = 16, POWER_H = 52;
    private static final int PROGRESS_X = 70, PROGRESS_Y = 35, PROGRESS_W = 36, PROGRESS_H = 13;
    private static final int GAUGE_Y = 16, GAUGE_W = 6, GAUGE_H = 52;
    private static final int IN_GAUGE_A = 16, IN_GAUGE_B = 32, OUT_GAUGE_A = 138, OUT_GAUGE_B = 154;

    public ScreenMachineGasCent(MenuMachineGasCent menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 206, 204);
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    private static List<Component> lines(String key) {
        List<Component> out = new ArrayList<>();
        for (String s : I18nUtil.resolveKeyArray(key)) out.add(Component.literal(s));
        return out;
    }

    private BlockEntityMachineGasCent be() {
        return menu.blockEntity();
    }

    @Override
    protected boolean drawTitle() {
        return false;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                0,
                0,
                0.0F,
                0.0F,
                imageWidth,
                imageHeight,
                SHEET,
                SHEET);

        BlockEntityMachineGasCent be = be();
        long max = BlockEntityMachineGasCent.MAX_POWER;

        int i = (int) (be.power * POWER_H / max);
        if (i > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    POWER_X,
                    POWER_BOTTOM - i,
                    206,
                    POWER_H - i,
                    POWER_W,
                    i,
                    SHEET,
                    SHEET);
        }

        int j = progressScaled(PROGRESS_W);
        if (j > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    PROGRESS_X,
                    PROGRESS_Y,
                    206,
                    POWER_H,
                    j,
                    PROGRESS_H,
                    SHEET,
                    SHEET);
        }

        drawStageGauge(graphics, IN_GAUGE_A, be.inputTank);
        drawStageGauge(graphics, IN_GAUGE_B, be.inputTank);
        drawStageGauge(graphics, OUT_GAUGE_A, be.outputTank);
        drawStageGauge(graphics, OUT_GAUGE_B, be.outputTank);

        drawInfoPanel(graphics, -12, 16, 16, 16, 3);
        drawInfoPanel(graphics, -12, 32, 16, 16, 2);

        drawCustomInfoStat(graphics, mouseX, mouseY, 15, 15, 24, 55, stageInfo(be.inputTank, true));
        drawCustomInfoStat(
                graphics, mouseX, mouseY, 137, 15, 25, 55, stageInfo(be.outputTank, false));

        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                POWER_X,
                POWER_BOTTOM - POWER_H,
                POWER_W,
                POWER_H,
                be.power,
                max);

        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                -12,
                16,
                16,
                16,
                -8,
                32,
                lines("desc.gui.gasCent.enrichment"));
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                -12,
                32,
                16,
                16,
                -8,
                48,
                lines("desc.gui.gasCent.output"));

        super.extractLabels(graphics, mouseX, mouseY);
    }

    private List<Component> stageInfo(StageTank tank, boolean input) {
        GasCentrifugeRecipe recipe = GasCentrifugeRecipes.INSTANCE.byStage(tank.getStage());
        String name = GasCentrifugeRecipe.stageName(tank.getStage());

        if (recipe != null && recipe.requiresUpgrade) {
            ChatFormatting colour =
                    !input || be().hasSpeedUpgrade()
                            ? ChatFormatting.GOLD
                            : ChatFormatting.DARK_RED;
            name = colour + name;
        }

        List<Component> out = new ArrayList<>(2);
        out.add(Component.literal(name));
        out.add(Component.literal(tank.getFill() + " / " + tank.getMaxFill() + " mB"));
        return out;
    }

    private void drawStageGauge(GuiGraphicsExtractor graphics, int x, StageTank tank) {
        Fluid type = be().tank.getTankType();
        if (type == null || tank.getMaxFill() <= 0) return;
        int filled = Math.min(GAUGE_H, tank.getFill() * GAUGE_H / tank.getMaxFill());
        if (filled > 0) FluidGauge.hanging(graphics, x, GAUGE_Y, GAUGE_W, filled, type);
    }

    private int progressScaled(int scale) {
        BlockEntityMachineGasCent be = be();
        GasCentrifugeRecipe recipe = GasCentrifugeRecipes.INSTANCE.byStage(be.inputTank.getStage());
        int time = be.processTime(recipe);
        return time <= 0 ? 0 : Math.min(scale, be.progress * scale / time);
    }
}
