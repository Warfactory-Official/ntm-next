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

import com.hbm.blockentity.machine.ReactorZirnoxBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.handler.BossSpawnHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class ReactorZirnoxBlock extends MultiblockCoreBlock
        implements TickingEntityBlock, CapabilityBlock {
    public ReactorZirnoxBlock(Properties props) {
        super(props);
    }

    @Override
    protected boolean tilts() {
        return true;
    }

    @Override
    public boolean wantsNeighborUpdates() {
        return true;
    }

    @Override
    public void cellNeighborChanged(ServerLevel level, BlockPos core, BlockPos cell) {
        if (!(level.getBlockEntity(core) instanceof ReactorZirnoxBlockEntity reactor)) return;

        boolean powered = false;
        for (int dx = -2; dx <= 2 && !powered; dx++) {
            for (int dy = 0; dy <= 4 && !powered; dy++) {
                for (int dz = -2; dz <= 2 && !powered; dz++) {
                    if (dx == -2 || dx == 2 || dy == 0 || dy == 4 || dz == -2 || dz == 2) {
                        if (level.hasNeighborSignal(core.offset(dx, dy, dz))) powered = true;
                    }
                }
            }
        }
        reactor.setRedstonePowered(powered);
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (!player.isSecondaryUseActive()) BossSpawnHandler.markFBI(player);
        return super.useAtCore(coreState, level, core, player, hit);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ReactorZirnoxBlockEntity(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.ZIRNOX).fluidIn().fluidOut().items().itemsAtCells();
    }
}
