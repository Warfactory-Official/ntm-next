// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes.ingredient;

import com.hbm.NuclearTech;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.items.machine.IFluidContainerItem;
import com.hbm.lib.Library;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.advancements.predicates.DataComponentMatchers;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public record HbmMatchersIngredient(Ingredient base, DataComponentMatchers matchers)
        implements ICustomIngredient {

    public static final Identifier ID = Library.id("matchers");

    private static final MapCodec<HbmMatchersIngredient> CODEC =
            RecordCodecBuilder.mapCodec(
                    i ->
                            i.group(
                                            Ingredient.CODEC
                                                    .fieldOf("base")
                                                    .forGetter(HbmMatchersIngredient::base),
                                            DataComponentMatchers.CODEC.forGetter(
                                                    HbmMatchersIngredient::matchers))
                                    .apply(i, HbmMatchersIngredient::new));

    private static final StreamCodec<RegistryFriendlyByteBuf, HbmMatchersIngredient> STREAM_CODEC =
            StreamCodec.composite(
                    Ingredient.CONTENTS_STREAM_CODEC,
                    HbmMatchersIngredient::base,
                    ByteBufCodecs.fromCodecWithRegistries(DataComponentMatchers.CODEC.codec()),
                    HbmMatchersIngredient::matchers,
                    HbmMatchersIngredient::new);

    @Override
    public boolean test(ItemStack stack) {
        return base.test(stack) && matchers.test(stack);
    }

    @Override
    public SlotDisplay display() {
        List<SlotDisplay> shown = new ArrayList<>();
        for (Holder<Item> item : items().toList()) {
            ItemStack stack = new ItemStack(item);
            stack.applyComponents(matchers.exact().asPatch());
            if (matchers.partial().get(FluidContentPredicate.TYPE.get())
                            instanceof FluidContentPredicate(Fluid fluid)
                    && fluid != Fluids.EMPTY
                    && stack.getItem() instanceof IFluidContainerItem container
                    && container.canStore(fluid)) {
                stack =
                        container.filledContainer(
                                stack, new FluidStackNTM(fluid, container.capacity(stack)));
            }
            shown.add(
                    new SlotDisplay.ItemStackSlotDisplay(
                            ItemStackTemplate.fromNonEmptyStack(stack)));
        }
        return new SlotDisplay.Composite(shown);
    }

    @Override
    public Stream<Holder<Item>> items() {
        return base.items();
    }

    private static final DeferredRegister<IngredientType<?>> INGREDIENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.INGREDIENT_TYPES, NuclearTech.MOD_ID);

    public static final DeferredHolder<IngredientType<?>, IngredientType<HbmMatchersIngredient>>
            TYPE =
                    INGREDIENT_TYPES.register(
                            ID.getPath(), () -> new IngredientType<>(CODEC, STREAM_CODEC));

    public static void subscribe(IEventBus modBus) {
        INGREDIENT_TYPES.register(modBus);
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IngredientType<?> getType() {
        return TYPE.get();
    }
}
