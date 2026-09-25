// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuMachinePUREX;
import com.hbm.inventory.recipes.PUREXRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.BlockEntityMachinePUREX;
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

public class ScreenMachinePUREX extends ScreenInfoContainer<MenuMachinePUREX> {

    private static final Identifier TEXTURE = Library.id("textures/gui/processing/gui_purex.png");

    private static final int POWER_X = 152, POWER_Y = 18, POWER_W = 16, POWER_H = 61;
    private static final int TANK_TOP = 18, TANK_W = 16, TANK_H = 52;
    private static final int IN_X0 = 8, TANK_STRIDE = 18;
    private static final int OUT_X = 116, OUT_TOP = 36;
    private static final int ARROW_X = 62,
            ARROW_Y = 126,
            ARROW_U = 176,
            ARROW_V = 61,
            ARROW_W = 70,
            ARROW_H = 16;
    private static final int BTN_X = 7, BTN_Y = 125, BTN_W = 18, BTN_H = 18;
    private static final int LED_LEFT_X = 51, LED_RIGHT_X = 56, LED_Y = 121, LED_W = 3, LED_H = 6;
    private static final int LED_LIT_U = 195, LED_ARMED_U = 192, LED_V = 0;
    private static final int ICON_X = 8, ICON_Y = 126;
    private static final int ITEM_IN_Y = 90;

    public ScreenMachinePUREX(MenuMachinePUREX menu, Inventory inventory, Component title) {
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
        long max = BlockEntityMachinePUREX.MAX_POWER;
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

        int arrow = menu.getProgressScaled(ARROW_W);
        if (arrow > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    ARROW_X,
                    ARROW_Y,
                    ARROW_U,
                    ARROW_V,
                    arrow,
                    ARROW_H,
                    256,
                    256);
        }

        BlockEntityMachinePUREX be = purex();
        GenericRecipe recipe = be.module.getRecipe();

        if (be.isProgressing) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    LED_LEFT_X,
                    LED_Y,
                    LED_LIT_U,
                    LED_V,
                    LED_W,
                    LED_H,
                    256,
                    256);
        } else if (recipe != null) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    LED_LEFT_X,
                    LED_Y,
                    LED_ARMED_U,
                    LED_V,
                    LED_W,
                    LED_H,
                    256,
                    256);
        }
        if (be.isProgressing) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    LED_RIGHT_X,
                    LED_Y,
                    LED_LIT_U,
                    LED_V,
                    LED_W,
                    LED_H,
                    256,
                    256);
        } else if (recipe != null && be.power >= recipe.power) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    LED_RIGHT_X,
                    LED_Y,
                    LED_ARMED_U,
                    LED_V,
                    LED_W,
                    LED_H,
                    256,
                    256);
        }

        graphics.item(
                recipe != null ? recipe.getIcon() : new ItemStack(ModItems.TEMPLATE_FOLDER),
                ICON_X,
                ICON_Y);

        if (recipe != null && recipe.inputItem != null) {
            for (int i = 0; i < recipe.inputItem.length; i++) {
                if (be.getItem(BlockEntityMachinePUREX.SLOT_ITEM_IN_START + i).isEmpty()) {
                    graphics.item(
                            recipe.inputItem[i].extractForCyclingDisplay(20),
                            IN_X0 + i * TANK_STRIDE,
                            ITEM_IN_Y);
                }
            }
            for (int i = 0; i < recipe.inputItem.length; i++) {
                if (be.getItem(BlockEntityMachinePUREX.SLOT_ITEM_IN_START + i).isEmpty()) {
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

        for (int i = 0; i < BlockEntityMachinePUREX.INPUT_TANK_COUNT; i++) {
            drawFluidBar(
                    graphics, IN_X0 + i * TANK_STRIDE, TANK_TOP, TANK_W, TANK_H, be.inputTanks[i]);
        }
        drawFluidBar(graphics, OUT_X, OUT_TOP, TANK_W, TANK_H, be.outputTanks[0]);

        drawElectricityInfo(
                graphics, mouseX, mouseY, POWER_X, POWER_Y, POWER_W, POWER_H, menu.getPower(), max);

        for (int i = 0; i < BlockEntityMachinePUREX.INPUT_TANK_COUNT; i++) {
            drawFluidGaugeInfo(
                    graphics,
                    mouseX,
                    mouseY,
                    IN_X0 + i * TANK_STRIDE,
                    TANK_TOP,
                    TANK_W,
                    TANK_H,
                    be.inputTanks[i]);
        }
        drawFluidGaugeInfo(
                graphics, mouseX, mouseY, OUT_X, OUT_TOP, TANK_W, TANK_H, be.outputTanks[0]);

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0
                && checkClick((int) event.x(), (int) event.y(), BTN_X, BTN_Y, BTN_W, BTN_H)) {
            BlockEntityMachinePUREX be = purex();
            String current = be.module.getRecipeName().isEmpty() ? null : be.module.getRecipeName();
            String pool =
                    ItemBlueprints.grabPool(be.getItem(BlockEntityMachinePUREX.SLOT_BLUEPRINT));
            GUIScreenRecipeSelector.openSelector(
                    PUREXRecipes.INSTANCE, be.getBlockPos(), current, 0, pool, this);
            playClick();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (!checkClick(mouseX, mouseY, BTN_X, BTN_Y, BTN_W, BTN_H)) return;

        GenericRecipe recipe = purex().module.getRecipe();
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

    private BlockEntityMachinePUREX purex() {
        return menu.blockEntity();
    }
}
