// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.lib.Library;
import com.hbm.packet.PacketWire;
import com.hbm.packet.toserver.NbtControlPayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.CommonColors;
import org.jspecify.annotations.Nullable;

public class GUIScreenRecipeSelector extends Screen {

    public static final String NULL_SELECTION = "null";
    private static final Identifier TEXTURE =
            Library.id("textures/gui/processing/gui_recipe_selector.png");
    private final int xSize = 176;
    private final int ySize = 132;
    private final GenericRecipes<?, ?> recipeSet;
    private final List<GenericRecipe> recipes = new ArrayList<>();
    private final BlockPos tilePos;
    private final Screen previousScreen;
    private final int index;
    private final @Nullable String installedPool;
    private int guiLeft;
    private int guiTop;
    private EditBox search;
    private int pageIndex;
    private int size;
    private String selection;

    public GUIScreenRecipeSelector(
            GenericRecipes<?, ?> recipeSet,
            BlockPos tilePos,
            String currentSelection,
            int index,
            @Nullable String installedPool,
            Screen previousScreen) {
        super(Component.translatable("gui.recipe.selector"));
        this.recipeSet = recipeSet;
        this.tilePos = tilePos;
        selection = currentSelection == null ? NULL_SELECTION : currentSelection;
        this.index = index;
        this.installedPool = installedPool;
        this.previousScreen = previousScreen;
        regenerateRecipes();
    }

    public static void openSelector(
            GenericRecipes<?, ?> recipeSet,
            BlockPos tilePos,
            String currentSelection,
            Screen previousScreen) {
        openSelector(recipeSet, tilePos, currentSelection, 0, null, previousScreen);
    }

    public static void openSelector(
            GenericRecipes<?, ?> recipeSet,
            BlockPos tilePos,
            String currentSelection,
            int index,
            @Nullable String installedPool,
            Screen previousScreen) {
        Minecraft.getInstance()
                .gui
                .setScreen(
                        new GUIScreenRecipeSelector(
                                recipeSet,
                                tilePos,
                                currentSelection,
                                index,
                                installedPool,
                                previousScreen));
    }

    @Override
    protected void init() {
        guiLeft = (width - xSize) / 2;
        guiTop = (height - ySize) / 2;

        search = new EditBox(font, guiLeft + 28, guiTop + 111, 102, 12, Component.empty());
        search.setTextColor(CommonColors.WHITE);
        search.setBordered(false);
        search.setMaxLength(32);
        search.setResponder(this::search);
        addRenderableWidget(search);
    }

    private void regenerateRecipes() {
        recipes.clear();
        for (GenericRecipe recipe : recipeSet.recipes()) {
            if (admitted(recipe)) recipes.add(recipe);
        }
        resetPaging();
    }

    private void search(String term) {
        recipes.clear();
        if (term.isEmpty()) {
            regenerateRecipes();
            return;
        }
        for (GenericRecipe recipe : recipeSet.recipes()) {
            if (admitted(recipe) && recipe.matchesSearch(term)) recipes.add(recipe);
        }
        resetPaging();
    }

    private boolean admitted(GenericRecipe recipe) {
        return !recipe.isPooled() || (installedPool != null && recipe.isPartOfPool(installedPool));
    }

    private void resetPaging() {
        pageIndex = 0;
        size = Math.max(0, (int) Math.ceil((recipes.size() - 40) / 8D));
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                guiLeft,
                guiTop,
                0.0F,
                0.0F,
                xSize,
                ySize,
                256,
                256);

        if (search.isFocused()) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    guiLeft + 26,
                    guiTop + 108,
                    0.0F,
                    132.0F,
                    106,
                    16,
                    256,
                    256);
        }

        hoverBlit(graphics, mouseX, mouseY, 152, 18, 176, 0);
        hoverBlit(graphics, mouseX, mouseY, 152, 36, 176, 16);
        hoverBlit(graphics, mouseX, mouseY, 152, 90, 176, 32);
        hoverBlit(graphics, mouseX, mouseY, 134, 108, 176, 48);
        hoverBlit(graphics, mouseX, mouseY, 8, 108, 176, 64);

        for (int i = pageIndex * 8; i < pageIndex * 8 + 40 && i < recipes.size(); i++) {
            int ind = i - pageIndex * 8;
            if (recipes.get(i).getInternalName().equals(selection)) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        guiLeft + 7 + 18 * (ind % 8),
                        guiTop + 17 + 18 * (ind / 8),
                        192.0F,
                        0.0F,
                        18,
                        18,
                        256,
                        256);
            }
        }

        for (int i = pageIndex * 8; i < pageIndex * 8 + 40 && i < recipes.size(); i++) {
            int ind = i - pageIndex * 8;
            graphics.item(
                    recipes.get(i).getIcon(),
                    guiLeft + 8 + 18 * (ind % 8),
                    guiTop + 18 + 18 * (ind / 8));
        }

        GenericRecipe selected = recipeSet.getRecipe(selection);
        if (selected != null) graphics.item(selected.getIcon(), guiLeft + 152, guiTop + 72);
    }

    private void hoverBlit(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, int x, int y, int u, int v) {
        if (isOver(mouseX, mouseY, x, y, 16, 16)) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    guiLeft + x,
                    guiTop + y,
                    (float) u,
                    (float) v,
                    16,
                    16,
                    256,
                    256);
        }
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        if (isOver(mouseX, mouseY, 7, 17, 144, 90)) {
            for (int i = pageIndex * 8; i < pageIndex * 8 + 40 && i < recipes.size(); i++) {
                int ind = i - pageIndex * 8;
                int ix = 7 + 18 * (ind % 8), iy = 17 + 18 * (ind / 8);
                if (isOver(mouseX, mouseY, ix, iy, 18, 18)) {
                    GUIElements.drawHoveringTextRecipe(
                            graphics, font, recipes.get(i).print(), mouseX, mouseY, width, height);
                }
            }
        }

        GenericRecipe selected = recipeSet.getRecipe(selection);
        if (selected != null && isOver(mouseX, mouseY, 151, 71, 18, 18)) {
            GUIElements.drawHoveringTextRecipe(
                    graphics, font, selected.print(), mouseX, mouseY, width, height);
        }
        if (isOver(mouseX, mouseY, 152, 90, 16, 16)) {
            graphics.setComponentTooltipForNextFrame(
                    font,
                    List.of(
                            Component.translatable("desc.gui.recipeSelector.close")
                                    .withStyle(ChatFormatting.YELLOW)),
                    mouseX,
                    mouseY);
        }
        if (isOver(mouseX, mouseY, 134, 108, 16, 16)) {
            graphics.setComponentTooltipForNextFrame(
                    font,
                    List.of(
                            Component.translatable("desc.gui.recipeSelector.clearSearch")
                                    .withStyle(ChatFormatting.YELLOW)),
                    mouseX,
                    mouseY);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        int x = (int) event.x(), y = (int) event.y();

        if (isOver(x, y, 152, 18, 16, 16)) {
            if (pageIndex > 0) pageIndex--;
            return true;
        }
        if (isOver(x, y, 152, 36, 16, 16)) {
            if (pageIndex < size) pageIndex++;
            return true;
        }

        if (isOver(x, y, 134, 108, 16, 16)) {
            search.setValue("");
            search.setFocused(true);
            return true;
        }

        for (int i = pageIndex * 8; i < pageIndex * 8 + 40 && i < recipes.size(); i++) {
            int ind = i - pageIndex * 8;
            int ix = 7 + 18 * (ind % 8), iy = 17 + 18 * (ind / 8);
            if (isOver(x, y, ix, iy, 18, 18)) {
                String clicked = recipes.get(i).getInternalName();
                selection = clicked.equals(selection) ? NULL_SELECTION : clicked;
                return true;
            }
        }

        if (isOver(x, y, 151, 71, 18, 18) && !NULL_SELECTION.equals(selection)) {
            selection = NULL_SELECTION;
            return true;
        }

        if (isOver(x, y, 152, 90, 16, 16)) {
            onClose();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY > 0 && pageIndex > 0) pageIndex--;
        if (scrollY < 0 && pageIndex < size) pageIndex++;
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 257 || event.key() == 335) {
            search.setFocused(!search.isFocused());
            return true;
        }
        if (!search.isFocused()
                && minecraft != null
                && minecraft.options.keyInventory.matches(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        CompoundTag data = new CompoundTag();
        data.putString("selection", selection);
        data.putInt("index", index);
        PacketWire.sendToServer(new NbtControlPayload(tilePos, data));
        if (minecraft != null) minecraft.gui.setScreen(previousScreen);
        Minecraft.getInstance()
                .getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1F));
    }

    private boolean isOver(int mx, int my, int x, int y, int w, int h) {
        return mx >= guiLeft + x && mx < guiLeft + x + w && my >= guiTop + y && my < guiTop + y + h;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
