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

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.jspecify.annotations.Nullable;

final class CellChunk14 {
    private final CompoundTag chunk;
    private final Int2ObjectOpenHashMap<CompoundTag> sectionTags = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectOpenHashMap<Section> sections = new Int2ObjectOpenHashMap<>();

    CellChunk14(CompoundTag chunk) {
        this.chunk = chunk;
        for (var tag : chunk.getListOrEmpty("sections")) {
            var section = (CompoundTag) tag;
            sectionTags.put(section.getIntOr("Y", 0), section);
        }
    }

    CompoundTag state(int x, int y, int z) {
        var section = section(y >> 4);
        return section == null ? air() : section.at(((y & 15) << 8) | ((z & 15) << 4) | (x & 15));
    }

    void set(int x, int y, int z, CompoundTag state) {
        var section = section(y >> 4);
        section.unpack();
        section.values[((y & 15) << 8) | ((z & 15) << 4) | (x & 15)] = state;
        section.changed = true;
    }

    CompoundTag blockEntity(int x, int y, int z) {
        for (var tag : chunk.getListOrEmpty("block_entities")) {
            var be = (CompoundTag) tag;
            if (be.getIntOr("x", 0) == x && be.getIntOr("y", 0) == y && be.getIntOr("z", 0) == z)
                return be;
        }
        return new CompoundTag();
    }

    void removeNames(Set<String> removed) {
        for (var entry : sectionTags.int2ObjectEntrySet()) {
            var palette =
                    entry.getValue().getCompoundOrEmpty("block_states").getListOrEmpty("palette");
            if (palette.stream()
                    .noneMatch(state -> removed.contains(BlockStateEncoding14.name(state))))
                continue;
            var section = section(entry.getIntKey());
            section.unpack();
            for (int i = 0; i < 4096; i++) {
                if (removed.contains(section.values[i].getStringOr("Name", "")))
                    section.values[i] = air();
            }
            section.changed = true;
        }
    }

    CompoundTag write() {
        if (sections.values().stream().noneMatch(section -> section.changed)) return chunk;
        var encoded = new ListTag();
        for (var tag : chunk.getListOrEmpty("sections")) {
            var section = shallow((CompoundTag) tag);
            section.remove("BlockLight");
            section.remove("SkyLight");
            var states = sections.get(section.getIntOr("Y", 0));
            if (states != null && states.changed) section.put("block_states", states.encode());
            encoded.add(section);
        }
        var result = shallow(chunk);
        result.put("sections", encoded);
        result.remove("Heightmaps");
        result.putBoolean("isLightOn", false);
        return result;
    }

    static CompoundTag shallow(CompoundTag tag) {
        var copy = new CompoundTag();
        for (var key : tag.keySet()) copy.put(key, tag.get(key));
        return copy;
    }

    static CompoundTag air() {
        var state = new CompoundTag();
        state.putString("Name", "minecraft:air");
        return state;
    }

    private @Nullable Section section(int sectionY) {
        var section = sections.get(sectionY);
        if (section != null) return section;
        var tag = sectionTags.get(sectionY);
        if (tag == null) return null;
        var states = tag.getCompoundOrEmpty("block_states");
        if (states.getListOrEmpty("palette").isEmpty()) return null;
        section = new Section(states);
        sections.put(sectionY, section);
        return section;
    }

    private static final class Section {
        final CompoundTag tag;
        final ListTag palette;
        final long[] words;
        final int bits;
        final int perLong;
        CompoundTag @Nullable [] values;
        boolean changed;

        Section(CompoundTag tag) {
            this.tag = tag;
            palette = BlockStateEncoding14.canonicalList(tag.getListOrEmpty("palette"));
            words = tag.getLongArray("data").orElse(new long[0]);
            bits = Math.max(4, 32 - Integer.numberOfLeadingZeros(palette.size() - 1));
            perLong = 64 / bits;
        }

        CompoundTag at(int index) {
            if (values != null) return values[index];
            int id =
                    palette.size() == 1
                            ? 0
                            : (int)
                                    (words[index / perLong] >>> (index % perLong * bits)
                                            & ((1L << bits) - 1));
            return palette.getCompoundOrEmpty(id);
        }

        void unpack() {
            if (values != null) return;
            var unpacked = new CompoundTag[4096];
            for (int i = 0; i < unpacked.length; i++) unpacked[i] = at(i);
            values = unpacked;
        }

        CompoundTag encode() {
            var palette = new ArrayList<CompoundTag>();
            Map<CompoundTag, Integer> ids = new HashMap<>();
            int[] indices = new int[4096];
            for (int i = 0; i < indices.length; i++) {
                indices[i] =
                        ids.computeIfAbsent(
                                values[i],
                                state -> {
                                    palette.add(state);
                                    return palette.size() - 1;
                                });
            }
            var encoded = new ListTag();
            for (var state : palette) encoded.add(BlockStateEncoding14.current(state));
            var result = shallow(tag);
            result.put("palette", encoded);
            if (palette.size() == 1) {
                result.remove("data");
            } else {
                int bits = Math.max(4, 32 - Integer.numberOfLeadingZeros(palette.size() - 1));
                int perLong = 64 / bits;
                long[] words = new long[(4096 + perLong - 1) / perLong];
                for (int i = 0; i < indices.length; i++)
                    words[i / perLong] |= (long) indices[i] << (i % perLong * bits);
                result.putLongArray("data", words);
            }
            return result;
        }
    }
}
