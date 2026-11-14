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

package com.hbm.items.tool;

import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.trait.CorrosiveTrait;
import com.hbm.inventory.fluid.trait.FluidTraitSimple.FT_Amat;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.FluidContainerItem;
import com.hbm.util.TooltipStyle;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public class PipetteItem extends Item implements FluidContainerItem {

    private final Variant variant;

    public PipetteItem(Properties props, Variant variant) {
        super(props);
        this.variant = variant;
    }

    public Variant variant() {
        return variant;
    }

    public boolean fizzlesOn(Fluid type) {
        return variant == Variant.PLAIN
                && type != NTMFluids.PEROXIDE
                && NTMFluidProperties.hasTrait(type, CorrosiveTrait.class);
    }

    public int capacity(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.PIPETTE_CAPACITY.get(), variant.maxFill);
    }

    private void setCapacity(ItemStack stack, int capacity) {
        stack.set(
                ModDataComponents.PIPETTE_CAPACITY.get(),
                Math.clamp(capacity, variant.step, variant.maxFill));
    }

    @Override
    public boolean canStore(Fluid type) {
        return false;
    }

    @Override
    public boolean machineFillable() {
        return true;
    }

    @Override
    public Fluid firstFluidType(ItemStack stack) {
        return getContent(stack).type();
    }

    @Override
    public FluidStackNTM getContent(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.FLUID_CONTENT.get(), EMPTY_CONTENT);
    }

    @Override
    public void setContent(ItemStack stack, FluidStackNTM content) {
        if (content == null || content.type() == Fluids.EMPTY || content.amount() <= 0) {
            stack.remove(ModDataComponents.FLUID_CONTENT.get());
            return;
        }
        stack.set(
                ModDataComponents.FLUID_CONTENT.get(),
                new FluidStackNTM(
                        content.type(),
                        Math.min(content.amount(), capacity(stack)),
                        content.pressure()));
    }

    @Override
    public int fill(ItemStack stack, Fluid type, int amount, int pressure) {
        if (type == null || type == Fluids.EMPTY || amount <= 0) return 0;
        if (NTMFluidProperties.hasTrait(type, FT_Amat.class)) return 0;
        FluidStackNTM held = getContent(stack);
        boolean empty = held.type() == Fluids.EMPTY;
        if (!empty && (held.type() != type || held.pressure() != pressure)) return 0;

        int have = empty ? 0 : (int) held.amount();
        int accepted = Math.min(amount, capacity(stack) - have);
        if (accepted <= 0) return 0;
        setContent(stack, new FluidStackNTM(type, have + accepted, pressure));
        if (fizzlesOn(type)) stack.setCount(0);
        return accepted;
    }

    @Override
    public int drain(ItemStack stack, Fluid type, int amount, int pressure) {
        FluidStackNTM held = getContent(stack);
        if (held.type() != type || held.pressure() != pressure || amount <= 0) return 0;
        int taken = (int) Math.min(amount, held.amount());
        setContent(stack, new FluidStackNTM(type, held.amount() - taken, pressure));
        return taken;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (getContent(stack).amount() > 0) {
            player.sendSystemMessage(Component.translatable("desc.item.pipette.noEmpty"));
            return InteractionResult.SUCCESS;
        }
        int step = player.isShiftKeyDown() ? -variant.step : variant.step;
        setCapacity(stack, capacity(stack) + step);
        player.sendSystemMessage(
                Component.translatable(
                        "desc.item.pipette.dialled", capacity(stack), variant.maxFill));
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        for (String key : variant.notes) {
            for (Component line : TooltipStyle.lines(Component.translatableWithFallback(key, "")))
                adder.accept(line);
        }
        FluidStackNTM held = getContent(stack);
        adder.accept(
                Component.translatable(
                        "desc.item.pipette.fluid", NTMFluidProperties.getDisplayName(held.type())));
        adder.accept(
                Component.translatable(
                        "desc.item.pipette.amount",
                        held.amount(),
                        capacity(stack),
                        variant.maxFill));
    }

    public enum Variant {
        PLAIN(1_000, 50, "desc.item.pipette.noCorrosive"),
        BORON(1_000, 50, "desc.item.pipette.corrosive"),
        LABORATORY(50, 1, "desc.item.pipette.corrosive", "desc.item.pipette.laboratory");

        public final int maxFill;
        public final int step;
        final String[] notes;

        Variant(int maxFill, int step, String... notes) {
            this.maxFill = maxFill;
            this.step = step;
            this.notes = notes;
        }
    }
}
