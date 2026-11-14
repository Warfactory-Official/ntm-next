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

package com.hbm.neoforge.inventory;

import appeng.api.stacks.GenericStack;
import com.hbm.inventory.InventoryChanges;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Predicate;

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
        if (!Objects.equals(previous, value)) InventoryChanges.slotChanged(source, slot);
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
    public boolean removeIf(Predicate<? super GenericStack> predicate) {
        GenericStack[] previous = toArray(GenericStack[]::new);
        boolean changed = super.removeIf(predicate);
        int first = 0;
        while (first < previous.length
                && first < size()
                && Objects.equals(previous[first], get(first))) first++;
        if (first < previous.length || first < size()) shifted(first, previous.length);
        return changed;
    }
}
