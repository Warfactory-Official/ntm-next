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

package com.hbm.inventory.recipes.crafting;

import com.hbm.items.weapon.grenade.*;
import com.hbm.items.weapon.grenade.*;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class GrenadeCraftingRecipe extends HbmCustomRecipe {

    public static final GrenadeCraftingRecipe INSTANCE = new GrenadeCraftingRecipe();
    public static final MapCodec<GrenadeCraftingRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, GrenadeCraftingRecipe> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<GrenadeCraftingRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public static boolean compatible(
            GrenadeShellItem.EnumGrenadeShell shell, GrenadeFillingDefinition filling) {
        return filling.compatible(shell);
    }

    private static Parts read(CraftingInput input) {
        GrenadeShellItem.EnumGrenadeShell shell = null;
        GrenadeFillingDefinition filling = null;
        GrenadeFuzeItem.EnumGrenadeFuze fuze = null;
        GrenadeExtraItem.EnumGrenadeExtra extra = null;
        int count = 0;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) continue;
            if (++count > 4) return null;
            if (stack.getItem() instanceof GrenadeShellItem part) {
                if (shell != null) return null;
                shell = part.type;
            } else if (stack.getItem() instanceof GrenadeFillingItem part) {
                if (filling != null) return null;
                filling = part.type;
            } else if (stack.getItem() instanceof GrenadeFuzeItem part) {
                if (fuze != null) return null;
                fuze = part.type;
            } else if (stack.getItem() instanceof GrenadeExtraItem part) {
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
    protected boolean matchesInput(CraftingInput input, Level level) {
        Parts parts = read(input);
        return parts != null && compatible(parts.shell, parts.filling);
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        Parts parts = read(input);
        return parts == null
                ? ItemStack.EMPTY
                : GrenadeUniversalItem.make(parts.shell, parts.filling, parts.fuze, parts.extra);
    }

    @Override
    public RecipeSerializer<GrenadeCraftingRecipe> getSerializer() {
        return SERIALIZER;
    }

    private record Parts(
            GrenadeShellItem.EnumGrenadeShell shell,
            GrenadeFillingDefinition filling,
            GrenadeFuzeItem.EnumGrenadeFuze fuze,
            GrenadeExtraItem.EnumGrenadeExtra extra) {}
}
