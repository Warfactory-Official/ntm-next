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

package com.hbm.explosion;

import com.hbm.NuclearTech;
import com.hbm.blocks.ModBlocks;
import com.hbm.config.BombConfig;
import com.hbm.handler.threading.BombForkJoinPool;
import com.hbm.util.BulkSectionUpdates;
import com.hbm.util.ChunkRequests;
import com.hbm.util.ChunkUtil;
import com.hbm.util.SectionAccess;
import com.hbm.util.SectionSnapshot;
import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;

public final class ExplosionTom {
    public static final int LEGACY_TERRAIN = 63;
    public static final int GROUND_REACH = 8;
    private static final int SPREAD = 2;
    private static final float WASH_DISTANCE = 500F;
    private static final long COLUMN_SALT = 0x9E3779B97F4A7C15L;
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final ThreadLocal<Long2ObjectOpenHashMap<BlockState>> TL_UPDATES =
            ThreadLocal.withInitial(() -> new Long2ObjectOpenHashMap<>(1 << 14));

    private final ServerLevel level;
    private final int posX;
    private final int posZ;
    private final int radius;
    private final int radius2;
    private final int terrain;
    private final long seed;
    private final BombForkJoinPool.IJobCancellable owner;

    private final LongArrayList order = new LongArrayList();
    private final LongOpenHashSet carved = new LongOpenHashSet();
    private final Long2IntOpenHashMap indexOf = new Long2IntOpenHashMap();
    private int span;
    private int nextRequest;
    private int nextSubmit;
    private int firstOpen;
    private final LongOpenHashSet loaded = new LongOpenHashSet();
    private final LongOpenHashSet resubmit = new LongOpenHashSet();
    private @Nullable ChunkRequests chunkRequests;
    private final LongOpenHashSet published = new LongOpenHashSet();
    private final LongOpenHashSet pendingResends = new LongOpenHashSet();
    private @Nullable ForkJoinPool pool;
    private @Nullable BulkSectionUpdates updates;
    private volatile @Nullable Throwable failure;
    private boolean closed;

    public ExplosionTom(
            ServerLevel level,
            int x,
            int z,
            int radius,
            int terrain,
            long seed,
            BombForkJoinPool.IJobCancellable owner) {
        this.level = level;
        this.posX = x;
        this.posZ = z;
        this.radius = radius;
        this.radius2 = radius * radius;
        this.terrain = terrain;
        this.seed = seed;
        this.owner = owner;
    }

    public static int groundLevel(ServerLevel level, int x, int z) {
        int side = GROUND_REACH * 2 + 1;
        int[] heights = new int[side * side];
        int i = 0;
        for (int dx = -GROUND_REACH; dx <= GROUND_REACH; dx++) {
            for (int dz = -GROUND_REACH; dz <= GROUND_REACH; dz++) {
                int bx = x + dx;
                int bz = z + dz;
                heights[i++] =
                        level.getChunk(bx >> 4, bz >> 4)
                                        .getHeight(
                                                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                                                bx & 15,
                                                bz & 15)
                                + 1;
            }
        }
        Arrays.sort(heights);
        return heights[heights.length / 2];
    }

    public boolean tick() {
        return tick(System.nanoTime() + BombConfig.mk5 * 1_000_000L);
    }

    public boolean tick(long deadline) {
        if (closed) return true;
        if (updates == null) start();
        int previous;
        do {
            previous = firstOpen;
            request(deadline);
            poll();
            updates.drain(deadline);
        } while (firstOpen != previous && System.nanoTime() < deadline);
        if (published.size() < carved.size() || updates.hasPending() || !pendingResends.isEmpty())
            return false;
        close();
        return true;
    }

    public void close() {
        if (closed) return;
        closed = true;
        if (updates != null) updates.close();
        if (chunkRequests != null) chunkRequests.close();
        if (pool != null) {
            BombForkJoinPool.unregister(pool, level.dimension().identifier(), owner);
            BombForkJoinPool.release(pool);
            pool = null;
        }
    }

    private void start() {
        gather();
        pool = BombForkJoinPool.acquire();
        BombForkJoinPool.register(pool, level.dimension().identifier(), owner);
        updates = new BulkSectionUpdates(level, pool, this::fail);
        chunkRequests = new ChunkRequests(level, this::chunkReady, this::fail);
    }

    private void gather() {
        int reach = radius + SPREAD + 1;
        long reach2 = (long) reach * reach;
        int c0x = (posX - reach) >> 4;
        int c1x = (posX + reach) >> 4;
        int c0z = (posZ - reach) >> 4;
        int c1z = (posZ + reach) >> 4;
        LongOpenHashSet union = new LongOpenHashSet();
        for (int cx = c0x; cx <= c1x; cx++) {
            for (int cz = c0z; cz <= c1z; cz++) {
                long dx = Math.max(Math.max((cx << 4) - posX, posX - ((cx << 4) + 15)), 0);
                long dz = Math.max(Math.max((cz << 4) - posZ, posZ - ((cz << 4) + 15)), 0);
                if (dx * dx + dz * dz > reach2) continue;
                long ck = ChunkPos.pack(cx, cz);
                carved.add(ck);
                for (int nx = -1; nx <= 1; nx++) {
                    for (int nz = -1; nz <= 1; nz++) union.add(ChunkPos.pack(cx + nx, cz + nz));
                }
            }
        }
        long[] keyed = new long[union.size()];
        int i = 0;
        for (LongIterator it = union.iterator(); it.hasNext(); ) {
            long ck = it.nextLong();
            keyed[i++] =
                    ((long) (ChunkPos.getZ(ck) ^ Integer.MIN_VALUE) << 32)
                            | ((ChunkPos.getX(ck) ^ Integer.MIN_VALUE) & 0xFFFFFFFFL);
        }
        Arrays.sort(keyed);
        for (long key : keyed) {
            long ck =
                    ChunkPos.pack(
                            (int) key ^ Integer.MIN_VALUE, (int) (key >>> 32) ^ Integer.MIN_VALUE);
            indexOf.put(ck, order.size());
            order.add(ck);
        }
        for (LongIterator it = carved.iterator(); it.hasNext(); ) {
            long ck = it.nextLong();
            int at = indexOf.get(ck);
            int cx = ChunkPos.getX(ck);
            int cz = ChunkPos.getZ(ck);
            for (int nx = -1; nx <= 1; nx++) {
                for (int nz = -1; nz <= 1; nz++) {
                    span = Math.max(span, indexOf.get(ChunkPos.pack(cx + nx, cz + nz)) - at);
                }
            }
        }
        advanceFirstOpen();
    }

    private void advanceFirstOpen() {
        while (firstOpen < order.size()) {
            long next = order.getLong(firstOpen);
            if (carved.contains(next) && !published.contains(next)) return;
            firstOpen++;
        }
    }

    private void request(long deadline) {
        int budget = BombConfig.chunksInFlight();
        int limit = Math.min(order.size(), firstOpen + span + 1 + budget);
        int retainedLimit = budget + (span << 1) + 1;
        while (nextRequest < limit
                && chunkRequests.pendingCount() < budget
                && chunkRequests.retainedCount() < retainedLimit
                && System.nanoTime() < deadline) {
            chunkRequests.request(order.getLong(nextRequest++));
        }
        chunkRequests.pump(deadline, budget);
    }

    private void chunkReady(long ck) {
        if (resubmit.remove(ck)) updates.submit(new CraterWork(ck));
        else if (carved.contains(ck)) loaded.add(ck);
    }

    private void poll() {
        while (nextSubmit < order.size()) {
            long ck = order.getLong(nextSubmit);
            if (!carved.contains(ck)) {
                nextSubmit++;
                continue;
            }
            if (!loaded.remove(ck)) break;
            updates.submit(new CraterWork(ck));
            nextSubmit++;
        }
    }

    private boolean neighbourhoodLive(long ck) {
        int cx = ChunkPos.getX(ck);
        int cz = ChunkPos.getZ(ck);
        for (int nx = -1; nx <= 1; nx++) {
            for (int nz = -1; nz <= 1; nz++) {
                if (ChunkUtil.liveChunkNow(level, ChunkPos.pack(cx + nx, cz + nz)) == null)
                    return false;
            }
        }
        return true;
    }

    private void releaseSettled(long ck) {
        int cx = ChunkPos.getX(ck);
        int cz = ChunkPos.getZ(ck);
        for (int nx = -1; nx <= 1; nx++) {
            for (int nz = -1; nz <= 1; nz++) {
                long held = ChunkPos.pack(cx + nx, cz + nz);
                if (chunkRequests.holds(held) && settled(held)) chunkRequests.release(held);
            }
        }
    }

    private boolean settled(long ck) {
        int cx = ChunkPos.getX(ck);
        int cz = ChunkPos.getZ(ck);
        for (int nx = -1; nx <= 1; nx++) {
            for (int nz = -1; nz <= 1; nz++) {
                long near = ChunkPos.pack(cx + nx, cz + nz);
                if (carved.contains(near)
                        && (!published.contains(near) || pendingResends.contains(near)))
                    return false;
            }
        }
        return true;
    }

    private void fail(Throwable cause) {
        synchronized (this) {
            if (failure != null) return;
            failure = cause;
        }
        level.getServer()
                .execute(
                        () -> {
                            NuclearTech.LOGGER.error(
                                    "TOM crater failed in {} at {} / {}",
                                    level.dimension().identifier(),
                                    posX,
                                    posZ,
                                    cause);
                            owner.cancelJob();
                        });
    }

    private long columnBits(int dx, int dz) {
        return HashCommon.mix(seed ^ (BlockPos.asLong(dx, 0, dz) * COLUMN_SALT));
    }

    private int floor(int dx, int dz) {
        double planar = Math.sqrt(dx * dx + dz * dz);
        double cA =
                (terrain - Math.pow(Math.E, -Math.pow(planar, 2) / 40000) * 13)
                        + (int) (columnBits(dx, dz) & 1);
        double cB = cA + Math.pow(Math.E, -Math.pow(planar - 200, 2) / 400) * 13;
        return (int) (cB + Math.pow(Math.E, -Math.pow(planar - 500, 2) / 2000) * 37);
    }

    private boolean needsWashing(long ck) {
        if (radius <= WASH_DISTANCE - SPREAD * Math.sqrt(2)) return false;
        int minX = (ChunkPos.getX(ck) << 4) - posX;
        int minZ = (ChunkPos.getZ(ck) << 4) - posZ;
        int maxX = minX + 15;
        int maxZ = minZ + 15;
        int nearX = Math.max(Math.max(minX, -maxX), 0);
        int nearZ = Math.max(Math.max(minZ, -maxZ), 0);
        int farX = Math.max(Math.abs(minX), Math.abs(maxX));
        int farZ = Math.max(Math.abs(minZ), Math.abs(maxZ));
        double margin = 2 * SPREAD * Math.sqrt(2);
        return Math.hypot(nearX, nearZ) <= WASH_DISTANCE + margin
                && Math.hypot(farX, farZ) >= WASH_DISTANCE - margin;
    }

    private final class CraterWork extends BulkSectionUpdates.Work {
        private final long ck;
        private final int minY = level.getMinY();
        private final int maxY = level.getMaxY();
        private final Long2ObjectOpenHashMap<BlockState> oldStates = new Long2ObjectOpenHashMap<>();
        private final Long2ObjectOpenHashMap<BlockState> deferred = new Long2ObjectOpenHashMap<>();
        private final IntArrayList washing = new IntArrayList();

        CraterWork(long ck) {
            super(ck);
            this.ck = ck;
        }

        @Override
        protected long observedSections() {
            return -1L;
        }

        @Override
        protected void calculate(SectionSnapshot snapshot) {
            Long2ObjectOpenHashMap<BlockState> writes = TL_UPDATES.get();
            writes.clear();
            int chunkX = ChunkPos.getX(ck) << 4;
            int chunkZ = ChunkPos.getZ(ck) << 4;
            for (int lx = 0; lx < 16; lx++) {
                for (int lz = 0; lz < 16; lz++) {
                    int dx = chunkX + lx - posX;
                    int dz = chunkZ + lz - posZ;
                    if (radius2 - (dx * dx + dz * dz) > 0)
                        carveColumn(snapshot.sections, writes, lx, lz, dx, dz);
                    else haloColumn(snapshot.sections, writes, lx, lz, dx, dz);
                }
            }
            if (needsWashing(ck)) {
                int x0 = ChunkPos.getX(ck) << 4;
                int z0 = ChunkPos.getZ(ck) << 4;
                for (int lx = 0; lx < 16; lx++) {
                    for (int lz = 0; lz < 16; lz++) {
                        int x = x0 + lx - posX;
                        int z = z0 + lz - posZ;
                        if (radius2 - (x * x + z * z) <= 0) continue;
                        float distance = (float) Math.sqrt((double) x * x + (double) z * z);
                        int craterFloor = floor(x, z);
                        int top = topBlock(snapshot.sections, lx, lz);
                        if (distance < WASH_DISTANCE
                                && distance >= WASH_DISTANCE - SPREAD * Math.sqrt(2)
                                && top > Math.max(craterFloor, terrain + 1)) {
                            washing.add(posX + x);
                            washing.add(posZ + z);
                            washing.add(Math.max(craterFloor, terrain + 1) + 1);
                            washing.add(top);
                        }
                    }
                }
            }
            ChunkUtil.applyToSnapshot(snapshot, writes, oldStates, deferred);
            writes.clear();
        }

        private void carveColumn(
                LevelChunkSection[] sections,
                Long2ObjectOpenHashMap<BlockState> writes,
                int lx,
                int lz,
                int dx,
                int dz) {
            int bx = posX + dx;
            int bz = posZ + dz;
            float distance = (float) Math.sqrt((double) dx * dx + (double) dz * dz);
            int craterFloor = floor(dx, dz);
            int height = LEGACY_TERRAIN - 14;
            int offset = 20;
            int shift = terrain - LEGACY_TERRAIN;
            int threshold =
                    shift
                            + (int)
                                    ((float) Math.sqrt(dx * dx + dz * dz)
                                            * (float) (height + offset)
                                            / (float) radius)
                            + (int) ((columnBits(dx, dz) >>> 1) & 1)
                            - offset;
            int bottom = Math.max(shift, minY);
            int top = topBlock(sections, lx, lz);
            int y = Math.max(craterFloor, top);
            if (craterFloor < terrain + 1) y = Math.max(y, terrain + 1);
            y = Math.min(y, maxY);
            BlockState tektite = ModBlocks.TEKTITE.get().defaultBlockState();
            BlockState osmiridium = ModBlocks.ORE_TEKTITE_OSMIRIDIUM.get().defaultBlockState();
            BlockState lava = Blocks.LAVA.defaultBlockState();
            for (; y > threshold && y > bottom; y--) {
                long pos = BlockPos.asLong(bx, y, bz);
                if (y <= craterFloor)
                    writes.put(pos, HashCommon.mix(seed ^ pos) % 200 == 0 ? osmiridium : tektite);
                else if (y > terrain + 1) {
                    if (distance < WASH_DISTANCE) writes.put(pos, AIR);
                } else writes.put(pos, lava);
            }
        }

        private void haloColumn(
                LevelChunkSection[] sections,
                Long2ObjectOpenHashMap<BlockState> writes,
                int lx,
                int lz,
                int dx,
                int dz) {
            int lowest = Integer.MAX_VALUE;
            for (int nx = -SPREAD; nx <= SPREAD; nx++) {
                for (int nz = -SPREAD; nz <= SPREAD; nz++) {
                    int ax = dx + nx;
                    int az = dz + nz;
                    if (radius2 - (ax * ax + az * az) > 0) lowest = Math.min(lowest, floor(ax, az));
                }
            }
            if (lowest == Integer.MAX_VALUE) return;
            int bx = posX + dx;
            int bz = posZ + dz;
            int from = Math.min(terrain + 1, maxY);
            int to = Math.max(lowest, minY - 1);
            for (int y = from; y > to; y--) {
                if (read(sections, lx, y, lz).isAir() || drenched(sections, lx, y, lz)) {
                    writes.put(BlockPos.asLong(bx, y, bz), Blocks.LAVA.defaultBlockState());
                }
            }
        }

        private BlockState read(LevelChunkSection[] sections, int lx, int y, int lz) {
            return SectionAccess.read(sections, minY >> 4, lx, y, lz);
        }

        private boolean drenched(LevelChunkSection[] sections, int lx, int y, int lz) {
            for (int j = -SPREAD; j <= SPREAD; j++) {
                BlockState state = read(sections, lx, y + j, lz);
                if (state.is(Blocks.WATER) || state.is(BlockTags.ICE)) return true;
            }
            return false;
        }

        private int topBlock(LevelChunkSection[] sections, int lx, int lz) {
            for (int index = sections.length - 1; index >= 0; index--) {
                LevelChunkSection section = sections[index];
                if (section.hasOnlyAir()) continue;
                for (int ly = 15; ly >= 0; ly--) {
                    if (!section.getBlockState(lx, ly, lz).isAir()) return minY + (index << 4) + ly;
                }
            }
            return minY - 1;
        }

        @Override
        protected boolean readyToPublish(LevelChunk chunk) {
            return neighbourhoodLive(ck);
        }

        @Override
        protected void published(LevelChunk chunk) {
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            LongArrayList lightChanges = new LongArrayList();
            long sectionMask = 0;
            int minSection = level.getMinSectionY();
            for (ObjectIterator<Long2ObjectMap.Entry<BlockState>> it =
                            oldStates.long2ObjectEntrySet().fastIterator();
                    it.hasNext(); ) {
                Long2ObjectMap.Entry<BlockState> entry = it.next();
                long lp = entry.getLongKey();
                pos.set(lp);
                BlockState oldState = entry.getValue();
                BlockState newState = chunk.getBlockState(pos);
                if (ChunkUtil.postCarveBlockUpdate(level, chunk, pos, oldState, newState))
                    lightChanges.add(lp);
                sectionMask |= 1L << ((pos.getY() >> 4) - minSection);
            }
            for (ObjectIterator<Long2ObjectMap.Entry<BlockState>> it =
                            deferred.long2ObjectEntrySet().fastIterator();
                    it.hasNext(); ) {
                Long2ObjectMap.Entry<BlockState> entry = it.next();
                ChunkUtil.replaceEmptied(
                        level, pos.set(entry.getLongKey()), entry.getValue(), Block.UPDATE_ALL);
            }
            wash();
            if (sectionMask != 0) {
                chunk.markUnsaved();
                CompletableFuture<?> lighting =
                        ChunkUtil.updateLight(level, chunk, lightChanges, sectionMask);
                pendingResends.add(ck);
                ChunkUtil.broadcastWholesaleResend(level, ck, lighting)
                        .whenComplete(
                                (_, cause) -> {
                                    var server = level.getServer();
                                    server.schedule(
                                            server.wrapRunnable(
                                                    () -> {
                                                        if (closed) return;
                                                        if (cause != null) {
                                                            fail(cause);
                                                            return;
                                                        }
                                                        pendingResends.remove(ck);
                                                        releaseSettled(ck);
                                                    }));
                                });
            }
            published.add(ck);
            releaseSettled(ck);
            advanceFirstOpen();
        }

        private void wash() {
            if (washing.isEmpty()) return;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            LongOpenHashSet seen = new LongOpenHashSet();
            for (int i = 0; i < washing.size(); i += 4) {
                int x = washing.getInt(i);
                int z = washing.getInt(i + 1);
                int lowest = washing.getInt(i + 2) - SPREAD;
                int highest = washing.getInt(i + 3) + SPREAD;
                for (int dx = -SPREAD; dx <= SPREAD; dx++) {
                    for (int dz = -SPREAD; dz <= SPREAD; dz++) {
                        int cx = x + dx - posX;
                        int cz = z + dz - posZ;
                        if ((float) Math.sqrt((double) cx * cx + (double) cz * cz) < WASH_DISTANCE)
                            continue;
                        for (int y = lowest; y <= highest; y++) {
                            pos.set(x + dx, y, z + dz);
                            if (!seen.add(pos.asLong())) continue;
                            if (isWashedAway(level.getBlockState(pos))) {
                                ChunkUtil.replaceEmptied(level, pos, AIR, Block.UPDATE_ALL);
                            }
                        }
                    }
                }
            }
        }

        @Override
        protected void missing() {
            resubmit.add(ck);
            chunkRequests.request(ck);
        }

        @Override
        protected void clearAttempt() {
            oldStates.clear();
            deferred.clear();
            washing.clear();
        }

        @Override
        protected void discard() {
            clearAttempt();
        }
    }

    private static boolean isWashedAway(BlockState state) {
        return state.is(Blocks.WATER)
                || state.is(BlockTags.ICE)
                || state.is(Blocks.SNOW)
                || state.is(Blocks.SNOW_BLOCK)
                || state.is(Blocks.POWDER_SNOW)
                || state.ignitedByLava();
    }
}
