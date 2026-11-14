/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.items.tool;

import com.hbm.interfaces.ToolType;
import com.hbm.interfaces.ToolTypeProvider;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluidProperty;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.FluidContainerItem;
import com.hbm.particle.helper.ParticleCreators;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;

public class BlowtorchItem extends Item implements FluidContainerItem, ToolTypeProvider {

    public static final int CAPACITY = 4_000;
    private static final int COST = 250;
    private static final int FILL_RATE = 50;

    private static final int[] GAS_CAPS = {CAPACITY};
    private static final int[] GAS_COSTS = {COST};
    private static final int[] ACETYLENE_CAPS = {8_000, 16_000};
    private static final int[] ACETYLENE_COSTS = {20, 10};
    private static final ChatFormatting[] GAUGE_COLORS = {
        ChatFormatting.YELLOW, ChatFormatting.AQUA
    };

    private final Variant variant;

    public BlowtorchItem(Properties props) {
        this(props, Variant.GAS);
    }

    public BlowtorchItem(Properties props, Variant variant) {
        super(props);
        this.variant = variant;
    }

    @Override
    public ToolType getToolType() {
        return ToolType.TORCH;
    }

    public Fluid[] fuels() {
        return variant == Variant.GAS
                ? new Fluid[] {NTMFluids.GAS}
                : new Fluid[] {NTMFluids.UNSATURATEDS, NTMFluids.OXYGEN};
    }

    public int[] capacities() {
        return variant == Variant.GAS ? GAS_CAPS : ACETYLENE_CAPS;
    }

    private int[] costs() {
        return variant == Variant.GAS ? GAS_COSTS : ACETYLENE_COSTS;
    }

    @Override
    public int capacity(ItemStack stack) {
        return capacities()[0];
    }

    public int fillOf(ItemStack stack, int tank) {
        if (variant == Variant.GAS) {
            FluidStackNTM content = stack.get(ModDataComponents.FLUID_CONTENT.get());
            return content == null ? CAPACITY : (int) content.amount();
        }
        List<FluidStackNTM> tanks = stack.get(ModDataComponents.TANK_CONTENTS.get());
        return tanks == null ? capacities()[tank] : (int) tanks.get(tank).amount();
    }

    private void setFill(ItemStack stack, int tank, int amount) {
        Fluid[] fuels = fuels();
        int[] caps = capacities();
        if (variant == Variant.GAS) {
            stack.set(
                    ModDataComponents.FLUID_CONTENT.get(),
                    new FluidStackNTM(fuels[0], Math.clamp(amount, 0, caps[0]), 0));
            return;
        }
        List<FluidStackNTM> written = new ArrayList<>(fuels.length);
        for (int i = 0; i < fuels.length; i++) {
            int held = i == tank ? Math.clamp(amount, 0, caps[i]) : fillOf(stack, i);
            written.add(new FluidStackNTM(fuels[i], held, 0));
        }
        stack.set(ModDataComponents.TANK_CONTENTS.get(), List.copyOf(written));
    }

    @Override
    public FluidStackNTM getContent(ItemStack stack) {
        int tank = leanest(stack);
        return new FluidStackNTM(fuels()[tank], fillOf(stack, tank), 0);
    }

    @Override
    public void setContent(ItemStack stack, FluidStackNTM content) {
        Fluid[] fuels = fuels();
        if (content == null) {
            for (int i = 0; i < fuels.length; i++) setFill(stack, i, 0);
            return;
        }
        for (int i = 0; i < fuels.length; i++) {
            if (content.type() != fuels[i]) continue;
            setFill(stack, i, (int) content.amount());
            return;
        }
        setFill(stack, 0, 0);
    }

    public ItemStack emptyTool() {
        ItemStack stack = new ItemStack(this);
        Fluid[] fuels = fuels();
        for (int i = 0; i < fuels.length; i++) setFill(stack, i, 0);
        return stack;
    }

    @Override
    public boolean machineFillable() {
        return true;
    }

    @Override
    public int fill(ItemStack stack, Fluid type, int amount, int pressure) {
        if (amount <= 0 || pressure != 0) return 0;
        Fluid[] fuels = fuels();
        for (int i = 0; i < fuels.length; i++) {
            if (fuels[i] != type) continue;
            int fill = fillOf(stack, i);
            int accepted = Math.min(Math.min(amount, FILL_RATE), capacities()[i] - fill);
            if (accepted <= 0) return 0;
            setFill(stack, i, fill + accepted);
            return accepted;
        }
        return 0;
    }

    @Override
    public int drain(ItemStack stack, Fluid type, int amount, int pressure) {
        return 0;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        ItemStack stack = ctx.getItemInHand();
        int[] costs = costs();
        for (int i = 0; i < costs.length; i++) {
            if (fillOf(stack, i) < costs[i]) return InteractionResult.PASS;
        }
        if (!ToolInteractions.useOn(ctx, getToolType())) return InteractionResult.PASS;
        Level level = ctx.getLevel();

        if (!level.isClientSide()) {
            for (int i = 0; i < costs.length; i++) setFill(stack, i, fillOf(stack, i) - costs[i]);
            Vec3 at = ctx.getClickLocation();
            ParticleCreators.sparks(level, at.x, at.y, at.z, 10, false, 50);
        }
        return InteractionResult.SUCCESS;
    }

    private int leanest(ItemStack stack) {
        int[] caps = capacities();
        int tank = 0;
        double lowest = Double.MAX_VALUE;
        for (int i = 0; i < caps.length; i++) {
            double fraction = (double) fillOf(stack, i) / caps[i];
            if (fraction < lowest) {
                lowest = fraction;
                tank = i;
            }
        }
        return tank;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        int tank = leanest(stack);
        return fillOf(stack, tank) < capacities()[tank];
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int tank = leanest(stack);
        return Math.round(fillOf(stack, tank) * 13.0F / capacities()[tank]);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        NTMFluidProperty prop = NTMFluidProperties.get(fuels()[leanest(stack)]);
        return prop != null ? ARGB.transparent(prop.color()) : 0xfffeed;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        Fluid[] fuels = fuels();
        int[] caps = capacities();
        for (int i = 0; i < fuels.length; i++) {
            Component label = NTMFluidProperties.getDisplayName(fuels[i]);
            adder.accept(
                    label.copy()
                            .append(": " + fillOf(stack, i) + " / " + caps[i] + " mB")
                            .withStyle(GAUGE_COLORS[i]));
        }
    }

    public enum Variant {
        GAS,
        ACETYLENE
    }
}
