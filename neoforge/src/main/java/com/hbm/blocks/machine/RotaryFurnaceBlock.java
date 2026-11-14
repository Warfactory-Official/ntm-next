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

package com.hbm.blocks.machine;

import com.hbm.blockentity.machine.RotaryFurnaceBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.LookOverlay;
import com.hbm.interfaces.LookOverlayProvider;
import com.hbm.inventory.fluid.NTMFluidProperties;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public class RotaryFurnaceBlock extends MultiblockCoreBlock
        implements TickingEntityBlock, LookOverlayProvider, CapabilityBlock {
    public RotaryFurnaceBlock(Properties props) {
        super(props);
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookOverlay info) {
        BlockPos hit = info.hit();
        if (hit == null) return;
        if (!(level.getBlockEntity(pos) instanceof RotaryFurnaceBlockEntity furnace)) return;

        Direction dir = furnace.coreFacing();
        Direction rot = dir.getCounterClockWise();
        BlockPos front = pos.relative(dir);
        BlockPos back = pos.relative(dir.getOpposite());

        if (hit.equals(back.relative(rot, -1)) || hit.equals(back.relative(rot, -2))) {
            info.row(
                    LookOverlay.inflow(
                            Component.literal(
                                    NTMFluidProperties.clientName(
                                            furnace.tanks[1].getTankType()))));
            info.row(
                    LookOverlay.outflow(
                            Component.literal(
                                    NTMFluidProperties.clientName(
                                            furnace.tanks[2].getTankType()))));
        }

        if (hit.equals(front.relative(rot, 2)) || hit.equals(back.relative(rot, 2))) {
            Fluid recipe = furnace.tanks[0].getTankType();
            info.row(
                    LookOverlay.inflow(
                            recipe == null
                                    ? Component.translatable("overlay.hbm.none")
                                    : Component.literal(NTMFluidProperties.clientName(recipe))));
        }

        if (hit.equals(front.relative(rot))) {
            info.row(
                    LookOverlay.marked(
                            "-> ",
                            ChatFormatting.YELLOW,
                            Component.translatable("overlay.hbm.fuel")));
        }

        if (!info.isEmpty()) {
            info.title(Component.empty().append(Component.translatable(getDescriptionId())));
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RotaryFurnaceBlockEntity(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.ROTARY_FURNACE)
                .fluidIn()
                .fluidOut()
                .items()
                .itemsAtCells();
    }
}
