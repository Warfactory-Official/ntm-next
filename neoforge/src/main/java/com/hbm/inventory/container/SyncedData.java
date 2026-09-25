// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

public final class SyncedData implements ContainerData {

    private static final String INDEX = "/com/hbm/inventory/container-sync.idx";
    private static final Map<Class<?>, Layout> LAYOUTS = new ConcurrentHashMap<>();
    private static volatile Map<String, List<String[]>> index;

    private final Layout layout;
    private final @Nullable BlockEntity source;
    private final int[] received;

    private SyncedData(Layout layout, @Nullable BlockEntity source) {
        this.layout = layout;
        this.source = source;
        this.received = source == null ? new int[layout.slots] : new int[0];
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
                            + " has no @ContainerSync field '"
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
        List<Class<?>> chain = new ArrayList<>();
        for (Class<?> c = beType;
                c != null && BlockEntity.class.isAssignableFrom(c);
                c = c.getSuperclass()) {
            chain.addFirst(c);
        }
        Map<String, Entry> entries = new LinkedHashMap<>();
        int slot = 0;
        for (Class<?> owner : chain) {
            for (String[] row : index().getOrDefault(owner.getName(), List.of())) {
                Entry entry = new Entry(field(owner, row[0]), row[1], slot);
                entries.put(row[0], entry);
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
            throw new IllegalStateException(
                    "the container-sync index names "
                            + owner.getName()
                            + "."
                            + name
                            + ", which does not exist - the index is stale",
                    e);
        }
    }

    private static Map<String, List<String[]>> index() {
        Map<String, List<String[]>> local = index;
        if (local != null) return local;
        Map<String, List<String[]>> built = new LinkedHashMap<>();
        try (InputStream in = SyncedData.class.getResourceAsStream(INDEX)) {
            if (in == null) {
                throw new IllegalStateException(
                        "the container-sync index is absent from the classpath at "
                                + INDEX
                                + " - the compile that writes it did not run");
            }
            try (BufferedReader reader =
                    new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                for (String line; (line = reader.readLine()) != null; ) {
                    if (line.isBlank()) continue;

                    String[] parts = line.split(" ");
                    built.computeIfAbsent(parts[0], k -> new ArrayList<>())
                            .add(new String[] {parts[1], parts[2]});
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read the container-sync index", e);
        }
        return index = built;
    }

    private record Layout(Class<?> owner, Map<String, Entry> entries, int slots, Entry[] bySlot) {}

    private static final class Entry {

        private final Field field;
        private final String type;
        private final int slot;
        private final int slots;

        Entry(Field field, String type, int slot) {
            this.field = field;
            this.type = type;
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
