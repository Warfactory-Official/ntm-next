/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

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
