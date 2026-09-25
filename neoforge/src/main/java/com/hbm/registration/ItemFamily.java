// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.registration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.jspecify.annotations.Nullable;

public final class ItemFamily<E extends Enum<E>, I extends Item>
        implements Iterable<Reg.Handle<I>> {

    private final E[] types;
    private final List<Reg.Handle<I>> members;
    private final Reg.Handle<I>[] byOrdinal;
    private List<E> creative;
    private volatile @Nullable Map<Item, E> byItem;

    private static final List<ItemFamily<?, ?>> ALL = new ArrayList<>();
    private static volatile @Nullable Map<Item, ItemFamily<?, ?>> byMember;

    @SuppressWarnings("unchecked")
    ItemFamily(E[] types, List<Reg.Handle<I>> members) {
        this.types = types;
        this.members = List.copyOf(members);
        this.creative = List.of(types);
        this.byOrdinal = new Reg.Handle[types[0].getDeclaringClass().getEnumConstants().length];
        for (int i = 0; i < types.length; i++) byOrdinal[types[i].ordinal()] = members.get(i);
        ALL.add(this);
    }

    public static @Nullable ItemFamily<?, ?> of(ItemLike item) {
        Map<Item, ItemFamily<?, ?>> table = byMember;
        if (table == null) {
            table = new IdentityHashMap<>();
            for (ItemFamily<?, ?> family : ALL) {
                for (Reg.Handle<?> member : family) table.put(member.asItem(), family);
            }
            byMember = table;
        }
        return table.get(item.asItem());
    }

    public Reg.Handle<I> handle(E type) {
        Reg.Handle<I> member = byOrdinal[type.ordinal()];
        if (member == null) throw new IllegalArgumentException(type + " has no member");
        return member;
    }

    public I get(E type) {
        return handle(type).get();
    }

    public ItemStack stack(E type) {
        return new ItemStack(get(type));
    }

    public ItemStack stack(E type, int count) {
        return new ItemStack(get(type), count);
    }

    public boolean is(ItemStack stack, E type) {
        return stack.is(get(type));
    }

    public @Nullable E typeOf(ItemStack stack) {
        return typeOf(stack.getItem());
    }

    public @Nullable E typeOf(ItemLike item) {
        Map<Item, E> table = byItem;
        if (table == null) {
            table = new IdentityHashMap<>();
            for (E type : types) table.put(get(type), type);
            byItem = table;
        }
        return table.get(item.asItem());
    }

    public List<E> types() {
        return List.of(types);
    }

    public List<Reg.Handle<I>> creative() {
        List<Reg.Handle<I>> out = new ArrayList<>(creative.size());
        for (E type : creative) out.add(handle(type));
        return out;
    }

    @SafeVarargs
    public final ItemFamily<E, I> creative(E... order) {
        creative = List.copyOf(Arrays.asList(order));
        return this;
    }

    public ItemFamily<E, I> addTo(List<? super Reg.Handle<I>> roster) {
        roster.addAll(creative());
        return this;
    }

    @Override
    public Iterator<Reg.Handle<I>> iterator() {
        return members.iterator();
    }
}
