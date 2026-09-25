// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.capability;

import com.hbm.blocks.ModBlocks;
import com.hbm.config.BalanceConfig;
import com.hbm.config.RadiationConfig;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.items.ModItems;
import com.hbm.items.machine.IFluidContainerItem;
import com.hbm.items.machine.ItemFluidTank;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public final class FluidContainerRows {

    public record Row(
            Supplier<? extends ItemLike> full,
            @Nullable Holder<Potion> potion,
            @Nullable Supplier<? extends ItemLike> empty,
            Supplier<Fluid> fluid,
            IntSupplier amount) {

        public boolean isFull(ItemStack stack) {
            return stack.is(full.get().asItem())
                    && (potion == null
                            || stack.getOrDefault(
                                            DataComponents.POTION_CONTENTS, PotionContents.EMPTY)
                                    .is(potion));
        }

        public ItemStack fullStack() {
            Item item = full.get().asItem();
            return potion == null
                    ? new ItemStack(item)
                    : PotionContents.createItemStack(item, potion);
        }

        public ItemStack emptyStack() {
            return empty == null ? ItemStack.EMPTY : new ItemStack(empty.get());
        }
    }

    private record FamilyFill(
            Supplier<? extends ItemLike> empty, Supplier<? extends ItemFluidTank> family) {}

    private static final List<Row> ROWS =
            List.of(
                    new Row(
                            () -> Items.POTION,
                            Potions.WATER,
                            () -> Items.GLASS_BOTTLE,
                            () -> NTMFluids.WATER,
                            () -> 250),
                    row(
                            () -> ModBlocks.RED_BARREL,
                            () -> ModItems.TANK_STEEL,
                            () -> NTMFluids.DIESEL,
                            () -> 10_000),
                    row(
                            () -> ModBlocks.PINK_BARREL,
                            () -> ModItems.TANK_STEEL,
                            () -> NTMFluids.KEROSENE,
                            () -> 10_000),
                    row(
                            () -> ModBlocks.LOX_BARREL,
                            () -> ModItems.TANK_STEEL,
                            () -> NTMFluids.OXYGEN,
                            () -> 10_000),
                    row(() -> ModBlocks.ORE_OIL, null, () -> NTMFluids.OIL, () -> 250),
                    row(
                            () -> ModBlocks.ORE_GNEISS_GAS,
                            null,
                            () -> NTMFluids.PETROLEUM,
                            () -> BalanceConfig.enable528 ? 50 : 250),
                    row(
                            () -> ModItems.BOTTLE_MERCURY,
                            () -> Items.GLASS_BOTTLE,
                            () -> NTMFluids.MERCURY,
                            () -> 1000),
                    row(() -> ModItems.NUGGET_MERCURY, null, () -> NTMFluids.MERCURY, () -> 125),
                    row(
                            () -> ModItems.ROD_ZIRNOX_TRITIUM,
                            () -> ModItems.ROD_ZIRNOX_EMPTY,
                            () -> NTMFluids.TRITIUM,
                            () -> 2000),
                    row(
                            () -> Items.EXPERIENCE_BOTTLE,
                            () -> Items.GLASS_BOTTLE,
                            () -> NTMFluids.XPJUICE,
                            () -> 100),
                    row(
                            () -> ModItems.CAN_MUG,
                            () -> ModItems.CAN_EMPTY,
                            () -> NTMFluids.MUG,
                            () -> 100));

    private static final List<FamilyFill> FAMILY_FILLS =
            List.of(
                    new FamilyFill(
                            () -> ModItems.DISPERSER_CANISTER_EMPTY,
                            () -> ModItems.DISPERSER_CANISTER.get()),
                    new FamilyFill(
                            () -> ModItems.GLYPHID_GLAND_EMPTY,
                            () -> ModItems.GLYPHID_GLAND.get()));

    private FluidContainerRows() {}

    private static Row row(
            Supplier<? extends ItemLike> full,
            @Nullable Supplier<? extends ItemLike> empty,
            Supplier<Fluid> fluid,
            IntSupplier amount) {
        return new Row(full, null, empty, fluid, amount);
    }

    public static @Nullable Row full(ItemStack stack) {
        for (Row row : ROWS) {
            if (row.isFull(stack)) return row;
        }
        return null;
    }

    public static int fillAmount(ItemStack stack, Fluid fluid) {
        Row row = fillRow(stack, fluid);
        if (row != null) return row.amount().getAsInt();
        FamilyFill fill = familyFill(stack, fluid);
        return fill == null ? 0 : fill.family().get().capacity;
    }

    public static ItemStack filled(ItemStack stack, Fluid fluid) {
        Row row = fillRow(stack, fluid);
        if (row != null) return row.fullStack();
        FamilyFill fill = familyFill(stack, fluid);
        if (fill == null) throw new IllegalArgumentException(stack + " fills with no " + fluid);
        ItemFluidTank family = fill.family().get();
        return family.make(fluid, family.capacity);
    }

    public static boolean answers(ItemStack stack) {
        return full(stack) != null || Resolved.EMPTY_ITEMS.contains(stack.getItem());
    }

    public static List<Item> fullItems() {
        List<Item> out = new ArrayList<>();
        for (Row row : ROWS) add(out, row.full().get().asItem());
        return out;
    }

    public static List<Item> emptyItems() {
        List<Item> out = new ArrayList<>();
        for (Row row : ROWS) {
            if (row.empty() != null) add(out, row.empty().get().asItem());
        }
        for (FamilyFill fill : FAMILY_FILLS) add(out, fill.empty().get().asItem());
        return out;
    }

    public static List<Item> items() {
        List<Item> out = fullItems();
        for (Item item : emptyItems()) add(out, item);
        return out;
    }

    private static void add(List<Item> out, Item item) {

        assert !(item instanceof IFluidContainerItem) : item;
        if (!out.contains(item)) out.add(item);
    }

    private static final class Resolved {
        static final Set<Item> EMPTY_ITEMS = Set.copyOf(emptyItems());
    }

    private static @Nullable Row fillRow(ItemStack stack, Fluid fluid) {
        for (Row row : ROWS) {
            if (row.empty() != null
                    && row.fluid().get() == fluid
                    && stack.is(row.empty().get().asItem())) return row;
        }
        return null;
    }

    private static @Nullable FamilyFill familyFill(ItemStack stack, Fluid fluid) {
        for (FamilyFill fill : FAMILY_FILLS) {
            if (stack.is(fill.empty().get().asItem()) && fill.family().get().canStore(fluid))
                return fill;
        }
        return null;
    }
}
