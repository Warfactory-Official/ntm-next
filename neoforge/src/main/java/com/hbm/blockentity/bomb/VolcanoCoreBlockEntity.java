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

package com.hbm.blockentity.bomb;

import com.hbm.blockentity.BlockEntityTicks;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.bomb.VolcanoBlock;
import com.hbm.entity.ModEntities;
import com.hbm.entity.projectile.ShrapnelEntity;
import com.hbm.explosion.vanillant.ComposableExplosion;
import com.hbm.explosion.vanillant.standard.AttenuatedRayBlockSelector;
import com.hbm.explosion.vanillant.standard.BlockMutatorLava;
import com.hbm.explosion.vanillant.standard.CellDestruction;
import com.hbm.particle.helper.VolcanoSmokeCreator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class VolcanoCoreBlockEntity extends BlockEntity implements BlockEntityTicks {
    public static final int CYCLE = 10;

    public int volcanoTimer;

    public VolcanoCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VOLCANO_CORE.get(), pos, state);
    }

    @Override
    public void tickServer() {
        var level = (ServerLevel) this.level;
        var state = getBlockState();
        VolcanoBlock.Mode mode = state.getValue(VolcanoBlock.MODE);
        this.volcanoTimer++;
        if (getUpdateRate(mode) > CYCLE) level.blockEntityChanged(worldPosition);

        if (this.volcanoTimer % CYCLE == 0) {
            if (hasVerticalChannel(mode)) {
                blastMagmaChannel(level, state);
                raiseMagma(level, state);
            }

            double magmaChamber = magmaChamberSize(mode);
            if (magmaChamber > 0) blastMagmaChamber(level, state, magmaChamber);

            if (mode == VolcanoBlock.Mode.SMOLDERING) meltSurface(level, state, 50, 50D, 10D);

            if (isSpewing(mode)) spawnBlobs(level, state);
            if (isSmoking(mode))
                VolcanoSmokeCreator.composeEffect(
                        level,
                        worldPosition.getX() + 0.5,
                        worldPosition.getY() + 10,
                        worldPosition.getZ() + 0.5);

            surroundLava(level, state);
        }

        if (this.volcanoTimer >= getUpdateRate(mode)) {
            this.volcanoTimer = 0;

            if (mode.growing() && worldPosition.getY() < 200) {
                level.setBlock(worldPosition.above(), state, Block.UPDATE_ALL);
                level.setBlockAndUpdate(worldPosition, lava(state).defaultBlockState());
            } else if (mode.extinguishing()) {
                level.setBlockAndUpdate(worldPosition, lava(state).defaultBlockState());
            }
        }
    }

    private static Block lava(BlockState state) {
        return VolcanoBlock.isRadioactive(state)
                ? ModBlocks.RAD_LAVA_BLOCK.get()
                : ModBlocks.VOLCANIC_LAVA_BLOCK.get();
    }

    private static ComposableExplosion blast(
            Level level, BlockState state, double x, double y, double z, float size) {
        return new ComposableExplosion(level, x, y, z, size)
                .blockSelector(new AttenuatedRayBlockSelector())
                .blockProcessor(
                        CellDestruction.noDrops().transform(new BlockMutatorLava(lava(state))));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.volcanoTimer = input.getIntOr("timer", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("timer", this.volcanoTimer);
    }

    private static boolean isSmoking(VolcanoBlock.Mode mode) {
        return mode != VolcanoBlock.Mode.SMOLDERING;
    }

    private static boolean isSpewing(VolcanoBlock.Mode mode) {
        return mode != VolcanoBlock.Mode.SMOLDERING;
    }

    private static boolean hasVerticalChannel(VolcanoBlock.Mode mode) {
        return mode != VolcanoBlock.Mode.SMOLDERING;
    }

    private static double magmaChamberSize(VolcanoBlock.Mode mode) {
        return mode == VolcanoBlock.Mode.SMOLDERING ? 15 : 0;
    }

    private static int getUpdateRate(VolcanoBlock.Mode mode) {
        return switch (mode) {
            case STATIC_EXTINGUISHING -> 60 * 60 * 20;
            case GROWING_ACTIVE, GROWING_EXTINGUISHING -> 60 * 60 * 20 / 250;
            default -> 10;
        };
    }

    private void blastMagmaChannel(ServerLevel level, BlockState state) {
        RandomSource rand = level.getRandom();
        blast(
                        level,
                        state,
                        worldPosition.getX() + 0.5,
                        worldPosition.getY() + rand.nextInt(15) + 1.5,
                        worldPosition.getZ() + 0.5,
                        7)
                .explode();
        int depth = worldPosition.getY() - level.getMinY() + 1;
        double y = depth > 0 ? level.getMinY() + rand.nextInt(depth) : worldPosition.getY();
        blast(
                        level,
                        state,
                        worldPosition.getX() + 0.5 + rand.nextGaussian() * 3,
                        y,
                        worldPosition.getZ() + 0.5 + rand.nextGaussian() * 3,
                        10)
                .explode();
    }

    private void blastMagmaChamber(ServerLevel level, BlockState state, double size) {
        RandomSource rand = level.getRandom();
        for (int i = 0; i < 2; i++) {
            double dist = size / (double) (i + 1);
            blast(
                            level,
                            state,
                            worldPosition.getX() + 0.5 + rand.nextGaussian() * dist,
                            worldPosition.getY() + 0.5 + rand.nextGaussian() * dist,
                            worldPosition.getZ() + 0.5 + rand.nextGaussian() * dist,
                            7)
                    .explode();
        }
    }

    private void meltSurface(
            ServerLevel level, BlockState state, int count, double radius, double depth) {
        RandomSource rand = level.getRandom();
        Block lava = lava(state);

        for (int i = 0; i < count; i++) {
            int x = (int) Math.floor(worldPosition.getX() + rand.nextGaussian() * radius);
            int z = (int) Math.floor(worldPosition.getZ() + rand.nextGaussian() * radius);
            int y =
                    level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z)
                            + 1
                            - (int) Math.floor(Math.abs(rand.nextGaussian() * depth));
            BlockPos pos = new BlockPos(x, y, z);

            BlockState target = level.getBlockState(pos);
            if (!target.isAir()
                    && target.getBlock().getExplosionResistance()
                            < Blocks.OBSIDIAN.getExplosionResistance()) {
                level.setBlockAndUpdate(
                        pos,
                        target.isSolidRender()
                                ? lava.defaultBlockState()
                                : Blocks.AIR.defaultBlockState());
            }
        }
    }

    private void raiseMagma(ServerLevel level, BlockState state) {
        RandomSource rand = level.getRandom();
        Block lava = lava(state);

        BlockPos pos =
                new BlockPos(
                        worldPosition.getX() - 10 + rand.nextInt(21),
                        worldPosition.getY() + rand.nextInt(11),
                        worldPosition.getZ() - 10 + rand.nextInt(21));

        if (level.getBlockState(pos).isAir() && level.getBlockState(pos.below()).is(lava)) {
            level.setBlockAndUpdate(pos, lava.defaultBlockState());
        }
    }

    private void surroundLava(ServerLevel level, BlockState state) {
        BlockState lava = lava(state).defaultBlockState();

        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                for (int k = -1; k <= 1; k++) {
                    if (i != 0 || j != 0 || k != 0) {
                        level.setBlockAndUpdate(worldPosition.offset(i, j, k), lava);
                    }
                }
            }
        }
    }

    private void spawnBlobs(ServerLevel level, BlockState state) {
        RandomSource rand = level.getRandom();
        int trail =
                VolcanoBlock.isRadioactive(state)
                        ? ShrapnelEntity.TRAIL_RAD_VOLCANO
                        : ShrapnelEntity.TRAIL_VOLCANO;

        for (int i = 0; i < 3; i++) {
            ShrapnelEntity frag = new ShrapnelEntity(ModEntities.SHRAPNEL.get(), level);
            frag.snapTo(
                    worldPosition.getX() + 0.5,
                    worldPosition.getY() + 1.5,
                    worldPosition.getZ() + 0.5,
                    0.0F,
                    0.0F);
            frag.setDeltaMovement(
                    rand.nextGaussian() * 0.2D, 1D + rand.nextDouble(), rand.nextGaussian() * 0.2D);
            frag.setTrail(trail);
            level.addFreshEntity(frag);
        }
    }
}
