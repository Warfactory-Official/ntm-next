// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.inventory;

import appeng.api.stacks.GenericStack;
import com.hbm.inventory.InventoryChanges;
import java.util.ArrayList;
import java.util.Collection;

public final class AE2SendList extends ArrayList<GenericStack> {
    private final Object source;

    public AE2SendList(Object source, Collection<GenericStack> entries) {
        super(entries);
        this.source = source;
    }

    public boolean owns(Object owner) {
        return source == owner;
    }

    @Override
    public GenericStack set(int slot, GenericStack value) {
        GenericStack previous = super.set(slot, value);
        if (!java.util.Objects.equals(previous, value)) InventoryChanges.slotChanged(source, slot);
        return previous;
    }

    private void shifted(int first, int previous) {
        for (int slot = first; slot < Math.max(size(), previous); slot++)
            InventoryChanges.slotChanged(source, slot);
    }

    @Override
    public boolean add(GenericStack value) {
        add(size(), value);
        return true;
    }

    @Override
    public void add(int slot, GenericStack value) {
        int previous = size();
        super.add(slot, value);
        shifted(slot, previous);
    }

    @Override
    public boolean addAll(Collection<? extends GenericStack> values) {
        return addAll(size(), values);
    }

    @Override
    public boolean addAll(int slot, Collection<? extends GenericStack> values) {
        int previous = size();
        boolean changed = super.addAll(slot, values);
        if (changed) shifted(slot, previous);
        return changed;
    }

    @Override
    public GenericStack remove(int slot) {
        int previous = size();
        GenericStack value = super.remove(slot);
        shifted(slot, previous);
        return value;
    }

    @Override
    public boolean remove(Object value) {
        int slot = indexOf(value);
        if (slot < 0) return false;
        remove(slot);
        return true;
    }

    @Override
    protected void removeRange(int first, int end) {
        int previous = size();
        super.removeRange(first, end);
        shifted(first, previous);
    }

    @Override
    public void clear() {
        removeRange(0, size());
    }

    @Override
    public boolean removeIf(java.util.function.Predicate<? super GenericStack> predicate) {
        GenericStack[] previous = toArray(GenericStack[]::new);
        try {
            return super.removeIf(predicate);
        } finally {
            int first = 0;
            while (first < previous.length
                    && first < size()
                    && java.util.Objects.equals(previous[first], get(first))) first++;
            if (first < previous.length || first < size()) shifted(first, previous.length);
        }
    }
}
