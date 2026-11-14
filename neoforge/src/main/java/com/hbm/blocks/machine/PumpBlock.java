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

package com.hbm.blocks.machine;

import com.hbm.blockentity.machine.ElectricPumpBlockEntity;
import com.hbm.blockentity.machine.PumpBlockEntity;
import com.hbm.blockentity.machine.SteamPumpBlockEntity;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.data.MachineData;
import com.hbm.interfaces.LookOverlay;
import com.hbm.interfaces.LookOverlayProvider;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.util.Blink;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class PumpBlock extends MultiblockCoreBlock
        implements TickingEntityBlock, LookOverlayProvider, CapabilityBlock {
    public PumpBlock(Properties props) {
        super(props);
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookOverlay info) {
        FoldedOwner owner = ownerOf(level, pos);
        if (owner == null) return;
        if (!(level.getBlockEntity(owner.pos()) instanceof PumpBlockEntity pump)) return;

        info.title(getName());

        if (pump instanceof SteamPumpBlockEntity steamPump) {
            info.row(
                    LookOverlay.inflow(
                            Component.translatable(
                                    "overlay.hbm.tank_spaced",
                                    NTMFluidProperties.clientName(steamPump.steam.getFluid()),
                                    String.format(Locale.ROOT, "%,d", steamPump.steam.getFill()),
                                    String.format(
                                            Locale.ROOT, "%,d", steamPump.steam.getMaxFill()))));
            info.row(
                    LookOverlay.outflow(
                            Component.translatable(
                                    "overlay.hbm.tank_spaced",
                                    NTMFluidProperties.clientName(steamPump.lps.getFluid()),
                                    String.format(Locale.ROOT, "%,d", steamPump.lps.getFill()),
                                    String.format(
                                            Locale.ROOT, "%,d", steamPump.lps.getMaxFill()))));
        } else if (pump instanceof ElectricPumpBlockEntity electricPump) {
            info.row(
                    LookOverlay.inflow(
                            Component.translatable(
                                    "overlay.hbm.charge",
                                    String.format(Locale.ROOT, "%,d", electricPump.getPower()),
                                    String.format(
                                            Locale.ROOT, "%,d", electricPump.getMaxPower()))));
        }

        info.row(
                LookOverlay.outflow(
                        Component.translatable(
                                "overlay.hbm.tank_spaced",
                                NTMFluidProperties.clientName(pump.water.getFluid()),
                                String.format(Locale.ROOT, "%,d", pump.water.getFill()),
                                String.format(Locale.ROOT, "%,d", pump.water.getMaxFill()))));

        if (owner.pos().getY() > MachineData.PUMP_GROUND_HEIGHT.get()) {
            info.row(
                    Component.translatable("overlay.hbm.altitude"),
                    Blink.lit() ? 0xFF0000 : 0xFFFF00);
        }
        if (!pump.onGround) {
            info.row(
                    Component.translatable("overlay.hbm.no_valid_ground"),
                    Blink.lit() ? 0xFF0000 : 0xFFFF00);
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return this == ModBlocks.PUMP_ELECTRIC.get()
                ? new ElectricPumpBlockEntity(pos, state)
                : new SteamPumpBlockEntity(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return this == ModBlocks.PUMP_ELECTRIC.get()
                ? MachineCaps.blockKeyed().powerIn().fluidOut().fe()
                : MachineCaps.blockKeyed().fluidIn().fluidOut();
    }
}
