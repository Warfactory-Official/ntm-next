// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.fluidmk2;

import com.hbm.handler.threading.TargetPoint;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluidProperty;
import com.hbm.packet.toclient.FluidPipeTintBulkPayload;
import com.hbm.packet.toclient.FluidPipeTintPayload;
import com.hbm.packet.toserver.FluidPipeTintRequestPayload;
import com.hbm.platform.Services;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.CommonColors;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

public final class FluidPipeTintData {

    public static final int DEFAULT_COLOR = 0xFF888888;
    public static final int UNKNOWN_FLUID_COLOR = CommonColors.WHITE;

    private static final Long2ObjectOpenHashMap<Long2ObjectOpenHashMap<Fluid>> FLUIDS =
            new Long2ObjectOpenHashMap<>();
    private static final LongOpenHashSet REQUESTED = new LongOpenHashSet();
    private static final LongOpenHashSet PENDING = new LongOpenHashSet();
    private static ResourceKey<Level> dimension;

    private FluidPipeTintData() {}

    private static long chunkOf(long posKey) {
        return ChunkPos.pack(BlockPos.of(posKey));
    }

    public static int colorFor(Fluid fluid) {
        if (fluid == Fluids.EMPTY) return DEFAULT_COLOR;
        NTMFluidProperty prop = NTMFluidProperties.get(fluid);
        return prop != null ? prop.colorARGB() : UNKNOWN_FLUID_COLOR;
    }

    public static int colorAt(ResourceKey<Level> dim, BlockPos pos) {
        return colorFor(fluidAt(dim, pos));
    }

    public static Fluid fluidAt(ResourceKey<Level> dim, BlockPos pos) {
        Fluid fluid = fluidAtIfKnown(dim, pos);
        return fluid == null ? Fluids.EMPTY : fluid;
    }

    public static @Nullable Fluid fluidAtIfKnown(ResourceKey<Level> dim, BlockPos pos) {
        select(dim);
        long posKey = pos.asLong();
        Long2ObjectOpenHashMap<Fluid> chunk = FLUIDS.get(chunkOf(posKey));
        Fluid fluid = chunk == null ? null : chunk.get(posKey);
        if (fluid == null && REQUESTED.add(posKey)) PENDING.add(posKey);
        return fluid;
    }

    public static void flushRequests() {
        if (PENDING.isEmpty()) return;
        long[] keys = new long[PENDING.size()];
        int count = 0;
        for (LongIterator it = PENDING.iterator(); it.hasNext(); ) {
            long key = it.nextLong();
            if (REQUESTED.contains(key) && !known(key)) keys[count++] = key;
        }
        PENDING.clear();
        if (count == 0) return;
        Services.NETWORK.sendToServer(
                new FluidPipeTintRequestPayload(
                        count == keys.length ? keys : Arrays.copyOf(keys, count)));
    }

    private static boolean known(long posKey) {
        Long2ObjectOpenHashMap<Fluid> chunk = FLUIDS.get(chunkOf(posKey));
        return chunk != null && chunk.containsKey(posKey);
    }

    public static void set(ResourceKey<Level> dim, long posKey, Fluid fluid) {
        select(dim);
        put(posKey, fluid);
    }

    public static void setAll(ResourceKey<Level> dim, long[] posKeys, Fluid fluid) {
        select(dim);
        for (long posKey : posKeys) put(posKey, fluid);
    }

    private static void put(long posKey, Fluid fluid) {
        FLUIDS.computeIfAbsent(chunkOf(posKey), k -> new Long2ObjectOpenHashMap<>())
                .put(posKey, fluid);
        REQUESTED.remove(posKey);
        PENDING.remove(posKey);
    }

    public static void evictChunk(long chunkKey) {
        FLUIDS.remove(chunkKey);
        REQUESTED.removeIf((long key) -> chunkOf(key) == chunkKey);
        PENDING.removeIf((long key) -> chunkOf(key) == chunkKey);
    }

    public static void sync(ServerLevel level, BlockPos pos, Fluid fluid) {
        Services.NETWORK.sendToAllTracking(
                new FluidPipeTintPayload(pos, fluid),
                new TargetPoint(
                        level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 64.0D));
    }

    public static void sync(ServerLevel level, long[] posKeys, Fluid fluid) {
        if (posKeys.length == 0) return;
        Long2ObjectOpenHashMap<LongArrayList> byChunk = new Long2ObjectOpenHashMap<>();
        for (long posKey : posKeys) {
            byChunk.computeIfAbsent(chunkOf(posKey), k -> new LongArrayList()).add(posKey);
        }
        for (var entry : byChunk.long2ObjectEntrySet()) {
            List<ServerPlayer> watching =
                    level.getChunkSource()
                            .chunkMap
                            .getPlayers(ChunkPos.unpack(entry.getLongKey()), false);
            if (watching.isEmpty()) continue;
            Services.NETWORK.sendToPlayers(
                    new FluidPipeTintBulkPayload(entry.getValue().toLongArray(), fluid), watching);
        }
    }

    public static void syncTo(ServerPlayer player, BlockPos pos, Fluid fluid) {
        Services.NETWORK.sendTo(new FluidPipeTintPayload(pos, fluid), player);
    }

    public static void syncTo(ServerPlayer player, long[] posKeys, Fluid fluid) {
        if (posKeys.length == 0) return;
        Services.NETWORK.sendTo(new FluidPipeTintBulkPayload(posKeys, fluid), player);
    }

    private static void select(ResourceKey<Level> dim) {
        if (dimension == null || !dimension.equals(dim)) {
            FLUIDS.clear();
            REQUESTED.clear();
            PENDING.clear();
            dimension = dim;
        }
    }
}
