// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.registration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jspecify.annotations.Nullable;

public final class OpenRegistry<T> {

    private final String what;
    private final Function<? super T, String> nameOf;
    private final List<T> declared = new ArrayList<>();
    private final Map<T, List<T>> placedAfter = new IdentityHashMap<>();
    private final Map<String, T> named = new HashMap<>();
    private volatile @Nullable Order<T> order;

    public OpenRegistry(String what, Function<? super T, String> nameOf) {
        this.what = what;
        this.nameOf = nameOf;
    }

    public synchronized <E extends T> E add(E entry) {
        claim(entry);
        declared.add(entry);
        return entry;
    }

    public synchronized <E extends T> E addAfter(T anchor, E entry) {
        claim(entry);
        placedAfter.computeIfAbsent(anchor, ignored -> new ArrayList<>()).add(entry);
        return entry;
    }

    public synchronized void moveAfter(T anchor, T entry) {
        if (order != null) {
            throw new IllegalStateException(
                    "registration is closed: "
                            + what
                            + " "
                            + nameOf.apply(entry)
                            + " was moved after the "
                            + what
                            + " order was read");
        }
        if (!declared.remove(entry)) {
            throw new IllegalStateException(nameOf.apply(entry) + " is not a declared " + what);
        }
        placedAfter.computeIfAbsent(anchor, ignored -> new ArrayList<>()).add(entry);
    }

    public List<T> values() {
        return order().values;
    }

    public int index(T entry) {
        Integer index = order().index.get(entry);
        if (index == null)
            throw new IllegalArgumentException(entry + " is not a registered " + what);
        return index;
    }

    public T byIndex(int index) {
        return order().values.get(index);
    }

    public T byWrappedIndex(int raw) {
        List<T> values = order().values;
        return values.get(Math.abs(raw) % values.size());
    }

    public @Nullable T byName(String name) {
        order();
        return named.get(name);
    }

    public T byName(String name, T fallback) {
        T entry = byName(name);
        return entry == null ? fallback : entry;
    }

    public String name(T entry) {
        return nameOf.apply(entry);
    }

    public Codec<T> codec() {
        return Codec.STRING.comapFlatMap(
                name -> {
                    T entry = byName(name);
                    return entry != null
                            ? DataResult.success(entry)
                            : DataResult.error(() -> "unknown " + what + " " + name);
                },
                nameOf::apply);
    }

    public StreamCodec<ByteBuf, T> streamCodec() {
        return ByteBufCodecs.VAR_INT.map(this::byIndex, this::index);
    }

    private void claim(T entry) {
        if (order != null) {
            throw new IllegalStateException(
                    "registration is closed: "
                            + what
                            + " "
                            + nameOf.apply(entry)
                            + " was added after the "
                            + what
                            + " order was read");
        }
        T clash = named.putIfAbsent(nameOf.apply(entry), entry);
        if (clash != null) {
            throw new IllegalStateException(
                    what + " name " + nameOf.apply(entry) + " is taken by " + clash);
        }
    }

    private Order<T> order() {
        Order<T> fixed = order;
        if (fixed == null) {
            synchronized (this) {
                fixed = order;
                if (fixed == null) order = fixed = fix();
            }
        }
        return fixed;
    }

    private Order<T> fix() {
        List<T> values = new ArrayList<>(named.size());
        for (T entry : declared) place(entry, values);
        if (values.size() != named.size()) {
            List<String> stranded = new ArrayList<>();
            for (T entry : named.values())
                if (!values.contains(entry)) stranded.add(nameOf.apply(entry));
            throw new IllegalStateException(
                    what + " entries placed after anchors never registered: " + stranded);
        }
        Map<T, Integer> index = new IdentityHashMap<>(values.size());
        for (int i = 0; i < values.size(); i++) index.put(values.get(i), i);
        return new Order<>(List.copyOf(values), index);
    }

    private void place(T entry, List<T> values) {
        values.add(entry);
        for (T addition : placedAfter.getOrDefault(entry, List.of())) place(addition, values);
    }

    private record Order<T>(List<T> values, Map<T, Integer> index) {}
}
