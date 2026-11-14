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

package com.hbm.world.bulk;

import com.hbm.blocks.multiblock.MultiblockCoreIndex;
import com.hbm.uninos.graph.GraphNode;
import com.hbm.uninos.graph.LevelNodeGraph;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.LongPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public final class BulkSnapshot {
    public static final String KEY = "hbm_bulk";
    private static final ThreadLocal<Long2ObjectMap<CompoundTag>> CLIENT = new ThreadLocal<>();

    private BulkSnapshot() {}

    public static @Nullable CompoundTag capture(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) {
            var snapshot = CLIENT.get();
            CompoundTag tag = snapshot == null ? null : snapshot.get(pos.asLong());
            return tag == null ? null : tag.copy();
        }
        var state = level.getBlockState(pos);
        if (!BulkAction.relevant(state) && !state.hasBlockEntity()) return null;
        int kind = BulkRegistry.kind(state.getBlock());
        if (BulkDispatch.cell(kind, state)) return null;
        var be = level.getBlockEntity(pos);
        var tag = be == null ? new CompoundTag() : be.saveWithFullMetadata(level.registryAccess());
        var data = new CompoundTag();
        var cells = new LongArrayList();
        Consumer<BlockPos> capture = cell -> captureCell(server, pos, cell, cells);
        boolean ownership = BulkDispatch.capture(kind, server, pos, state, data, capture);
        if (be != null)
            ownership |= BulkDispatch.entityCells(BulkRegistry.kind(be), be, server, capture);
        if (ownership) data.putLongArray("cells", cells.toLongArray());
        var links = new CompoundTag();
        LevelNodeGraph.visitKinds(
                graphKind ->
                        captureLinks(
                                graphKind.loaded(server),
                                pos,
                                links,
                                graphKind.type.id().toString()));
        if (!links.isEmpty()) data.put("links", links);
        if (be == null && data.isEmpty()) return null;
        data.putString("block", BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
        data.putBoolean("entity", be != null);
        tag.put(KEY, data);
        if (be == null) tag.putString("id", "hbm:bulk");
        BulkDispatch.relocate(kind, tag, point -> point.subtract(pos));
        return tag;
    }

    private static void captureCell(
            ServerLevel level, BlockPos owner, BlockPos cell, LongArrayList cells) {
        var chunk = level.getChunkSource().getChunkNow(cell.getX() >> 4, cell.getZ() >> 4);
        if (chunk == null) return;
        long entry =
                MultiblockCoreIndex.lookup(chunk, level, cell.getX(), cell.getY(), cell.getZ());
        if (entry == MultiblockCoreIndex.NO_ENTRY
                || MultiblockCoreIndex.corePacked(entry, cell.getX(), cell.getY(), cell.getZ())
                        != owner.asLong()) return;
        cells.add(
                MultiblockCoreIndex.pack(
                        0,
                        cell.getX() - owner.getX(),
                        cell.getY() - owner.getY(),
                        cell.getZ() - owner.getZ(),
                        MultiblockCoreIndex.populationOf(entry)));
    }

    public static boolean withClientSnapshot(
            Long2ObjectMap<CompoundTag> data, BooleanSupplier body) {
        var previous = CLIENT.get();
        CLIENT.set(data);
        try {
            return body.getAsBoolean();
        } finally {
            CLIENT.set(previous);
        }
    }

    private static void captureLinks(
            @Nullable LevelNodeGraph<?> graph, BlockPos pos, CompoundTag data, String key) {
        if (graph == null) return;
        GraphNode<?> node = graph.getNode(pos.asLong());
        if (node == null || !node.hasRemoteLinks()) return;
        int[] offsets = new int[node.remoteLinks.size() * 3];
        int i = 0;
        for (long peer : node.remoteLinks) {
            BlockPos offset = BlockPos.of(peer).subtract(pos);
            offsets[i++] = offset.getX();
            offsets[i++] = offset.getY();
            offsets[i++] = offset.getZ();
        }
        data.putIntArray(key, offsets);
    }

    public static CompoundTag transform(CompoundTag original, BulkTransform transform) {
        if (!original.contains(KEY)) return original;
        CompoundTag tag = original.copy();
        CompoundTag data = tag.getCompoundOrEmpty(KEY);
        if (data.contains("cells")
                && !transform.apply(new BlockPos(0, 1, 0)).equals(new BlockPos(0, 1, 0)))
            throw new IllegalArgumentException(
                    "NTM multiblocks support horizontal transforms only");
        data.getLongArray("cells")
                .ifPresent(
                        cells -> {
                            long[] transformed = cells.clone();
                            for (int i = 0; i < cells.length; i++) {
                                BlockPos offset =
                                        transform.apply(
                                                new BlockPos(
                                                        MultiblockCoreIndex.dxOf(cells[i]),
                                                        MultiblockCoreIndex.dyOf(cells[i]),
                                                        MultiblockCoreIndex.dzOf(cells[i])));
                                transformed[i] =
                                        MultiblockCoreIndex.pack(
                                                0,
                                                offset.getX(),
                                                offset.getY(),
                                                offset.getZ(),
                                                MultiblockCoreIndex.populationOf(cells[i]));
                            }
                            data.putLongArray("cells", transformed);
                        });
        var links = data.getCompoundOrEmpty("links");
        for (var key : List.copyOf(links.keySet())) transformLinks(links, key, transform);
        if (data.contains("block")) {
            BulkDispatch.transform(schemaKind(data), tag, transform);
        }
        return tag;
    }

    private static void transformLinks(CompoundTag data, String key, BulkTransform transform) {
        data.getIntArray(key)
                .ifPresent(
                        offsets -> {
                            int[] transformed = offsets.clone();
                            for (int i = 0; i < transformed.length; i += 3) {
                                BlockPos offset =
                                        transform.apply(
                                                new BlockPos(
                                                        offsets[i],
                                                        offsets[i + 1],
                                                        offsets[i + 2]));
                                transformed[i] = offset.getX();
                                transformed[i + 1] = offset.getY();
                                transformed[i + 2] = offset.getZ();
                            }
                            data.putIntArray(key, transformed);
                        });
    }

    public static CompoundTag forPlacement(CompoundTag original, BlockPos pos) {
        var tag = original.copy();
        relocate(tag, original, offset -> offset.offset(pos));
        tag.remove(KEY);
        tag.putInt("x", pos.getX());
        tag.putInt("y", pos.getY());
        tag.putInt("z", pos.getZ());
        return tag;
    }

    public static CompoundTag withoutInventory(
            CompoundTag original, BlockState state, HolderLookup.Provider registries) {
        var data = original.getCompoundOrEmpty(KEY);
        if (!data.getBooleanOr("entity", false)) return original.copy();
        var copy =
                BlockEntity.loadStatic(
                        BlockPos.ZERO, state, forPlacement(original, BlockPos.ZERO), registries);
        BulkDispatch.clearInventory(BulkRegistry.kind(copy), copy);
        var tag = copy.saveWithFullMetadata(registries);
        tag.put(KEY, data.copy());
        return tag;
    }

    public static CompoundTag publicData(CompoundTag original) {
        var data = original.getCompoundOrEmpty(KEY);
        var tag = BulkDispatch.publicData(schemaKind(data), original);
        tag.put(KEY, data.copy());
        if (original.contains("id")) tag.put("id", original.get("id").copy());
        return tag;
    }

    public static void relativeData(CompoundTag tag, BlockPos pos, CompoundTag snapshot) {
        relocate(tag, snapshot, point -> point.subtract(pos));
        if (snapshot.contains(KEY)) tag.put(KEY, snapshot.getCompoundOrEmpty(KEY).copy());
    }

    private static void relocate(CompoundTag tag, CompoundTag snapshot, BulkTransform transform) {
        var data = snapshot.getCompoundOrEmpty(KEY);
        if (data.contains("block")) BulkDispatch.relocate(schemaKind(data), tag, transform);
    }

    private static int schemaKind(CompoundTag data) {
        var id = Identifier.parse(data.getString("block").orElseThrow());
        return BulkRegistry.kind(BuiltInRegistries.BLOCK.getOptional(id).orElseThrow());
    }

    static void restoreLinks(
            ServerLevel level, BlockPos pos, CompoundTag data, LongPredicate copied) {
        var links = data.getCompoundOrEmpty("links");
        LevelNodeGraph.visitKinds(
                kind ->
                        restoreLinks(
                                level,
                                kind.loaded(level),
                                pos,
                                links,
                                kind.type.id().toString(),
                                copied));
    }

    private static void restoreLinks(
            ServerLevel level,
            @Nullable LevelNodeGraph<?> graph,
            BlockPos pos,
            CompoundTag data,
            String key,
            LongPredicate copied) {
        if (graph == null || !graph.containsCell(pos.asLong())) return;
        data.getIntArray(key)
                .ifPresent(
                        offsets -> {
                            for (int i = 0; i < offsets.length; i += 3) {
                                long peer =
                                        pos.offset(offsets[i], offsets[i + 1], offsets[i + 2])
                                                .asLong();
                                if (copied.test(peer) && graph.containsCell(peer)) {
                                    graph.addRemoteLink(pos.asLong(), peer);
                                    BulkAction.wireChanged(level, pos.asLong(), peer);
                                }
                            }
                        });
    }
}
