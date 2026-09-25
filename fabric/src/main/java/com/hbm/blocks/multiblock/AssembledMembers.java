// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.multiblock;

import com.hbm.platform.Services;
import com.hbm.uninos.graph.EndpointRegistry;
import com.hbm.uninos.graph.LevelNodeGraph;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jspecify.annotations.Nullable;

public final class AssembledMembers {

    private static final Map<ServerLevel, Long2ObjectMap<LongOpenHashSet>> WAITING =
            new WeakHashMap<>();

    private AssembledMembers() {}

    public static void init() {
        Services.SERVER.onServerStopping(server -> WAITING.clear());
    }

    public interface Member {

        void verify(ServerLevel level, BlockPos pos, BlockState state, BlockPos owner);

        default int endpointDomains(BlockState state) {
            return 0;
        }
    }

    public static void assemble(ServerLevel level, BlockPos owner, Collection<BlockPos> members) {
        BlockMultiblockCore.claimCells(level, owner, members);
        for (BlockPos member : members) {
            BlockState state = level.getBlockState(member);
            if (!declare(level, member, state, owner)) continue;
            Services.CAPS.invalidateCaps(level, member);
            LevelNodeGraph.invalidateEndpointsAround(level, member);
            BlockMultiblockCore.renotifyNeighbours(level, member);
        }
    }

    private static boolean declare(
            ServerLevel level, BlockPos member, BlockState state, BlockPos owner) {
        int domains = state.getBlock() instanceof Member m ? m.endpointDomains(state) : 0;
        if (domains == 0) return false;
        EndpointRegistry.of(level).declare(member, owner, BlockMultiblockCore.MASK_ALL, domains);
        return true;
    }

    public static @Nullable BlockPos owner(ServerLevel level, BlockPos member) {
        long packed =
                MultiblockSurface.recordedCorePacked(
                        level, member.getX(), member.getY(), member.getZ());
        return MultiblockSurface.hasCore(packed) ? BlockPos.of(packed) : null;
    }

    public static void release(ServerLevel level, BlockPos member) {
        BlockMultiblockCore.unindexCell(level, member);
        EndpointRegistry endpoints = EndpointRegistry.of(level);
        if (endpoints.declared(member.asLong()) == null) return;
        endpoints.withdraw(member);
        Services.CAPS.invalidateCaps(level, member);
        LevelNodeGraph.invalidateEndpointsAround(level, member);
    }

    public static List<BlockPos> members(ServerLevel level, BlockPos owner, BoundingBox bounds) {
        List<BlockPos> found = new ArrayList<>();
        long ownerPacked = owner.asLong();
        for (int cx = bounds.minX() >> 4; cx <= bounds.maxX() >> 4; cx++) {
            for (int cz = bounds.minZ() >> 4; cz <= bounds.maxZ() >> 4; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;
                long[] entries = chunk.hbm$coreIndex();
                for (int i = 0, n = entries == null ? 0 : chunk.hbm$coreIndexSize(); i < n; i++) {
                    long entry = entries[i];
                    if (entry < 0) continue;
                    long cell = MultiblockCoreIndex.cellPacked(chunk.getPos(), level, entry);
                    int x = BlockPos.getX(cell), y = BlockPos.getY(cell), z = BlockPos.getZ(cell);
                    if (MultiblockCoreIndex.corePacked(entry, x, y, z) != ownerPacked) continue;
                    BlockPos pos = BlockPos.of(cell);
                    if (chunk.getBlockState(pos).getBlock() instanceof Member) found.add(pos);
                }
            }
        }
        return found;
    }

    public static void onChunkFull(ServerLevel level, LevelChunk chunk) {
        Long2ObjectMap<LongOpenHashSet> byChunk = WAITING.get(level);
        LongOpenHashSet due = byChunk == null ? null : byChunk.remove(chunk.getPos().pack());
        long[] entries = chunk.hbm$coreIndex();
        for (int i = 0, n = entries == null ? 0 : chunk.hbm$coreIndexSize(); i < n; i++) {
            long entry = entries[i];
            if (entry < 0) continue;
            long cell = MultiblockCoreIndex.cellPacked(chunk.getPos(), level, entry);
            BlockPos pos = BlockPos.of(cell);
            BlockState state = chunk.getBlockState(pos);
            if (!(state.getBlock() instanceof Member)) continue;

            declare(
                    level,
                    pos,
                    state,
                    BlockPos.of(
                            MultiblockCoreIndex.corePacked(
                                    entry, pos.getX(), pos.getY(), pos.getZ())));
            if (due == null) due = new LongOpenHashSet();
            due.add(cell);
        }
        if (due == null) return;
        LongOpenHashSet checks = due;
        MinecraftServer server = level.getServer();
        server.schedule(
                server.wrapRunnable(() -> checks.forEach(cell -> check(level, BlockPos.of(cell)))));
    }

    private static void check(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);

        if (chunk == null) return;
        BlockState state = chunk.getBlockState(pos);
        if (!(state.getBlock() instanceof Member member)) return;
        BlockPos owner = owner(level, pos);
        if (owner == null) return;
        if (level.getChunkSource().getChunkNow(owner.getX() >> 4, owner.getZ() >> 4) == null) {
            WAITING.computeIfAbsent(level, l -> new Long2ObjectOpenHashMap<>())
                    .computeIfAbsent(ChunkPos.pack(owner), k -> new LongOpenHashSet())
                    .add(pos.asLong());
            return;
        }
        member.verify(level, pos, state, owner);
    }

    public static int awaiting(ServerLevel level, long chunkKey) {
        Long2ObjectMap<LongOpenHashSet> byChunk = WAITING.get(level);
        LongOpenHashSet waiting = byChunk == null ? null : byChunk.get(chunkKey);
        return waiting == null ? 0 : waiting.size();
    }
}
