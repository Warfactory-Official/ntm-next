// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuMachineChemicalFactory;
import com.hbm.inventory.recipes.ChemicalPlantRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.BlockEntityMachineChemicalFactory;
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

public class ScreenMachineChemicalFactory extends ScreenInfoContainer<MenuMachineChemicalFactory> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/processing/gui_chemical_factory.png");

    private static final int BTN_X = 74, BTN_W = 18, BTN_H = 18;

    public ScreenMachineChemicalFactory(
            MenuMachineChemicalFactory menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 248, 216);
        this.inventoryLabelX = 26;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    private static List<Component> toComponents(List<String> strings) {
        List<Component> out = new ArrayList<>(strings.size());
        for (String s : strings) out.add(Component.literal(s));
        return out;
    }

    @Override
    protected int titleCenterX() {
        return 106;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        BlockEntityMachineChemicalFactory be = menu.blockEntity();

        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 0, 0, 0.0F, 0.0F, 248, 116, 256, 256);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED, TEXTURE, 18, 116, 18.0F, 116.0F, 230, 100, 256, 256);

        long maxPower = Math.max(1L, be.maxPower);
        int p = (int) (be.power * 68L / maxPower);
        if (p > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    224,
                    86 - p,
                    0.0F,
                    184.0F - p,
                    16,
                    p,
                    256,
                    256);
        }

        for (int g = 0; g < BlockEntityMachineChemicalFactory.MODULES; g++) {
            GenericRecipe recipe = be.module[g].getRecipe();

            if (be.module[g].progress > 0) {
                int j = (int) Math.ceil(22 * be.module[g].progress);
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        113,
                        29 + g * 22,
                        0.0F,
                        216.0F,
                        j,
                        6,
                        256,
                        256);
            }

            if (be.didProcess[g]) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        113,
                        21 + g * 22,
                        4.0F,
                        222.0F,
                        4,
                        4,
                        256,
                        256);
            } else if (recipe != null) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        113,
                        21 + g * 22,
                        0.0F,
                        222.0F,
                        4,
                        4,
                        256,
                        256);
            }
            if (be.didProcess[g]) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        121,
                        21 + g * 22,
                        4.0F,
                        222.0F,
                        4,
                        4,
                        256,
                        256);
            } else if (recipe != null && be.power >= recipe.power && be.canCool()) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        121,
                        21 + g * 22,
                        0.0F,
                        222.0F,
                        4,
                        4,
                        256,
                        256);
            }

            graphics.item(
                    recipe != null ? recipe.getIcon() : new ItemStack(ModItems.TEMPLATE_FOLDER),
                    75,
                    20 + g * 22);

            if (recipe != null && recipe.inputItem != null) {
                for (int i = 0; i < recipe.inputItem.length; i++) {
                    if (be.getItem(5 + g * BlockEntityMachineChemicalFactory.SLOTS_PER_MODULE + i)
                            .isEmpty()) {
                        graphics.item(
                                recipe.inputItem[i].extractForCyclingDisplay(20),
                                10 + i * 16,
                                20 + g * 22);
                    }
                }
                for (int i = 0; i < recipe.inputItem.length; i++) {
                    if (be.getItem(5 + g * BlockEntityMachineChemicalFactory.SLOTS_PER_MODULE + i)
                            .isEmpty()) {
                        int x = 10 + i * 16, y = 20 + g * 22;
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
        }

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < BlockEntityMachineChemicalFactory.MODULES; j++) {
                drawFluidBar(graphics, 60 + i * 5, 20 + j * 22, 3, 16, be.inputTanks[i + j * 3]);
                drawFluidBar(graphics, 189 + i * 5, 20 + j * 22, 3, 16, be.outputTanks[i + j * 3]);
            }
        }
        drawFluidBar(graphics, 224, 125, 7, 52, be.water());
        drawFluidBar(graphics, 233, 125, 7, 52, be.lps());

        drawElectricityInfo(graphics, mouseX, mouseY, 224, 18, 16, 68, be.power, be.maxPower);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < BlockEntityMachineChemicalFactory.MODULES; j++) {
                drawFluidGaugeInfo(
                        graphics,
                        mouseX,
                        mouseY,
                        60 + i * 5,
                        20 + j * 22,
                        3,
                        16,
                        be.inputTanks[i + j * 3]);
                drawFluidGaugeInfo(
                        graphics,
                        mouseX,
                        mouseY,
                        189 + i * 5,
                        20 + j * 22,
                        3,
                        16,
                        be.outputTanks[i + j * 3]);
            }
        }
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 224, 125, 7, 52, be.water());
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 233, 125, 7, 52, be.lps());

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        BlockEntityMachineChemicalFactory be = menu.blockEntity();

        for (int i = 0; i < BlockEntityMachineChemicalFactory.MODULES; i++) {
            if (!checkClick(mouseX, mouseY, BTN_X, 19 + i * 22, BTN_W, BTN_H)) continue;
            GenericRecipe recipe = be.module[i].getRecipe();
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
                        Component.translatable("gui.recipe.setRecipe")
                                .withStyle(ChatFormatting.YELLOW),
                        mouseX,
                        mouseY);
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            BlockEntityMachineChemicalFactory be = menu.blockEntity();
            for (int i = 0; i < BlockEntityMachineChemicalFactory.MODULES; i++) {
                if (checkClick(
                        (int) event.x(), (int) event.y(), BTN_X, 19 + i * 22, BTN_W, BTN_H)) {
                    String current =
                            be.module[i].getRecipeName().isEmpty()
                                    ? null
                                    : be.module[i].getRecipeName();
                    String pool =
                            ItemBlueprints.grabPool(
                                    be.getItem(
                                            4
                                                    + i
                                                            * BlockEntityMachineChemicalFactory
                                                                    .SLOTS_PER_MODULE));
                    GUIScreenRecipeSelector.openSelector(
                            ChemicalPlantRecipes.INSTANCE,
                            be.getBlockPos(),
                            current,
                            i,
                            pool,
                            this);
                    playClick();
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
}
