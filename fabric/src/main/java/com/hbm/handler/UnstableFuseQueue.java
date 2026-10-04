// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler;

import com.hbm.inventory.InventoryLocation;
import com.hbm.inventory.ItemInteriorSubscriptions;
import com.hbm.inventory.SlotDeltaBuffer;
import com.hbm.inventory.SlotSource;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.Arrays;
import net.minecraft.server.MinecraftServer;

public final class UnstableFuseQueue {
    private ItemInteriorSubscriptions subscriptions;

    public ItemInteriorSubscriptions interiorSubscriptions(MinecraftServer server) {
        if (subscriptions == null) subscriptions = new ItemInteriorSubscriptions(server);
        return subscriptions;
    }

    public ItemInteriorSubscriptions interiorSubscriptions() {
        return subscriptions;
    }

    private final Reference2ObjectOpenHashMap<InventoryLocation, SlotSource[]> interiors =
            new Reference2ObjectOpenHashMap<>();

    public SlotSource[] interiorSources(InventoryLocation owner) {
        return interiors.get(owner);
    }

    public void interiorSources(InventoryLocation owner, SlotSource[] sources) {
        if (sources == null) interiors.remove(owner);
        else interiors.put(owner, sources);
    }

    private final Reference2IntOpenHashMap<Object> indices = new Reference2IntOpenHashMap<>();
    private final Reference2IntOpenHashMap<Object> changed = new Reference2IntOpenHashMap<>();
    private final Reference2ObjectOpenHashMap<Object, FuseSlots> slotIndices =
            new Reference2ObjectOpenHashMap<>();
    private final ReferenceOpenHashSet<Object> wholeOwners = new ReferenceOpenHashSet<>();
    private final SlotDeltaBuffer slotChanges = new SlotDeltaBuffer();
    private Object[] changes = new Object[16];
    private Object[] pendingChanges = new Object[16];
    private int changeCount;
    private Object[] owners = new Object[16];
    private long[] deadlines = new long[16];
    private int size;

    public UnstableFuseQueue() {
        indices.defaultReturnValue(-1);
        changed.defaultReturnValue(-1);
    }

    public long nextDeadline() {
        return size == 0 ? Long.MAX_VALUE : deadlines[0];
    }

    public long deadline(Object owner) {
        int index = indices.getInt(owner);
        return index < 0 ? Long.MAX_VALUE : deadlines[index];
    }

    public void changed(Object owner) {
        if (changed.containsKey(owner)) return;
        if (changeCount == pendingChanges.length)
            pendingChanges = Arrays.copyOf(pendingChanges, changeCount * 2);
        changed.put(owner, changeCount);
        pendingChanges[changeCount++] = owner;
    }

    public boolean hasChanges() {
        return !changed.isEmpty();
    }

    public boolean isChanged(Object owner) {
        return changed.containsKey(owner);
    }

    public FuseSlots slots(Object owner) {
        return slotIndices.get(owner);
    }

    public boolean wholeOwner(Object owner) {
        return wholeOwners.contains(owner);
    }

    public void wholeOwner(Object owner, boolean whole) {
        if (whole) wholeOwners.add(owner);
        else wholeOwners.remove(owner);
    }

    public void slots(Object owner, FuseSlots slots) {
        if (slots == null || slots.isEmpty()) slotIndices.remove(owner);
        else slotIndices.put(owner, slots);
    }

    public void slotChanged(Object source, int slot) {
        slotChanges.record(source, slot, null);
    }

    public SlotDeltaBuffer.Batch takeSlots() {
        return slotChanges.isEmpty() ? null : slotChanges.take();
    }

    public int takeChanges() {
        int count = changeCount;
        if (count == 0) return 0;
        Object[] previous = changes;
        changes = pendingChanges;
        pendingChanges = previous;
        for (int i = 0; i < count; i++) changed.removeInt(changes[i]);
        changeCount = 0;
        return count;
    }

    public Object changedOwner(int index) {
        Object owner = changes[index];
        changes[index] = null;
        return owner;
    }

    public void put(Object owner, long deadline) {
        if (deadline == Long.MAX_VALUE) {
            removeDeadline(owner);
            return;
        }
        int index = indices.getInt(owner);
        if (index < 0) {
            if (size == owners.length) {
                owners = Arrays.copyOf(owners, size * 2);
                deadlines = Arrays.copyOf(deadlines, size * 2);
            }
            index = size++;
            owners[index] = owner;
            deadlines[index] = deadline;
            indices.put(owner, index);
            up(index);
        } else {
            long previous = deadlines[index];
            deadlines[index] = deadline;
            if (deadline < previous) up(index);
            else if (deadline > previous) down(index);
        }
    }

    public Object removeFirst() {
        assert size != 0;
        Object owner = owners[0];
        removeDeadline(owner);
        return owner;
    }

    public void remove(Object owner) {
        int index = changed.removeInt(owner);
        if (index >= 0) {
            int last = --changeCount;
            if (index != last) {
                pendingChanges[index] = pendingChanges[last];
                changed.put(pendingChanges[index], index);
            }
            pendingChanges[last] = null;
        }
        slotIndices.remove(owner);
        wholeOwners.remove(owner);
        removeDeadline(owner);
    }

    private void removeDeadline(Object owner) {
        int index = indices.removeInt(owner);
        if (index < 0) return;
        int last = --size;
        if (index != last) {
            owners[index] = owners[last];
            deadlines[index] = deadlines[last];
            indices.put(owners[index], index);
        }
        owners[last] = null;
        if (index == last) return;
        if (index > 0 && deadlines[index] < deadlines[(index - 1) >>> 1]) up(index);
        else down(index);
    }

    private void up(int index) {
        while (index > 0) {
            int parent = (index - 1) >>> 1;
            if (deadlines[parent] <= deadlines[index]) break;
            swap(index, parent);
            index = parent;
        }
    }

    private void down(int index) {
        for (int child = index * 2 + 1; child < size; child = index * 2 + 1) {
            if (child + 1 < size && deadlines[child + 1] < deadlines[child]) child++;
            if (deadlines[index] <= deadlines[child]) break;
            swap(index, child);
            index = child;
        }
    }

    private void swap(int a, int b) {
        Object owner = owners[a];
        long deadline = deadlines[a];
        owners[a] = owners[b];
        deadlines[a] = deadlines[b];
        owners[b] = owner;
        deadlines[b] = deadline;
        indices.put(owners[a], a);
        indices.put(owners[b], b);
    }
}
