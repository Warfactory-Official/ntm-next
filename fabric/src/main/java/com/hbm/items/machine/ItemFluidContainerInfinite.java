// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.machine;

import com.hbm.items.ModItems;
import com.hbm.lib.Library;
import com.hbm.registration.OverridePoint;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public class ItemFluidContainerInfinite extends Item {
    public static final OverridePoint<Condition> CONDITION =
            OverridePoint.of(Library.id("infinite_fluid_condition"), new Condition() {});

    public interface Condition {
        default boolean operates(ItemFluidContainerInfinite item, ItemStack stack) {
            return true;
        }

        default void sample(ItemFluidContainerInfinite item, ItemStack stack, ServerLevel level) {}

        default void describe(
                ItemFluidContainerInfinite item, ItemStack stack, Consumer<Component> adder) {}
    }

    private final @Nullable Supplier<Fluid> fluid;
    private final int amount;
    private final int chance;

    public ItemFluidContainerInfinite(
            Properties props, @Nullable Supplier<Fluid> fluid, int amount) {
        this(props, fluid, amount, 1);
    }

    public ItemFluidContainerInfinite(
            Properties props, @Nullable Supplier<Fluid> fluid, int amount, int chance) {
        super(props);
        this.fluid = fluid;
        this.amount = amount;
        this.chance = chance;
    }

    public @Nullable Fluid fluid() {
        return fluid == null ? null : fluid.get();
    }

    public int amount() {
        return amount;
    }

    public int chance() {
        return chance;
    }

    public boolean allowPressure(int pressure) {
        return this == ModItems.FLUID_BARREL_INFINITE.get() || pressure == 0;
    }

    public boolean operates(ItemStack stack) {
        return CONDITION.get().operates(this, stack);
    }

    @Override
    public void inventoryTick(
            ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        CONDITION.get().sample(this, stack, level);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        CONDITION.get().describe(this, stack, adder);
    }
}
