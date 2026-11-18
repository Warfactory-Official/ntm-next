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
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.LongStream;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.ChunkPos;

public final class CellDataFixContext {
    public static final String OWNERS = "__hbm_cell_owners";

    private CellDataFixContext() {}

    public static CompletableFuture<Optional<CompoundTag>> prepare(
            Optional<CompoundTag> value,
            Function<ChunkPos, CompletableFuture<Optional<CompoundTag>>> reader) {
        if (value.isEmpty() || value.get().getIntOr(HbmDataFixers.VERSION_KEY, 0) >= 14)
            return CompletableFuture.completedFuture(value);
        var chunk = value.get();
        long[] rows = rows(chunk);
        if (rows.length == 0) return CompletableFuture.completedFuture(value);
        int chunkX = chunk.getIntOr("xPos", 0), chunkZ = chunk.getIntOr("zPos", 0);
        int minY = chunk.getIntOr("yPos", 0) * 16;
        long chunkKey = ChunkPos.pack(chunkX, chunkZ);
        var cells = new CellChunk14(chunk);
        var groups = new Long2ObjectOpenHashMap<LongOpenHashSet>();
        for (long row : rows) {
            int population = (int) (row >>> 59) & 3;
            if (population != 0 && population != 3) continue;
            int key = (int) (row >>> 39) & 0xfffff;
            int x = (chunkX << 4) + (key >>> 16),
                    z = (chunkZ << 4) + ((key >>> 12) & 15),
                    y = minY + (key & 4095);
            if (!MultiblockCellLayoutFix.shared(
                    cells.state(x, y, z).getStringOr("Name", "minecraft:air"))) continue;
            var core = core(row, x, y, z);
            long ownerChunk = ChunkPos.pack(core);
            if (ownerChunk != chunkKey)
                groups.computeIfAbsent(
                                ownerChunk, _ -> new LongOpenHashSet())
                        .add(core.asLong());
        }
        if (groups.isEmpty()) return CompletableFuture.completedFuture(value);
        var reads = new ArrayList<CompletableFuture<ListTag>>();
        for (var group : groups.long2ObjectEntrySet()) {
            var ownerChunk =
                    new ChunkPos(
                            ChunkPos.getX(group.getLongKey()), ChunkPos.getZ(group.getLongKey()));
            reads.add(
                    reader.apply(ownerChunk)
                            .exceptionally(_ -> Optional.empty())
                            .thenApply(raw -> owners(raw, group.getValue())));
        }
        return CompletableFuture.allOf(reads.toArray(CompletableFuture[]::new))
                .thenApply(
                        _ -> {
                            var owners = new ListTag();
                            for (var read : reads) owners.addAll(read.resultNow());
                            var prepared = CellChunk14.shallow(chunk);
                            prepared.put(OWNERS, owners);
                            return Optional.of(prepared);
                        });
    }

    private static ListTag owners(Optional<CompoundTag> raw, LongOpenHashSet positions) {
        var owners = new ListTag();
        if (raw.isEmpty()) return owners;
        var palette = new CellChunk14(raw.get());
        for (long packed : positions) {
            var pos = BlockPos.of(packed);
            var owner = new CompoundTag();
            owner.putLong("pos", packed);
            owner.put("state", palette.state(pos.getX(), pos.getY(), pos.getZ()).copy());
            owner.put("be", palette.blockEntity(pos.getX(), pos.getY(), pos.getZ()).copy());
            owners.add(owner);
        }
        return owners;
    }

    private static long[] rows(CompoundTag chunk) {
        var rows = chunk.getLongArray(ChunkExtension.CORE_INDEX_NBT_KEY);
        if (rows.isPresent()) return rows.get();
        return LongStream.of(
                        chunk.getLongArray(CoreIndexWideningFix.LEGACY_KEY).orElse(new long[0]))
                .map(CoreIndexWideningFix::widen)
                .toArray();
    }

    static BlockPos core(long row, int x, int y, int z) {
        int dx = (int) row << 19 >> 19;
        int dy = (int) (row >>> 13) << 19 >> 19;
        int dz = (int) (row >>> 26) << 19 >> 19;
        return new BlockPos(x + dx, y + dy, z + dz);
    }
}
