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

import com.hbm.client.Keybinds;
import com.hbm.inventory.container.AnvilMenu;
import com.hbm.inventory.recipes.anvil.AnvilConstructionRecipe.AnvilOutput;
import com.hbm.inventory.recipes.anvil.AnvilConstructionRecipe.OverlayType;
import com.hbm.inventory.recipes.anvil.AnvilConstructionRecipe;
import com.hbm.inventory.recipes.anvil.AnvilConstructionRecipes;
import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.AnvilCraftLayout;
import com.hbm.util.InventoryUtil;
import com.hbm.util.RegistryUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class AnvilScreen extends InfoContainerScreen<AnvilMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/processing/gui_anvil.png");

    private final int tier;
    private final List<AnvilConstructionRecipe> originList = new ArrayList<>();
    private final List<AnvilConstructionRecipe> recipes = new ArrayList<>();
    private int pageIndex;
    private int maxPage;
    private int selection = -1;
    private int lastSize;
    private EditBox search;
    private OverlayType filter = OverlayType.NONE;

    public AnvilScreen(AnvilMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 222);
        tier = menu.tier;

        for (AnvilConstructionRecipe recipe : AnvilConstructionRecipes.INSTANCE.recipes()) {
            if (recipe.isTierValid(tier)) originList.add(recipe);
        }
        regenerateRecipes();
    }

    @Override
    protected void init() {
        super.init();

        search = new EditBox(font, leftPos + 10, topPos + 111, 84, 12, Component.empty());
        search.setTextColor(CommonColors.WHITE);
        search.setBordered(false);
        search.setMaxLength(25);
        search.setResponder(this::search);
        addRenderableWidget(search);
        regenerateRecipes();
    }

    private void regenerateRecipes() {
        regenerateRecipes(search == null ? "" : search.getValue());
    }

    private void regenerateRecipes(String term) {
        recipes.clear();
        String lower = term.toLowerCase(Locale.ROOT);
        for (AnvilConstructionRecipe recipe : originList) {
            if (filter != OverlayType.NONE && recipe.getOverlay() != filter) continue;
            if (lower.isEmpty()) {
                recipes.add(recipe);
                continue;
            }
            for (String s : recipeToSearchList(recipe)) {
                if (s.contains(lower)) {
                    recipes.add(recipe);
                    break;
                }
            }
        }
        resetPaging();
    }

    private void search(String term) {
        regenerateRecipes(term);
    }

    private void resetPaging() {
        pageIndex = 0;
        selection = -1;
        maxPage = Math.max(0, (int) Math.ceil((recipes.size() - 10) / 2D));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int x = (int) event.x(), y = (int) event.y();

        if (checkClick(x, y, 7, 71, 9, 36)) {
            playClick();
            if (pageIndex > 0) pageIndex--;
            return true;
        }
        if (checkClick(x, y, 106, 71, 9, 36)) {
            playClick();
            if (pageIndex < maxPage) pageIndex++;
            return true;
        }
        if (checkClick(x, y, 52, 53, 18, 18)) {
            if (selection >= 0) {
                playClick();
                int mode = Keybinds.BATCH.isDown() ? 1 : 0;
                PacketWire.sendToServer(
                        AnvilCraftLayout.encode(
                                menu.containerId, recipes.get(selection).getInternalName(), mode));
            }
            return true;
        }
        if (checkClick(x, y, 88, 53, 18, 18)) {
            playClick();
            setFocused(null);
            OverlayType[] values = OverlayType.values();
            filter = values[(filter.ordinal() + 1) % values.length];
            regenerateRecipes();
            return true;
        }
        for (int i = pageIndex * 2; i < pageIndex * 2 + 10 && i < recipes.size(); i++) {
            int ind = i - pageIndex * 2;
            int ix = 16 + 18 * (ind / 2), iy = 71 + 18 * (ind % 2);
            if (checkClick(x, y, ix, iy, 18, 18)) {
                selection = selection == i ? -1 : i;
                playClick();
                return true;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (checkClick((int) mouseX, (int) mouseY, 0, 0, imageWidth, imageHeight)
                && hoveredSlot == null) {
            if (scrollY > 0 && pageIndex > 0) pageIndex--;
            if (scrollY < 0 && pageIndex < maxPage) pageIndex++;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                leftPos,
                topPos,
                0.0F,
                0.0F,
                imageWidth,
                imageHeight,
                256,
                256);
        if (search != null && search.isFocused()) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    leftPos + 8,
                    topPos + 108,
                    168,
                    222,
                    88,
                    16,
                    256,
                    256);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        List<Component> selectedLines =
                selection >= 0 ? recipeToList(recipes.get(selection)) : List.of();
        int longest = 0;
        for (Component s : selectedLines) longest = Math.max(longest, font.width(s));
        lastSize = selection >= 0 ? (int) (longest * 0.5F) : 0;

        int slide = recipePanelSlide();
        int mul = 1;
        while (slide >= 51 * mul) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    125 + 51 * mul,
                    17,
                    125,
                    17,
                    54,
                    108,
                    256,
                    256);
            mul++;
        }
        graphics.blit(
                RenderPipelines.GUI_TEXTURED, TEXTURE, 125 + slide, 17, 125, 17, 54, 108, 256, 256);

        if (checkClick(mouseX, mouseY, 7, 71, 9, 36)) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 7, 71, 176, 186, 9, 36, 256, 256);
        }
        if (checkClick(mouseX, mouseY, 106, 71, 9, 36)) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 106, 71, 185, 186, 9, 36, 256, 256);
        }
        if (checkClick(mouseX, mouseY, 52, 53, 18, 18)) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 52, 53, 176, 150, 18, 18, 256, 256);
        }
        if (filter != OverlayType.NONE) {
            int u =
                    switch (filter) {
                        case SMITHING -> 200;
                        case CONSTRUCTION -> 218;
                        case RECYCLING -> 236;
                        default -> throw new IllegalStateException();
                    };
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 88, 53, u, 0, 18, 18, 256, 256);
            if (checkClick(mouseX, mouseY, 88, 53, 18, 18)) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED, TEXTURE, 88, 53, u, 18, 18, 18, 256, 256);
            }
        }
        String filterKey =
                switch (filter) {
                    case NONE -> "gui.hbm.anvil.filter.all";
                    case SMITHING -> "gui.hbm.anvil.filter.smithing";
                    case CONSTRUCTION -> "gui.hbm.anvil.filter.construction";
                    case RECYCLING -> "gui.hbm.anvil.filter.recycling";
                };
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                88,
                53,
                18,
                18,
                List.of(Component.translatable(filterKey)));

        for (int i = pageIndex * 2; i < pageIndex * 2 + 10 && i < recipes.size(); i++) {
            int ind = i - pageIndex * 2;
            AnvilConstructionRecipe recipe = recipes.get(i);

            graphics.item(recipe.getDisplay(), 17 + 18 * (ind / 2), 72 + 18 * (ind % 2));

            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    16 + 18 * (ind / 2),
                    71 + 18 * (ind % 2),
                    18 + 18 * recipe.getOverlay().ordinal(),
                    222,
                    18,
                    18,
                    256,
                    256);

            if (selection == i) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        16 + 18 * (ind / 2),
                        71 + 18 * (ind % 2),
                        0,
                        222,
                        18,
                        18,
                        256,
                        256);
            }
        }

        String name = title.getString();
        graphics.text(font, name, 61 - font.width(name) / 2, 8, 0xFF404040, false);
        graphics.text(font, playerInventoryTitle, 8, imageHeight - 96 + 2, 0xFF404040, false);

        if (selection >= 0) {
            graphics.pose().pushMatrix();
            graphics.pose().scale(0.5F, 0.5F);
            int offset = 0;
            for (Component s : selectedLines) {
                graphics.text(font, s, 260, 50 + offset, CommonColors.WHITE, false);
                offset += 9;
            }
            graphics.pose().popMatrix();
        }
    }

    public Rect2i recipePanelArea() {
        return new Rect2i(leftPos + 125, topPos + 17, 54 + recipePanelSlide(), 108);
    }

    private int recipePanelSlide() {
        return Mth.clamp(lastSize - 42, 0, 1000);
    }

    private List<Component> recipeToList(AnvilConstructionRecipe recipe) {
        List<Component> list = new ArrayList<>();

        list.add(Component.translatable("info.template_in_p").withStyle(ChatFormatting.YELLOW));
        for (CountIngredient ci : recipe.input()) {
            ItemStack display = AnvilConstructionRecipe.representativeStack(ci);
            boolean has =
                    minecraft != null
                            && minecraft.player != null
                            && InventoryUtil.hasIngredients(minecraft.player, List.of(ci));
            var line = Component.literal(">" + ci.count() + "x ").append(display.getHoverName());
            list.add(has ? line : line.withStyle(ChatFormatting.RED));
        }

        list.add(Component.empty());
        list.add(Component.translatable("info.template_out_p").withStyle(ChatFormatting.YELLOW));
        for (AnvilOutput out : recipe.outputs()) {
            var line =
                    Component.literal(">" + out.stack().getCount() + "x ")
                            .append(out.stack().getHoverName());
            if (out.chance() != 1F)
                line.append(" (")
                        .append(Component.translatable("unit.percent", out.chance() * 100))
                        .append(")");
            list.add(line);
        }

        return list;
    }

    private List<String> recipeToSearchList(AnvilConstructionRecipe recipe) {
        List<String> list = new ArrayList<>();

        for (CountIngredient ci : recipe.input()) {
            for (ItemStack stack : ci.displayStacks()) {
                list.add(stack.getHoverName().getString().toLowerCase(Locale.ROOT));
            }
        }
        for (AnvilOutput out : recipe.outputs()) {
            list.add(out.stack().getHoverName().getString().toLowerCase(Locale.ROOT));
        }

        return list;
    }
}
