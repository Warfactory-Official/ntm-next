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

package com.hbm.world.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;

public record OilBubbleFeature(
        BlockState core, RuleTest replaceable, int minSize, int maxSize, boolean fuzzy)
        implements Feature {
    public static final MapCodec<OilBubbleFeature> CODEC =
            RecordCodecBuilder.<OilBubbleFeature>mapCodec(
                            inst ->
                                    inst.group(
                                                    BlockState.CODEC
                                                            .fieldOf("core")
                                                            .forGetter(OilBubbleFeature::core),
                                                    RuleTest.CODEC
                                                            .fieldOf("replaceable")
                                                            .forGetter(
                                                                    OilBubbleFeature::replaceable),
                                                    Codec.intRange(0, Integer.MAX_VALUE)
                                                            .fieldOf("min_size")
                                                            .forGetter(OilBubbleFeature::minSize),
                                                    Codec.intRange(1, Integer.MAX_VALUE)
                                                            .fieldOf("max_size")
                                                            .forGetter(OilBubbleFeature::maxSize),
                                                    Codec.BOOL
                                                            .fieldOf("fuzzy")
                                                            .forGetter(OilBubbleFeature::fuzzy))
                                            .apply(inst, OilBubbleFeature::new))
                    .validate(
                            config ->
                                    config.maxSize() > config.minSize()
                                            ? DataResult.success(config)
                                            : DataResult.error(
                                                    () -> "max_size must exceed min_size"));

    @Override
    public MapCodec<OilBubbleFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(
            WorldGenLevel level,
            ChunkGenerator chunkGenerator,
            RandomSource random,
            BlockPos featureOrigin) {
        OilBubbleFeature cfg = this;
        RandomSource rand = random;
        BlockPos origin = featureOrigin;

        int radius = cfg.minSize() + rand.nextInt(cfg.maxSize() - cfg.minSize());
        double radiusSqr = (radius * radius) / 2.0D;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx < radius; dx++) {
            int xx = dx * dx;
            for (int dy = -radius; dy < radius; dy++) {
                int yy = xx + dy * dy * 3;
                for (int dz = -radius; dz < radius; dz++) {
                    int zz = yy + dz * dz;
                    double threshold = radiusSqr;
                    if (cfg.fuzzy()) threshold += rand.nextDouble() * radiusSqr / 3.0D;
                    if (zz >= threshold) continue;

                    pos.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (cfg.replaceable().test(level.getBlockState(pos), (BlockPos) pos, rand)) {
                        level.setBlock(pos, cfg.core(), 2);
                    }
                }
            }
        }

        OilSurfaceScarring.addSurfaceSpot(level, rand, origin.getX(), origin.getZ());
        return true;
    }
}
