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

package com.hbm.blockentity;

import com.hbm.blocks.multiblock.AssemblyPattern;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.blocks.multiblock.MultiblockMaskTable;
import com.hbm.platform.ServerEvents;
import com.hbm.util.PositionWatchers;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public abstract class StructureAssemblyBlockEntity extends BlockEntity {
    private static final PositionWatchers<StructureAssemblyBlockEntity> WATCHERS =
            new PositionWatchers<>();
    private static final List<StructureAssemblyBlockEntity> PENDING = new ArrayList<>();
    private static final long[] NO_CHUNKS = {};

    private AssemblyPattern pattern;
    private Direction facing;
    private long[] matching;
    private int[] missing;
    private long[] chunks = NO_CHUNKS;
    private boolean[] readable;
    private int ownerChunk;
    private boolean queued;
    private boolean reset;

    protected StructureAssemblyBlockEntity(
            BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static void init() {
        final Consumer<MinecraftServer> callback = _ -> processPending();
        ServerEvents.TICK_POST.add(callback);
        final Consumer<MinecraftServer> callback2 =
                _ -> {
                    WATCHERS.clear();
                    PENDING.clear();
                };
        ServerEvents.STOPPING.add(callback2);
    }

    public abstract AssemblyPattern getPattern();

    public abstract AABB getPreviewBounds();

    protected abstract void assemble(int variant);

    public Direction getAssemblyFacing() {
        return Direction.SOUTH;
    }

    public int getPreviewVariant(long milliseconds) {
        return 0;
    }

    public boolean usesSouthPreviewNormal() {
        return false;
    }

    public AABB getVisualBounds() {
        return getPreviewBounds();
    }

    public final void initializeAssembly() {
        var server = (ServerLevel) level;
        detach(server);
        pattern = getPattern();
        facing = getAssemblyFacing();
        matching = new long[(pattern.size() + 63) >> 6];
        missing = pattern.createMissingCounts();
        var touched = new LongOpenHashSet();
        touched.add(ChunkPos.pack(worldPosition));
        var cursor = new BlockPos.MutableBlockPos();
        for (int i = 0; i < pattern.size(); i++) {
            position(i, cursor);
            touched.add(ChunkPos.pack(cursor));
        }
        chunks = touched.toLongArray();
        Arrays.sort(chunks);
        ownerChunk = Arrays.binarySearch(chunks, ChunkPos.pack(worldPosition));
        readable = new boolean[chunks.length];
        for (long chunk : chunks) WATCHERS.add(server, chunk, this);
        reset = false;
        for (long key : chunks) {
            var chunk = readableChunk(server, key);
            refreshChunk(key, chunk != null);
        }
    }

    private static @Nullable LevelChunk readableChunk(ServerLevel level, long key) {
        int x = ChunkPos.getX(key), z = ChunkPos.getZ(key);
        var publishing = ChunkPublication.loading(level, x, z);
        return publishing != null
                ? publishing
                : MultiblockCoreBlock.readableChunk(level, x << 4, z << 4);
    }

    private void position(int cell, BlockPos.MutableBlockPos out) {
        long offset = pattern.getOffset(cell);
        int x = BlockPos.getX(offset), y = BlockPos.getY(offset), z = BlockPos.getZ(offset);
        out.setWithOffset(
                worldPosition,
                MultiblockMaskTable.worldX(x, y, z, facing),
                y,
                MultiblockMaskTable.worldZ(x, y, z, facing));
    }

    private void updateMatch(int cell, boolean matches) {
        long bit = 1L << cell;
        int word = cell >> 6;
        if (((matching[word] & bit) != 0) == matches) return;
        matching[word] ^= bit;
        int delta = matches ? -1 : 1;
        for (int bits = pattern.getVariantMask(cell); bits != 0; bits &= bits - 1) {
            missing[Integer.numberOfTrailingZeros(bits)] += delta;
        }
    }

    private int completedVariant() {
        if (!readable[ownerChunk]) return -1;
        for (int i = 0; i < missing.length; i++) if (missing[i] == 0) return i;
        return -1;
    }

    private void queue() {
        if (queued) return;
        queued = true;
        PENDING.add(this);
    }

    private void changed(BlockPos pos, BlockState state) {
        if (pos.equals(worldPosition)) {
            if (pattern != getPattern() || facing != getAssemblyFacing()) {
                reset = true;
                queue();
            }
            return;
        }
        int chunk = Arrays.binarySearch(chunks, ChunkPos.pack(pos));
        if (!readable[chunk]) return;
        int dx = pos.getX() - worldPosition.getX(), dy = pos.getY() - worldPosition.getY();
        int dz = pos.getZ() - worldPosition.getZ();
        int cell =
                pattern.findCell(
                        BlockPos.asLong(
                                MultiblockMaskTable.localX(dx, dy, dz, facing),
                                dy,
                                MultiblockMaskTable.localZ(dx, dy, dz, facing)));
        if (cell < 0) return;
        updateMatch(cell, pattern.matches(cell, state));
        if (completedVariant() >= 0) queue();
    }

    private void refreshChunk(long key, boolean available) {
        int index = Arrays.binarySearch(chunks, key);
        if (readable[index] == available) return;
        readable[index] = available;
        var chunk = available ? readableChunk((ServerLevel) level, key) : null;
        var cursor = new BlockPos.MutableBlockPos();
        for (int i = 0; i < pattern.size(); i++) {
            if (pattern.getOffset(i) == 0) continue;
            position(i, cursor);
            if (ChunkPos.pack(cursor) == key) {
                updateMatch(i, available && pattern.matches(i, chunk.getBlockState(cursor)));
            }
        }
        if (completedVariant() >= 0) queue();
    }

    public static void blockChanged(LevelChunk chunk, BlockPos pos) {
        if (!(chunk.getLevel() instanceof ServerLevel server)) return;
        var watchers = WATCHERS.at(server, chunk.getPos().pack());
        if (watchers.isEmpty()) return;
        BlockState state = chunk.getBlockState(pos);
        for (var seed : watchers) seed.changed(pos, state);
    }

    public static void blockChanged(ServerLevel level, BlockPos pos, BlockState state) {
        for (var seed : WATCHERS.at(level, ChunkPos.pack(pos))) seed.changed(pos, state);
    }

    public static void chunkReadabilityChanged(ServerLevel level, long chunk, boolean readable) {
        for (var seed : WATCHERS.at(level, chunk)) seed.refreshChunk(chunk, readable);
    }

    public static void processPending() {
        for (int i = 0; i < PENDING.size(); i++) {
            var seed = PENDING.get(i);
            if (!seed.isRemoved()) {
                if (seed.reset) seed.initializeAssembly();
                int variant = seed.completedVariant();
                if (variant >= 0) {
                    seed.detach((ServerLevel) seed.level);
                    seed.assemble(variant);
                }
            }
            seed.queued = false;
        }
        PENDING.clear();
    }

    private void detach(ServerLevel server) {
        for (long chunk : chunks) WATCHERS.remove(server, chunk, this);
        chunks = NO_CHUNKS;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level instanceof ServerLevel server) detach(server);
    }
}
