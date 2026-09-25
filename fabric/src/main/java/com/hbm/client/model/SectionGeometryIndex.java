// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.Arrays;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.state.level.ChunkLoadingRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

public final class SectionGeometryIndex {
    private final Long2IntOpenHashMap owners = new Long2IntOpenHashMap();
    private final Long2ObjectOpenHashMap<long[]> sections = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<IntArrayList> chunkOwners = new Long2ObjectOpenHashMap<>();
    private final LongOpenHashSet loadedChunks = new LongOpenHashSet();
    private final LongOpenHashSet changedSections = new LongOpenHashSet();
    private final IntArrayList free = new IntArrayList();
    private final IntOpenHashSet appearanceChanges = new IntOpenHashSet();
    private final LongOpenHashSet appearanceChunks = new LongOpenHashSet();
    private final LongOpenHashSet appearanceNeighbors = new LongOpenHashSet();
    private final LongOpenHashSet lightColumns = new LongOpenHashSet();
    private long[] positions = new long[16];
    private BlockState[] states = new BlockState[16];
    private SectionedModel[] models = new SectionedModel[16];
    private SectionGeometryLayout[] layouts = new SectionGeometryLayout[16];
    private SectionGeometry.Prepared[] prepared = new SectionGeometry.Prepared[16];
    private int[][] contributionSlots = new int[16][];
    private int[] chunkSlots = new int[16];
    private int used;
    private int contextualOwners;

    public SectionGeometryIndex() {
        owners.defaultReturnValue(-1);
    }

    private static void resetEmptySection(ClientLevel level, long section) {
        int x = SectionPos.x(section), y = SectionPos.y(section), z = SectionPos.z(section);
        var chunk = level.getChunkSource().getChunk(x, z, false);
        if (chunk != null && !chunk.getSection(level.getSectionIndexFromSectionY(y)).hasOnlyAir())
            return;
        var view = Minecraft.getInstance().levelRenderer.viewArea();
        if (view == null) return;
        var renderSection = view.getRenderSectionAt(new BlockPos(x * 16, y * 16, z * 16));
        if (renderSection == null) return;
        var mesh = renderSection.getSectionMesh();

        if (mesh != CompiledSectionMesh.UNCOMPILED && mesh != CompiledSectionMesh.EMPTY)
            renderSection.reset();
    }

    private static long worldSection(BlockPos pos, long local) {
        return SectionPos.asLong(
                (pos.getX() >> 4) + SectionPos.x(local),
                (pos.getY() >> 4) + SectionPos.y(local),
                (pos.getZ() >> 4) + SectionPos.z(local));
    }

    public boolean has(long section) {
        return sections.containsKey(section);
    }

    public void update(ClientLevel level, BlockPos pos, BlockState state) {
        assert Minecraft.getInstance().isSameThread();
        long key = pos.asLong();
        int previous = owners.get(key);
        var model = SectionedModel.forState(state);
        if (previous != -1 && states[previous] == state && models[previous] == model) return;
        if (previous != -1) remove(level, previous);
        if (model == null) return;
        int id = free.isEmpty() ? used++ : free.removeInt(free.size() - 1);
        if (id == positions.length) {
            int capacity = positions.length * 2;
            positions = Arrays.copyOf(positions, capacity);
            states = Arrays.copyOf(states, capacity);
            models = Arrays.copyOf(models, capacity);
            layouts = Arrays.copyOf(layouts, capacity);
            prepared = Arrays.copyOf(prepared, capacity);
            contributionSlots = Arrays.copyOf(contributionSlots, capacity);
            chunkSlots = Arrays.copyOf(chunkSlots, capacity);
        }
        owners.put(key, id);
        positions[id] = key;
        states[id] = state;
        models[id] = model;
        if (model.contextual) contextualOwners++;
        prepared[id] = model.prepare(level, pos, state);
        layouts[id] = model.acquire(pos, prepared[id].mesh());
        var chunkRow =
                chunkOwners.computeIfAbsent(
                        ChunkPos.pack(pos.getX() >> 4, pos.getZ() >> 4), k -> new IntArrayList());
        chunkSlots[id] = chunkRow.size();
        chunkRow.add(id);
        addParts(level, id);
    }

    private void addParts(ClientLevel level, int id) {
        BlockPos pos = BlockPos.of(positions[id]);
        var layout = layouts[id];
        int[] slots = new int[layout.sections.length];
        Arrays.fill(slots, -1);
        contributionSlots[id] = slots;
        for (int part = 0; part < layout.sections.length; part++) {
            long section = worldSection(pos, layout.sections[part]);
            if (SectionPos.y(section) < level.getMinSectionY()
                    || SectionPos.y(section) > level.getMaxSectionY()) continue;
            long[] row = sections.get(section);
            if (row == null) {
                row = new long[6];
                sections.put(section, row);
            }
            int size = (int) row[0];
            if (size + 2 == row.length) {

                if (row[1] * 2 <= size) {
                    compactContributions(row);
                    size = (int) row[0];
                } else {
                    row = Arrays.copyOf(row, row.length * 2);
                    sections.put(section, row);
                }
            }
            slots[part] = size + 2;
            row[size + 2] = (long) id << 32 | part;
            row[0] = size + 1;
            row[1]++;
            dirty(level, section);
        }
    }

    private void removeParts(ClientLevel level, int id) {
        BlockPos pos = BlockPos.of(positions[id]);
        long[] local = layouts[id].sections;
        for (int part = 0; part < local.length; part++) {
            int at = contributionSlots[id][part];
            if (at < 0) continue;
            long section = worldSection(pos, local[part]);
            long[] row = sections.get(section);
            assert row[at] == ((long) id << 32 | part);
            row[at] = -1;
            contributionSlots[id][part] = -1;
            if (--row[1] == 0) {
                sections.remove(section);
                resetEmptySection(level, section);
            }
            dirty(level, section);
        }
    }

    private void compactContributions(long[] row) {
        if (row[0] == row[1]) return;
        int write = 2;
        int end = (int) row[0] + 2;
        for (int read = 2; read < end; read++) {
            long contribution = row[read];
            if (contribution < 0) continue;
            row[write] = contribution;
            contributionSlots[(int) (contribution >>> 32)][(int) contribution] = write++;
        }
        row[0] = write - 2;
        assert row[0] == row[1];
    }

    private void remove(ClientLevel level, int id) {
        BlockPos pos = BlockPos.of(positions[id]);
        removeParts(level, id);
        owners.remove(positions[id]);
        long chunk = ChunkPos.pack(pos.getX() >> 4, pos.getZ() >> 4);
        var row = chunkOwners.get(chunk);
        int at = chunkSlots[id];
        int moved = row.removeInt(row.size() - 1);
        if (moved != id) {
            row.set(at, moved);
            chunkSlots[moved] = at;
        }
        if (row.isEmpty()) chunkOwners.remove(chunk);
        states[id] = null;
        if (models[id].contextual) contextualOwners--;
        models[id] = null;
        SectionedModel.release(layouts[id]);
        layouts[id] = null;
        prepared[id] = null;
        contributionSlots[id] = null;
        appearanceChanges.remove(id);
        free.add(id);
    }

    public void appearance(int x, int z) {
        if (contextualOwners != 0) appearanceChunks.add(ChunkPos.pack(x, z));
    }

    public void refresh(ClientLevel level) {
        assert Minecraft.getInstance().isSameThread();
        for (long center : appearanceChunks) {
            int x = ChunkPos.getX(center), z = ChunkPos.getZ(center);
            for (int cz = z - 1; cz <= z + 1; cz++) {
                for (int cx = x - 1; cx <= x + 1; cx++)
                    appearanceNeighbors.add(ChunkPos.pack(cx, cz));
            }
        }
        appearanceChunks.clear();
        for (long chunk : appearanceNeighbors) {
            var row = chunkOwners.get(chunk);
            if (row == null) continue;
            for (int i = 0; i < row.size(); i++) {
                int id = row.getInt(i);
                if (models[id].contextual) appearanceChanges.add(id);
            }
        }
        appearanceNeighbors.clear();
        for (int id : appearanceChanges) {
            BlockPos pos = BlockPos.of(positions[id]);
            var changed = models[id].prepare(level, pos, states[id]);
            if (changed == prepared[id]) continue;
            var layout = models[id].acquire(pos, changed.mesh());
            if (layout != layouts[id]) {
                removeParts(level, id);
                SectionedModel.release(layouts[id]);
                layouts[id] = layout;
                prepared[id] = changed;
                addParts(level, id);
            } else {
                SectionedModel.release(layout);
                prepared[id] = changed;
                for (long local : layout.sections) dirty(level, worldSection(pos, local));
            }
        }
        appearanceChanges.clear();
    }

    private void dirty(ClientLevel level, long section) {
        changedSections.add(section);
        int x = SectionPos.x(section), y = SectionPos.y(section), z = SectionPos.z(section);
        level.setSectionRangeDirty(x, y, z, x, y, z);
    }

    public void lightChanged(int x, int z) {
        if (contextualOwners != 0) lightColumns.add(ChunkPos.pack(x, z));
    }

    public void lightApplied() {
        if (lightColumns.isEmpty()) return;
        for (long column : lightColumns) appearance(ChunkPos.getX(column), ChunkPos.getZ(column));
        lightColumns.clear();
    }

    public void load(ClientLevel level, LevelChunk chunk) {
        appearance(chunk.getPos().x(), chunk.getPos().z());
        loadedChunks.add(chunk.getPos().pack());
        var previous = chunkOwners.get(chunk.getPos().pack());
        if (previous != null) {
            while (!previous.isEmpty()) remove(level, previous.getInt(previous.size() - 1));
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        var storage = chunk.getSections();
        for (int section = 0; section < storage.length; section++) {
            var cells = storage[section];
            if (!cells.maybeHas(state -> SectionedModel.forState(state) != null)) continue;
            int bottom = chunk.getSectionYFromSectionIndex(section) << 4;
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        BlockState state = cells.getBlockState(x, y, z);
                        if (SectionedModel.forState(state) == null) continue;
                        pos.set(
                                chunk.getPos().getMinBlockX() + x,
                                bottom + y,
                                chunk.getPos().getMinBlockZ() + z);
                        update(level, pos, state);
                    }
                }
            }
        }
    }

    public void unload(ClientLevel level, LevelChunk chunk) {
        appearance(chunk.getPos().x(), chunk.getPos().z());
        loadedChunks.remove(chunk.getPos().pack());
        var row = chunkOwners.get(chunk.getPos().pack());
        if (row != null) {
            while (!row.isEmpty()) remove(level, row.getInt(row.size() - 1));
        }
    }

    public void reload(ClientLevel level) {
        for (long section : sections.keySet()) dirty(level, section);
        long[] chunks = loadedChunks.toLongArray();
        clear();
        for (long key : chunks) {
            var chunk =
                    level.getChunkSource().getChunk(ChunkPos.getX(key), ChunkPos.getZ(key), false);
            if (chunk != null) load(level, chunk);
        }
    }

    public void clear() {
        for (int id : owners.values()) SectionedModel.release(layouts[id]);
        owners.clear();
        sections.clear();
        chunkOwners.clear();
        loadedChunks.clear();
        free.clear();
        appearanceChanges.clear();
        appearanceChunks.clear();
        appearanceNeighbors.clear();
        Arrays.fill(states, null);
        Arrays.fill(models, null);
        Arrays.fill(layouts, null);
        Arrays.fill(prepared, null);
        Arrays.fill(contributionSlots, null);
        used = 0;
        contextualOwners = 0;
    }

    public SectionGeometry.@Nullable Snapshot capture(ClientLevel level, long section) {
        refresh(level);
        long[] row = sections.get(section);
        if (row == null) return null;
        compactContributions(row);
        int count = (int) row[0];
        var parts = new ArrayList<SectionGeometry.Part>(count);
        for (int i = 2; i < count + 2; i++) {
            long contribution = row[i];
            int id = (int) (contribution >>> 32);
            BlockPos pos = BlockPos.of(positions[id]);
            parts.add(
                    new SectionGeometry.Part(
                            models[id],
                            states[id],
                            positions[id],
                            layouts[id],
                            (int) contribution,
                            pos.getX() - (SectionPos.x(section) << 4),
                            pos.getY() - (SectionPos.y(section) << 4),
                            pos.getZ() - (SectionPos.z(section) << 4),
                            prepared[id]));
        }
        return new SectionGeometry.Snapshot(SectionPos.of(section).origin(), parts);
    }

    public void emptySections(ClientLevel level, ChunkLoadingRenderState state) {
        refresh(level);
        var added = state.addedEmptySections.longIterator();
        while (added.hasNext()) {
            long section = added.nextLong();
            if (has(section)) {
                added.remove();
                state.removedEmptySections.add(section);
            }
        }
        for (long section : changedSections) {
            if (has(section)) {
                state.addedEmptySections.remove(section);
                state.removedEmptySections.add(section);
            } else {
                var chunk =
                        level.getChunkSource()
                                .getChunk(SectionPos.x(section), SectionPos.z(section), false);
                int y = level.getSectionIndexFromSectionY(SectionPos.y(section));
                if (chunk != null
                        && y >= 0
                        && y < chunk.getSections().length
                        && chunk.getSection(y).hasOnlyAir()) {
                    state.removedEmptySections.remove(section);
                    state.addedEmptySections.add(section);
                }
            }
        }
        changedSections.clear();
    }
}
