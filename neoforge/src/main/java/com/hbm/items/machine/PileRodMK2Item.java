/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
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

package com.hbm.items.machine;

import com.hbm.client.PileRodLore;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;

public class PileRodMK2Item extends Item {
    private static final String DEPLETION = "depletion";
    public final RodType type;

    public PileRodMK2Item(Properties properties, RodType type) {
        super(properties);
        this.type = type;
    }

    public static double getDepletion(ItemStack stack) {
        CustomData data = stack.get(ModDataComponents.PERSISTENT_DATA.get());
        return data == null ? 0D : data.copyTag().getDoubleOr(DEPLETION, 0D);
    }

    public static void setDepletion(ItemStack stack, double depletion) {
        CustomData.update(
                ModDataComponents.PERSISTENT_DATA.get(),
                stack,
                tag -> tag.putDouble(DEPLETION, depletion));
    }

    public static void clearDepletion(ItemStack stack) {
        if (stack.get(ModDataComponents.PERSISTENT_DATA.get()) != null)
            CustomData.update(
                    ModDataComponents.PERSISTENT_DATA.get(), stack, tag -> tag.remove(DEPLETION));
    }

    public static double getDepletionPercent(ItemStack stack) {
        if (!(stack.getItem() instanceof PileRodMK2Item rod) || rod.type.life <= 0D) return 0D;
        return getDepletion(stack) / rod.type.life * 100D;
    }

    public static double getReactivity(ItemStack stack, double incomingFlux) {
        RodType type = ((PileRodMK2Item) stack.getItem()).type;
        double output = type.neutronSource;
        if (type.reactionMult > 0D) {
            double x = incomingFlux;
            output +=
                    (Math.sqrt(x + 1D / ((x + 2D) * (x + 2D))) - 1D / (x + 2D)) * type.reactionMult;
        }
        return output;
    }

    public static double getHeatPerNeutron(ItemStack stack) {
        return ((PileRodMK2Item) stack.getItem()).type.heatMult;
    }

    public static ItemStack react(ItemStack stack, double outputFlux) {
        RodType type = ((PileRodMK2Item) stack.getItem()).type;
        if (type.life <= 0D) return stack;
        double depletion = getDepletion(stack) + outputFlux;
        if (depletion < type.life) {
            setDepletion(stack, depletion);
            return stack;
        }
        return ModItems.PILE_ROD.stack(RodType.VALUES[type.turnsInto]);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return type.life > 0D && getDepletion(stack) > 0D;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        if (type.life <= 0D) return 13;
        return Mth.clamp((int) Math.round(13D * (1D - getDepletion(stack) / type.life)), 0, 13);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        if (type.life > 0D) {
            adder.accept(
                    Component.translatable("tooltip.hbm.pile_rod.lifetime", Math.round(type.life)));
            double depletion = getDepletionPercent(stack);
            if (depletion > 0D)
                adder.accept(
                        Component.translatable(
                                "tooltip.hbm.pile_rod.depletion", Math.round(depletion)));
        }
        PileRodLore.add(getDescriptionId() + ".desc", adder);
    }

    public enum RodType {
        RA226BE(1D),
        PO210BE(1D),
        ZR(0D, 0D, 0D, 2),
        NU(1D, 25_000D, 0.25D, 4),
        PU239(1D, 500D, 0.5D, 5),
        RGP(1D, 1_000D, 0.5D, 6),
        WASTE(1D, 0D, 1.5D, 6),
        THORIUM(1D, 35_000D, 0.25D, 8),
        THORIUM_FUEL(1D, 2_000D, 0.5D, 6);

        public static final RodType[] VALUES = values();

        public final double reactionMult;
        public final double life;
        public final double heatMult;
        public final double neutronSource;
        public final int turnsInto;

        RodType(double neutronSource) {
            this(0D, 0D, 0D, 0, neutronSource);
        }

        RodType(double reactionMult, double life, double heatMult, int nextOrdinal) {
            this(reactionMult, life, heatMult, nextOrdinal, 0D);
        }

        RodType(
                double reactionMult,
                double life,
                double heatMult,
                int nextOrdinal,
                double neutronSource) {
            this.reactionMult = reactionMult;
            this.life = life;
            this.heatMult = heatMult;
            this.turnsInto = nextOrdinal;
            this.neutronSource = neutronSource;
        }
    }
}
