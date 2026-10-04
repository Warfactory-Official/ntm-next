// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory;

import com.hbm.handler.UnstableFuses;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

public final class ItemInteriorSubscriptions {
    private final MinecraftServer server;
    private final Reference2ObjectOpenHashMap<Object, Int2ObjectOpenHashMap<Root>> roots =
            new Reference2ObjectOpenHashMap<>();
    private final IdentityHashMap<ItemInteriorAdapter, HashMap<UUID, Entry>> stores =
            new IdentityHashMap<>();
    private final Reference2ObjectOpenHashMap<ItemInteriorBackend, Entry> backends =
            new Reference2ObjectOpenHashMap<>();
    private final IdentityHashMap<Object, Entry> claims = new IdentityHashMap<>();
    private final Reference2ObjectOpenHashMap<Object, Native> natives =
            new Reference2ObjectOpenHashMap<>();
    private final Reference2ObjectOpenHashMap<Object, ReferenceOpenHashSet<Object>> rootSources =
            new Reference2ObjectOpenHashMap<>();
    private final Reference2ObjectOpenHashMap<Object, ReferenceOpenHashSet<Object>> nativeSources =
            new Reference2ObjectOpenHashMap<>();
    private final IdentityHashMap<Object, ReferenceOpenHashSet<Object>> aliases =
            new IdentityHashMap<>();
    private boolean updating;
    private boolean pending;
    private boolean dirty;

    public ItemInteriorSubscriptions(MinecraftServer server) {
        this.server = server;
    }

    public void publish(Object owner, Object source, int slot, ItemStack item) {
        if (owner instanceof ItemInteriorBackend backend) {
            Entry entry = backends.get(backend);
            if (entry == null) return;
            boolean changed =
                    slot == InventoryChanges.LAYOUT
                            ? updateReferences(entry, (SlotSource) source, slot)
                            : updateReferences(
                                    entry,
                                    (SlotSource) source,
                                    slot,
                                    ItemInteriors.references(item, server));
            if (changed) invalidate();
            return;
        }
        if (slot == InventoryChanges.LAYOUT) return;
        Object[] references = ItemInteriors.references(item, server);
        Int2ObjectOpenHashMap<Root> bySlot = roots.get(source);
        Root root = bySlot == null ? null : bySlot.get(slot);
        if (references.length == 0) {
            if (root != null) {
                bySlot.remove(slot);
                if (bySlot.isEmpty()) roots.remove(source);
                forgetRootSource(root.owner, source, bySlot);
                invalidate();
            }
            return;
        }
        Object dimension = InventoryOwnerAccess.of(owner).level(owner).dimension();
        if (root != null
                && root.owner == owner
                && root.dimension == dimension
                && sameReferences(root.references, references)) {
            root.references = references;
            return;
        }
        if (bySlot == null) {
            bySlot = new Int2ObjectOpenHashMap<>();
            roots.put(source, bySlot);
        }
        if (root == null) {
            root = new Root(owner, dimension, references);
            bySlot.put(slot, root);
        } else {
            Object previousOwner = root.owner;
            root.owner = owner;
            root.dimension = dimension;
            root.references = references;
            if (previousOwner != owner) forgetRootSource(previousOwner, source, bySlot);
        }
        rootSources.computeIfAbsent(owner, key -> new ReferenceOpenHashSet<>(1)).add(source);
        invalidate();
    }

    private void forgetRootSource(Object owner, Object source, Int2ObjectOpenHashMap<Root> slots) {
        for (Root root : slots.values()) if (root.owner == owner) return;
        var owned = rootSources.get(owner);
        owned.remove(source);
        if (owned.isEmpty()) rootSources.remove(owner);
    }

    public void retire(Object owner) {
        boolean changed = false;
        var rooted = rootSources.remove(owner);
        if (rooted != null)
            for (Object source : rooted) {
                var slots = roots.get(source);
                changed |= slots.values().removeIf(root -> root.owner == owner);
                if (slots.isEmpty()) roots.remove(source);
            }
        var owned = nativeSources.remove(owner);
        if (owned != null)
            for (Object source : owned) {
                Native previous = natives.remove(source);
                assert previous.owner == owner;
                changed |= claims.containsKey(previous.identity);
                forgetAlias(source, previous.identity);
            }
        if (changed) invalidate();
    }

    public void replaceSources(Object owner, Object sources) {
        boolean changed = false;
        var owned = nativeSources.get(owner);
        if (owned != null) {
            for (var iterator = owned.iterator(); iterator.hasNext(); ) {
                Object source = iterator.next();
                if (contains(sources, source)) continue;
                Native previous = natives.remove(source);
                assert previous.owner == owner;
                changed |= claims.containsKey(previous.identity);
                forgetAlias(source, previous.identity);
                iterator.remove();
            }
            if (owned.isEmpty()) nativeSources.remove(owner);
        }
        if (sources instanceof Object[] multiple) {
            for (Object source : multiple) changed |= registerNative(owner, source);
        } else if (sources != null) changed |= registerNative(owner, sources);
        var rooted = rootSources.get(owner);
        if (rooted != null) {
            for (var iterator = rooted.iterator(); iterator.hasNext(); ) {
                Object source = iterator.next();
                if (contains(sources, source)) continue;
                var slots = roots.get(source);
                changed |= slots.values().removeIf(root -> root.owner == owner);
                if (slots.isEmpty()) roots.remove(source);
                iterator.remove();
            }
            if (rooted.isEmpty()) rootSources.remove(owner);
        }
        if (changed) invalidate();
    }

    public void suppressSource(Object source) {
        var slots = roots.remove(source);
        if (slots == null) return;
        for (Root root : slots.values()) {
            var owned = rootSources.get(root.owner);
            if (owned == null) continue;
            owned.remove(source);
            if (owned.isEmpty()) rootSources.remove(root.owner);
        }
        invalidate();
    }

    private void invalidate() {
        dirty = true;
        if (updating) pending = true;
    }

    public void flush() {
        if (dirty) rebuild();
    }

    private static boolean contains(Object sources, Object source) {
        if (sources instanceof Object[] multiple) {
            for (Object candidate : multiple) if (candidate == source) return true;
            return false;
        }
        return sources == source;
    }

    private boolean registerNative(Object owner, Object source) {
        if (!(source instanceof InventoryAlias alias)) return false;
        Object identity = alias.inventoryIdentity();
        Object dimension = InventoryOwnerAccess.of(owner).level(owner).dimension();
        Native previous = natives.get(source);
        if (previous != null
                && previous.owner == owner
                && previous.identity == identity
                && previous.dimension == dimension) return false;
        boolean changed = claims.containsKey(identity);
        if (previous != null) {
            changed |= claims.containsKey(previous.identity);
            forgetAlias(source, previous.identity);
            var owned = nativeSources.get(previous.owner);
            owned.remove(source);
            if (owned.isEmpty()) nativeSources.remove(previous.owner);
        }
        natives.put(source, new Native(owner, identity, dimension));
        nativeSources.computeIfAbsent(owner, key -> new ReferenceOpenHashSet<>(1)).add(source);
        aliases.computeIfAbsent(identity, key -> new ReferenceOpenHashSet<>(1)).add(source);
        return changed;
    }

    private void forgetAlias(Object source, Object identity) {
        var sources = aliases.get(identity);
        sources.remove(source);
        if (sources.isEmpty()) aliases.remove(identity);
    }

    public SlotSource canonicalSource(Object source) {
        if (!(source instanceof InventoryAlias alias)) return null;
        Entry entry = claims.get(alias.inventoryIdentity());
        if (entry == null || !entry.active) return null;
        for (SlotSource candidate : entry.sources) {
            if (((InventoryAlias) candidate).inventoryIdentity() == alias.inventoryIdentity())
                return candidate == source ? null : candidate;
        }
        throw new IllegalStateException("Registered interior has no authoritative source");
    }

    public void changed(ItemInteriorAdapter adapter, UUID id) {
        var map = stores.get(adapter);
        Entry entry = map == null ? null : map.get(id);
        if (entry == null) return;
        entry.backend.changed();
        bindCurrentSources(entry);
        invalidate();
    }

    private Entry entry(ItemInteriorAdapter adapter, UUID id, ItemStackTemplate prototype) {
        var map = stores.computeIfAbsent(adapter, key -> new HashMap<>());
        Entry entry = map.get(id);
        if (entry != null) return entry;
        entry = new Entry(adapter, id, adapter.backend(server, id, prototype));
        map.put(id, entry);
        backends.put(entry.backend, entry);
        bindCurrentSources(entry);
        if (updating) pending = true;
        return entry;
    }

    private void bindCurrentSources(Entry entry) {
        SlotSource[] sources = entry.backend.inventorySources();
        boolean same = sources.length == entry.sources.length;
        if (same)
            for (int i = 0; i < sources.length; i++)
                if (sources[i] != entry.sources[i]) {
                    same = false;
                    break;
                }
        if (same) {
            for (SlotSource source : sources) source.refreshInventorySlots();
            return;
        }
        if (entry.active) UnstableFuses.unregisterInterior(server, entry.backend);
        for (SlotSource source : entry.sources) {
            Object identity = ((InventoryAlias) source).inventoryIdentity();
            if (claims.get(identity) == entry) claims.remove(identity);
            if (InventoryChanges.owner(source) == entry.backend)
                InventorySlotAccess.of(source).bind(source, null);
            if (entry.active) reindexAliases(identity);
        }
        entry.sources = sources.clone();
        entry.references.clear();
        for (SlotSource source : sources) {
            Entry previous = claims.put(((InventoryAlias) source).inventoryIdentity(), entry);
            assert previous == null || previous == entry;
            InventorySlotAccess.of(source).bind(source, entry.backend);
            updateReferences(entry, source, InventoryChanges.LAYOUT);
        }
        if (entry.active) UnstableFuses.registerInterior(server, entry.backend, sources);
        if (entry.active)
            for (SlotSource source : sources)
                reindexAliases(((InventoryAlias) source).inventoryIdentity());
    }

    private void reindexAliases(Object identity) {
        var sources = aliases.get(identity);
        if (sources != null)
            for (Object source : sources) UnstableFuses.reindexSource(server, source);
    }

    private void evict(Entry entry) {
        for (SlotSource source : entry.sources) {
            Object identity = ((InventoryAlias) source).inventoryIdentity();
            if (claims.get(identity) == entry) claims.remove(identity);
            if (InventoryChanges.owner(source) == entry.backend)
                InventorySlotAccess.of(source).bind(source, null);
        }
        backends.remove(entry.backend);
        var store = stores.get(entry.adapter);
        store.remove(entry.id);
        if (store.isEmpty()) stores.remove(entry.adapter);
    }

    private boolean updateReferences(Entry entry, SlotSource source, int slot) {
        if (slot == InventoryChanges.LAYOUT) {
            entry.references.remove(source);
            for (int i = 0; i < source.storedSlotCount(); i++) updateReferences(entry, source, i);
            return true;
        }
        Object[] next =
                source.physicalSlot(slot)
                        ? ItemInteriors.references(source.storedItem(slot), server)
                        : ItemInteriors.EMPTY;
        return updateReferences(entry, source, slot, next);
    }

    private boolean updateReferences(Entry entry, SlotSource source, int slot, Object[] next) {
        var slots = entry.references.get(source);
        Object[] previous =
                slots == null ? ItemInteriors.EMPTY : slots.getOrDefault(slot, ItemInteriors.EMPTY);
        if (previous == next) return false;
        if (next.length == 0) {
            if (slots != null) slots.remove(slot);
        } else {
            if (slots == null) {
                slots = new Int2ObjectOpenHashMap<>();
                entry.references.put(source, slots);
            }
            slots.put(slot, next);
        }
        return !sameReferences(previous, next);
    }

    private static boolean sameReferences(Object[] first, Object[] second) {
        if (first == second) return true;
        if (first.length != second.length) return false;
        for (int i = 0; i < first.length; i += 3)
            if (first[i] != second[i] || !first[i + 1].equals(second[i + 1])) return false;
        return true;
    }

    private void reach(Object owner, Object[] references, ReferenceOpenHashSet<Entry> seen) {
        for (int i = 0; i < references.length; i += 3) {
            Entry entry =
                    entry(
                            (ItemInteriorAdapter) references[i],
                            (UUID) references[i + 1],
                            (ItemStackTemplate) references[i + 2]);
            if (!seen.add(entry)) continue;
            entry.observe(owner);
            for (var slots : entry.references.values())
                for (Object[] children : slots.values()) reach(owner, children, seen);
        }
    }

    private void rebuild() {
        if (updating) {
            pending = true;
            return;
        }
        updating = true;
        try {
            do {
                pending = false;
                dirty = false;
                for (Entry entry : backends.values()) entry.selected = null;
                for (var slots : roots.values())
                    for (Root root : slots.values()) {
                        if (InventoryOwnerAccess.of(root.owner).live(root.owner))
                            reach(root.owner, root.references, new ReferenceOpenHashSet<>());
                    }
                for (Entry entry : new ArrayList<>(backends.values())) {
                    for (SlotSource claimed : entry.sources) {
                        var matching = aliases.get(((InventoryAlias) claimed).inventoryIdentity());
                        if (matching == null) continue;
                        for (Object source : matching) {
                            Native nativeSource = natives.get(source);
                            if (!InventoryOwnerAccess.of(nativeSource.owner)
                                    .live(nativeSource.owner)) continue;
                            var seen = new ReferenceOpenHashSet<Entry>();
                            seen.add(entry);
                            entry.observe(nativeSource.owner);
                            for (var values : entry.references.values())
                                for (Object[] children : values.values())
                                    reach(nativeSource.owner, children, seen);
                        }
                    }
                }
                for (Entry entry : new ArrayList<>(backends.values())) {
                    if (entry.selected != null
                            && !InventoryOwnerAccess.of(entry.selected).live(entry.selected)) {
                        pending = true;
                        continue;
                    }
                    entry.backend.carrier(entry.selected);
                    boolean active = entry.selected != null;
                    if (active != entry.active) {
                        entry.active = active;
                        if (active)
                            UnstableFuses.registerInterior(server, entry.backend, entry.sources);
                        else UnstableFuses.unregisterInterior(server, entry.backend);
                        for (SlotSource source : entry.sources)
                            reindexAliases(((InventoryAlias) source).inventoryIdentity());
                    }
                    if (!active) evict(entry);
                }
            } while (pending);
        } finally {
            updating = false;
        }
    }

    private static final class Root {
        Object owner;
        Object dimension;
        Object[] references;

        Root(Object owner, Object dimension, Object[] references) {
            this.owner = owner;
            this.dimension = dimension;
            this.references = references;
        }
    }

    private record Native(Object owner, Object identity, Object dimension) {}

    private static final class Entry {
        final ItemInteriorAdapter adapter;
        final UUID id;
        final ItemInteriorBackend backend;
        Object selected;
        final IdentityHashMap<SlotSource, Int2ObjectOpenHashMap<Object[]>> references =
                new IdentityHashMap<>();
        SlotSource[] sources = new SlotSource[0];
        boolean active;

        Entry(ItemInteriorAdapter adapter, UUID id, ItemInteriorBackend backend) {
            this.adapter = adapter;
            this.id = id;
            this.backend = backend;
        }

        void observe(Object carrier) {
            if (selected == null || ItemInteriors.compareCarriers(carrier, selected) < 0)
                selected = carrier;
        }
    }
}
