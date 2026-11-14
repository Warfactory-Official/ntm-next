/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.client.gui;

import com.hbm.blockentity.machine.CrucibleBlockEntity;
import com.hbm.data.MachineData;
import com.hbm.inventory.container.CrucibleMenu;
import com.hbm.inventory.recipes.CrucibleRecipe;
import com.hbm.inventory.recipes.CrucibleRecipes;
import com.hbm.items.ModItems;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.CrucibleRecipeLayout;
import com.hbm.util.RegistryUtil;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class CrucibleScreen extends InfoContainerScreen<CrucibleMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/processing/gui_crucible.png");
    private static final MoltenFluidBar MELT = new MoltenFluidBar(TEXTURE, 176, 210, 34, 79, 89);
    private static final int RECIPE_X = 106, RECIPE_Y = 80;

    public CrucibleScreen(CrucibleMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 214);
        titleLabelY = 6;
        inventoryLabelY = imageHeight - 96 + 2;
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

        CrucibleBlockEntity be = crucible();
        int processTime = MachineData.CRUCIBLE_PROCESS_TIME.get();
        int maxHeat = MachineData.CRUCIBLE_MAX_HEAT.get();
        gauge(graphics, 82, 0, be.progress * 33 / processTime);
        gauge(graphics, 91, 5, be.heat * 33 / maxHeat);

        CrucibleRecipe recipe = CrucibleRecipes.INSTANCE.getRecipe(be.recipe);
        graphics.item(
                recipe != null ? recipe.getIcon() : new ItemStack(ModItems.TEMPLATE_FOLDER),
                RECIPE_X + 1,
                RECIPE_Y + 1);

        MELT.draw(graphics, be.recipeStack, MachineData.CRUCIBLE_RECIPE_CAPACITY.get(), 62, 97);
        MELT.draw(graphics, be.wasteStack, MachineData.CRUCIBLE_WASTE_CAPACITY.get(), 17, 97);

        drawCustomInfoStat(
                graphics, mouseX, mouseY, 16, 17, 36, 81, MoltenFluidBar.describe(be.wasteStack));
        drawCustomInfoStat(
                graphics, mouseX, mouseY, 61, 17, 36, 81, MoltenFluidBar.describe(be.recipeStack));
        drawCustomInfoStat(
                graphics, mouseX, mouseY, 125, 81, 34, 7, thermalUnits(be.progress, processTime));
        drawCustomInfoStat(
                graphics, mouseX, mouseY, 125, 90, 34, 7, thermalUnits(be.heat, maxHeat));

        if (checkClick(mouseX, mouseY, RECIPE_X, RECIPE_Y, 18, 18)) {
            List<Component> lines =
                    recipe != null
                            ? recipe.print()
                            : List.of(
                                    Component.translatable("gui.recipe.setRecipe")
                                            .withStyle(ChatFormatting.YELLOW));
            graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }

    private static void gauge(GuiGraphicsExtractor graphics, int y, int v, int length) {
        if (length > 0)
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 126, y, 176, v, length, 5, 256, 256);
    }

    private static List<Component> thermalUnits(int value, int max) {
        return List.of(Component.literal(String.format(Locale.ROOT, "%,d / %,dTU", value, max)));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT
                && checkClick((int) event.x(), (int) event.y(), RECIPE_X, RECIPE_Y, 18, 18)) {
            CrucibleBlockEntity crucible = crucible();
            GUIScreenRecipeSelector.openSelector(
                    CrucibleRecipes.INSTANCE,
                    crucible.getBlockPos(),
                    crucible.recipe,
                    this,
                    (pos, index, selection) ->
                            PacketWire.sendToServer(
                                    CrucibleRecipeLayout.encode(menu.containerId, selection)));
            playClick();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private CrucibleBlockEntity crucible() {
        return menu.blockEntity();
    }
}
