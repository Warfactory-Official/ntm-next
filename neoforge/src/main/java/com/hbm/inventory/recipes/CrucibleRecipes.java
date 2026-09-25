// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial.SmeltingBehavior;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.recipes.loader.SimpleGenericRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemMold;
import com.hbm.items.machine.ItemScraps;
import com.hbm.registration.RegistryHandle;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class CrucibleRecipes extends SimpleGenericRecipes<CrucibleRecipe> {

    public static final CrucibleRecipes INSTANCE = new CrucibleRecipes();

    @Override
    protected String registryName() {
        return "crucible";
    }

    @Override
    protected RecipeSerializer<CrucibleRecipe> serializer() {
        return CrucibleRecipe.SERIALIZER;
    }

    public static List<Cast> moldRecipes() {
        List<Cast> out = new ArrayList<>();

        for (NTMMaterial material : Mats.orderedList) {
            if (material.smeltable != SmeltingBehavior.SMELTABLE) continue;

            for (RegistryHandle<ItemMold> handle : ModItems.MOLDS) {
                ItemMold item = handle.get();
                ItemStack result = item.mold.getOutput(material);
                if (result == null) continue;

                out.add(
                        new Cast(
                                ItemScraps.create(
                                        new MaterialStack(material, item.mold.getCost()), true),
                                new ItemStack(item),
                                new ItemStack(
                                        item.mold.size == ItemMold.SIZE_SMALL
                                                ? ModBlocks.FOUNDRY_MOLD.get()
                                                : ModBlocks.FOUNDRY_BASIN.get()),
                                result));
            }
        }

        return out;
    }

    public record Cast(ItemStack metal, ItemStack mold, ItemStack block, ItemStack result) {}
}
