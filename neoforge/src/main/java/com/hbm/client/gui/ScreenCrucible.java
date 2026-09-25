// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.client.ModifierKeys;
import com.hbm.client.render.HbmRenderPipelines;
import com.hbm.data.MachineData;
import com.hbm.inventory.container.MenuCrucible;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial.SmeltingBehavior;
import com.hbm.inventory.recipes.CrucibleRecipe;
import com.hbm.inventory.recipes.CrucibleRecipes;
import com.hbm.items.ModItems;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.BlockEntityCrucible;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class ScreenCrucible extends ScreenInfoContainer<MenuCrucible> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/processing/gui_crucible.png");

    public ScreenCrucible(MenuCrucible menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 214);
        this.titleLabelY = 6;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected int titleColor() {
        return CommonColors.WHITE;
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

        BlockEntityCrucible be = crucible();

        int pGauge = be.progress * 33 / MachineData.CRUCIBLE_PROCESS_TIME.get();
        if (pGauge > 0)
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 126, 82, 176, 0, pGauge, 5, 256, 256);
        int hGauge = be.heat * 33 / MachineData.CRUCIBLE_MAX_HEAT.get();
        if (hGauge > 0)
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 126, 91, 176, 5, hGauge, 5, 256, 256);

        CrucibleRecipe recipe = CrucibleRecipes.INSTANCE.getRecipe(be.recipe);
        graphics.item(
                recipe != null ? recipe.getIcon() : new ItemStack(ModItems.TEMPLATE_FOLDER),
                107,
                81);

        if (!be.recipeStack.isEmpty())
            drawStack(graphics, be.recipeStack, MachineData.CRUCIBLE_RECIPE_CAPACITY.get(), 62, 97);
        if (!be.wasteStack.isEmpty())
            drawStack(graphics, be.wasteStack, MachineData.CRUCIBLE_WASTE_CAPACITY.get(), 17, 97);

        drawStackInfo(graphics, be.wasteStack, mouseX, mouseY, 16, 17);
        drawStackInfo(graphics, be.recipeStack, mouseX, mouseY, 61, 17);

        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                125,
                81,
                34,
                7,
                List.of(
                        Component.literal(
                                String.format(Locale.US, "%,d", be.progress)
                                        + " / "
                                        + String.format(
                                                Locale.US,
                                                "%,d",
                                                MachineData.CRUCIBLE_PROCESS_TIME.get())
                                        + "TU")));
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                125,
                90,
                34,
                7,
                List.of(
                        Component.literal(
                                String.format(Locale.US, "%,d", be.heat)
                                        + " / "
                                        + String.format(
                                                Locale.US,
                                                "%,d",
                                                MachineData.CRUCIBLE_MAX_HEAT.get())
                                        + "TU")));

        if (checkClick(mouseX, mouseY, 106, 80, 18, 18)) {
            if (recipe != null) {
                List<Component> lines = new ArrayList<>();
                for (String s : recipe.print()) lines.add(Component.literal(s));
                graphics.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY);
            } else {
                graphics.setComponentTooltipForNextFrame(
                        this.font,
                        List.of(
                                Component.translatable("gui.recipe.setRecipe")
                                        .withStyle(ChatFormatting.YELLOW)),
                        mouseX,
                        mouseY);
            }
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }

    private void drawStack(
            GuiGraphicsExtractor graphics, List<MaterialStack> stack, int capacity, int x, int y) {
        int lastHeight = 0;
        int lastQuant = 0;

        for (MaterialStack sta : stack) {
            int targetHeight = (lastQuant + sta.amount) * 79 / capacity;

            if (lastHeight != targetHeight) {
                int offset = sta.material.smeltable == SmeltingBehavior.ADDITIVE ? 34 : 0;
                int h = targetHeight - lastHeight;

                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        x,
                        y - targetHeight,
                        176 + offset,
                        89 - targetHeight,
                        34,
                        h,
                        256,
                        256,
                        ARGB.opaque(sta.material.moltenColor));
                graphics.blit(
                        HbmRenderPipelines.GUI_TEXTURED_ADDITIVE,
                        TEXTURE,
                        x,
                        y - targetHeight,
                        176 + offset,
                        89 - targetHeight,
                        34,
                        h,
                        256,
                        256,
                        0x4CFFFFFF);
            }

            lastQuant += sta.amount;
            lastHeight = targetHeight;
        }
    }

    private void drawStackInfo(
            GuiGraphicsExtractor graphics,
            List<MaterialStack> stack,
            int mouseX,
            int mouseY,
            int x,
            int y) {
        List<Component> list = new ArrayList<>();

        if (stack.isEmpty())
            list.add(Component.translatable("desc.shared.empty").withStyle(ChatFormatting.RED));

        boolean shift = ModifierKeys.leftShiftHeld();
        for (MaterialStack sta : stack) {
            list.add(
                    Component.literal(
                                    sta.material.getLocalizedName()
                                            + ": "
                                            + Mats.formatAmount(sta.amount, shift))
                            .withStyle(ChatFormatting.YELLOW));
        }

        drawCustomInfoStat(graphics, mouseX, mouseY, x, y, 36, 81, list);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && checkClick((int) event.x(), (int) event.y(), 106, 80, 18, 18)) {
            BlockEntityCrucible crucible = crucible();
            GUIScreenRecipeSelector.openSelector(
                    CrucibleRecipes.INSTANCE, crucible.getBlockPos(), crucible.recipe, this);
            playClick();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private BlockEntityCrucible crucible() {
        return menu.blockEntity();
    }
}
