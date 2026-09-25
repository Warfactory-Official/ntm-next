// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes.crafting;

import com.hbm.itempool.LoreBooks;
import com.hbm.items.ModItems;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public final class MkuRecipe extends CustomRecipe {

    public static final MkuRecipe INSTANCE = new MkuRecipe();
    public static final MapCodec<MkuRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, MkuRecipe> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<MkuRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    @Override
    public boolean matches(CraftingInput input, Level level) {

        if (!(level instanceof ServerLevel server)) return false;

        Pattern pattern = patternFor(server.getSeed());
        if (input.width() != pattern.width() || input.height() != pattern.height()) return false;

        for (int y = 0; y < pattern.height(); y++) {
            for (int x = 0; x < pattern.width(); x++) {
                ItemStack held = input.getItem(x, y);
                Item wanted = pattern.slots().get(x + y * pattern.width());
                if (wanted == null) {
                    if (!held.isEmpty()) return false;
                } else if (!held.is(wanted)) {
                    return false;
                }
            }
        }
        return true;
    }

    private record Pattern(long seed, int width, int height, List<@Nullable Item> slots) {}

    private static volatile @Nullable Pattern pattern;

    private static Pattern patternFor(long worldSeed) {
        Pattern cached = pattern;
        if (cached != null && cached.seed() == worldSeed) return cached;

        List<Item> layout = LoreBooks.mkuLayout(worldSeed);
        int left = 2;
        int right = 0;
        int top = 2;
        int bottom = 0;
        for (int i = 0; i < 9; i++) {
            if (layout.get(i) == null) continue;
            left = Math.min(left, i % 3);
            right = Math.max(right, i % 3);
            top = Math.min(top, i / 3);
            bottom = Math.max(bottom, i / 3);
        }

        int width = right - left + 1;
        int height = bottom - top + 1;
        List<@Nullable Item> slots = new ArrayList<>(width * height);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) slots.add(layout.get(x + left + (y + top) * 3));
        }

        Pattern built = new Pattern(worldSeed, width, height, Collections.unmodifiableList(slots));
        pattern = built;
        return built;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        return new ItemStack(ModItems.SYRINGE_MKUNICORN);
    }

    @Override
    public RecipeSerializer<MkuRecipe> getSerializer() {
        return SERIALIZER;
    }
}
