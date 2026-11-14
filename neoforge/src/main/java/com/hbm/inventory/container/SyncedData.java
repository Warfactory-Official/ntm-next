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

package com.hbm.inventory.container;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

public final class SyncedData implements ContainerData {

    private static final String[] NONE = {};
    private static final Map<Class<?>, Layout> LAYOUTS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, String[]> DECLARED = new ConcurrentHashMap<>();

    private final Layout layout;
    private final @Nullable BlockEntity source;
    private final int[] received;

    private SyncedData(Layout layout, @Nullable BlockEntity source) {
        this.layout = layout;
        this.source = source;
        this.received = source == null ? new int[layout.slots] : new int[0];
    }

    public static void fields(Class<?> owner, String... names) {
        DECLARED.put(owner, names);
    }

    public static SyncedData of(BlockEntity be) {
        return new SyncedData(layoutOf(be.getClass()), be);
    }

    public static SyncedData client(Class<? extends BlockEntity> beType) {
        return new SyncedData(layoutOf(beType), null);
    }

    @Override
    public int get(int id) {
        if (source == null) return received[id];
        Entry entry = layout.bySlot[id];
        return (int) ((entry.read(source) >>> (16 * (id - entry.slot))) & 0xFFFF);
    }

    @Override
    public void set(int id, int value) {
        if (source == null) received[id] = value;
    }

    @Override
    public int getCount() {
        return layout.slots;
    }

    public long get(String field) {
        Entry entry = layout.entries.get(field);
        if (entry == null) {
            throw new IllegalArgumentException(
                    layout.owner.getName()
                            + " ships no menu field '"
                            + field
                            + "'; it declares "
                            + layout.entries.keySet());
        }
        long raw = 0;
        for (int i = 0; i < entry.slots; i++) raw |= (get(entry.slot + i) & 0xFFFFL) << (16 * i);
        return switch (entry.type) {
            case "boolean" -> raw != 0 ? 1 : 0;
            case "enum" -> raw;
            case "short" -> (short) raw;
            case "int" -> (int) raw;
            default -> raw;
        };
    }

    public int getInt(String field) {
        return (int) get(field);
    }

    public boolean getBoolean(String field) {
        return get(field) != 0;
    }

    private static Layout layoutOf(Class<?> beType) {
        return LAYOUTS.computeIfAbsent(beType, SyncedData::buildLayout);
    }

    private static Layout buildLayout(Class<?> beType) {
        try {
            Class.forName(beType.getName(), true, beType.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
        List<Class<?>> chain = new ArrayList<>();
        for (Class<?> c = beType;
                c != null && BlockEntity.class.isAssignableFrom(c);
                c = c.getSuperclass()) {
            chain.addFirst(c);
        }
        Map<String, Entry> entries = new LinkedHashMap<>();
        int slot = 0;
        for (Class<?> owner : chain) {
            for (String name : DECLARED.getOrDefault(owner, NONE)) {
                Entry entry = new Entry(field(owner, name), slot);
                entries.put(name, entry);
                slot += entry.slots;
            }
        }
        Entry[] bySlot = new Entry[slot];
        for (Entry e : entries.values()) {
            for (int i = 0; i < e.slots; i++) bySlot[e.slot + i] = e;
        }
        return new Layout(beType, entries, slot, bySlot);
    }

    private static Field field(Class<?> owner, String name) {
        try {
            Field f = owner.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }

    private record Layout(Class<?> owner, Map<String, Entry> entries, int slots, Entry[] bySlot) {}

    private static final class Entry {

        private final Field field;
        private final String type;
        private final int slot;
        private final int slots;

        Entry(Field field, int slot) {
            Class<?> declared = field.getType();
            this.field = field;
            this.type = declared.isEnum() ? "enum" : declared.getName();
            this.slot = slot;
            this.slots =
                    switch (type) {
                        case "boolean", "short", "enum" -> 1;
                        case "int" -> 2;
                        case "long" -> 4;
                        default ->
                                throw new IllegalStateException("unpackable synced type " + type);
                    };
        }

        long read(BlockEntity be) {
            try {
                return switch (type) {
                    case "boolean" -> field.getBoolean(be) ? 1 : 0;
                    case "enum" -> ((Enum<?>) field.get(be)).ordinal();
                    default -> field.getLong(be);
                };
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("cannot read synced field " + field, e);
            }
        }
    }
}
