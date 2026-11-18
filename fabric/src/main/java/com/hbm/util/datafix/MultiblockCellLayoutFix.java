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

package com.hbm.util.datafix;

import com.hbm.interfaces.injected.ChunkExtension;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.util.Arrays;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;

final class MultiblockCellLayoutFix extends DataFix {
    private static final long ROW_MASK = (1L << 59) - 1;
    private static final Set<String> REMOVED =
            Set.of(
                    "hbm:multiblock_cell_h3_r3",
                    "hbm:multiblock_cell_h3_r3_m6",
                    "hbm:multiblock_cell_h3_r3_l15",
                    "hbm:multiblock_cell_h3_r6_m13",
                    "hbm:multiblock_cell_h3_r6_m6",
                    "hbm:multiblock_cell_h3_r18",
                    "hbm:multiblock_cell_h5_r6",
                    "hbm:multiblock_cell_h5_r10",
                    "hbm:multiblock_cell_h5_r12",
                    "hbm:multiblock_cell_h5_r18",
                    "hbm:multiblock_cell_h5_r30_m6",
                    "hbm:multiblock_cell_h5_r36_m6",
                    "hbm:multiblock_cell_h5_r36",
                    "hbm:multiblock_cell_h5_r60",
                    "hbm:multiblock_cell_h5_r360_m6",
                    "hbm:multiblock_cell_h10_r6_m6",
                    "hbm:multiblock_cell_h10_r6",
                    "hbm:multiblock_cell_h10_r12_m6",
                    "hbm:multiblock_cell_h10_r12",
                    "hbm:multiblock_cell_h10_r60_m6",
                    "hbm:multiblock_cell_h10_r450_m6",
                    "hbm:multiblock_cell_h10_r600_m6",
                    "hbm:multiblock_cell_h20_r1200_m6",
                    "hbm:multiblock_cell_h50_r36",
                    "hbm:multiblock_cell_h100_r480",
                    "hbm:multiblock_geometry_cell_door_h5_r30_m6",
                    "hbm:multiblock_geometry_cell_door_h10_r60_m6",
                    "hbm:multiblock_geometry_cell_door_h10_r450_m6",
                    "hbm:multiblock_geometry_cell_door_h10_r600_m6",
                    "hbm:multiblock_geometry_cell_door_sealed_h10_r600_m6",
                    "hbm:multiblock_geometry_cell_door_sealed_h20_r1200_m6",
                    "hbm:multiblock_geometry_cell_static_h3_r18",
                    "hbm:multiblock_geometry_cell_static_h5_r6_m6",
                    "hbm:multiblock_geometry_cell_static_h5_r36_m6",
                    "hbm:multiblock_geometry_cell_static_h5_r6",
                    "hbm:multiblock_geometry_cell_static_h5_r12",
                    "hbm:multiblock_geometry_cell_static_h5_r60",
                    "hbm:multiblock_geometry_cell_static_h5_r360_m6",
                    "hbm:multiblock_geometry_cell_static_h50_r36",
                    "hbm:multiblock_geometry_cell_rail_h5_r6");

    MultiblockCellLayoutFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        return fixTypeEverywhereTyped(
                "MultiblockCellLayoutFix",
                getInputSchema().getType(HbmDataFixers.CHUNK),
                chunk -> chunk.update(DSL.remainderFinder(), MultiblockCellLayoutFix::fix));
    }

    private static <T> Dynamic<T> fix(Dynamic<T> input) {
        var tag = (CompoundTag) input.convert(NbtOps.INSTANCE).getValue();
        long[] rows = tag.getLongArray(ChunkExtension.CORE_INDEX_NBT_KEY).orElse(null);
        if (rows == null) return input;
        var cells = new CellChunk14(tag);
        var owners = new Long2ObjectOpenHashMap<CompoundTag>();
        for (var entry : tag.getListOrEmpty(CellDataFixContext.OWNERS)) {
            var owner = (CompoundTag) entry;
            owners.put(owner.getLong("pos").orElseThrow(), owner);
        }
        var retained = new LongArrayList(rows.length);
        int chunkX = tag.getIntOr("xPos", 0), chunkZ = tag.getIntOr("zPos", 0);
        int minY = tag.getIntOr("yPos", 0) * 16;
        for (long row : rows) {
            int population = (int) (row >>> 59) & 3;
            int key = (int) (row >>> 39) & 0xfffff;
            int x = (chunkX << 4) + (key >>> 16), z = (chunkZ << 4) + ((key >>> 12) & 15);
            int y = minY + (key & 4095);
            var actual = cells.state(x, y, z);
            String name = actual.getStringOr("Name", "minecraft:air");
            if (population == 3) {
                int resolved =
                        CellLayouts14.isMember(name) ? 2 : CellLayouts14.isPort(name) ? 1 : 0;
                row = row & ROW_MASK | (long) resolved << 59;
            }
            if ((population != 0 && population != 3) || !shared(name)) {
                retained.add(row);
                continue;
            }
            var core = CellDataFixContext.core(row, x, y, z);
            CompoundTag state, be;
            if (core.getX() >> 4 == chunkX && core.getZ() >> 4 == chunkZ) {
                state = cells.state(core.getX(), core.getY(), core.getZ());
                be = cells.blockEntity(core.getX(), core.getY(), core.getZ());
            } else {
                var owner = owners.get(core.asLong());
                state = owner == null ? CellChunk14.air() : owner.getCompoundOrEmpty("state");
                be = owner == null ? new CompoundTag() : owner.getCompoundOrEmpty("be");
            }
            var replacement =
                    CellLayouts14.has(state.getStringOr("Name", "minecraft:air"))
                            ? CellLayouts14.reconstruct(
                                    state,
                                    x - core.getX(),
                                    y - core.getY(),
                                    z - core.getZ(),
                                    flags(actual),
                                    be)
                            : null;
            cells.set(x, y, z, replacement == null ? CellChunk14.air() : replacement);
            if (replacement != null) retained.add(row & ROW_MASK);
        }
        cells.removeNames(REMOVED);
        var fixed = cells.write();
        boolean rowsChanged =
                retained.size() != rows.length
                        || !Arrays.equals(
                                rows, 0, rows.length, retained.elements(), 0, retained.size());
        if (fixed == tag && !rowsChanged && !tag.contains(CellDataFixContext.OWNERS)) return input;
        if (fixed == tag) fixed = CellChunk14.shallow(tag);
        if (rowsChanged)
            fixed.putLongArray(ChunkExtension.CORE_INDEX_NBT_KEY, retained.toLongArray());
        fixed.remove(CellDataFixContext.OWNERS);
        return new Dynamic<>(NbtOps.INSTANCE, fixed).convert(input.getOps());
    }

    static boolean shared(String name) {
        return name.contains(":multiblock_cell_") || name.contains(":multiblock_geometry_cell_");
    }

    static long flags(CompoundTag state) {
        long flags = 0;
        var properties = state.getCompoundOrEmpty("Properties");
        if (properties.contains("open")) {
            flags |= 1L << 59;
            if (properties.getStringOr("open", "false").equals("true")) flags |= 1L << 60;
        }
        if (properties.contains("waterlogged")) {
            flags |= 1L << 61;
            if (properties.getStringOr("waterlogged", "false").equals("true")) flags |= 1L << 62;
        }
        return flags;
    }
}
