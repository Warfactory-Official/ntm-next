// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes.loader;

import com.hbm.util.DataCodecs;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import org.jspecify.annotations.Nullable;

public final class Outputs {

    public static final int RESOLUTION = 1000;

    public static final Codec<WeightedList<ItemStack>> CODEC =
            WeightedList.codec(ItemStack.OPTIONAL_CODEC);
    public static final StreamCodec<RegistryFriendlyByteBuf, WeightedList<ItemStack>> STREAM_CODEC =
            WeightedList.streamCodec(ItemStack.OPTIONAL_STREAM_CODEC);

    private static final Codec<WeightedList<Optional<ItemStackTemplate>>> WEIGHTED =
            WeightedList.codec(
                    ExtraCodecs.optionalEmptyMap(
                            DataCodecs.strict(ItemStackTemplate.MAP_CODEC).codec()));
    private static final Codec<WeightedList<Optional<ItemStackTemplate>>> SINGLE =
            DataCodecs.strict(
                            Codec.mapPair(
                                    ItemStackTemplate.MAP_CODEC,
                                    Codec.FLOAT.optionalFieldOf("chance")))
                    .flatXmap(
                            pair ->
                                    pair.getSecond()
                                            .map(chance -> chance(pair.getFirst(), chance))
                                            .orElseGet(
                                                    () ->
                                                            DataResult.success(
                                                                    WeightedList.of(
                                                                            Optional.of(
                                                                                    pair
                                                                                            .getFirst())))),
                            list -> {
                                List<Weighted<Optional<ItemStackTemplate>>> entries = list.unwrap();
                                ItemStackTemplate first = entries.getFirst().value().orElseThrow();
                                Integer weight = chanceWeight(entries);
                                return DataResult.success(
                                        Pair.of(
                                                first,
                                                weight == null
                                                        ? Optional.empty()
                                                        : Optional.of(
                                                                weight / (float) RESOLUTION)));
                            })
                    .codec();

    public static final Codec<WeightedList<Optional<ItemStackTemplate>>> TEMPLATE_CODEC =
            Codec.either(Item.CODEC, Codec.either(SINGLE, WEIGHTED))
                    .xmap(
                            either ->
                                    either.map(
                                            item ->
                                                    WeightedList.of(
                                                            Optional.of(
                                                                    new ItemStackTemplate(
                                                                            item,
                                                                            1,
                                                                            DataComponentPatch
                                                                                    .EMPTY))),
                                            list ->
                                                    list.map(
                                                            single -> single,
                                                            weighted -> weighted)),
                            Outputs::shortest);
    public static final StreamCodec<
                    RegistryFriendlyByteBuf, WeightedList<Optional<ItemStackTemplate>>>
            TEMPLATE_STREAM_CODEC =
                    WeightedList.streamCodec(
                            ByteBufCodecs.optional(ItemStackTemplate.STREAM_CODEC));

    private Outputs() {}

    private static DataResult<WeightedList<Optional<ItemStackTemplate>>> chance(
            ItemStackTemplate stack, float chance) {
        int weight = Math.round(chance * RESOLUTION);
        if (weight <= 0 || weight >= RESOLUTION) {
            return DataResult.error(
                    () ->
                            "chance "
                                    + chance
                                    + " rounds to weight "
                                    + weight
                                    + " of "
                                    + RESOLUTION
                                    + ", outside (0, "
                                    + RESOLUTION
                                    + ")");
        }
        return DataResult.success(
                WeightedList.<Optional<ItemStackTemplate>>builder()
                        .add(Optional.of(stack), weight)
                        .add(Optional.empty(), RESOLUTION - weight)
                        .build());
    }

    private static @Nullable Integer chanceWeight(
            List<Weighted<Optional<ItemStackTemplate>>> entries) {
        if (entries.size() != 2
                || entries.get(0).value().isEmpty()
                || entries.get(1).value().isPresent()) return null;
        int weight = entries.get(0).weight();
        if (weight <= 0 || weight >= RESOLUTION || weight + entries.get(1).weight() != RESOLUTION)
            return null;
        return Math.round(weight / (float) RESOLUTION * RESOLUTION) == weight ? weight : null;
    }

    private static Either<
                    Holder<Item>,
                    Either<
                            WeightedList<Optional<ItemStackTemplate>>,
                            WeightedList<Optional<ItemStackTemplate>>>>
            shortest(WeightedList<Optional<ItemStackTemplate>> list) {
        List<Weighted<Optional<ItemStackTemplate>>> entries = list.unwrap();
        if (entries.size() == 1
                && entries.getFirst().weight() == 1
                && entries.getFirst().value().isPresent()) {
            ItemStackTemplate only = entries.getFirst().value().get();
            return only.count() == 1 && only.components().isEmpty()
                    ? Either.left(only.item())
                    : Either.right(Either.left(list));
        }
        return chanceWeight(entries) != null
                ? Either.right(Either.left(list))
                : Either.right(Either.right(list));
    }

    public static WeightedList<ItemStack> fixed(ItemStack stack) {
        return WeightedList.of(stack);
    }

    public static WeightedList<ItemStack> chance(ItemStack stack, float chance) {
        if (chance >= 1F) return fixed(stack);
        int weight = Math.round(chance * RESOLUTION);
        if (weight <= 0) return WeightedList.of();
        return WeightedList.<ItemStack>builder()
                .add(stack, weight)
                .add(ItemStack.EMPTY, RESOLUTION - weight)
                .build();
    }

    @SafeVarargs
    public static WeightedList<ItemStack> weighted(Weighted<ItemStack>... entries) {
        return WeightedList.of(entries);
    }

    public static Weighted<ItemStack> entry(ItemStack stack, int weight) {
        return new Weighted<>(stack, weight);
    }
}
