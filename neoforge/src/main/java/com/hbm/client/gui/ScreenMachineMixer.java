// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.container.MenuMachineMixer;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.recipes.MixerRecipe;
import com.hbm.inventory.recipes.MixerRecipes;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.machine.BlockEntityMachineMixer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class ScreenMachineMixer extends ScreenInfoContainer<MenuMachineMixer> {

    private static final Identifier TEXTURE = Library.id("textures/gui/processing/gui_mixer.png");

    private static final int POWER_X = 12, POWER_Y = 18, POWER_W = 16, POWER_H = 52;
    private static final int POWER_V_BOTTOM = 52;
    private static final int TANK_IN1_X = 52, TANK_IN2_X = 61, TANK_OUT_X = 126, TANK_Y = 18;
    private static final int TANK_IN_W = 7, TANK_OUT_W = 16, TANK_H = 52;
    private static final int RECIPE_BTN_X = 71, RECIPE_BTN_Y = 17, RECIPE_BTN_SIZE = 12;
    private static final int PROGRESS_X = 71, PROGRESS_Y = 31, PROGRESS_W = 52, PROGRESS_H = 44;

    public ScreenMachineMixer(MenuMachineMixer menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 204);
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected int titleCenterX() {
        return imageWidth / 2 + 20;
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
                256,
                256);

        long power = menu.getPower();
        int filled = (int) Math.min(POWER_H, power * POWER_H / BlockEntityMachineMixer.MAX_POWER);
        if (filled > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    POWER_X,
                    POWER_Y + (POWER_H - filled),
                    176,
                    POWER_V_BOTTOM - filled,
                    POWER_W,
                    filled,
                    256,
                    256);
        }

        BlockEntityMachineMixer be = mixer();
        if (be.processTime > 0 && be.progress > 0) {
            int j = be.progress * PROGRESS_W / be.processTime;
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    PROGRESS_X,
                    PROGRESS_Y,
                    192,
                    0,
                    j,
                    PROGRESS_H,
                    256,
                    256);
        }

        drawFluidBar(
                graphics,
                TANK_IN1_X,
                TANK_Y,
                TANK_IN_W,
                TANK_H,
                be.tanks[BlockEntityMachineMixer.TANK_IN1]);
        drawFluidBar(
                graphics,
                TANK_IN2_X,
                TANK_Y,
                TANK_IN_W,
                TANK_H,
                be.tanks[BlockEntityMachineMixer.TANK_IN2]);
        drawFluidBar(
                graphics,
                TANK_OUT_X,
                TANK_Y,
                TANK_OUT_W,
                TANK_H,
                be.tanks[BlockEntityMachineMixer.TANK_OUT]);

        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                POWER_X,
                POWER_Y,
                POWER_W,
                POWER_H,
                power,
                BlockEntityMachineMixer.MAX_POWER);

        drawFluidGaugeInfo(
                graphics,
                mouseX,
                mouseY,
                TANK_IN1_X,
                TANK_Y,
                TANK_IN_W,
                TANK_H,
                be.tanks[BlockEntityMachineMixer.TANK_IN1]);
        drawFluidGaugeInfo(
                graphics,
                mouseX,
                mouseY,
                TANK_IN2_X,
                TANK_Y,
                TANK_IN_W,
                TANK_H,
                be.tanks[BlockEntityMachineMixer.TANK_IN2]);
        drawFluidGaugeInfo(
                graphics,
                mouseX,
                mouseY,
                TANK_OUT_X,
                TANK_Y,
                TANK_OUT_W,
                TANK_H,
                be.tanks[BlockEntityMachineMixer.TANK_OUT]);
        drawInfoPanel(graphics, 152, 55, 8, 8, 8);
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                152,
                55,
                8,
                8,
                resolveLines(
                        "desc.gui.upgrade",
                        "desc.gui.upgrade.speed",
                        "desc.gui.upgrade.power",
                        "desc.gui.upgrade.overdrive"));

        List<MixerRecipe> recipeSet =
                MixerRecipes.INSTANCE.getOutput(
                        be.tanks[BlockEntityMachineMixer.TANK_OUT].getTankType());
        if (recipeSet.size() > 1) {
            List<Component> lines = new ArrayList<>();
            lines.add(
                    Component.translatable(
                                    "desc.gui.machineMixer.currentRecipe",
                                    be.recipeIndex + 1,
                                    recipeSet.size())
                            .withStyle(ChatFormatting.YELLOW));
            MixerRecipe recipe = recipeSet.get(be.recipeIndex % recipeSet.size());
            if (recipe.input1() != null)
                lines.add(
                        Component.literal(
                                "-" + NTMFluidProperties.clientName(recipe.input1().type())));
            if (recipe.input2() != null)
                lines.add(
                        Component.literal(
                                "-" + NTMFluidProperties.clientName(recipe.input2().type())));
            ItemStack solid =
                    recipe.solidInput() == null
                            ? ItemStack.EMPTY
                            : recipe.solidInput().extractForCyclingDisplay(20);
            if (!solid.isEmpty()) lines.add(Component.literal("-").append(solid.getHoverName()));
            lines.add(
                    Component.translatable("desc.gui.machineMixer.clickToChange")
                            .withStyle(ChatFormatting.RED));
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    RECIPE_BTN_X,
                    RECIPE_BTN_Y,
                    RECIPE_BTN_SIZE,
                    RECIPE_BTN_SIZE,
                    lines);
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0
                && checkClick(
                        (int) event.x(),
                        (int) event.y(),
                        RECIPE_BTN_X,
                        RECIPE_BTN_Y,
                        RECIPE_BTN_SIZE,
                        RECIPE_BTN_SIZE)) {
            CompoundTag data = new CompoundTag();
            data.putBoolean("toggle", true);
            Services.NETWORK.sendToServer(new NbtControlPayload(mixer().getBlockPos(), data));
            playClick();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private BlockEntityMachineMixer mixer() {
        return menu.blockEntity();
    }
}
