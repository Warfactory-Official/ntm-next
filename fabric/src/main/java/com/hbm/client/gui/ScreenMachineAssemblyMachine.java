// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuMachineAssemblyMachine;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.BlockEntityMachineAssemblyMachine;
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

public class ScreenMachineAssemblyMachine extends ScreenInfoContainer<MenuMachineAssemblyMachine> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/processing/gui_assembler.png");

    private static final int POWER_X = 152, POWER_Y = 18, POWER_W = 16, POWER_H = 61;
    private static final int ARROW_X = 62,
            ARROW_Y = 126,
            ARROW_U = 176,
            ARROW_V = 61,
            ARROW_W = 70,
            ARROW_H = 16;
    private static final int BTN_X = 7, BTN_Y = 125, BTN_W = 18, BTN_H = 18;
    private static final int LED_LEFT_X = 51, LED_RIGHT_X = 56, LED_Y = 121, LED_W = 3, LED_H = 6;
    private static final int LED_U_ARMED = 192, LED_U_LIT = 195, LED_V = 0;
    private static final int ICON_X = 8, ICON_Y = 126;
    private static final int IN_X0 = 8, IN_Y0 = 18, IN_STRIDE = 18, IN_COLS = 3;
    private static final int TANK_X_IN = 8, TANK_X_OUT = 80, TANK_Y = 99, TANK_W = 52, TANK_H = 16;

    public ScreenMachineAssemblyMachine(
            MenuMachineAssemblyMachine menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 256);
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    private static int inputSlotX(int i) {
        return IN_X0 + (i % IN_COLS) * IN_STRIDE;
    }

    private static int inputSlotY(int i) {
        return IN_Y0 + (i / IN_COLS) * IN_STRIDE;
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
        if (power > 0) {
            int filled = (int) (power * POWER_H / BlockEntityMachineAssemblyMachine.MAX_POWER);
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

        BlockEntityMachineAssemblyMachine be = assembler();
        int arrow = menu.getProgressScaled(ARROW_W);
        if (arrow > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    ARROW_X,
                    ARROW_Y,
                    ARROW_U,
                    ARROW_V + (be.recipeModule.restrictedMode ? ARROW_H : 0),
                    arrow,
                    ARROW_H,
                    256,
                    256);
        }

        GenericRecipe recipe = be.recipeModule.getRecipe();

        if (be.isProgressing) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    LED_LEFT_X,
                    LED_Y,
                    LED_U_LIT,
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
                    LED_U_ARMED,
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
                    LED_U_LIT,
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
                    LED_U_ARMED,
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
                if (be.getItem(BlockEntityMachineAssemblyMachine.SLOT_INPUT_START + i).isEmpty()) {
                    graphics.item(
                            recipe.inputItem[i].extractForCyclingDisplay(20),
                            inputSlotX(i),
                            inputSlotY(i));
                }
            }

            for (int i = 0; i < recipe.inputItem.length; i++) {
                if (be.getItem(BlockEntityMachineAssemblyMachine.SLOT_INPUT_START + i).isEmpty()) {
                    int x = inputSlotX(i), y = inputSlotY(i);
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

        drawFluidBarH(graphics, TANK_X_IN, TANK_Y, TANK_W, TANK_H, be.inputTank);
        drawFluidBarH(graphics, TANK_X_OUT, TANK_Y, TANK_W, TANK_H, be.outputTank);

        drawFluidGaugeInfo(
                graphics, mouseX, mouseY, TANK_X_IN, TANK_Y, TANK_W, TANK_H, be.inputTank);
        drawFluidGaugeInfo(
                graphics, mouseX, mouseY, TANK_X_OUT, TANK_Y, TANK_W, TANK_H, be.outputTank);

        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                POWER_X,
                POWER_Y,
                POWER_W,
                POWER_H,
                menu.getPower(),
                BlockEntityMachineAssemblyMachine.MAX_POWER);

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (!checkClick(mouseX, mouseY, BTN_X, BTN_Y, BTN_W, BTN_H)) return;

        GenericRecipe recipe = assembler().recipeModule.getRecipe();
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

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0
                && checkClick((int) event.x(), (int) event.y(), BTN_X, BTN_Y, BTN_W, BTN_H)) {
            BlockEntityMachineAssemblyMachine be = assembler();
            String current =
                    be.recipeModule.getRecipeName().isEmpty()
                            ? null
                            : be.recipeModule.getRecipeName();
            String pool =
                    ItemBlueprints.grabPool(
                            be.getItem(BlockEntityMachineAssemblyMachine.SLOT_BLUEPRINT));
            GUIScreenRecipeSelector.openSelector(
                    AssemblyMachineRecipes.INSTANCE, be.getBlockPos(), current, 0, pool, this);
            playClick();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private BlockEntityMachineAssemblyMachine assembler() {
        return menu.blockEntity();
    }
}
