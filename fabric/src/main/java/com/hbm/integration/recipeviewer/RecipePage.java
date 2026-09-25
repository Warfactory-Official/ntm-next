// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.recipeviewer;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public abstract class RecipePage<T> {

    private final Identifier id;
    private final Class<? extends T> rowType;

    protected RecipePage(Identifier id, Class<? extends T> rowType) {
        this.id = id;
        this.rowType = rowType;
    }

    public final Identifier id() {
        return id;
    }

    public final Class<? extends T> rowType() {
        return rowType;
    }

    public abstract Component title();

    public abstract ItemStack icon();

    public int width() {
        return RecipePanel.WIDTH;
    }

    public int height() {
        return RecipePanel.HEIGHT;
    }

    public abstract List<T> rows();

    public abstract Identifier rowId(T row);

    public abstract List<ItemStack> catalysts();

    public abstract void layout(T row, PageLayout layout);

    public void draw(T row, GuiGraphicsExtractor graphics) {}

    public void tooltip(T row, double mouseX, double mouseY, Consumer<Component> output) {}

    public boolean hidden(T row) {
        return false;
    }

    public @Nullable LookupFilter lookupFilter() {
        return null;
    }

    public enum Lookup {
        USES,

        RECIPES
    }

    @FunctionalInterface
    public interface LookupFilter {

        boolean answers(ItemStack looked, Lookup lookup);
    }
}
