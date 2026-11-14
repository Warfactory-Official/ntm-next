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

package com.hbm.handler.pollution;

import com.hbm.util.RegistryUtil;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class PollutionSavedData extends SavedData {

    public static final int REGION_SHIFT = 6;
    static final float SURFACE_DAMAGE_POISON = 15F;

    private static final int SOOT = PollutionType.SOOT.ordinal();
    private static final int POISON = PollutionType.POISON.ordinal();
    private static final int HEAVYMETAL = PollutionType.HEAVYMETAL.ordinal();
    private static final int STRIDE = PollutionType.COUNT;

    private static final Codec<Region> REGION =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            Codec.INT.fieldOf("chunkX").forGetter(Region::x),
                                            Codec.INT.fieldOf("chunkZ").forGetter(Region::z),
                                            Codec.FLOAT
                                                    .listOf()
                                                    .fieldOf("values")
                                                    .forGetter(Region::values))
                                    .apply(i, Region::new));
    private static final Codec<PollutionSavedData> CODEC =
            REGION.listOf()
                    .optionalFieldOf("entries", List.of())
                    .codec()
                    .comapFlatMap(PollutionSavedData::decode, PollutionSavedData::encode);
    static final SavedDataType<PollutionSavedData> TYPE =
            new SavedDataType<>(RegistryUtil.id("pollution"), PollutionSavedData::new, CODEC, null);

    private final Long2IntOpenHashMap slots = new Long2IntOpenHashMap();
    private long[] keys = new long[16];
    private float[] values = new float[16 * STRIDE];
    private int size;
    private int[] hot = new int[16];
    private int[] hotAt = new int[16];
    private int hotCount;
    private float[] shares = new float[0];

    PollutionSavedData() {
        slots.defaultReturnValue(-1);
    }

    static long region(int blockX, int blockZ) {
        return pack(blockX >> REGION_SHIFT, blockZ >> REGION_SHIFT);
    }

    static long pack(int regionX, int regionZ) {
        return (long) regionX << 32 | regionZ & 0xFFFFFFFFL;
    }

    static int regionX(long key) {
        return (int) (key >> 32);
    }

    static int regionZ(long key) {
        return (int) key;
    }

    private static DataResult<PollutionSavedData> decode(List<Region> regions) {
        var field = new PollutionSavedData();
        for (var region : regions) {
            if (region.values().size() != STRIDE) {
                return DataResult.error(
                        () ->
                                "Pollution region "
                                        + region.x()
                                        + ","
                                        + region.z()
                                        + " holds "
                                        + region.values().size()
                                        + " values, not "
                                        + STRIDE);
            }
            int slot = field.slotOrCreate(pack(region.x(), region.z()));
            for (int kind = 0; kind < STRIDE; kind++)
                field.values[slot * STRIDE + kind] = region.values().get(kind);
            field.refreshHot(slot);
        }
        return DataResult.success(field);
    }

    private List<Region> encode() {
        var out = new ArrayList<Region>(size);
        for (int slot = 0; slot < size; slot++) {
            var regionValues = new ArrayList<Float>(STRIDE);
            for (int kind = 0; kind < STRIDE; kind++)
                regionValues.add(values[slot * STRIDE + kind]);
            out.add(new Region(regionX(keys[slot]), regionZ(keys[slot]), regionValues));
        }
        return out;
    }

    int slot(long key) {
        return slots.get(key);
    }

    float value(int slot, int kind) {
        return values[slot * STRIDE + kind];
    }

    void copy(int slot, float[] into) {
        System.arraycopy(values, slot * STRIDE, into, 0, STRIDE);
    }

    void write(long key, int kind, float value) {
        int slot = slotOrCreate(key);
        values[slot * STRIDE + kind] = value;
        if (kind == POISON) refreshHot(slot);
        setDirty();
    }

    void add(long key, int kind, float amount, double multiplier) {
        int slot = slotOrCreate(key);
        int at = slot * STRIDE + kind;
        values[at] = Math.clamp((float) (values[at] + amount * multiplier), 0F, 10_000F);
        if (kind == POISON) refreshHot(slot);
        setDirty();
    }

    int size() {
        return size;
    }

    int hotCount() {
        return hotCount;
    }

    long hotRegion(int index) {
        return keys[hot[index]];
    }

    void evolve() {
        int senders = size;
        if (shares.length < senders * 2)
            shares = new float[Math.max(senders * 2, shares.length * 2)];
        for (int slot = 0; slot < senders; slot++) {
            int base = slot * STRIDE;
            float soot = values[base + SOOT];
            float poison = values[base + POISON];
            float sootShare = 0F;
            float poisonShare = 0F;
            if (soot > 10F) {
                sootShare = soot * 0.05F;
                values[base + SOOT] = soot * 0.8F * 0.99F;
            } else {
                values[base + SOOT] = soot * 0.99F;
            }
            if (poison > 10F) {
                poisonShare = poison * 0.025F;
                values[base + POISON] = poison * 0.9F;
            } else {
                values[base + POISON] = poison * 0.995F;
            }
            values[base + HEAVYMETAL] = values[base + HEAVYMETAL] * 0.9995F;
            shares[slot * 2] = sootShare;
            shares[slot * 2 + 1] = poisonShare;
        }
        for (int slot = 0; slot < senders; slot++) {
            float sootShare = shares[slot * 2];
            float poisonShare = shares[slot * 2 + 1];
            if (sootShare == 0F && poisonShare == 0F) continue;
            long key = keys[slot];
            int x = regionX(key);
            int z = regionZ(key);
            receive(pack(x + 1, z), sootShare, poisonShare);
            receive(pack(x - 1, z), sootShare, poisonShare);
            receive(pack(x, z + 1), sootShare, poisonShare);
            receive(pack(x, z - 1), sootShare, poisonShare);
        }
        int slot = 0;
        while (slot < size) {
            if (anyPositive(slot)) {
                slot++;
                continue;
            }
            slots.remove(keys[slot]);
            int last = --size;
            if (slot != last) {
                keys[slot] = keys[last];
                System.arraycopy(values, last * STRIDE, values, slot * STRIDE, STRIDE);
                slots.put(keys[slot], slot);
            }
        }
        hotCount = 0;
        for (int i = 0; i < size; i++) {
            hotAt[i] = -1;
            refreshHot(i);
        }
        setDirty();
    }

    private void receive(long key, float sootShare, float poisonShare) {
        int base = slotOrCreate(key) * STRIDE;
        values[base + SOOT] += sootShare;
        values[base + POISON] += poisonShare;
    }

    private boolean anyPositive(int slot) {
        int base = slot * STRIDE;
        for (int kind = 0; kind < STRIDE; kind++) if (values[base + kind] > 0F) return true;
        return false;
    }

    private int slotOrCreate(long key) {
        int slot = slots.get(key);
        if (slot >= 0) return slot;
        slot = size++;
        if (slot == keys.length) {
            keys = Arrays.copyOf(keys, slot * 2);
            values = Arrays.copyOf(values, slot * 2 * STRIDE);
            hotAt = Arrays.copyOf(hotAt, slot * 2);
        }
        keys[slot] = key;
        Arrays.fill(values, slot * STRIDE, slot * STRIDE + STRIDE, 0F);
        hotAt[slot] = -1;
        slots.put(key, slot);
        return slot;
    }

    private void refreshHot(int slot) {
        boolean poisoned = values[slot * STRIDE + POISON] >= SURFACE_DAMAGE_POISON;
        int at = hotAt[slot];
        if (poisoned == at >= 0) return;
        if (poisoned) {
            if (hotCount == hot.length) hot = Arrays.copyOf(hot, hotCount * 2);
            hot[hotCount] = slot;
            hotAt[slot] = hotCount++;
        } else {
            int moved = hot[--hotCount];
            hot[at] = moved;
            hotAt[moved] = at;
            hotAt[slot] = -1;
        }
    }

    private record Region(int x, int z, List<Float> values) {}
}
