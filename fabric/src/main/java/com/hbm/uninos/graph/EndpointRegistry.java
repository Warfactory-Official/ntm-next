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

package com.hbm.uninos.graph;

import com.hbm.blocks.CapabilityDependentShape;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.platform.ServerEvents;
import com.hbm.util.ChunkPositionIndex;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.lang.ref.WeakReference;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

public final class EndpointRegistry {
    private static final int NET_DOMAINS =
            MachineCaps.POWER_IN
                    | MachineCaps.POWER_OUT
                    | MachineCaps.FLUID_IN
                    | MachineCaps.FLUID_OUT
                    | MachineCaps.FE;
    private final Long2ObjectOpenHashMap<Endpoint> byCell = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<LongOpenHashSet> remoteByOwner =
            new Long2ObjectOpenHashMap<>();
    private final ChunkPositionIndex byChunk = new ChunkPositionIndex();
    private int capabilityShapeCount;

    public static EndpointRegistry of(ServerLevel level) {
        return ((com.hbm.interfaces.injected.ServerLevelExtension) (level)).hbm$endpoints();
    }

    public static void init() {
        final BiConsumer<ServerLevel, LevelChunk> callback =
                (level, chunk) -> of(level).onChunkUnload(chunk.getPos().pack());
        ServerEvents.CHUNK_UNLOAD.add(callback);
    }

    public static void declareSelfIfEndpoint(ServerLevel level, BlockEntity be) {
        int domains = selfDeclaredBits(be);
        if ((domains & NET_DOMAINS) != 0) {
            ((com.hbm.interfaces.injected.ServerLevelExtension) (level))
                    .hbm$endpoints()
                    .declare(
                            be.getBlockPos(),
                            be.getBlockPos(),
                            MultiblockCoreBlock.MASK_ALL,
                            domains,
                            be.getBlockState().getBlock()
                                    instanceof CapabilityDependentShape.Endpoint);
        }
    }

    public static boolean isSelfEndpoint(BlockEntity be) {
        return (selfDeclaredBits(be) & NET_DOMAINS) != 0;
    }

    private static int selfDeclaredBits(BlockEntity be) {
        return MachineCaps.declaredDomains(be);
    }

    private static boolean stillFull(@Nullable WeakReference<LevelChunk> memo) {
        LevelChunk chunk = memo == null ? null : memo.get();
        return chunk != null && chunk.getFullStatus().isOrAfter(FullChunkStatus.FULL);
    }

    public void onChunkUnload(long chunkKey) {
        LongOpenHashSet cells = byChunk.take(chunkKey);
        if (cells == null) return;
        for (LongIterator it = cells.iterator(); it.hasNext(); ) {
            long cellKey = it.nextLong();
            Endpoint removed = byCell.remove(cellKey);
            removeRemote(cellKey, removed);
            if (removed.capabilityShape) capabilityShapeCount--;
        }
    }

    public void declare(BlockPos cell, BlockPos owner, int faceMask, int domains) {
        declare(cell, owner, faceMask, domains, false);
    }

    private void declare(
            BlockPos cell, BlockPos owner, int faceMask, int domains, boolean capabilityShape) {
        long cellKey = cell.asLong();
        if (faceMask == 0) {
            withdraw(cell);
            return;
        }
        long ownerKey = owner.asLong();
        Endpoint previous =
                byCell.put(cellKey, new Endpoint(ownerKey, faceMask, domains, capabilityShape));
        if (previous != null) {
            removeRemote(cellKey, previous);
            if (previous.capabilityShape) capabilityShapeCount--;
        }
        if (ownerKey != cellKey) {
            remoteByOwner.computeIfAbsent(ownerKey, ignored -> new LongOpenHashSet()).add(cellKey);
        }
        if (capabilityShape) capabilityShapeCount++;
        byChunk.add(cellKey);
    }

    public void withdraw(BlockPos cell) {
        long cellKey = cell.asLong();
        Endpoint removed = byCell.remove(cellKey);
        if (removed != null) {
            removeRemote(cellKey, removed);
            if (removed.capabilityShape) capabilityShapeCount--;
        }
        byChunk.remove(cellKey);
    }

    private void removeRemote(long cellKey, Endpoint endpoint) {
        if (endpoint.owner == cellKey) return;
        LongOpenHashSet cells = remoteByOwner.get(endpoint.owner);
        assert cells != null;
        boolean removed = cells.remove(cellKey);
        assert removed;
        if (cells.isEmpty()) remoteByOwner.remove(endpoint.owner);
    }

    public void forEachRemoteCell(long ownerKey, int domains, CellVisitor visitor) {
        LongOpenHashSet cells = remoteByOwner.get(ownerKey);
        if (cells == null) return;
        for (long cellKey : cells) {
            Endpoint endpoint = byCell.get(cellKey);
            assert endpoint != null;
            if ((endpoint.domains & domains) != 0) visitor.visit(cellKey, endpoint.faceMask);
        }
    }

    @FunctionalInterface
    public interface CellVisitor {
        void visit(long cellKey, int faceMask);
    }

    public @Nullable Endpoint declared(long cellKey) {
        return byCell.get(cellKey);
    }

    public @Nullable BlockEntity resolve(
            ServerLevel level, long cellKey, @Nullable Direction face, int domains) {
        Endpoint ep = byCell.get(cellKey);
        if (ep == null) return null;
        if ((ep.domains() & domains) == 0) return null;
        if (face != null && !ep.isOpen(face)) return null;
        WeakReference<BlockEntity> memo = ep.resolved;
        BlockEntity cached = memo == null ? null : memo.get();
        if (cached != null && !cached.isRemoved() && stillFull(ep.resolvedChunk)) return cached;
        BlockPos owner = BlockPos.of(ep.owner());
        LevelChunk chunk = MultiblockCoreBlock.readableChunk(level, owner.getX(), owner.getZ());
        if (chunk == null) {
            ep.resolved = null;
            ep.resolvedChunk = null;
            return null;
        }
        BlockEntity be = chunk.getBlockEntity(owner);
        if (be == null || be.isRemoved()) {
            ep.resolved = null;
            ep.resolvedChunk = null;
            return null;
        }
        ep.resolved = new WeakReference<>(be);
        ep.resolvedChunk = new WeakReference<>(chunk);
        return be;
    }

    public @Nullable LongOpenHashSet cellsInChunk(long chunkKey) {
        return byChunk.get(chunkKey);
    }

    public boolean hasCapabilityShapes() {
        return capabilityShapeCount != 0;
    }

    public boolean hasCapabilityShape(long posKey) {
        Endpoint endpoint = byCell.get(posKey);
        return endpoint != null && endpoint.capabilityShape;
    }

    public int capabilityNeighbours(long posKey) {
        if (capabilityShapeCount == 0) return 0;
        int neighbours = 0;
        for (Direction direction : Direction.VALUES) {
            if (hasCapabilityShape(LevelNodeGraph.offset(posKey, direction)))
                neighbours |= 1 << direction.ordinal();
        }
        return neighbours;
    }

    public int size() {
        return byCell.size();
    }

    public static final class Endpoint {

        private final long owner;
        private final int faceMask;
        private final int domains;
        private final boolean capabilityShape;

        private @Nullable WeakReference<BlockEntity> resolved;

        private @Nullable WeakReference<LevelChunk> resolvedChunk;

        public Endpoint(long owner, int faceMask, int domains) {
            this(owner, faceMask, domains, false);
        }

        private Endpoint(long owner, int faceMask, int domains, boolean capabilityShape) {
            this.owner = owner;
            this.faceMask = faceMask;
            this.domains = domains;
            this.capabilityShape = capabilityShape;
        }

        public long owner() {
            return owner;
        }

        public int faceMask() {
            return faceMask;
        }

        public int domains() {
            return domains;
        }

        public boolean isOpen(Direction face) {
            return (faceMask & (1 << face.ordinal())) != 0;
        }
    }
}
