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

package com.hbm.items.tool;

import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.items.special.BedrockOreSet;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.OreDensityLayout;
import com.hbm.util.TickPhase;
import com.hbm.world.NtmWorldgenFields;
import com.hbm.world.feature.BedrockOreFeature;
import java.util.ArrayList;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

public final class OreDensityScannerItem extends Item {

    private record DensityBand(String key, ChatFormatting color) {}

    private static final DensityBand VERY_POOR = band("verypoor", ChatFormatting.DARK_RED);
    private static final DensityBand POOR = band("poor", ChatFormatting.RED);
    private static final DensityBand LOW = band("low", ChatFormatting.GOLD);
    private static final DensityBand MODERATE = band("moderate", ChatFormatting.YELLOW);
    private static final DensityBand HIGH = band("high", ChatFormatting.GREEN);
    private static final DensityBand VERY_HIGH = band("veryhigh", ChatFormatting.BLUE);
    private static final DensityBand EXCELLENT = band("excellent", ChatFormatting.AQUA);
    private static final DensityBand ENRICHED = band("excellent", ChatFormatting.LIGHT_PURPLE);

    public OreDensityScannerItem(Properties properties) {
        super(properties);
    }

    private static DensityBand band(String suffix, ChatFormatting color) {
        return new DensityBand("item.hbm.ore_density_scanner." + suffix, color);
    }

    private static DensityBand densityBand(double density) {
        if (Double.isNaN(density)) return MODERATE;
        if (density <= 0.1D) return VERY_POOR;
        if (density <= 0.35D) return POOR;
        if (density <= 0.75D) return LOW;
        if (density < 1.25D) return MODERATE;
        if (density < 1.65D) return HIGH;
        if (density < 1.9D) return VERY_HIGH;
        if (density <= 2D) return EXCELLENT;
        return ENRICHED;
    }

    public static String getDensityLabel(double density) {
        return densityBand(density).key();
    }

    public static ChatFormatting getDensityColor(double density) {
        return densityBand(density).color();
    }

    @Override
    public void inventoryTick(
            ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        if (!(owner instanceof ServerPlayer player)
                || player.isSpectator()
                || !TickPhase.every(player, 5)) return;

        int x = Mth.floor(player.getX());
        int z = Mth.floor(player.getZ());
        var field = NtmWorldgenFields.get(level).bedrock();
        var kinds = BedrockOreSet.OF.get().apply(level).kinds();
        double richness = field.richness(x, z);
        double total = 0D;
        var rows = new ArrayList<Component>(kinds.size());
        for (var kind : kinds) {
            double density = field.oreLevel(richness, x, z, kind);
            total += density;
            double displayed = (int) (density * 100D) / 100D;
            var band = densityBand(density);
            rows.add(
                    kind.typeName()
                            .copy()
                            .append(Component.literal(": " + displayed + " ("))
                            .append(Component.translatable(band.key()).withStyle(band.color()))
                            .append(Component.literal(")")));
        }

        int tier = BedrockOreFeature.getTier(total / kinds.size());
        var summary =
                Component.translatable("item.hbm.ore_density_scanner.tier", tier)
                        .withStyle(ChatFormatting.YELLOW);
        var fluid = BedrockOreFeature.BORE_FLUID.get().apply(level);
        if (fluid == null) fluid = BedrockOreFeature.getBoreFluid(tier);
        if (fluid != null && fluid.type() != Fluids.EMPTY) {
            summary.append(Component.literal(" - " + fluid.amount() + "mB "))
                    .append(NTMFluidProperties.getDisplayName(fluid.type()));
        }
        PacketWire.sendTo(OreDensityLayout.encode(777, 4_000, rows, summary), player);
    }
}
