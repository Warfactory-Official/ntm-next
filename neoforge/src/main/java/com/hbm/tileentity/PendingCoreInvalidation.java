// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.platform.Services;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

public final class PendingCoreInvalidation {

    private static final Map<ServerLevel, Long2ObjectMap<LongArrayList>> WAITING =
            new WeakHashMap<>();

    private PendingCoreInvalidation() {}

    public static void init() {

        Services.SERVER.onServerStopping(server -> WAITING.clear());
    }

    public static void await(ServerLevel level, BlockPos core) {
        long chunkKey = ChunkPos.pack(core);
        LongArrayList waiting =
                WAITING.computeIfAbsent(level, l -> new Long2ObjectOpenHashMap<>())
                        .computeIfAbsent(chunkKey, k -> new LongArrayList());
        long packed = core.asLong();
        for (int i = 0; i < waiting.size(); i++) {
            if (waiting.getLong(i) == packed) return;
        }
        waiting.add(packed);
    }

    public static void onChunkFull(ServerLevel level, LevelChunk chunk) {
        Long2ObjectMap<LongArrayList> byChunk = WAITING.get(level);
        if (byChunk == null) return;
        LongArrayList waiting = byChunk.remove(chunk.getPos().pack());
        if (waiting == null) return;
        for (int i = 0; i < waiting.size(); i++) {
            BlockPos core = BlockPos.of(waiting.getLong(i));

            BlockState state = chunk.getBlockState(core);
            if (state.getBlock() instanceof BlockMultiblockCore folded) {
                folded.reindexLoadedCells(level, core);
            }
        }
    }

    public static int awaiting(ServerLevel level, long chunkKey) {
        Long2ObjectMap<LongArrayList> byChunk = WAITING.get(level);
        if (byChunk == null) return 0;
        LongArrayList waiting = byChunk.get(chunkKey);
        return waiting == null ? 0 : waiting.size();
    }

    public static boolean coreChunkMissing(ServerLevel level, BlockPos core) {
        return level.getChunkSource()
                        .getChunkNow(
                                SectionPos.blockToSectionCoord(core.getX()),
                                SectionPos.blockToSectionCoord(core.getZ()))
                == null;
    }
}
