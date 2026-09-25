// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuMachineAmmoPress;
import com.hbm.inventory.recipes.AmmoPressRecipe;
import com.hbm.inventory.recipes.AmmoPressRecipes;
import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class ScreenMachineAmmoPress extends ScreenInfoContainer<MenuMachineAmmoPress> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/processing/gui_ammo_press.png");

    private final List<AmmoPressRecipe> recipes = new ArrayList<>();
    private int index;
    private int size;
    private int selection;
    private EditBox search;

    public ScreenMachineAmmoPress(MenuMachineAmmoPress menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 200);
        this.inventoryLabelY = this.imageHeight - 96 + 2;
        this.selection = menu.blockEntity().selectedRecipe;
        search("");
    }

    @Override
    protected void init() {
        super.init();
        search = new EditBox(font, leftPos + 10, topPos + 75, 66, 12, Component.empty());
        search.setTextColor(CommonColors.WHITE);
        search.setBordered(false);
        search.setMaxLength(25);
        search.setResponder(this::search);
        addRenderableWidget(search);
    }

    @Override
    protected int titleColor() {
        return CommonColors.WHITE;
    }

    private void search(String term) {
        String lower = term.toLowerCase(Locale.ROOT);
        recipes.clear();
        for (AmmoPressRecipe recipe : AmmoPressRecipes.INSTANCE.recipes()) {
            if (lower.isEmpty()
                    || recipe.output()
                            .getHoverName()
                            .getString()
                            .toLowerCase(Locale.ROOT)
                            .contains(lower)) {
                recipes.add(recipe);
            }
        }
        index = 0;
        size = Math.max(0, (int) Math.ceil((recipes.size() - 12) / 3D));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int x = (int) event.x(), y = (int) event.y();

        if (checkClick(x, y, 7, 17, 9, 54)) {
            playClick();
            if (index > 0) index--;
            return true;
        }
        if (checkClick(x, y, 88, 17, 9, 54)) {
            playClick();
            if (index < size) index++;
            return true;
        }
        for (int i = index * 3; i < index * 3 + 12 && i < recipes.size(); i++) {
            int ind = i - index * 3;
            if (checkClick(x, y, 16 + 18 * (ind / 3), 17 + 18 * (ind % 3), 18, 18)) {
                int clicked = AmmoPressRecipes.INSTANCE.recipes().indexOf(recipes.get(i));
                selection = selection != clicked ? clicked : -1;
                CompoundTag data = new CompoundTag();
                data.putInt("selection", selection);
                Services.NETWORK.sendToServer(
                        new NbtControlPayload(menu.blockEntity().getBlockPos(), data));
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
            if (scrollY > 0 && index > 0) index--;
            if (scrollY < 0 && index < size) index++;
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
        if (checkClick(mouseX, mouseY, 7, 17, 9, 54)) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    leftPos + 7,
                    topPos + 17,
                    176,
                    0,
                    9,
                    54,
                    256,
                    256);
        }
        if (checkClick(mouseX, mouseY, 88, 17, 9, 54)) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    leftPos + 88,
                    topPos + 17,
                    185,
                    0,
                    9,
                    54,
                    256,
                    256);
        }
        if (search != null && search.isFocused()) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    leftPos + 8,
                    topPos + 72,
                    176,
                    54,
                    70,
                    16,
                    256,
                    256);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        List<AmmoPressRecipe> all = AmmoPressRecipes.INSTANCE.recipes();
        for (int i = index * 3; i < index * 3 + 12 && i < recipes.size(); i++) {
            int ind = i - index * 3;
            int x = 16 + 18 * (ind / 3), y = 17 + 18 * (ind % 3);
            ItemStack output = recipes.get(i).output();
            graphics.item(output, x + 1, y + 1);
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    x,
                    y,
                    selection == all.indexOf(recipes.get(i)) ? 194 : 212,
                    0,
                    18,
                    18,
                    256,
                    256);
            graphics.pose().pushMatrix();
            graphics.pose().translate(x + 9, y + 9);
            graphics.pose().scale(0.5F, 0.5F);
            graphics.itemDecorations(font, output, 0, 0, Integer.toString(output.getCount()));
            graphics.pose().popMatrix();
        }

        if (selection >= 0 && selection < all.size()) {
            CountIngredient[] input = all.get(selection).input();
            for (int i = 0; i < input.length; i++) {
                if (input[i] == null || !menu.blockEntity().getItem(i).isEmpty()) continue;
                ItemStack ghost = input[i].extractForCyclingDisplay(20);
                int x = 116 + 18 * (i % 3), y = 18 + 18 * (i / 3);
                graphics.item(ghost, x, y);
                graphics.itemDecorations(
                        font,
                        ghost,
                        x,
                        y,
                        ghost.getCount() > 1 ? Integer.toString(ghost.getCount()) : null);
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        x,
                        y,
                        (float) x,
                        (float) y,
                        18,
                        18,
                        256,
                        256,
                        ARGB.white(0.5F));
            }
        }
        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (hoveredSlot != null && hoveredSlot.hasItem()) return;
        for (int i = index * 3; i < index * 3 + 12 && i < recipes.size(); i++) {
            int ind = i - index * 3;
            if (checkClick(mouseX, mouseY, 16 + 18 * (ind / 3), 17 + 18 * (ind % 3), 18, 18)) {
                graphics.setTooltipForNextFrame(font, recipes.get(i).output(), mouseX, mouseY);
            }
        }
    }
}
