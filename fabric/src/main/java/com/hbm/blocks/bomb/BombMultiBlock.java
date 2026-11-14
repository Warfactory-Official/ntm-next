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

package com.hbm.blocks.bomb;

import com.hbm.blockentity.bomb.BombMultiBlockEntity;
import com.hbm.blocks.machine.HorizontalMachineBlock;
import com.hbm.entity.effect.MistEntity;
import com.hbm.explosion.BlockConversionTable;
import com.hbm.explosion.CompositeExplosion;
import com.hbm.explosion.ExplosionChaos;
import com.hbm.interfaces.TriggerHandler;
import com.hbm.interfaces.TriggerResult;
import com.hbm.inventory.fluid.NTMFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class BombMultiBlock extends HorizontalMachineBlock implements TriggerHandler {
    private static final float BASE_STRENGTH = 8.0F;
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 8, 16);

    public BombMultiBlock(Properties props) {
        super(props);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState()
                .setValue(FACING, ctx.getHorizontalDirection().getCounterClockWise());
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BombMultiBlockEntity(pos, state);
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block neighborBlock,
            @Nullable Orientation orientation,
            boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
        if (level.isClientSide()) return;
        if (level.hasNeighborSignal(pos)) trigger(level, pos, null);
    }

    @Override
    public TriggerResult trigger(Level level, BlockPos pos, @Nullable Entity detonator) {
        if (!(level instanceof ServerLevel server)) return TriggerResult.NONE;
        if (!(server.getBlockEntity(pos) instanceof BombMultiBlockEntity bomb)
                || !bomb.isLoaded()) {
            return TriggerResult.MISSING_COMPONENT;
        }

        float strength = BASE_STRENGTH;
        int cluster = 0;
        int fire = 0;
        int poison = 0;
        int gas = 0;
        for (int slot : BombMultiBlockEntity.PAYLOAD_SLOTS) {
            switch (bomb.payload(slot)) {
                case GUNPOWDER -> strength += 1.0F;
                case TNT -> strength += 4.0F;
                case CLUSTER -> cluster += 50;
                case FIRE -> fire += 10;
                case POISON -> poison += 15;
                case GAS -> gas += 50;
                case NONE -> {}
            }
        }

        bomb.clearSlots();
        server.removeBlock(pos, false);

        CompositeExplosion.detonate(
                server,
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                strength,
                CompositeExplosion.CLOUD | CompositeExplosion.RUBBLE | CompositeExplosion.SHRAPNEL);

        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D;

        if (cluster > 0) {
            ExplosionChaos.cluster(
                    server,
                    x,
                    y,
                    z,
                    cluster,
                    0F,
                    (float) Math.PI * 0.5F,
                    (float) Math.PI * 2F,
                    (float) Math.PI * 0.125F,
                    0.375F);
        }
        if (fire > 0) {
            ExplosionChaos.igniteAllBlocks(server, pos.getX(), pos.getY(), pos.getZ(), fire);
        }
        if (poison > 0) {
            BlockConversionTable.POISON.sphere(server, pos, poison, server.getRandom());
        }
        if (gas > 0) {
            MistEntity mist = new MistEntity(server);
            mist.setType(NTMFluids.CHLORINE);
            mist.setPos(x, y, z);
            mist.setArea(gas * 15F / 50F, gas * 7.5F / 50F);
            server.addFreshEntity(mist);
        }
        return TriggerResult.DETONATED;
    }

    public static float yawFor(Direction facing) {
        return switch (facing) {
            case SOUTH -> (float) Math.PI;
            case WEST -> (float) (Math.PI * 1.5);
            case EAST -> (float) (Math.PI * 0.5);
            default -> 0F;
        };
    }
}
