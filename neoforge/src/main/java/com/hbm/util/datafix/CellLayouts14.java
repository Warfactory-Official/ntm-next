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

package com.hbm.util.datafix;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.LongConsumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import org.jspecify.annotations.Nullable;

final class CellLayouts14 {
    private static final String RESOURCE = "hbm/datafix/multiblock_cells_14.nbt";

    private CellLayouts14() {}

    static boolean has(String coreName) {
        return Tables.DATA.layouts().containsKey(coreName);
    }

    static void visit(CompoundTag core, LongConsumer visitor) {
        var layout = Tables.DATA.layouts().get(core.getStringOr("Name", ""));
        var cells =
                layout.facings()
                        .get(core.getCompoundOrEmpty("Properties").getStringOr("facing", ""));
        for (long offset : cells.keySet()) visitor.accept(offset);
    }

    static boolean isMember(String blockName) {
        return Tables.DATA.members().contains(blockName);
    }

    static boolean isPort(String blockName) {
        return Tables.DATA.ports().contains(blockName);
    }

    static @Nullable CompoundTag reconstruct(
            CompoundTag core, int dx, int dy, int dz, long flags, @Nullable CompoundTag be) {
        var layout = Tables.DATA.layouts().get(core.getStringOr("Name", ""));
        if (layout == null) return null;
        var facing =
                layout.facings()
                        .get(core.getCompoundOrEmpty("Properties").getStringOr("facing", ""));
        if (facing == null) return null;
        long offset =
                ((long) dx & 0x3ffffffL) << 38
                        | ((long) dz & 0x3ffffffL) << 12
                        | (long) dy & 0xfffL;
        var cell = facing.get(offset);
        if (cell == null) return null;
        var state = cell.state().copy();
        var properties = state.getCompoundOrEmpty("Properties");
        if (properties.contains("open")) {
            if ((flags & 1L << 59) != 0) {
                properties.putString("open", Boolean.toString((flags & 1L << 60) != 0));
            } else if (be != null) {
                if (layout.door().equals("generic")) {
                    properties.putString("open", Boolean.toString(genericOpen(cell, be)));
                } else if (layout.door().equals("blast")) {
                    properties.putString("open", Boolean.toString(blastOpen(dx, dy, dz, be)));
                }
            }
        }
        if (properties.contains("waterlogged") && (flags & 1L << 61) != 0) {
            properties.putString("waterlogged", Boolean.toString((flags & 1L << 62) != 0));
        }
        return state;
    }

    private static boolean genericOpen(Cell cell, CompoundTag be) {
        var motion = cell.motion();
        if (motion == null) return false;
        int tick = be.getIntOr("openTicks", 0);
        return switch (be.getByteOr("state", (byte) 0)) {
            case 0 -> false;
            case 1 -> true;
            case 2 -> bit(motion.closing(), tick);
            case 3 -> bit(motion.opening(), tick);
            default ->
                    throw new IllegalArgumentException(
                            "door phase " + be.getByteOr("state", (byte) 0));
        };
    }

    private static boolean bit(long[] bits, int tick) {
        return (bits[tick >>> 6] & 1L << (tick & 63)) != 0;
    }

    private static boolean blastOpen(int dx, int dy, int dz, CompoundTag be) {
        if (dx != 0 || dz != 0 || dy < 1 || dy > 5) return false;
        return switch (be.getIntOr("state", 0)) {
            case 0 -> false;
            case 2 -> true;
            case 1 -> {
                int tick = be.getIntOr("timer", 0);
                yield be.getBooleanOr("isOpening", false)
                        ? tick > 0 && tick >= (dy - 1) * 20
                        : tick < (6 - dy) * 20;
            }
            default ->
                    throw new IllegalArgumentException(
                            "blast door phase " + be.getIntOr("state", 0));
        };
    }

    private static Data load() {
        var snapshots = new TreeMap<String, CompoundTag>();
        try {
            var module = CellLayouts14.class.getModule();
            if (module.isNamed()) {
                for (var owner : module.getLayer().modules()) {
                    try (var in = owner.getResourceAsStream(RESOURCE)) {
                        if (in != null)
                            addSnapshot(
                                    snapshots,
                                    NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap()));
                    }
                }
            } else {
                var resources = CellLayouts14.class.getClassLoader().getResources(RESOURCE);
                while (resources.hasMoreElements()) {
                    try (var in = resources.nextElement().openStream()) {
                        addSnapshot(
                                snapshots, NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap()));
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("reading " + RESOURCE, e);
        }
        var base = snapshots.remove("hbm");
        if (base == null)
            throw new IllegalStateException("missing frozen v14 cell layouts at " + RESOURCE);
        var ordered = new ArrayList<CompoundTag>();
        ordered.add(base);
        ordered.addAll(snapshots.values());
        var layouts = new HashMap<String, Layout>();
        var reshaped = new HashMap<String, String>();
        var members = new HashSet<String>();
        var ports = new HashSet<String>();
        var states = new HashMap<CompoundTag, CompoundTag>();
        for (var snapshot : ordered) {
            String namespace = snapshot.getString("namespace").orElseThrow();
            snapshot.getList("members")
                    .orElseThrow()
                    .forEach(tag -> members.add(tag.asString().orElseThrow()));
            snapshot.getList("ports")
                    .orElseThrow()
                    .forEach(tag -> ports.add(tag.asString().orElseThrow()));
            var entries = snapshot.getCompound("layouts").orElseThrow();
            for (String id : entries.keySet()) {
                if (!id.startsWith(namespace + ":")) {
                    String prior = reshaped.putIfAbsent(id, namespace);
                    if (prior != null)
                        throw new IllegalStateException(
                                prior + " and " + namespace + " both reshape " + id);
                } else if (reshaped.containsKey(id)) {
                    continue;
                }
                layouts.put(id, read(entries.getCompound(id).orElseThrow(), states));
            }
        }
        return new Data(Map.copyOf(layouts), Set.copyOf(members), Set.copyOf(ports));
    }

    private static void addSnapshot(Map<String, CompoundTag> snapshots, CompoundTag snapshot) {
        String namespace = snapshot.getString("namespace").orElseThrow();
        if (snapshots.putIfAbsent(namespace, snapshot) != null)
            throw new IllegalStateException("duplicate v14 cell layouts for " + namespace);
    }

    private static Layout read(CompoundTag tag, Map<CompoundTag, CompoundTag> states) {
        var facings = new HashMap<String, Long2ObjectMap<Cell>>();
        var directions = tag.getCompound("facings").orElseThrow();
        for (String direction : directions.keySet()) {
            var cells = new Long2ObjectOpenHashMap<Cell>();
            for (var entry :
                    directions.getList(direction).orElseThrow().compoundStream().toList()) {
                Motion motion =
                        entry.contains("opening")
                                ? new Motion(
                                        entry.getLongArray("opening").orElseThrow(),
                                        entry.getLongArray("closing").orElseThrow())
                                : null;
                var state =
                        states.computeIfAbsent(
                                entry.getCompound("state").orElseThrow(), value -> value);
                cells.put(entry.getLong("offset").orElseThrow(), new Cell(state, motion));
            }
            facings.put(direction, cells);
        }
        return new Layout(
                tag.getStringOr("door", ""), tag.getIntOr("ticks", 0), Map.copyOf(facings));
    }

    private record Data(Map<String, Layout> layouts, Set<String> members, Set<String> ports) {}

    private record Layout(String door, int ticks, Map<String, Long2ObjectMap<Cell>> facings) {}

    private record Cell(CompoundTag state, @Nullable Motion motion) {}

    private record Motion(long[] opening, long[] closing) {}

    private static final class Tables {
        private static final Data DATA = load();
    }
}
