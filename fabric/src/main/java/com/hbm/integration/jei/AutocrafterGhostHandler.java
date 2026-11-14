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

package com.hbm.integration.jei;

import com.hbm.blockentity.machine.TemplateCrafterBlockEntity;
import com.hbm.client.gui.AutocrafterScreen;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;

public final class AutocrafterGhostHandler implements IGhostIngredientHandler<AutocrafterScreen> {

    @Override
    public <I> List<Target<I>> getTargetsTyped(
            AutocrafterScreen gui, ITypedIngredient<I> ingredient, boolean doStart) {
        if (ingredient.getCastIngredient(VanillaTypes.ITEM_STACK) == null) return List.of();

        List<Target<I>> targets = new ArrayList<>(TemplateCrafterBlockEntity.TEMPLATE_COUNT);
        for (int i = 0; i < TemplateCrafterBlockEntity.TEMPLATE_COUNT; i++) {
            int index = i;
            targets.add(
                    new Target<>() {
                        @Override
                        public Rect2i getArea() {
                            return gui.patternArea(index);
                        }

                        @Override
                        public void accept(I dropped) {
                            if (dropped instanceof ItemStack stack) gui.sendPattern(index, stack);
                        }
                    });
        }
        return targets;
    }

    @Override
    public void onComplete() {}
}
