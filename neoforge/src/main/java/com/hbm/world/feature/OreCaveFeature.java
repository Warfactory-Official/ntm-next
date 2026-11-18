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

package com.hbm.world.feature;

import com.hbm.blocks.generic.SpikeBlock;
import com.hbm.world.NtmWorldgenFields;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import org.jspecify.annotations.Nullable;

public record OreCaveFeature(
        BlockState ore,
        Optional<BlockState> fluid,
        Optional<BlockState> stalactite,
        Optional<BlockState> stalagmite,
        RuleTest replaceable,
        double threshold,
        int rangeMult,
        int maxRange,
        int yLevel,
        Identifier domain,
        boolean exposedByFluid)
        implements Feature {

    public static final MapCodec<OreCaveFeature> CODEC =
            RecordCodecBuilder.mapCodec(
                    inst ->
                            inst.group(
                                            BlockState.CODEC
                                                    .fieldOf("ore")
                                                    .forGetter(OreCaveFeature::ore),
                                            BlockState.CODEC
                                                    .optionalFieldOf("fluid")
                                                    .forGetter(OreCaveFeature::fluid),
                                            BlockState.CODEC
                                                    .optionalFieldOf("stalactite")
                                                    .forGetter(OreCaveFeature::stalactite),
                                            BlockState.CODEC
                                                    .optionalFieldOf("stalagmite")
                                                    .forGetter(OreCaveFeature::stalagmite),
                                            RuleTest.CODEC
                                                    .fieldOf("replaceable")
                                                    .forGetter(OreCaveFeature::replaceable),
                                            Codec.DOUBLE
                                                    .fieldOf("threshold")
                                                    .forGetter(OreCaveFeature::threshold),
                                            Codec.INT
                                                    .fieldOf("range_mult")
                                                    .forGetter(OreCaveFeature::rangeMult),
                                            Codec.INT
                                                    .fieldOf("max_range")
                                                    .forGetter(OreCaveFeature::maxRange),
                                            Codec.INT
                                                    .fieldOf("y_level")
                                                    .forGetter(OreCaveFeature::yLevel),
                                            Identifier.CODEC
                                                    .fieldOf("domain")
                                                    .forGetter(OreCaveFeature::domain),
                                            Codec.BOOL
                                                    .optionalFieldOf("exposed_by_fluid", false)
                                                    .forGetter(OreCaveFeature::exposedByFluid))
                                    .apply(inst, OreCaveFeature::new));

    @Override
    public MapCodec<OreCaveFeature> codec() {
        return CODEC;
    }

    private static final int UPDATE = 2 | 16;
    private static final double SCALE = 0.01D;
    private static final int SIZE = 16;
    private static final ThreadLocal<Scratch> SCRATCH = ThreadLocal.withInitial(Scratch::new);

    @Override
    public boolean place(
            WorldGenLevel level,
            ChunkGenerator chunkGenerator,
            RandomSource random,
            BlockPos featureOrigin) {
        OreCaveFeature cfg = this;
        RandomSource rand = random;

        FractalSimplexNoise noise = NtmWorldgenFields.get(level).fractal(cfg.domain(), 2);

        int baseX = featureOrigin.getX() & ~15;
        int baseZ = featureOrigin.getZ() & ~15;
        int yLevel = cfg.yLevel();
        BlockState ore = cfg.ore();
        @Nullable BlockState fluid = cfg.fluid().orElse(null);
        @Nullable BlockState stalactite = cfg.stalactite().orElse(null);
        @Nullable BlockState stalagmite = cfg.stalagmite().orElse(null);
        boolean exposedByFluid = cfg.exposedByFluid();

        Scratch scratch = SCRATCH.get();
        double[] noiseValues = scratch.noiseValues;
        noise.getGrid(baseX, baseZ, SIZE, SIZE, SCALE, SCALE, noiseValues);
        BlockPos.MutableBlockPos pos = scratch.pos;
        BlockPos.MutableBlockPos npos = scratch.neighbor;
        ChunkAccess center = level.getChunk(baseX >> 4, baseZ >> 4);

        int index = 0;
        for (int x = baseX; x < baseX + SIZE; x++) {
            for (int z = baseZ; z < baseZ + SIZE; z++) {
                double n = noiseValues[index++];
                int range =
                        NoiseBand.halfHeight(n, cfg.threshold(), cfg.rangeMult(), cfg.maxRange());
                if (range < 0) continue;

                for (int y = yLevel - range; y <= yLevel + range; y++) {
                    pos.set(x, y, z);
                    BlockState cur = center.getBlockState(pos);

                    if (cfg.replaceable().test(cur, (BlockPos) pos, rand)) {
                        boolean shouldGen = false;
                        boolean canGenFluid = fluid != null && rand.nextBoolean();

                        for (Direction dir : Direction.VALUES) {
                            npos.setWithOffset(pos, dir);
                            BlockState nb =
                                    (npos.getX() & ~15) == baseX && (npos.getZ() & ~15) == baseZ
                                            ? center.getBlockState(npos)
                                            : level.getBlockState(npos);
                            if (nb.isAir()
                                    || nb.getBlock() instanceof SpikeBlock
                                    || exposedByFluid && !nb.getFluidState().isEmpty()) {
                                shouldGen = true;
                            }
                            if (shouldGen && (fluid == null || !canGenFluid)) break;

                            if (fluid != null) {
                                switch (dir) {
                                    case UP -> {
                                        if (!nb.isAir() && !(nb.getBlock() instanceof SpikeBlock))
                                            canGenFluid = false;
                                    }
                                    case DOWN -> {
                                        if (!nb.isCollisionShapeFullBlock(level, npos))
                                            canGenFluid = false;
                                    }
                                    default -> {
                                        if (!nb.isCollisionShapeFullBlock(level, npos)
                                                && nb.getBlock() != fluid.getBlock())
                                            canGenFluid = false;
                                    }
                                }
                            }
                        }

                        if (fluid != null && canGenFluid) {
                            level.setBlock(pos, fluid, UPDATE);
                            npos.setWithOffset(pos, Direction.DOWN);
                            level.setBlock(npos, ore, UPDATE);
                            placeOreIfSolid(level, pos, npos, ore, Direction.NORTH);
                            placeOreIfSolid(level, pos, npos, ore, Direction.EAST);
                            placeOreIfSolid(level, pos, npos, ore, Direction.SOUTH);
                            placeOreIfSolid(level, pos, npos, ore, Direction.WEST);
                        } else if (shouldGen) {
                            level.setBlock(pos, ore, UPDATE);
                        }
                    } else if (stalactite != null
                            && (cur.isAir() || !cur.isCollisionShapeFullBlock(level, pos))
                            && rand.nextInt(5) == 0
                            && cur.getFluidState().isEmpty()) {
                        if (stalactite.canSurvive(level, pos))
                            level.setBlock(pos, stalactite, UPDATE);
                        else if (stalagmite != null && stalagmite.canSurvive(level, pos)) {
                            level.setBlock(pos, stalagmite, UPDATE);
                        }
                    }
                }
            }
        }
        return true;
    }

    private static void placeOreIfSolid(
            WorldGenLevel level,
            BlockPos pos,
            BlockPos.MutableBlockPos neighbor,
            BlockState ore,
            Direction direction) {
        neighbor.setWithOffset(pos, direction);
        if (level.getBlockState(neighbor).isCollisionShapeFullBlock(level, neighbor)) {
            level.setBlock(neighbor, ore, UPDATE);
        }
    }

    private static final class Scratch {
        private final double[] noiseValues = new double[SIZE * SIZE];
        private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        private final BlockPos.MutableBlockPos neighbor = new BlockPos.MutableBlockPos();
    }
}
