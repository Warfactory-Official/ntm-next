// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.jei;

import com.hbm.client.gui.ScreenMachineAutocrafter;
import com.hbm.tileentity.machine.BlockEntityMachineAutocrafter;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;

public final class AutocrafterGhostHandler
        implements IGhostIngredientHandler<ScreenMachineAutocrafter> {

    @Override
    public <I> List<Target<I>> getTargetsTyped(
            ScreenMachineAutocrafter gui, ITypedIngredient<I> ingredient, boolean doStart) {
        if (ingredient.getCastIngredient(VanillaTypes.ITEM_STACK) == null) return List.of();

        List<Target<I>> targets = new ArrayList<>(BlockEntityMachineAutocrafter.GRID_SIZE);
        for (int i = 0; i < BlockEntityMachineAutocrafter.GRID_SIZE; i++) {
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
