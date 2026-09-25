// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.rei;

import com.hbm.integration.recipeviewer.RecipePage;
import java.util.List;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.view.ViewSearchBuilder;
import me.shedaniel.rei.api.client.view.Views;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.world.item.ItemStack;

final class ReiLookupFilter {

    private ReiLookupFilter() {}

    static boolean answers(ReiPageDisplay display) {
        RecipePage.LookupFilter filter = display.page().lookupFilter();
        if (filter == null) return true;
        ViewSearchBuilder search = Views.getInstance().getContext();
        if (search == null) return true;
        CategoryIdentifier<?> category = display.getCategoryIdentifier();
        if (search.getCategories().contains(category)) return true;
        boolean refused = false;
        for (EntryStack<?> looked : search.getUsagesFor()) {

            if (workstation(category, looked)) return true;
            if (!answers(filter, looked, RecipePage.Lookup.USES)) refused = true;
            else if (names(display.getInputEntries(), looked)) return true;
        }
        for (EntryStack<?> looked : search.getRecipesFor()) {
            if (!answers(filter, looked, RecipePage.Lookup.RECIPES)) refused = true;
            else if (names(display.getOutputEntries(), looked)) return true;
        }
        return !refused;
    }

    private static boolean answers(
            RecipePage.LookupFilter filter, EntryStack<?> looked, RecipePage.Lookup lookup) {
        return !(looked.getValue() instanceof ItemStack stack) || filter.answers(stack, lookup);
    }

    private static boolean workstation(CategoryIdentifier<?> category, EntryStack<?> looked) {
        for (EntryIngredient station :
                CategoryRegistry.getInstance().get(category).getWorkstations()) {
            if (EntryIngredients.testFuzzy(station, looked)) return true;
        }
        return false;
    }

    private static boolean names(List<EntryIngredient> sides, EntryStack<?> looked) {
        for (EntryIngredient ingredient : sides) {
            for (EntryStack<?> entry : ingredient) {
                if (EntryStacks.equalsFuzzy(entry, looked)) return true;
            }
        }
        return false;
    }
}
