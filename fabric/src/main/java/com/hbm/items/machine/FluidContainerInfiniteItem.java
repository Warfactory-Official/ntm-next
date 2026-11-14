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

package com.hbm.items.machine;

import com.hbm.items.ModItems;
import com.hbm.registration.OverridePoint;
import com.hbm.util.RegistryUtil;
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

public class FluidContainerInfiniteItem extends Item {
    public static final OverridePoint<Condition> CONDITION =
            OverridePoint.of(RegistryUtil.id("infinite_fluid_condition"), new Condition() {});

    public interface Condition {
        default boolean operates(FluidContainerInfiniteItem item, ItemStack stack) {
            return true;
        }

        default void sample(FluidContainerInfiniteItem item, ItemStack stack, ServerLevel level) {}

        default void describe(
                FluidContainerInfiniteItem item, ItemStack stack, Consumer<Component> adder) {}
    }

    private final @Nullable Supplier<Fluid> fluid;
    private final int amount;
    private final int chance;

    public FluidContainerInfiniteItem(
            Properties props, @Nullable Supplier<Fluid> fluid, int amount) {
        this(props, fluid, amount, 1);
    }

    public FluidContainerInfiniteItem(
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
