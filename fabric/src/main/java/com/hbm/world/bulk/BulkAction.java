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

import com.hbm.blockentity.ChunkPublication;
import com.hbm.blockentity.StructureAssemblyBlockEntity;
import com.hbm.blockentity.network.PylonBaseBlockEntity;
import com.hbm.blocks.multiblock.AssembledMembers;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.blocks.multiblock.MultiblockCoreIndex;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.platform.Caps;
import com.hbm.uninos.graph.EndpointRegistry;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.hbm.uninos.graph.PhysicalGraph;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.IdentityHashMap;
import java.util.function.LongPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.TagValueInput;
import org.jspecify.annotations.Nullable;

public final class BulkAction implements AutoCloseable {
    private static final ThreadLocal<BulkAction> CURRENT = new ThreadLocal<>();
    private static final ThreadLocal<LevelChanges> PUBLISHING = new ThreadLocal<>();
    private final @Nullable BulkAction parent;
    private final IdentityHashMap<ServerLevel, LevelChanges> changesByLevel;
    private final boolean suppressDrops;
    private final BulkSession session;

    private BulkAction(boolean suppressDrops, @Nullable BulkSession session) {
        parent = CURRENT.get();
        if (parent == null) {
            this.suppressDrops = suppressDrops;
            this.session = session == null ? new BulkSession() : session;
            changesByLevel = new IdentityHashMap<>();
        } else {
            this.suppressDrops = parent.suppressDrops;
            this.session = parent.session;
            changesByLevel = parent.changesByLevel;
        }
        CURRENT.set(this);
    }

    public static BulkAction begin() {
        return new BulkAction(true, null);
    }

    public static BulkAction begin(boolean suppressDrops) {
        return new BulkAction(suppressDrops, null);
    }

    public static BulkAction begin(BulkSession session) {
        return new BulkAction(true, session);
    }

    public static boolean suppressesDrops() {
        BulkAction action = CURRENT.get();
        return action != null && action.suppressDrops;
    }

    public static boolean active() {
        return CURRENT.get() != null;
    }

    public static @Nullable BulkHistory history(ResourceKey<Level> dimension) {
        BulkAction action = CURRENT.get();
        if (action != null) {
            for (LevelChanges changes : action.changesByLevel.values()) {
                if (changes.level.dimension().equals(dimension)) {
                    return changes.history();
                }
            }
        }
        return null;
    }

    public static boolean touched(ServerLevel level, long pos) {
        BulkAction action = CURRENT.get();
        LevelChanges changes = action == null ? null : action.changesByLevel.get(level);
        return changes != null && changes.originalSnapshots.containsKey(pos);
    }

    public static boolean owns(BlockState state) {
        return state.getBlock().hbm$bulkManaged();
    }

    public static boolean relevant(BlockState state) {
        int kind = BulkRegistry.kind(state.getBlock());
        return owns(state) && BulkDispatch.relevant(kind, state);
    }

    public static void record(Level world, BlockPos pos, BlockState old, BlockState next) {
        stage(world, pos, old, next);
        BulkAction action = CURRENT.get();
        if (action != null && world instanceof ServerLevel level && touched(level, pos.asLong()))
            action.session.recordWrite(level, pos.asLong());
    }

    private static void stage(Level world, BlockPos pos, BlockState old, BlockState next) {
        BulkAction action = CURRENT.get();
        if (action == null
                || !(world instanceof ServerLevel level)
                || (!relevant(old) && !relevant(next))) return;
        if (!level.getServer().isSameThread())
            throw new IllegalStateException("Bulk edits must run on the level thread");
        LevelChanges changes =
                action.changesByLevel.computeIfAbsent(
                        level, l -> new LevelChanges(l, action.session));
        long key = pos.asLong();
        if (changes.originalSnapshots.containsKey(key)) return;
        changes.originalSnapshots.put(
                key, new BulkHistory.Snapshot(old, null, MultiblockCoreIndex.NO_ENTRY, -1));
        var chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        long claim =
                chunk == null
                        ? MultiblockCoreIndex.NO_ENTRY
                        : MultiblockCoreIndex.lookup(
                                chunk, level, pos.getX(), pos.getY(), pos.getZ());
        int ownerId = -1;
        if (claim != MultiblockCoreIndex.NO_ENTRY) {
            BlockPos owner =
                    pos.offset(
                            MultiblockCoreIndex.dxOf(claim),
                            MultiblockCoreIndex.dyOf(claim),
                            MultiblockCoreIndex.dzOf(claim));
            var ownerChunk =
                    level.getChunkSource().getChunkNow(owner.getX() >> 4, owner.getZ() >> 4);
            if (ownerChunk != null)
                ownerId = BuiltInRegistries.BLOCK.getId(ownerChunk.getBlockState(owner).getBlock());
        }
        CompoundTag tag = BulkSnapshot.capture(level, pos);
        changes.originalSnapshots.put(key, new BulkHistory.Snapshot(old, tag, claim, ownerId));
        if (tag != null)
            tag.getCompoundOrEmpty(BulkSnapshot.KEY)
                    .getLongArray("cells")
                    .ifPresent(
                            cells -> {
                                for (long vector : cells) {
                                    BlockPos cell =
                                            pos.offset(
                                                    MultiblockCoreIndex.dxOf(vector),
                                                    MultiblockCoreIndex.dyOf(vector),
                                                    MultiblockCoreIndex.dzOf(vector));
                                    var loaded =
                                            level.getChunkSource()
                                                    .getChunkNow(
                                                            cell.getX() >> 4, cell.getZ() >> 4);
                                    if (loaded == null) continue;
                                    BlockState state = loaded.getBlockState(cell);
                                    stage(level, cell, state, state);
                                }
                            });
    }

    public static void imported(ServerLevel level, BlockPos pos, CompoundTag tag) {
        BulkAction action = CURRENT.get();
        if (action == null) throw new IllegalStateException("Block data outside a bulk edit");
        BlockState state = level.getBlockState(pos);
        stage(level, pos, state, state);
        LevelChanges changes =
                action.changesByLevel.computeIfAbsent(
                        level, l -> new LevelChanges(l, action.session));
        changes.importedSnapshots.put(pos.asLong(), tag.copy());
        changes.blockEntityData.add(pos.asLong());
        action.session.recordWrite(level, pos.asLong());
        tag.getCompoundOrEmpty(BulkSnapshot.KEY)
                .getLongArray("cells")
                .ifPresent(
                        cells -> {
                            action.session.register(level, pos, cells);
                            for (long vector : cells) {
                                BlockPos cell =
                                        pos.offset(
                                                MultiblockCoreIndex.dxOf(vector),
                                                MultiblockCoreIndex.dyOf(vector),
                                                MultiblockCoreIndex.dzOf(vector));
                                if (!action.session.wasWritten(level, cell.asLong())) continue;
                                var chunk =
                                        level.getChunkSource()
                                                .getChunkNow(cell.getX() >> 4, cell.getZ() >> 4);
                                if (chunk != null) {
                                    BlockState current = chunk.getBlockState(cell);
                                    stage(level, cell, current, current);
                                }
                            }
                        });
    }

    public static boolean deferPublication(ServerLevel level, BlockEntity be) {
        if (!active() || !owns(be.getBlockState())) return false;
        stage(level, be.getBlockPos(), be.getBlockState(), be.getBlockState());
        return true;
    }

    public static boolean deferNotification(ServerLevel level, BlockPos pos) {
        if (!active()) return false;
        BlockState state = level.getBlockState(pos);
        stage(level, pos, state, state);
        return true;
    }

    public static void claimed(ServerLevel level, BlockPos owner, BlockPos cell, int population) {
        BlockState state = level.getBlockState(cell);
        stage(level, cell, state, state);
        CURRENT.get().session.recordWrite(level, cell.asLong());
        CURRENT.get().session.claim(level, owner, cell, population);
    }

    public static void linkTargets(ServerLevel level, LongPredicate copied) {
        BulkAction action = CURRENT.get();
        action.changesByLevel.computeIfAbsent(level, l -> new LevelChanges(l, action.session))
                        .linkTargets =
                copied;
    }

    public static void typed(ServerLevel level, BlockPos pos, Fluid fluid) {
        BlockState state = level.getBlockState(pos);
        stage(level, pos, state, state);
        LevelChanges changes = CURRENT.get().changesByLevel.get(level);
        CompoundTag tag = changes.importedSnapshots.get(pos.asLong());
        if (tag == null) tag = new CompoundTag();
        CompoundTag data = tag.getCompoundOrEmpty(BulkSnapshot.KEY);
        data.putString("fluid", BuiltInRegistries.FLUID.getKey(fluid).toString());
        tag.put(BulkSnapshot.KEY, data);
        changes.importedSnapshots.put(pos.asLong(), tag);
    }

    public static boolean deferGraph(@Nullable ServerLevel level, long pos) {
        if (!active() || level == null) return false;
        BlockPos at = BlockPos.of(pos);
        BlockState state = level.getBlockState(at);
        stage(level, at, state, state);
        return touched(level, pos);
    }

    public static boolean hasExplicitOwnership(ServerLevel level, BlockPos core) {
        LevelChanges changes = PUBLISHING.get();
        if (changes == null || changes.level != level) return false;
        CompoundTag tag = changes.importedSnapshots.get(core.asLong());
        return tag != null && tag.getCompoundOrEmpty(BulkSnapshot.KEY).contains("cells");
    }

    static void wireChanged(ServerLevel level, long first, long second) {
        LevelChanges changes = PUBLISHING.get();
        assert changes != null && changes.level == level;
        changes.changedWireHosts.add(first);
        changes.changedWireHosts.add(second);
    }

    @Override
    public void close() {
        if (CURRENT.get() != this)
            throw new IllegalStateException("Bulk edit scopes closed out of order");
        if (parent == null) {
            CURRENT.remove();
            for (LevelChanges changes : changesByLevel.values()) {
                changes.publish();
            }
        } else {
            CURRENT.set(parent);
        }
    }

    private static final class LevelChanges {
        private final ServerLevel level;
        private final Long2ObjectLinkedOpenHashMap<BulkHistory.Snapshot> originalSnapshots =
                new Long2ObjectLinkedOpenHashMap<>();
        private final Long2ObjectOpenHashMap<CompoundTag> importedSnapshots =
                new Long2ObjectOpenHashMap<>();
        private final LongOpenHashSet blockEntityData = new LongOpenHashSet();
        private final BulkSession session;
        private final LongOpenHashSet changedWireHosts = new LongOpenHashSet();
        private final Long2ObjectOpenHashMap<ClaimGroup[]> ownershipGroups =
                new Long2ObjectOpenHashMap<>();
        @Nullable LongPredicate linkTargets;

        private LevelChanges(ServerLevel level, BulkSession session) {
            this.level = level;
            this.session = session;
        }

        private BulkHistory history() {
            var previous = new Long2ObjectLinkedOpenHashMap<>(originalSnapshots);
            var after = new Long2ObjectLinkedOpenHashMap<BulkHistory.Snapshot>();
            for (var entry : originalSnapshots.long2ObjectEntrySet()) {
                long key = entry.getLongKey();
                BlockPos pos = BlockPos.of(key);
                CompoundTag tag =
                        blockEntityData.contains(key)
                                ? importedSnapshots.get(key)
                                : BulkSnapshot.capture(level, pos);
                if (tag == null) tag = new CompoundTag();
                else tag = tag.copy();
                CompoundTag extra = importedSnapshots.get(key);
                if (extra != null && extra.contains(BulkSnapshot.KEY))
                    tag.put(BulkSnapshot.KEY, extra.getCompoundOrEmpty(BulkSnapshot.KEY).copy());
                long[] owned = session.cells(level, pos);
                if (owned.length != 0) {
                    CompoundTag data = tag.getCompoundOrEmpty(BulkSnapshot.KEY);
                    data.putLongArray("cells", owned);
                    tag.put(BulkSnapshot.KEY, data);
                }
                long claim = session.at(level, key);
                int ownerId = -1;
                if (claim != MultiblockCoreIndex.NO_ENTRY) {
                    BlockPos owner =
                            pos.offset(
                                    MultiblockCoreIndex.dxOf(claim),
                                    MultiblockCoreIndex.dyOf(claim),
                                    MultiblockCoreIndex.dzOf(claim));
                    ownerId = session.ownerId(level, owner.asLong());
                } else if (entry.getValue().state().is(level.getBlockState(pos).getBlock())) {
                    claim = entry.getValue().claim();
                    ownerId = entry.getValue().ownerId();
                }
                after.put(
                        key,
                        new BulkHistory.Snapshot(
                                level.getBlockState(pos),
                                tag.isEmpty() ? null : tag,
                                claim,
                                ownerId));
            }
            return new BulkHistory(previous, after);
        }

        private void publish() {
            LevelChanges previous = PUBLISHING.get();
            PUBLISHING.set(this);
            try {
                publishNow();
            } finally {
                if (previous == null) {
                    PUBLISHING.remove();
                } else {
                    PUBLISHING.set(previous);
                }
            }
        }

        private void publishNow() {
            try (var physical = PhysicalGraph.begin(level);
                    var removals = MultiblockCoreIndex.removalBatch()) {
                retireOriginalState();
                loadBlockEntities();
                publishBlocks();
                collectOwnership();
                publishOwnership();
                restoreLinksAndRefresh();
            }
        }

        private void retireOriginalState() {
            for (long key : originalSnapshots.keySet()) {
                BlockPos pos = BlockPos.of(key);
                for (LevelNodeGraph<?> graph :
                        ((com.hbm.interfaces.injected.ServerLevelExtension) (level)).hbm$graphs())
                    if (graph != null) {
                        var node = graph.getNode(key);
                        if (node != null && node.hasRemoteLinks()) {
                            changedWireHosts.add(key);
                            changedWireHosts.addAll(node.remoteLinks);
                        }
                        graph.removeNode(key);
                    }
                MultiblockCoreBlock.unindexCell(level, pos);
                EndpointRegistry.of(level).withdraw(pos);
            }
        }

        private void loadBlockEntities() {
            for (var entry : importedSnapshots.long2ObjectEntrySet()) {
                if (!blockEntityData.contains(entry.getLongKey())) continue;
                BlockPos pos = BlockPos.of(entry.getLongKey());
                BlockEntity be = level.getBlockEntity(pos);
                if (be != null) {
                    CompoundTag tag = BulkSnapshot.forPlacement(entry.getValue(), pos);
                    be.loadWithComponents(
                            TagValueInput.create(
                                    ProblemReporter.DISCARDING, level.registryAccess(), tag));
                    be.setChanged();
                }
            }
        }

        private void publishBlocks() {
            for (long key : originalSnapshots.keySet()) {
                BlockPos pos = BlockPos.of(key);
                BlockState state = level.getBlockState(pos);
                StructureAssemblyBlockEntity.blockChanged(level, pos, state);
                if (!owns(state)) continue;
                MultiblockCoreBlock.withoutTeardown(
                        () -> state.onPlace(level, pos, Blocks.AIR.defaultBlockState(), false));
                BlockEntity be = level.getBlockEntity(pos);
                if (be != null) ChunkPublication.added(level, be);
                if (be != null) BulkDispatch.entityPublished(BulkRegistry.kind(be), be);
                var tag = dataAt(key);
                var data =
                        tag == null ? new CompoundTag() : tag.getCompoundOrEmpty(BulkSnapshot.KEY);
                BulkDispatch.blockPublished(
                        BulkRegistry.kind(state.getBlock()), level, pos, state, data);
            }
        }

        private void collectOwnership() {
            for (long key : originalSnapshots.keySet()) {
                BlockPos pos = BlockPos.of(key);
                long claim =
                        session.wasWritten(level, key)
                                ? session.at(level, key)
                                : MultiblockCoreIndex.NO_ENTRY;
                if (claim != MultiblockCoreIndex.NO_ENTRY) {
                    BlockPos owner =
                            pos.offset(
                                    MultiblockCoreIndex.dxOf(claim),
                                    MultiblockCoreIndex.dyOf(claim),
                                    MultiblockCoreIndex.dzOf(claim));
                    claim(pos, claim, session.ownerId(level, owner.asLong()), true);
                } else {
                    BulkHistory.Snapshot old = originalSnapshots.get(key);
                    if (old.claim() != MultiblockCoreIndex.NO_ENTRY
                            && old.state().is(level.getBlockState(pos).getBlock()))
                        claim(pos, old.claim(), old.ownerId(), false);
                }
            }
        }

        private void publishOwnership() {
            for (ClaimGroup[] groups : ownershipGroups.values()) {
                for (int population = 0; population < groups.length; population++) {
                    ClaimGroup group = groups[population];
                    if (group != null) {
                        if (population == MultiblockCoreIndex.MEMBER) {
                            AssembledMembers.claimBulk(level, group.owner, group.cells);
                        } else {
                            MultiblockCoreBlock.claimBulkCells(
                                    level, group.owner, group.cells, population);
                        }
                    }
                }
            }
        }

        private void restoreLinksAndRefresh() {
            LongPredicate selectedPositions =
                    linkTargets == null ? peer -> session.wasWritten(level, peer) : linkTargets;
            LongPredicate allPositions = ignored -> true;
            for (long key : originalSnapshots.keySet()) {
                BlockPos pos = BlockPos.of(key);
                CompoundTag tag = dataAt(key);
                if (tag != null)
                    BulkSnapshot.restoreLinks(
                            level,
                            pos,
                            tag.getCompoundOrEmpty(BulkSnapshot.KEY),
                            importedSnapshots.containsKey(key) ? selectedPositions : allPositions);
                changedWireHosts.add(key);
                Caps.invalidateCaps(level, pos);
                LevelNodeGraph.invalidateTransportAround(level, pos);
                PhysicalGraph.refreshAround(level, pos);
            }
            for (long host : changedWireHosts)
                PylonBaseBlockEntity.refreshLoadedEndpoint(level, host);
        }

        private @Nullable CompoundTag dataAt(long key) {
            CompoundTag copied = importedSnapshots.get(key);
            if (copied != null) return copied;
            BulkHistory.Snapshot old = originalSnapshots.get(key);
            return old.state().is(level.getBlockState(BlockPos.of(key)).getBlock())
                    ? old.tag()
                    : null;
        }

        private void claim(BlockPos pos, long claim, int ownerId, boolean explicit) {
            BlockState state = level.getBlockState(pos);
            if (!MultiblockSurface.isSurface(state)
                    && !BulkDispatch.cell(BulkRegistry.kind(state.getBlock()), state)) return;
            BlockPos owner =
                    pos.offset(
                            MultiblockCoreIndex.dxOf(claim),
                            MultiblockCoreIndex.dyOf(claim),
                            MultiblockCoreIndex.dzOf(claim));
            var chunk = level.getChunkSource().getChunkNow(owner.getX() >> 4, owner.getZ() >> 4);
            if (chunk == null) return;
            int population = MultiblockCoreIndex.populationOf(claim);
            BlockState ownerState = chunk.getBlockState(owner);
            if (BuiltInRegistries.BLOCK.getId(ownerState.getBlock()) != ownerId) return;
            if (population == MultiblockCoreIndex.FOLDED) {
                MultiblockCoreBlock core = MultiblockSurface.foldedCore(ownerState);
                if (core == null) return;
                if (!core.claimsCell(
                        level, owner, pos, ownerState.getValue(MultiblockCoreBlock.FACING))) {
                    if (explicit)
                        throw new IllegalArgumentException(
                                "Transformed cell does not fit its machine: "
                                        + pos
                                        + " -> "
                                        + owner);
                    return;
                }
            }
            if (population > MultiblockCoreIndex.MEMBER) return;
            ClaimGroup[] groups =
                    ownershipGroups.computeIfAbsent(
                            owner.asLong(),
                            ignored -> new ClaimGroup[MultiblockCoreIndex.MEMBER + 1]);
            ClaimGroup group = groups[population];
            if (group == null) groups[population] = group = new ClaimGroup(owner);
            group.cells.add(pos.asLong());
        }

        private static final class ClaimGroup {
            private final BlockPos owner;
            private final LongArrayList cells = new LongArrayList();

            private ClaimGroup(BlockPos owner) {
                this.owner = owner;
            }
        }
    }
}
