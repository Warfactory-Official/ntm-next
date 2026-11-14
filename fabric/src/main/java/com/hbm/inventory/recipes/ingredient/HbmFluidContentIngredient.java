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

package com.hbm.inventory.recipes.ingredient;

import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.items.machine.FluidContainerItem;
import com.hbm.platform.Caps;
import com.hbm.util.RegistryUtil;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import java.util.stream.Stream;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public record HbmFluidContentIngredient(Fluid fluid, int amount, boolean drain)
        implements IngredientState, CustomIngredient {

    public static final Identifier ID = RegistryUtil.id("fluid_content");

    private static final MapCodec<HbmFluidContentIngredient> CODEC =
            RecordCodecBuilder.mapCodec(
                    i ->
                            i.group(
                                            BuiltInRegistries.FLUID
                                                    .byNameCodec()
                                                    .fieldOf("fluid")
                                                    .forGetter(HbmFluidContentIngredient::fluid),
                                            ExtraCodecs.POSITIVE_INT
                                                    .fieldOf("amount")
                                                    .forGetter(HbmFluidContentIngredient::amount),
                                            Codec.BOOL
                                                    .optionalFieldOf("drain", false)
                                                    .forGetter(HbmFluidContentIngredient::drain))
                                    .apply(i, HbmFluidContentIngredient::new));

    private static final StreamCodec<RegistryFriendlyByteBuf, HbmFluidContentIngredient>
            STREAM_CODEC =
                    StreamCodec.composite(
                            ByteBufCodecs.registry(BuiltInRegistries.FLUID.key()),
                            HbmFluidContentIngredient::fluid,
                            ByteBufCodecs.VAR_INT,
                            HbmFluidContentIngredient::amount,
                            ByteBufCodecs.BOOL,
                            HbmFluidContentIngredient::drain,
                            HbmFluidContentIngredient::new);

    public HbmFluidContentIngredient(Fluid fluid, int amount) {
        this(fluid, amount, false);
    }

    @Override
    public boolean test(ItemStack stack) {
        if (drain) return drainedRemainder(stack) != null;
        if (stack.isEmpty() || !acceptsState(stack)) return false;
        if (acceptsBdcl() && stack.is(ModItems.BDCL.get())) return true;

        if (stack.getItem() instanceof FluidContainerItem container) {
            FluidStackNTM content = container.getContent(stack);
            return container.machineConvertible()
                    && content.type() == fluid
                    && content.amount() == amount;
        }

        Storage<FluidVariant> store = Caps.findFluidStore(stack);
        return store != null && Caps.extractable(store, fluid) == amount;
    }

    @Override
    public boolean acceptsState(ItemStack stack) {
        if (drain) return true;
        if (stack.getComponentsPatch().isEmpty()) return true;
        if (acceptsBdcl() && stack.is(ModItems.BDCL.get())) return false;
        if (stack.getItem() instanceof FluidContainerItem) {
            return RecipeInputState.only(stack, ModDataComponents.FLUID_CONTENT.get());
        }
        return false;
    }

    public @Nullable ItemStack drainedRemainder(ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (acceptsBdcl() && stack.is(ModItems.BDCL.get()))
            return stack.getComponentsPatch().isEmpty() ? ItemStack.EMPTY : null;
        if (!(stack.getItem() instanceof FluidContainerItem container)) {
            ItemStack left = Caps.drainFluidContainer(stack, fluid, amount);
            if (left != null
                    && left.is(Items.BUCKET)
                    && (stack.is(Items.WATER_BUCKET) || stack.is(Items.LAVA_BUCKET)))
                left.applyComponents(stack.getComponentsPatch());
            return left;
        }
        FluidStackNTM held = container.getContent(stack);
        if (!container.machineConvertible() || held.type() != fluid || held.amount() < amount)
            return null;
        ItemStack copy = stack.copyWithCount(1);
        if (container.drain(copy, fluid, amount, held.pressure()) != amount) return null;
        ItemStack left =
                held.amount() == amount
                        ? container.emptyContainer(copy)
                        : container.filledContainer(
                                copy,
                                new FluidStackNTM(fluid, held.amount() - amount, held.pressure()));
        for (var change : stack.getComponentsPatch().map.entrySet()) {
            if (change.getKey() != ModDataComponents.FLUID_CONTENT.get()
                    && !Objects.equals(stack.get(change.getKey()), left.get(change.getKey())))
                return null;
        }
        return left;
    }

    @Override
    public Stream<Holder<Item>> items() {
        return paying().map(ItemStack::typeHolder);
    }

    @Override
    public SlotDisplay display() {
        return new SlotDisplay.Composite(
                paying().<SlotDisplay>map(
                                stack ->
                                        new SlotDisplay.ItemStackSlotDisplay(
                                                ItemStackTemplate.fromNonEmptyStack(stack)))
                        .toList());
    }

    private boolean acceptsBdcl() {
        return fluid == NTMFluids.LUBRICANT && amount == 1_000;
    }

    private Stream<ItemStack> paying() {
        Stream<ItemStack> ours =
                BuiltInRegistries.ITEM.stream()
                        .filter(
                                item ->
                                        item instanceof FluidContainerItem container
                                                && container.canStore(fluid))
                        .map(
                                item ->
                                        ((FluidContainerItem) item)
                                                .filledContainer(
                                                        new ItemStack(item),
                                                        new FluidStackNTM(fluid, amount)));
        Stream<ItemStack> listed =
                acceptsBdcl()
                        ? Stream.of(
                                new ItemStack(fluid.getBucket()),
                                new ItemStack(ModItems.BDCL.get()))
                        : Stream.of(new ItemStack(fluid.getBucket()));
        return Stream.concat(listed, ours).filter(this::test);
    }

    public static final CustomIngredientSerializer<HbmFluidContentIngredient> SERIALIZER =
            new Serializer();

    @Override
    public boolean requiresTesting() {
        return true;
    }

    @Override
    public CustomIngredientSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    private static final class Serializer
            implements CustomIngredientSerializer<HbmFluidContentIngredient> {

        @Override
        public Identifier getIdentifier() {
            return ID;
        }

        @Override
        public MapCodec<HbmFluidContentIngredient> getCodec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, HbmFluidContentIngredient> getStreamCodec() {
            return STREAM_CODEC;
        }
    }
}
