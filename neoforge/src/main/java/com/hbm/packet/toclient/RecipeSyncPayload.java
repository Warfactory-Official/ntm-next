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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import org.jetbrains.annotations.NotNull;

public record RecipeSyncPayload(List<RecipeHolder<?>> recipes) implements CustomPacketPayload {

    public static final Type<RecipeSyncPayload> TYPE = new Type<>(Library.id("recipe_sync"));
    private static final StreamCodec<RegistryFriendlyByteBuf, Recipe<?>> RECIPE_CODEC =
            Identifier.STREAM_CODEC
                    .<RegistryFriendlyByteBuf>cast()
                    .map(
                            BuiltInRegistries.RECIPE_SERIALIZER::getValue,
                            BuiltInRegistries.RECIPE_SERIALIZER::getKey)
                    .dispatch(Recipe::getSerializer, RecipeSerializer::streamCodec);
    private static final StreamCodec<RegistryFriendlyByteBuf, RecipeHolder<?>> HOLDER_CODEC =
            StreamCodec.composite(
                    ResourceKey.streamCodec(Registries.RECIPE),
                    RecipeHolder::id,
                    RECIPE_CODEC,
                    RecipeHolder::value,
                    RecipeHolder::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, RecipeSyncPayload> STREAM_CODEC =
            HOLDER_CODEC
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
