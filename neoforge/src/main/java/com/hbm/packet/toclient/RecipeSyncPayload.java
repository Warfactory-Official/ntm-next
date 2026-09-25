// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toclient;

import com.hbm.inventory.recipes.ArcFurnaceRecipes;
import com.hbm.inventory.recipes.ModRecipes;
import com.hbm.inventory.recipes.loader.RecipeSource;
import com.hbm.lib.Library;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import org.jetbrains.annotations.NotNull;

public record RecipeSyncPayload(List<RecipeHolder<?>> recipes) implements CustomPacketPayload {

    public static final Type<RecipeSyncPayload> TYPE = new Type<>(Library.id("recipe_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RecipeSyncPayload> STREAM_CODEC =
            RecipeHolder.STREAM_CODEC
                    .apply(ByteBufCodecs.list())
                    .map(RecipeSyncPayload::new, RecipeSyncPayload::recipes);

    public static RecipeSyncPayload of(MinecraftServer server) {
        Set<RecipeType<?>> tables = Set.copyOf(ModRecipes.datapackBackedTypes());
        List<RecipeHolder<?>> recipes = new ArrayList<>();
        for (RecipeHolder<?> holder : server.getRecipeManager().getRecipes()) {
            if (tables.contains(holder.value().getType())
                    || ArcFurnaceRecipes.yieldsFurnaceRows(holder)) {
                recipes.add(holder);
            }
        }
        return new RecipeSyncPayload(List.copyOf(recipes));
    }

    public static void apply(RecipeSyncPayload payload) {
        RecipeSource.publishClient(payload, payload.recipes);
    }

    @Override
    public @NotNull Type<RecipeSyncPayload> type() {
        return TYPE;
    }
}
