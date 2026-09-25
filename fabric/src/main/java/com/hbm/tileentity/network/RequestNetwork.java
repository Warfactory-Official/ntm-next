// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.network;

import com.hbm.module.ModulePatternMatcher;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

public final class RequestNetwork {

    public static final int MAX_AGE = 40;

    public static final double MAX_LINK_RANGE = 24;

    private static final Map<ResourceKey<Level>, Long2ObjectMap<Map<BlockPos, PathNode>>> NODES =
            new HashMap<>();

    private static long tick;

    private RequestNetwork() {}

    public static void push(ServerLevel level, PathNode node) {
        node.lease = tick;
        NODES.computeIfAbsent(level.dimension(), key -> new Long2ObjectOpenHashMap<>())
                .computeIfAbsent(chunkKey(node.pos), key -> new HashMap<BlockPos, PathNode>())
                .put(node.pos, node);
    }

    public static Map<BlockPos, PathNode> localNodes(
            ServerLevel level, BlockPos centre, int chunkRange) {
        Long2ObjectMap<Map<BlockPos, PathNode>> chunks = NODES.get(level.dimension());
        if (chunks == null) return Map.of();

        Map<BlockPos, PathNode> nodes = new HashMap<>();
        int x = SectionPos.blockToSectionCoord(centre.getX());
        int z = SectionPos.blockToSectionCoord(centre.getZ());

        for (int i = -chunkRange; i <= chunkRange; i++) {
            for (int j = -chunkRange; j <= chunkRange; j++) {
                Map<BlockPos, PathNode> inChunk = chunks.get(ChunkPos.pack(x + i, z + j));
                if (inChunk != null) nodes.putAll(inChunk);
            }
        }

        return nodes;
    }

    public static void updateEntries(MinecraftServer server) {
        tick++;

        NODES.values()
                .removeIf(
                        chunks -> {
                            chunks.values()
                                    .removeIf(
                                            inChunk -> {
                                                inChunk.values()
                                                        .removeIf(
                                                                node ->
                                                                        node.lease
                                                                                < tick - MAX_AGE);
                                                return inChunk.isEmpty();
                                            });
                            return chunks.isEmpty();
                        });
    }

    public static void onServerStopping() {
        clear();
    }

    public static void clear() {
        NODES.clear();
        tick = 0;
    }

    public static boolean hasPath(ServerLevel level, BlockPos from, BlockPos to) {
        Vec3 start = Vec3.atCenterOf(from);
        Vec3 end = Vec3.atCenterOf(to);
        if (start.distanceTo(end) > MAX_LINK_RANGE) return false;

        return level.clip(
                                new ClipContext(
                                        start,
                                        end,
                                        ClipContext.Block.COLLIDER,
                                        ClipContext.Fluid.NONE,
                                        CollisionContext.empty()))
                        .getType()
                == HitResult.Type.MISS;
    }

    private static long chunkKey(BlockPos pos) {
        return ChunkPos.pack(
                SectionPos.blockToSectionCoord(pos.getX()),
                SectionPos.blockToSectionCoord(pos.getZ()));
    }

    public static class PathNode {

        public final BlockPos pos;

        public final boolean waypoint;
        public final Set<BlockPos> reachable;
        public boolean active = true;
        long lease;

        public PathNode(BlockPos pos, Set<BlockPos> reachable) {
            this(pos, reachable, true);
        }

        PathNode(BlockPos pos, Set<BlockPos> reachable, boolean waypoint) {
            this.pos = pos;
            this.waypoint = waypoint;
            this.reachable = new HashSet<>(reachable);
        }
    }

    public static final class OfferNode extends PathNode {

        public final List<ItemStack> offer;

        public OfferNode(BlockPos pos, Set<BlockPos> reachable, List<ItemStack> offer) {
            super(pos, reachable, false);
            this.offer = new ArrayList<>(offer);
        }
    }

    public record Demand(ItemStack filter, String mode) {

        public static final Codec<Demand> CODEC =
                RecordCodecBuilder.create(
                        i ->
                                i.group(
                                                ItemStack.CODEC
                                                        .fieldOf("filter")
                                                        .forGetter(Demand::filter),
                                                Codec.STRING
                                                        .fieldOf("mode")
                                                        .forGetter(Demand::mode))
                                        .apply(i, Demand::new));

        public boolean test(ItemStack stack) {
            return ModulePatternMatcher.matches(mode, filter, stack);
        }
    }

    public static final class RequestNode extends PathNode {

        public final List<Demand> request;

        public RequestNode(BlockPos pos, Set<BlockPos> reachable, List<Demand> request) {
            super(pos, reachable, false);
            this.request = new ArrayList<>(request);
        }
    }
}
