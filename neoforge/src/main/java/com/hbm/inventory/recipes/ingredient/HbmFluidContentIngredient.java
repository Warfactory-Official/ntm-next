// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes.ingredient;

import com.hbm.NuclearTech;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.items.machine.IFluidContainerItem;
import com.hbm.lib.Library;
import com.hbm.platform.IFluidHandlerView;
import com.hbm.platform.Services;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.stream.Stream;
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
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public record HbmFluidContentIngredient(Fluid fluid, int amount) implements ICustomIngredient {

    public static final Identifier ID = Library.id("fluid_content");

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
                                                    .forGetter(HbmFluidContentIngredient::amount))
                                    .apply(i, HbmFluidContentIngredient::new));

    private static final StreamCodec<RegistryFriendlyByteBuf, HbmFluidContentIngredient>
            STREAM_CODEC =
                    StreamCodec.composite(
                            ByteBufCodecs.registry(BuiltInRegistries.FLUID.key()),
                            HbmFluidContentIngredient::fluid,
                            ByteBufCodecs.VAR_INT,
                            HbmFluidContentIngredient::amount,
                            HbmFluidContentIngredient::new);

    @Override
    public boolean test(ItemStack stack) {
        if (stack.isEmpty()) return false;

        if (stack.getItem() instanceof IFluidContainerItem container) {
            FluidStackNTM content = container.getContent(stack);
            return container.machineConvertible()
                    && content.type() == fluid
                    && content.amount() == amount;
        }

        IFluidHandlerView view = Services.CAPS.findFluidHandler(stack);
        return view != null && view.extractable(fluid) == amount;
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

    private Stream<ItemStack> paying() {
        Stream<ItemStack> ours =
                BuiltInRegistries.ITEM.stream()
                        .filter(
                                item ->
                                        item instanceof IFluidContainerItem container
                                                && container.canStore(fluid))
                        .map(
                                item ->
                                        ((IFluidContainerItem) item)
                                                .filledContainer(
                                                        new ItemStack(item),
                                                        new FluidStackNTM(fluid, amount)));
        return Stream.concat(Stream.of(new ItemStack(fluid.getBucket())), ours).filter(this::test);
    }

    private static final DeferredRegister<IngredientType<?>> INGREDIENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.INGREDIENT_TYPES, NuclearTech.MOD_ID);

    public static final DeferredHolder<IngredientType<?>, IngredientType<HbmFluidContentIngredient>>
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
