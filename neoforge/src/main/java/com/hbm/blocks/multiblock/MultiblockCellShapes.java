/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.blocks.multiblock;

import com.hbm.blocks.ModBlocks;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public final class MultiblockCellShapes {

    public static final int PLAIN = -1;
    private static final Map<CellBuckets.Geometry, List<VoxelShape[]>> BY_ID = new HashMap<>();
    private static final Map<CellBuckets.Geometry, List<SoundType>> SOUND_BY_ID = new HashMap<>();
    private static final Map<CellBuckets.Geometry, List<@Nullable AABB>> CLIMB_BY_ID =
            new HashMap<>();
    private static final Map<CellBuckets.Geometry, Object2IntOpenHashMap<String>> INTERNED =
            new HashMap<>();
    private static final Map<MultiblockCoreBlock, Long2IntOpenHashMap> BY_BLOCK = new HashMap<>();
    private static final Set<MultiblockCoreBlock> SHAPED = new HashSet<>();
    private static final Set<CellBuckets.Geometry> UNPUBLISHED = new HashSet<>();

    static {
        reset();
    }

    private MultiblockCellShapes() {}

    public static void install(MultiblockCoreBlock block, MultiblockLayout.CellGeometry geometry) {
        Long2IntOpenHashMap ids = new Long2IntOpenHashMap();
        ids.defaultReturnValue(PLAIN);
        CellBuckets.Geometry bucket = null;
        int[] interned = new int[geometry.keys().length];
        Arrays.fill(interned, PLAIN);
        for (Direction facing : Direction.VALUES) {
            if (facing.getAxis() == Direction.Axis.Y) continue;
            int f = facing.get2DDataValue();
            long[] cells = geometry.cells()[f];
            int[] tuples = geometry.tuples()[f];
            for (int i = 0; i < cells.length; i++) {
                if (bucket == null) bucket = block.cellBucket();
                int t = tuples[i];
                if (interned[t] == PLAIN)
                    interned[t] = intern(bucket, geometry, t, block.cellSoundType());
                long cell = cells[i];
                ids.put(
                        key(facing, BlockPos.getX(cell), BlockPos.getY(cell), BlockPos.getZ(cell)),
                        interned[t]);
            }
        }
        if (ids.isEmpty()) BY_BLOCK.remove(block);
        else BY_BLOCK.put(block, ids);
        if (geometry.shaped()) SHAPED.add(block);
        else SHAPED.remove(block);
    }

    private static int intern(
            CellBuckets.Geometry part,
            MultiblockLayout.CellGeometry geometry,
            int tuple,
            SoundType sound) {
        String canonical = geometry.keys()[tuple];
        Object2IntOpenHashMap<String> interned = INTERNED.get(part);
        int existing = interned.getInt(canonical);
        if (existing != PLAIN) return existing;
        List<VoxelShape[]> byId = BY_ID.get(part);
        int id = byId.size();
        if (id >= part.maxShapes()) {
            throw new IllegalStateException(
                    "geometry cell bucket "
                            + part.name()
                            + " overflowed its"
                            + " alphabet at "
                            + part.maxShapes()
                            + "; raise that bucket's capacity in CellBuckets first");
        }
        VoxelShape[] shapes = new VoxelShape[MultiblockLayout.CellGeometry.VARIANT_COUNT];
        for (int v = 0; v < shapes.length; v++) {
            shapes[v] = MultiblockLayout.CellGeometry.shape(geometry.tupleBoxes()[tuple][v]);
        }
        byId.add(shapes);
        SOUND_BY_ID.get(part).add(sound);
        CLIMB_BY_ID.get(part).add(MultiblockLayout.CellGeometry.box(geometry.climbs()[tuple]));
        interned.put(canonical, id);
        UNPUBLISHED.add(part);
        return id;
    }

    public static void publish() {
        for (CellBuckets.Geometry part : UNPUBLISHED) {
            ModBlocks.geometryCell(part)
                    .get()
                    .publish(
                            new MultiblockGeometryCellBlock.Alphabet(
                                    BY_ID.get(part).toArray(VoxelShape[][]::new),
                                    SOUND_BY_ID.get(part).toArray(SoundType[]::new),
                                    CLIMB_BY_ID.get(part).toArray(AABB[]::new)));
        }
        UNPUBLISHED.clear();
    }

    public static int idAt(MultiblockCoreBlock block, int lx, int ly, int lz, Direction facing) {
        Long2IntOpenHashMap ids = BY_BLOCK.get(block);
        return ids == null ? PLAIN : ids.get(key(facing, lx, ly, lz));
    }

    public static int idAtWorld(
            MultiblockCoreBlock block, BlockPos core, BlockPos pos, Direction facing) {
        int dx = pos.getX() - core.getX(),
                dy = pos.getY() - core.getY(),
                dz = pos.getZ() - core.getZ();
        return idAt(
                block,
                MultiblockMaskTable.localX(dx, dy, dz, facing),
                MultiblockMaskTable.localY(dx, dy, dz, facing),
                MultiblockMaskTable.localZ(dx, dy, dz, facing),
                facing);
    }

    public static VoxelShape shape(
            CellBuckets.Geometry part, int id, boolean open, boolean forCollision) {
        return ModBlocks.geometryCell(part).get().shape(id, open, forCollision);
    }

    public static int alphabetSize(CellBuckets.Geometry part) {
        return BY_ID.get(part).size();
    }

    public static int alphabetSize() {
        int total = 0;
        for (CellBuckets.Geometry part : CellBuckets.geometry()) total += alphabetSize(part);
        return total;
    }

    public static boolean anyDeclared() {
        return !BY_BLOCK.isEmpty();
    }

    public static boolean hasGeometry(MultiblockCoreBlock block) {
        return SHAPED.contains(block);
    }

    public static boolean hasGeometryCells(MultiblockCoreBlock block) {
        return BY_BLOCK.containsKey(block);
    }

    private static long key(Direction facing, int lx, int ly, int lz) {
        return ((long) facing.ordinal() << 48)
                | ((long) (lx + 512) << 32)
                | ((long) (ly + 512) << 16)
                | (lz + 512);
    }

    public static void reset() {
        for (CellBuckets.Geometry part : CellBuckets.geometry()) {
            initializeBucket(part);
        }
        BY_BLOCK.clear();
        SHAPED.clear();
    }

    static void addBucket(CellBuckets.Geometry part) {
        if (!BY_ID.containsKey(part)) initializeBucket(part);
    }

    private static void initializeBucket(CellBuckets.Geometry part) {
        BY_ID.put(part, new ArrayList<>());
        SOUND_BY_ID.put(part, new ArrayList<>());
        CLIMB_BY_ID.put(part, new ArrayList<>());
        Object2IntOpenHashMap<String> interned = new Object2IntOpenHashMap<>();
        interned.defaultReturnValue(PLAIN);
        INTERNED.put(part, interned);
        UNPUBLISHED.add(part);
    }
}
