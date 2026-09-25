// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes.crafting;

import com.hbm.items.weapon.grenade.*;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class GrenadeCraftingRecipe extends CustomRecipe {

    public static final GrenadeCraftingRecipe INSTANCE = new GrenadeCraftingRecipe();
    public static final MapCodec<GrenadeCraftingRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, GrenadeCraftingRecipe> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<GrenadeCraftingRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public static boolean compatible(
            ItemGrenadeShell.EnumGrenadeShell shell,
            ItemGrenadeFilling.EnumGrenadeFilling filling) {
        return switch (filling) {
            case POWDER, HE, DEMO, INC, WP, CLUSTER ->
                    shell == ItemGrenadeShell.EnumGrenadeShell.FRAG
                            || shell == ItemGrenadeShell.EnumGrenadeShell.STICK;
            case EMP, PLASMA, LASER -> shell == ItemGrenadeShell.EnumGrenadeShell.TECH;
            case CLUSTER_HEAVY, NUCLEAR, NUCLEAR_DEMO, SCHRAB ->
                    shell == ItemGrenadeShell.EnumGrenadeShell.NUKE;
        };
    }

    private static Parts read(CraftingInput input) {
        ItemGrenadeShell.EnumGrenadeShell shell = null;
        ItemGrenadeFilling.EnumGrenadeFilling filling = null;
        ItemGrenadeFuze.EnumGrenadeFuze fuze = null;
        ItemGrenadeExtra.EnumGrenadeExtra extra = null;
        int count = 0;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) continue;
            if (++count > 4) return null;
            if (stack.getItem() instanceof ItemGrenadeShell part) {
                if (shell != null) return null;
                shell = part.type;
            } else if (stack.getItem() instanceof ItemGrenadeFilling part) {
                if (filling != null) return null;
                filling = part.type;
            } else if (stack.getItem() instanceof ItemGrenadeFuze part) {
                if (fuze != null) return null;
                fuze = part.type;
            } else if (stack.getItem() instanceof ItemGrenadeExtra part) {
                if (extra != null) return null;
                extra = part.type;
            } else {
                return null;
            }
        }
        return shell == null || filling == null || fuze == null
                ? null
                : new Parts(shell, filling, fuze, extra);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        Parts parts = read(input);
        return parts != null && compatible(parts.shell, parts.filling);
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        Parts parts = read(input);
        return parts == null
                ? ItemStack.EMPTY
                : ItemGrenadeUniversal.make(parts.shell, parts.filling, parts.fuze, parts.extra);
    }

    @Override
    public RecipeSerializer<GrenadeCraftingRecipe> getSerializer() {
        return SERIALIZER;
    }

    private record Parts(
            ItemGrenadeShell.EnumGrenadeShell shell,
            ItemGrenadeFilling.EnumGrenadeFilling filling,
            ItemGrenadeFuze.EnumGrenadeFuze fuze,
            ItemGrenadeExtra.EnumGrenadeExtra extra) {}
}
