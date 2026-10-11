// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.capability;

import com.hbm.api.fluidmk2.FlushIndex;
import com.hbm.blocks.CapabilityDependentShape;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.uninos.graph.EndpointRegistry;
import com.hbm.uninos.graph.LevelNodeGraph;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jspecify.annotations.Nullable;

public final class CapabilityInvalidation {
    private static final Direction[] SHAPE_ORDER = {
        Direction.WEST,
        Direction.EAST,
        Direction.NORTH,
        Direction.SOUTH,
        Direction.DOWN,
        Direction.UP
    };

    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) level.hbm$redrawCapabilityShapes();
    }

    public static boolean hasConsumers(
            @Nullable Long2IntOpenHashMap graphChunks, @Nullable EndpointRegistry endpoints) {
        return graphChunks != null && !graphChunks.isEmpty()
                || endpoints != null && endpoints.hasCapabilityShapes();
    }

    private static boolean hasGraphBeside(@Nullable Long2IntOpenHashMap graphChunks, long posKey) {
        if (graphChunks == null || graphChunks.isEmpty()) return false;
        int x = BlockPos.getX(posKey);
        int z = BlockPos.getZ(posKey);
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        return graphChunks.containsKey(ChunkPos.pack(chunkX, chunkZ))
                || (x & 15) == 0 && graphChunks.containsKey(ChunkPos.pack(chunkX - 1, chunkZ))
                || (x & 15) == 15 && graphChunks.containsKey(ChunkPos.pack(chunkX + 1, chunkZ))
                || (z & 15) == 0 && graphChunks.containsKey(ChunkPos.pack(chunkX, chunkZ - 1))
                || (z & 15) == 15 && graphChunks.containsKey(ChunkPos.pack(chunkX, chunkZ + 1));
    }

    public static int invalidatePosition(
            ServerLevel level,
            LevelNodeGraph<?>[] graphs,
            @Nullable Long2IntOpenHashMap graphChunks,
            @Nullable EndpointRegistry endpoints,
            long posKey) {
        int neighbours = 0;
        if (hasGraphBeside(graphChunks, posKey)) {
            for (LevelNodeGraph<?> graph : graphs) {
                if (graph == null || !graph.hasCellsBeside(posKey)) continue;
                if (graph.containsCell(posKey)) graph.invalidateNode(posKey);
                else neighbours |= graph.invalidateAround(posKey);
            }
        }
        if (endpoints != null) neighbours |= endpoints.capabilityNeighbours(posKey);
        FlushIndex.invalidateAt(level, posKey);
        return neighbours;
    }

    public static void invalidateChunk(
            LevelNodeGraph<?>[] graphs,
            @Nullable Long2IntOpenHashMap graphChunks,
            @Nullable EndpointRegistry endpoints,
            long chunkKey,
            Long2IntOpenHashMap shapes) {
        int chunkX = ChunkPos.getX(chunkKey);
        int chunkZ = ChunkPos.getZ(chunkKey);
        for (int bucket = 0; bucket < 5; bucket++) {
            long neighbourChunk =
                    ChunkPos.pack(
                            chunkX + (bucket == 1 ? -1 : bucket == 2 ? 1 : 0),
                            chunkZ + (bucket == 3 ? -1 : bucket == 4 ? 1 : 0));
            if (graphChunks != null && graphChunks.containsKey(neighbourChunk)) {
                for (LevelNodeGraph<?> graph : graphs) {
                    if (graph == null) continue;
                    LongOpenHashSet cells = graph.cellsInChunk(neighbourChunk);
                    if (cells == null) continue;
                    for (LongIterator iterator = cells.iterator(); iterator.hasNext(); ) {
                        long cellKey = iterator.nextLong();
                        for (Direction face : Direction.VALUES) {
                            long target = LevelNodeGraph.offset(cellKey, face);
                            if (LevelNodeGraph.chunkOf(target) != chunkKey
                                    || graph.containsCell(target)) continue;
                            if (graph.invalidateFace(cellKey, face.ordinal())) {
                                queue(shapes, target, 1 << face.getOpposite().ordinal());
                            }
                        }
                    }
                }
            }
            LongOpenHashSet cells =
                    endpoints == null || !endpoints.hasCapabilityShapes()
                            ? null
                            : endpoints.cellsInChunk(neighbourChunk);
            if (cells == null) continue;
            for (LongIterator iterator = cells.iterator(); iterator.hasNext(); ) {
                long cellKey = iterator.nextLong();
                if (!endpoints.hasCapabilityShape(cellKey)) continue;
                for (Direction face : Direction.VALUES) {
                    long target = LevelNodeGraph.offset(cellKey, face);
                    if (LevelNodeGraph.chunkOf(target) == chunkKey) {
                        queue(shapes, target, 1 << face.getOpposite().ordinal());
                    }
                }
            }
        }
    }

    public static void queue(Long2IntOpenHashMap shapes, long posKey, int neighbours) {
        shapes.put(posKey, shapes.get(posKey) | neighbours);
    }

    public static void redrawShapes(ServerLevel level, Long2IntOpenHashMap shapes) {
        BlockPos.MutableBlockPos neighbour = new BlockPos.MutableBlockPos();
        for (var iterator = shapes.long2IntEntrySet().fastIterator(); iterator.hasNext(); ) {
            var entry = iterator.next();
            BlockPos at = BlockPos.of(entry.getLongKey());
            LevelChunk source = BlockMultiblockCore.readableChunk(level, at.getX(), at.getZ());
            if (source == null) continue;
            BlockState state = source.getBlockState(at);
            int mask = entry.getIntValue();
            for (Direction direction : SHAPE_ORDER) {
                if ((mask & (1 << direction.ordinal())) == 0) continue;
                neighbour.setWithOffset(at, direction);
                LevelChunk chunk =
                        BlockMultiblockCore.readableChunk(
                                level, neighbour.getX(), neighbour.getZ(), source);
                if (chunk == null) continue;
                Block block = chunk.getBlockState(neighbour).getBlock();
                if (block instanceof CapabilityDependentShape) {
                    level.neighborShapeChanged(
                            direction.getOpposite(),
                            neighbour,
                            at,
                            state,
                            Block.UPDATE_CLIENTS,
                            512);
                }
            }
        }
        shapes.clear();
    }
}
