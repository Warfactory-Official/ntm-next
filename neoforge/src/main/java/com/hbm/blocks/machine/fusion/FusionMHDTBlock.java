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

package com.hbm.blocks.machine.fusion;

import com.hbm.blockentity.machine.fusion.FusionMHDTBlockEntity;
import com.hbm.blockentity.machine.fusion.FusionPorts;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.data.MachineData;
import com.hbm.interfaces.LookOverlay;
import com.hbm.interfaces.LookOverlayProvider;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.util.Blink;
import com.hbm.util.NumberFormatter;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class FusionMHDTBlock extends FusionMachineBlock
        implements LookOverlayProvider, CapabilityBlock {
    public FusionMHDTBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookOverlay card) {
        if (!(level.getBlockEntity(pos) instanceof FusionMHDTBlockEntity generator)) {
            return;
        }
        boolean hasMinimumPlasma = generator.hasMinimumPlasma();
        boolean cool = generator.isCool();
        long power =
                (long) Math.floor(generator.plasmaEnergy * FusionMHDTBlockEntity.PLASMA_EFFICIENCY);
        if (!hasMinimumPlasma) {
            power /= 2;
        }
        long displayedPower = 0;
        if (cool) {
            displayedPower = power;
        }

        card.title(getName());
        var plasmaStyle = ChatFormatting.GOLD;
        if (hasMinimumPlasma) {
            plasmaStyle = ChatFormatting.RESET;
        }
        var plasmaRate =
                Component.translatable(
                                "overlay.hbm.plasma_rate",
                                NumberFormatter.formatCompact(generator.plasmaEnergy),
                                NumberFormatter.formatCompact(
                                        MachineData.MHD_TURBINE_MINIMUM_PLASMA.get()))
                        .withStyle(plasmaStyle);
        card.row(LookOverlay.inflow(plasmaRate));
        card.row(
                LookOverlay.outflow(
                        Component.translatable(
                                "unit.energy_per_tick",
                                NumberFormatter.formatCompact(displayedPower))));
        for (int index = 0; index < generator.tanks.length; index++) {
            var tank = generator.tanks[index];
            var row =
                    Component.translatable(
                            "overlay.hbm.tank",
                            NTMFluidProperties.clientName(tank.getFluid()),
                            tank.getFill(),
                            tank.getMaxFill());
            card.row(LookOverlay.flow(index == 0, row));
        }

        if (generator.plasmaEnergy > 0 && !hasMinimumPlasma) {
            int color = 0xFFFF00;
            if (Blink.lit()) {
                color = 0xFF8000;
            }
            card.row(Component.translatable("overlay.hbm.low_power"), color);
        }
        if (!cool) {
            int color = 0xFFFF00;
            if (Blink.lit()) {
                color = 0xFF0000;
            }
            card.row(Component.translatable("overlay.hbm.insufficient_cooling"), color);
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FusionMHDTBlockEntity(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.blockKeyed().powerOut().fluidIn().fluidOut().fe();
    }

    @Override
    public List<FusionPorts.Port> links(BlockPos core, Direction facing) {
        return FusionMHDTBlockEntity.links(core, facing);
    }
}
