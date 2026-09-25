// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuMachineChemicalPlant;
import com.hbm.inventory.recipes.ChemicalPlantRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.BlockEntityMachineChemicalPlant;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class ScreenMachineChemicalPlant extends ScreenInfoContainer<MenuMachineChemicalPlant> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/processing/gui_chemplant.png");

    private static final int POWER_X = 152, POWER_Y = 18, POWER_W = 16, POWER_H = 61;
    private static final int TANK_TOP = 18, TANK_W = 16, TANK_H = 34;
    private static final int IN_X0 = 8, OUT_X0 = 80, TANK_STRIDE = 18;
    private static final int ITEM_IN_Y = 99;
    private static final int BTN_X = 7, BTN_Y = 125, BTN_W = 18, BTN_H = 18;

    public ScreenMachineChemicalPlant(
            MenuMachineChemicalPlant menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 256);
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    private static List<Component> toComponents(List<String> strings) {
        List<Component> out = new ArrayList<>(strings.size());
        for (String s : strings) out.add(Component.literal(s));
        return out;
    }

    @Override
    protected int titleCenterX() {
        return 70;
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
        long max = BlockEntityMachineChemicalPlant.MAX_POWER;
        if (power > 0 && max > 0) {
            int filled = (int) Math.min(POWER_H, power * POWER_H / max);
            if (filled > 0) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        POWER_X,
                        POWER_Y + (POWER_H - filled),
                        176,
                        POWER_H - filled,
                        POWER_W,
                        filled,
                        256,
                        256);
            }
        }

        BlockEntityMachineChemicalPlant be = chemplant();
        GenericRecipe recipe = be.module.getRecipe();

        if (be.module.progress > 0) {
            int arrowW = (int) Math.ceil(70.0 * be.module.progress);
            float arrowV = be.module.restrictedMode ? 77.0F : 61.0F;
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    62,
                    126,
                    176.0F,
                    arrowV,
                    arrowW,
                    16,
                    256,
                    256);
        }

        if (be.isProgressing) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 51, 121, 195.0F, 0.0F, 3, 6, 256, 256);
        } else if (recipe != null) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 51, 121, 192.0F, 0.0F, 3, 6, 256, 256);
        }
        if (be.isProgressing) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 56, 121, 195.0F, 0.0F, 3, 6, 256, 256);
        } else if (recipe != null && be.power >= recipe.power) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 56, 121, 192.0F, 0.0F, 3, 6, 256, 256);
        }

        graphics.item(
                recipe != null ? recipe.getIcon() : new ItemStack(ModItems.TEMPLATE_FOLDER),
                8,
                126);

        if (recipe != null && recipe.inputItem != null) {
            for (int i = 0; i < recipe.inputItem.length; i++) {
                if (be.getItem(BlockEntityMachineChemicalPlant.SLOT_ITEM_IN_START + i).isEmpty()) {
                    graphics.item(
                            recipe.inputItem[i].extractForCyclingDisplay(20),
                            IN_X0 + i * TANK_STRIDE,
                            ITEM_IN_Y);
                }
            }

            for (int i = 0; i < recipe.inputItem.length; i++) {
                if (be.getItem(BlockEntityMachineChemicalPlant.SLOT_ITEM_IN_START + i).isEmpty()) {
                    int x = IN_X0 + i * TANK_STRIDE, y = ITEM_IN_Y;
                    graphics.blit(
                            RenderPipelines.GUI_TEXTURED,
                            TEXTURE,
                            x,
                            y,
                            (float) x,
                            (float) y,
                            16,
                            16,
                            256,
                            256,
                            ARGB.white(0.5F));
                }
            }
        }

        for (int i = 0; i < BlockEntityMachineChemicalPlant.TANK_COUNT; i++) {
            drawFluidBar(
                    graphics, IN_X0 + i * TANK_STRIDE, TANK_TOP, TANK_W, TANK_H, be.inputTanks[i]);
            drawFluidBar(
                    graphics,
                    OUT_X0 + i * TANK_STRIDE,
                    TANK_TOP,
                    TANK_W,
                    TANK_H,
                    be.outputTanks[i]);
        }

        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                POWER_X,
                POWER_Y,
                POWER_W,
                POWER_H,
                menu.getPower(),
                BlockEntityMachineChemicalPlant.MAX_POWER);

        for (int i = 0; i < BlockEntityMachineChemicalPlant.TANK_COUNT; i++) {
            drawFluidGaugeInfo(
                    graphics,
                    mouseX,
                    mouseY,
                    IN_X0 + i * TANK_STRIDE,
                    TANK_TOP,
                    TANK_W,
                    TANK_H,
                    be.inputTanks[i]);
            drawFluidGaugeInfo(
                    graphics,
                    mouseX,
                    mouseY,
                    OUT_X0 + i * TANK_STRIDE,
                    TANK_TOP,
                    TANK_W,
                    TANK_H,
                    be.outputTanks[i]);
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0
                && checkClick((int) event.x(), (int) event.y(), BTN_X, BTN_Y, BTN_W, BTN_H)) {
            BlockEntityMachineChemicalPlant be = chemplant();
            String current = be.module.getRecipeName().isEmpty() ? null : be.module.getRecipeName();
            String pool =
                    ItemBlueprints.grabPool(
                            be.getItem(BlockEntityMachineChemicalPlant.SLOT_SCHEMATIC));
            GUIScreenRecipeSelector.openSelector(
                    ChemicalPlantRecipes.INSTANCE, be.getBlockPos(), current, 0, pool, this);
            playClick();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (!checkClick(mouseX, mouseY, BTN_X, BTN_Y, BTN_W, BTN_H)) return;

        GenericRecipe recipe = chemplant().module.getRecipe();
        if (recipe != null) {
            GUIElements.drawHoveringTextRecipe(
                    graphics,
                    this.font,
                    toComponents(recipe.print()),
                    mouseX,
                    mouseY,
                    this.width,
                    this.height);
        } else {
            graphics.setTooltipForNextFrame(
                    this.font,
                    Component.translatable("gui.recipe.setRecipe").withStyle(ChatFormatting.YELLOW),
                    mouseX,
                    mouseY);
        }
    }

    private BlockEntityMachineChemicalPlant chemplant() {
        return menu.blockEntity();
    }
}
