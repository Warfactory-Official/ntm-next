// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.material;

import com.hbm.NuclearTech;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.inventory.recipes.loader.INamedRecipe;
import com.hbm.inventory.recipes.loader.NtmRecipeInput;
import com.hbm.util.DataCodecs;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public record MaterialDistributionRecipe(HolderSet<Item> targets, List<MaterialStack> materials)
        implements Recipe<NtmRecipeInput>, INamedRecipe {

    public static final MapCodec<MaterialDistributionRecipe> MAP_CODEC =
            DataCodecs.recipe(
                    RecordCodecBuilder.<MaterialDistributionRecipe>mapCodec(
                                    i ->
                                            i.group(
                                                            RegistryCodecs.homogeneousList(
                                                                            Registries.ITEM)
                                                                    .fieldOf("targets")
                                                                    .forGetter(
                                                                            MaterialDistributionRecipe
                                                                                    ::targets),
                                                            MaterialStack.CODEC
                                                                    .listOf()
                                                                    .fieldOf("materials")
                                                                    .forGetter(
                                                                            MaterialDistributionRecipe
                                                                                    ::materials))
                                                    .apply(i, MaterialDistributionRecipe::new))
                            .validate(
                                    row ->
                                            row.targets()
                                                            .unwrap()
                                                            .right()
                                                            .filter(List::isEmpty)
                                                            .isPresent()
                                                    ? DataResult.error(
                                                            () ->
                                                                    "material_distribution targets must not be empty")
                                                    : DataResult.success(row)));

    public static final StreamCodec<RegistryFriendlyByteBuf, MaterialDistributionRecipe>
            STREAM_CODEC =
                    StreamCodec.composite(
                            ByteBufCodecs.holderSet(Registries.ITEM),
                            MaterialDistributionRecipe::targets,
                            MaterialStack.STREAM_CODEC.apply(ByteBufCodecs.list()),
                            MaterialDistributionRecipe::materials,
                            MaterialDistributionRecipe::new);

    public static final RecipeSerializer<MaterialDistributionRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public MaterialDistributionRecipe {
        materials = List.copyOf(materials);
    }

    public MaterialDistributionRecipe(
            @Nullable Item item, @Nullable TagKey<Item> tag, List<MaterialStack> materials) {
        this(
                tag != null
                        ? HolderSet.emptyNamed(BuiltInRegistries.ITEM, tag)
                        : HolderSet.direct(item.builtInRegistryHolder()),
                materials);
        assert (item == null) != (tag == null);
    }

    @Override
    public String getInternalName() {
        return targets.unwrap()
                .map(
                        tag -> "tag_" + targetName(tag.location()),
                        items ->
                                (items.size() == 1 ? "item_" : "items_")
                                        + items.stream()
                                                .map(
                                                        item ->
                                                                targetName(
                                                                        item.unwrapKey()
                                                                                .orElseThrow()
                                                                                .identifier()))
                                                .collect(Collectors.joining("__")));
    }

    private static String targetName(Identifier id) {
        String namespace =
                id.getNamespace().equals(NuclearTech.MOD_ID) ? "" : id.getNamespace() + "_";
        return namespace + id.getPath().replace('/', '_');
    }

    @Override
    public RecipeType<MaterialDistributionRecipe> getType() {
        return MatDistribution.INSTANCE.type();
    }

    @Override
    public RecipeSerializer<MaterialDistributionRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return INamedRecipe.BOOK_CATEGORY;
    }

    @Override
    public boolean matches(NtmRecipeInput input, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(NtmRecipeInput input) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }
}
