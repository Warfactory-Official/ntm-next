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

package com.hbm.inventory;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.Spliterator;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import net.minecraft.world.item.ItemStack;

public final class SlotList extends ArrayList<ItemStack> {
    private final Object source;

    public SlotList(Object source, Collection<ItemStack> items) {
        super(items);
        this.source = source;
        for (int slot = 0; slot < size(); slot++)
            InventorySlotBinding.bind(get(slot), source, slot);
    }

    @Override
    public ItemStack set(int slot, ItemStack item) {
        ItemStack previous = super.set(slot, item);
        replaced(slot, previous, item);
        return previous;
    }

    private void replaced(int slot, ItemStack previous, ItemStack item) {
        if (previous != item) {
            InventorySlotBinding.unbind(previous, source, slot);
            InventorySlotBinding.bind(item, source, slot);
            InventoryChanges.slotChanged(source, slot);
        }
    }

    private void shifted(int first, int previousSize) {
        for (int slot = first; slot < size(); slot++) {
            InventorySlotBinding.bind(get(slot), source, slot);
            InventoryChanges.slotChanged(source, slot);
        }
        for (int slot = size(); slot < previousSize; slot++)
            InventoryChanges.slotChanged(source, slot);
    }

    @Override
    public boolean add(ItemStack item) {
        add(size(), item);
        return true;
    }

    @Override
    public void add(int slot, ItemStack item) {
        int old = size();
        super.add(slot, item);
        shifted(slot, old);
    }

    @Override
    public boolean addAll(Collection<? extends ItemStack> items) {
        return addAll(size(), items);
    }

    @Override
    public boolean addAll(int slot, Collection<? extends ItemStack> items) {
        int old = size();
        boolean changed = super.addAll(slot, items);
        if (changed) shifted(slot, old);
        return changed;
    }

    @Override
    public ItemStack remove(int slot) {
        int old = size();
        ItemStack item = super.remove(slot);
        InventorySlotBinding.unbind(item, source, slot);
        shifted(slot, old);
        return item;
    }

    @Override
    public boolean remove(Object item) {
        int index = indexOf(item);
        if (index < 0) return false;
        remove(index);
        return true;
    }

    @Override
    protected void removeRange(int first, int end) {
        Objects.checkFromToIndex(first, end, size());
        int old = size();
        for (int slot = first; slot < end; slot++)
            InventorySlotBinding.unbind(get(slot), source, slot);
        super.removeRange(first, end);
        shifted(first, old);
    }

    @Override
    public void clear() {
        removeRange(0, size());
    }

    @Override
    public boolean removeIf(Predicate<? super ItemStack> predicate) {
        ItemStack[] previous = toArray(ItemStack[]::new);
        boolean changed = super.removeIf(predicate);
        bulkChanged(previous);
        return changed;
    }

    @Override
    public boolean removeAll(Collection<?> items) {
        ItemStack[] previous = toArray(ItemStack[]::new);
        try {
            return super.removeAll(items);
        } finally {
            bulkChanged(previous);
        }
    }

    @Override
    public boolean retainAll(Collection<?> items) {
        ItemStack[] previous = toArray(ItemStack[]::new);
        try {
            return super.retainAll(items);
        } finally {
            bulkChanged(previous);
        }
    }

    private void bulkChanged(ItemStack[] previous) {
        int first = 0;
        while (first < previous.length && first < size() && previous[first] == get(first)) first++;
        if (first == previous.length && first == size()) return;
        for (int slot = first; slot < previous.length; slot++)
            InventorySlotBinding.unbind(previous[slot], source, slot);
        shifted(first, previous.length);
    }

    @Override
    public List<ItemStack> subList(int first, int end) {
        return new SlotView(super.subList(first, end), first);
    }

    private final class SlotView extends AbstractList<ItemStack> implements RandomAccess {
        private final List<ItemStack> view;
        private final int first;

        private SlotView(List<ItemStack> view, int first) {
            this.view = view;
            this.first = first;
        }

        @Override
        public int size() {
            return view.size();
        }

        @Override
        public ItemStack get(int slot) {
            return view.get(slot);
        }

        @Override
        public ItemStack set(int slot, ItemStack item) {
            ItemStack previous = view.set(slot, item);
            replaced(first + slot, previous, item);
            return previous;
        }

        @Override
        public Iterator<ItemStack> iterator() {
            return view.iterator();
        }

        @Override
        public ListIterator<ItemStack> listIterator(int slot) {
            return view.listIterator(slot);
        }

        @Override
        public Spliterator<ItemStack> spliterator() {
            return view.spliterator();
        }

        @Override
        public Object[] toArray() {
            return view.toArray();
        }

        @Override
        public <T> T[] toArray(T[] items) {
            return view.toArray(items);
        }

        @Override
        public void add(int slot, ItemStack item) {
            view.add(slot, item);
        }

        @Override
        public ItemStack remove(int slot) {
            return view.remove(slot);
        }

        @Override
        public boolean addAll(Collection<? extends ItemStack> items) {
            return view.addAll(items);
        }

        @Override
        public boolean addAll(int slot, Collection<? extends ItemStack> items) {
            return view.addAll(slot, items);
        }

        @Override
        protected void removeRange(int from, int to) {
            view.subList(from, to).clear();
        }

        @Override
        public List<ItemStack> subList(int from, int to) {
            return new SlotView(view.subList(from, to), first + from);
        }

        @Override
        public boolean removeIf(Predicate<? super ItemStack> predicate) {
            ItemStack[] previous = SlotList.this.toArray(ItemStack[]::new);
            boolean changed = view.removeIf(predicate);
            bulkChanged(previous);
            return changed;
        }

        @Override
        public boolean removeAll(Collection<?> items) {
            ItemStack[] previous = SlotList.this.toArray(ItemStack[]::new);
            try {
                return view.removeAll(items);
            } finally {
                bulkChanged(previous);
            }
        }

        @Override
        public boolean retainAll(Collection<?> items) {
            ItemStack[] previous = SlotList.this.toArray(ItemStack[]::new);
            try {
                return view.retainAll(items);
            } finally {
                bulkChanged(previous);
            }
        }
    }

    @Override
    public void replaceAll(UnaryOperator<ItemStack> operator) {
        for (int slot = 0; slot < size(); slot++) set(slot, operator.apply(get(slot)));
    }

    @Override
    public void sort(Comparator<? super ItemStack> comparator) {
        super.sort(comparator);
        shifted(0, size());
    }

    @Override
    public Object clone() {
        return new ArrayList<>(this);
    }
}
