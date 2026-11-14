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

import com.hbm.blockentity.machine.reactor.ResearchReactorBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.handler.BossSpawnHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class ReactorResearchBlock extends MultiblockCoreBlock
        implements TickingEntityBlock, CapabilityBlock {
    private static final Direction[] BUBBLE_SIDES = {
        Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };

    public ReactorResearchBlock(Properties props) {
        super(props);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        float centerX = pos.getX() + 0.5F;
        float centerY = pos.getY() + 0.5F;
        float centerZ = pos.getZ() + 0.5F;
        for (var side : BUBBLE_SIDES) {
            if (!ResearchReactorBlockEntity.hasWater(level, pos.relative(side))) continue;
            double x = (double) (centerX + side.getStepX()) + random.nextDouble() - 0.5D;
            double y = (double) centerY + random.nextDouble() - 0.5D;
            double z = (double) (centerZ + side.getStepZ()) + random.nextDouble() - 0.5D;
            double travel = random.nextDouble();
            if (side.getStepX() != 0)
                x = centerX + 0.5D * side.getStepX() + 0.125D * travel * side.getStepX();
            else z = centerZ + 0.5D * side.getStepZ() + 0.125D * travel * side.getStepZ();
            level.addParticle(ParticleTypes.BUBBLE, x, y, z, 0D, 0.2D, 0D);
        }
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) return InteractionResult.SUCCESS;
        BossSpawnHandler.markFBI(player);
        if (level.getBlockEntity(core) instanceof MenuProvider menu) {
            openCoreMenu(player, core, menu);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ResearchReactorBlockEntity(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.REACTOR_RESEARCH).items();
    }
}
