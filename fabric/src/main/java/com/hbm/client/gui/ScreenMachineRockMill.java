// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuMachineRockMill;
import com.hbm.inventory.recipes.RockMillRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.BlockEntityMachineRockMill;
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

public class ScreenMachineRockMill extends ScreenInfoContainer<MenuMachineRockMill> {
    private static final Identifier TEXTURE =
            Library.id("textures/gui/processing/gui_rockmill.png");
    private static final int BUTTON_X = 7, BUTTON_Y = 89, BUTTON_W = 18, BUTTON_H = 18;

    public ScreenMachineRockMill(MenuMachineRockMill menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 220);
        inventoryLabelY = imageHeight - 96 + 2;
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
                0,
                0,
                imageWidth,
                imageHeight,
                256,
                256);
        BlockEntityMachineRockMill be = menu.blockEntity();
        long power = menu.getPower();
        if (power > 0 && be.maxPower > 0) {
            int filled = (int) Math.min(71, power * 71 / be.maxPower);
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    152,
                    89 - filled,
                    176,
                    71 - filled,
                    16,
                    filled,
                    256,
                    256);
        }
        if (be.module.progress > 0) {
            int width = (int) Math.ceil(70 * be.module.progress);
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 62, 90, 176, 71, width, 16, 256, 256);
        }

        GenericRecipe recipe = be.module.getRecipe();
        if (be.didProcess || recipe != null) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    51,
                    85,
                    be.didProcess ? 195 : 192,
                    0,
                    3,
                    6,
                    256,
                    256);
        }
        if (be.didProcess || recipe != null && be.power >= recipe.power) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    56,
                    85,
                    be.didProcess ? 195 : 192,
                    0,
                    3,
                    6,
                    256,
                    256);
        }

        graphics.item(
                recipe != null ? recipe.getIcon() : new ItemStack(ModItems.TEMPLATE_FOLDER), 8, 90);
        if (recipe != null) {
            for (int i = 0; i < recipe.inputItem.length; i++) {
                if (be.getItem(BlockEntityMachineRockMill.SLOT_INPUT_START + i).isEmpty()) {
                    graphics.item(recipe.inputItem[i].extractForCyclingDisplay(20), 8 + i * 18, 27);
                    graphics.blit(
                            RenderPipelines.GUI_TEXTURED,
                            TEXTURE,
                            8 + i * 18,
                            27,
                            8 + i * 18,
                            27,
                            16,
                            16,
                            256,
                            256,
                            ARGB.white(0.5F));
                }
            }
        }

        drawFluidBarH(graphics, 8, 63, 52, 16, be.inputTanks[0]);
        drawFluidBarH(graphics, 80, 63, 52, 16, be.outputTanks[0]);
        drawElectricityInfo(graphics, mouseX, mouseY, 152, 18, 16, 71, power, be.maxPower);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 8, 63, 52, 16, be.inputTanks[0]);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 80, 63, 52, 16, be.outputTanks[0]);
        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0
                && checkClick(
                        (int) event.x(), (int) event.y(), BUTTON_X, BUTTON_Y, BUTTON_W, BUTTON_H)) {
            BlockEntityMachineRockMill be = menu.blockEntity();
            String current = be.module.getRecipeName().isEmpty() ? null : be.module.getRecipeName();
            String pool =
                    ItemBlueprints.grabPool(be.getItem(BlockEntityMachineRockMill.SLOT_SCHEMATIC));
            GUIScreenRecipeSelector.openSelector(
                    RockMillRecipes.INSTANCE, be.getBlockPos(), current, 0, pool, this);
            playClick();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (!checkClick(mouseX, mouseY, BUTTON_X, BUTTON_Y, BUTTON_W, BUTTON_H)) return;
        GenericRecipe recipe = menu.blockEntity().module.getRecipe();
        if (recipe == null) {
            graphics.setTooltipForNextFrame(
                    font,
                    Component.translatable("gui.recipe.setRecipe").withStyle(ChatFormatting.YELLOW),
                    mouseX,
                    mouseY);
            return;
        }
        List<Component> lines = new ArrayList<>();
        for (String line : recipe.print()) lines.add(Component.literal(line));
        GUIElements.drawHoveringTextRecipe(graphics, font, lines, mouseX, mouseY, width, height);
    }
}
