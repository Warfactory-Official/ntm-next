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

package com.hbm.entity.effect;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.bomb.VolcanoBlock;
import com.hbm.blocks.generic.FalloutBlock;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.config.FalloutConfigJSON.FalloutEntry;
import com.hbm.util.BiomeUtil;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.List;
import java.util.function.IntConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.RandomSupport;
import org.jspecify.annotations.Nullable;

public final class FalloutTerrain {
    public static final int NONE = 0;
    public static final int INNER = 1;
    public static final int OUTER = 2;
    private static final int MAX_SOLID_DEPTH = 3;
    private static final float HARDNESS_BOUND = 6.0F;

    private FalloutTerrain() {}

    public static int ring(int chunkX, int chunkZ, double px, double pz, int radius) {
        double radiusSq = (double) radius * radius;
        int minZ = chunkZ << 4;
        int maxZ = minZ + 15;
        double dz = Math.max(minZ, Math.min(maxZ, (int) Math.round(pz))) - pz;
        int minX = chunkX << 4;
        int maxX = minX + 15;
        double dx = Math.max(minX, Math.min(maxX, (int) Math.round(px))) - px;
        if (dx * dx + dz * dz > radiusSq) return NONE;
        double farZ = Math.max(Math.abs(minZ - pz), Math.abs(maxZ - pz));
        double farX = Math.max(Math.abs(minX - px), Math.abs(maxX - px));
        return farX * farX + farZ * farZ > radiusSq ? OUTER : INNER;
    }

    public static long chunkSeed(long opSeed, long chunkKey) {
        return RandomSupport.mixStafford13(opSeed ^ chunkKey);
    }

    public static void convert(
            ServerLevel server,
            LevelChunkSection[] sections,
            @Nullable Long2ObjectMap<BlockState> overlay,
            FalloutRainEntity.WorkerScratch scratch,
            int chunkX,
            int chunkZ,
            double px,
            double pz,
            int scale,
            boolean outerRing,
            Block layer,
            List<FalloutEntry> table,
            long seed) {
        int minY = server.getMinY();
        int maxY = server.getMaxY();
        scratch.random.setSeed(seed);
        int topY = minY;
        for (int i = sections.length - 1; i >= 0; i--) {
            if (!sections[i].hasOnlyAir()) {
                topY = Math.min(maxY, minY + (i << 4) + 15);
                break;
            }
        }
        int minX = chunkX << 4;
        int minZ = chunkZ << 4;
        for (int lx = 0; lx < 16; lx++) {
            int x = minX + lx;
            for (int lz = 0; lz < 16; lz++) {
                int z = minZ + lz;
                double dist = Math.hypot(x - px, z - pz);
                if (outerRing && dist > scale) continue;
                double percent = scale <= 0 ? 100.0D : dist * 100.0D / scale;
                stompColumn(
                        server, sections, overlay, scratch, layer, table, lx, lz, x, z, percent,
                        minY, topY);
            }
        }
    }

    public static void dropUnsupportedLayers(
            ServerLevel server,
            LevelChunkSection[] sections,
            @Nullable Long2ObjectMap<BlockState> overlay,
            Long2ObjectOpenHashMap<BlockState> updates,
            Block layer) {
        var below = new BlockPos.MutableBlockPos();
        for (var iterator = updates.long2ObjectEntrySet().fastIterator(); iterator.hasNext(); ) {
            var entry = iterator.next();
            if (!entry.getValue().is(layer)) continue;
            long pos = entry.getLongKey();
            int x = BlockPos.getX(pos), y = BlockPos.getY(pos) - 1, z = BlockPos.getZ(pos);
            BlockState support = updates.get(BlockPos.asLong(x, y, z));
            if (support == null) support = read(server, sections, overlay, x & 15, y, z & 15, x, z);
            if (!FalloutBlock.supports(
                    layer, support, EmptyBlockGetter.INSTANCE, below.set(x, y, z)))
                iterator.remove();
        }
    }

    public static void repaintBiomes(
            LevelChunkSection[] sections,
            int chunkX,
            int chunkZ,
            double px,
            double pz,
            int scale,
            Holder<Biome> inner,
            Holder<Biome> crater,
            Holder<Biome> outer,
            IntConsumer changed) {
        int qx = QuartPos.fromSection(chunkX);
        int qz = QuartPos.fromSection(chunkZ);
        var innerKey = inner.unwrapKey().orElseThrow();
        var craterKey = crater.unwrapKey().orElseThrow();
        var outerKey = outer.unwrapKey().orElseThrow();
        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            LevelChunkSection section = sections[sectionIndex];
            var replacement = section.getBiomes().recreate();
            boolean repainted = false;
            for (int lx = 0; lx < 4; lx++) {
                for (int lz = 0; lz < 4; lz++) {
                    for (int ly = 0; ly < 4; ly++) {
                        Holder<Biome> existing = section.getNoiseBiome(lx, ly, lz);
                        Holder<Biome> target =
                                BiomeUtil.craterBiome(
                                        existing, qx + lx, qz + lz, px, pz, scale, inner, crater,
                                        outer, innerKey, craterKey, outerKey);
                        replacement.getAndSetUnchecked(lx, ly, lz, target);
                        repainted |= target != existing;
                    }
                }
            }
            if (repainted) {
                section.biomes = replacement;
                changed.accept(sectionIndex);
            }
        }
    }

    static BlockState read(
            ServerLevel server,
            LevelChunkSection[] sections,
            @Nullable Long2ObjectMap<BlockState> overlay,
            int lx,
            int y,
            int lz,
            int x,
            int z) {
        if (overlay != null) {
            BlockState deferred = overlay.get(BlockPos.asLong(x, y, z));
            if (deferred != null) return deferred;
        }
        return FalloutRainEntity.readSection(server, sections, lx, y, lz);
    }

    public static void stompColumn(
            ServerLevel server,
            LevelChunkSection[] sections,
            @Nullable Long2ObjectMap<BlockState> overlay,
            FalloutRainEntity.WorkerScratch scratch,
            Block layer,
            List<FalloutEntry> table,
            int lx,
            int lz,
            int x,
            int z,
            double percent,
            int minY,
            int maxY) {
        int solidDepth = 0;
        BlockPos.MutableBlockPos pos = scratch.pos;
        Long2ObjectOpenHashMap<BlockState> updates = scratch.updates;
        Long2ObjectOpenHashMap<BlockState> spawnFalling = scratch.spawnFalling;
        LongOpenHashSet fallingSurfaceCells = scratch.fallingSurfaceCells;
        RandomSource random = scratch.random;
        Block fallout = ModBlocks.FALLOUT.get();

        for (int y = maxY; y >= minY; y--) {
            if (solidDepth >= MAX_SOLID_DEPTH) return;

            BlockState state = read(server, sections, overlay, lx, y, lz, x, z);
            if (state.isAir()) continue;
            if (state.is(fallout)) continue;
            if (state.is(ModBlocks.VOLCANO_CORE.get())) {
                updates.put(
                        BlockPos.asLong(x, y, z),
                        ModBlocks.VOLCANO_RAD_CORE
                                .get()
                                .defaultBlockState()
                                .setValue(VolcanoBlock.MODE, state.getValue(VolcanoBlock.MODE)));
                continue;
            }

            BlockState aboveState = null;
            int upY = y + 1;
            if (solidDepth == 0 && upY <= maxY) {
                aboveState = read(server, sections, overlay, lx, upY, lz, x, z);
                boolean replaceable =
                        aboveState.isAir() || (aboveState.canBeReplaced() && !aboveState.liquid());
                if (replaceable) {
                    double d = percent / 100.0D;
                    double chance = 0.1D - Math.pow(d - 0.7D, 2);
                    if (chance >= random.nextDouble()
                            && FalloutBlock.supports(
                                    layer, state, EmptyBlockGetter.INSTANCE, pos.set(x, y, z))) {
                        BlockState target;
                        if (aboveState.is(layer)) {
                            int existing = aboveState.getValue(FalloutBlock.LAYERS);
                            target =
                                    existing < SnowLayerBlock.MAX_HEIGHT
                                            ? aboveState.setValue(FalloutBlock.LAYERS, existing + 1)
                                            : null;
                        } else {
                            target = layer.defaultBlockState();
                        }
                        if (target != null) updates.put(BlockPos.asLong(x, upY, z), target);
                    }
                }
            }

            if (percent < 65D && FalloutRainEntity.isFlammable(state) && upY <= maxY) {
                if (aboveState == null)
                    aboveState = read(server, sections, overlay, lx, upY, lz, x, z);
                if (aboveState.isAir() && random.nextInt(5) == 0) {
                    updates.put(BlockPos.asLong(x, upY, z), Blocks.FIRE.defaultBlockState());
                }
            }

            boolean transformed = false;
            for (int i = 0, n = table.size(); i < n; i++) {
                FalloutEntry entry = table.get(i);
                BlockState result = entry.eval(server, pos.set(x, y, z), state, percent, random);
                if (result != null) {
                    updates.put(BlockPos.asLong(x, y, z), result);
                    if (entry.restrictDepth()) solidDepth++;
                    transformed = true;
                    break;
                }
            }

            if (!transformed
                    && y > minY
                    && percent < 65D
                    && MultiblockSurface.isSurface(state)
                    && read(server, sections, overlay, lx, y - 1, lz, x, z).isAir()) {
                fallingSurfaceCells.add(BlockPos.asLong(x, y, z));
            }
            if (y > minY && percent < 65D && !MultiblockSurface.isSurface(state)) {
                float hardness = state.getDestroySpeed(server, pos.set(x, y, z));
                if (hardness >= 0F && hardness <= HARDNESS_BOUND) {
                    BlockState belowState = read(server, sections, overlay, lx, y - 1, lz, x, z);
                    if (belowState.isAir()) {
                        for (int i = 0; i <= solidDepth; i++) {
                            int yy = y + i;
                            if (yy > maxY) break;
                            BlockState colState = updates.get(BlockPos.asLong(x, yy, z));
                            if (colState == null)
                                colState = read(server, sections, overlay, lx, yy, lz, x, z);
                            if (colState.isAir()) continue;
                            float h = colState.getDestroySpeed(server, pos.set(x, yy, z));
                            if (h >= 0F && h <= HARDNESS_BOUND) {
                                spawnFalling.putIfAbsent(BlockPos.asLong(x, yy, z), colState);
                            }
                        }
                    }
                }
            }

            if (!transformed && state.isSolidRender() && !state.isSignalSource()) solidDepth++;
        }
    }
}
