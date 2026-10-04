// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import java.util.Arrays;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class AE2StorageQueries {
    private static final int LIST = 1;
    private static final int INSERT = 2;
    private static final int EXTRACT = 3;
    private static final ThreadLocal<QueryStack> QUERIES = ThreadLocal.withInitial(QueryStack::new);

    private AE2StorageQueries() {}

    public static void beginListing(KeyCounter out) {
        QUERIES.get().begin(LIST, null, out);
    }

    public static void beginSimulation(AEKey what, boolean insertion) {
        QUERIES.get().begin(insertion ? INSERT : EXTRACT, what, null);
    }

    public static void end() {
        QUERIES.get().end();
    }

    public static boolean beginSourceListing(OwnedMEStorage storage, KeyCounter out) {
        QueryStack queries = QUERIES.get();
        Frame frame = queries.current();
        if (frame == null || frame.kind != LIST || frame.out != out) {
            queries.begin(LIST, null, out);
            frame = queries.current();
            frame.sourceOnly = true;
        }
        assert frame.sourceOwner == null;
        frame.sourceOwner = storage.physicalOwner();
        if (frame.sourceAmounts == null) frame.sourceAmounts = new Object2LongOpenHashMap<>();
        return true;
    }

    public static void endSourceListing() {
        QueryStack queries = QUERIES.get();
        Frame frame = queries.current();
        frame.sourceOwner = null;
        frame.sourceAmounts.clear();
        if (frame.sourceOnly) queries.end();
    }

    public static long simulate(
            OwnedMEStorage storage,
            AEKey what,
            long amount,
            IActionSource source,
            boolean insertion) {
        assert amount >= 0;
        BlockEntity owner = storage.physicalOwner();
        if (owner.isRemoved() || amount == 0) return 0;
        Frame frame = QUERIES.get().current();
        if (frame == null
                || frame.kind != (insertion ? INSERT : EXTRACT)
                || frame.what != what && !frame.what.equals(what)) {
            return insertion
                    ? storage.insertPhysical(what, amount, Actionable.SIMULATE, source)
                    : storage.extractPhysical(what, amount, Actionable.SIMULATE, source);
        }
        int index = frame.index(owner);
        long remaining = frame.quotas[index];
        if (remaining < 0) {
            remaining =
                    insertion
                            ? storage.insertPhysical(
                                    what, Long.MAX_VALUE, Actionable.SIMULATE, source)
                            : storage.extractPhysical(
                                    what, Long.MAX_VALUE, Actionable.SIMULATE, source);
            assert remaining >= 0;
        }
        long accepted = Math.min(amount, remaining);
        frame.quotas[index] = remaining - accepted;
        return accepted;
    }

    public static void add(OwnedMEStorage storage, KeyCounter out, AEKey what, long amount) {
        Frame frame = QUERIES.get().current();
        BlockEntity owner = storage.physicalOwner();
        if (frame == null || frame.kind != LIST || frame.out != out || frame.sourceOwner != owner) {
            out.add(what, amount);
            return;
        }
        amount += frame.sourceAmounts.addTo(what, amount);
        addOwned(out, owner, what, amount);
    }

    public static void capturedListing(KeyCounter counter) {
        Frame frame = QUERIES.get().current();
        assert frame != null && frame.kind == LIST;
        frame.filteredSource = counter;
    }

    public static void addFiltered(KeyCounter out, AEKey what, long amount) {
        Frame frame = QUERIES.get().current();
        assert frame != null && frame.kind == LIST && frame.filteredSource != null;
        addProjected(out, frame.filteredSource, what, amount);
    }

    public static void addAll(KeyCounter out, KeyCounter source) {
        Origins origins = ((CounterMetadata) (Object) source).hbm$listingOrigins();
        if (origins == null || origins.used == 0) {
            out.addAll(source);
            return;
        }
        for (var entry : source) addProjected(out, source, entry.getKey(), entry.getLongValue());
    }

    private static void addProjected(KeyCounter out, KeyCounter source, AEKey what, long amount) {
        Origins origins = ((CounterMetadata) (Object) source).hbm$listingOrigins();
        if (origins == null) {
            out.add(what, amount);
            return;
        }
        int head = origins.keyHeads.getInt(what);
        long owned = 0;
        for (int index = head; index >= 0; index = origins.nextKey[index])
            owned += origins.amounts[index];
        long opaque = amount - owned;
        assert opaque >= 0;
        if (opaque != 0) out.add(what, opaque);
        for (int index = head; index >= 0; index = origins.nextKey[index])
            addOwned(out, origins.owners[index], what, origins.amounts[index]);
    }

    private static void addOwned(KeyCounter out, BlockEntity owner, AEKey what, long amount) {
        if (owner.isRemoved() || amount == 0) return;
        CounterMetadata metadata = (CounterMetadata) (Object) out;
        Origins origins = metadata.hbm$listingOrigins();
        if (origins == null) metadata.hbm$listingOrigins(origins = new Origins());
        long added = origins.record(owner, what, amount);
        if (added != 0) out.add(what, added);
    }

    public interface CounterMetadata {
        Origins hbm$listingOrigins();

        void hbm$listingOrigins(Origins origins);
    }

    public static final class Origins {
        private final Long2IntOpenHashMap ownerKeyHeads = new Long2IntOpenHashMap(4);
        private final Object2IntOpenHashMap<AEKey> keyHeads = new Object2IntOpenHashMap<>(4);
        private BlockEntity[] owners = new BlockEntity[4];
        private AEKey[] keys = new AEKey[4];
        private long[] amounts = new long[4];
        private int[] nextOwnerKey = new int[4];
        private int[] nextKey = new int[4];
        private int used;

        private Origins() {
            ownerKeyHeads.defaultReturnValue(-1);
            keyHeads.defaultReturnValue(-1);
        }

        private long record(BlockEntity owner, AEKey what, long amount) {
            long hash = (long) System.identityHashCode(owner) << 32 | what.hashCode() & 0xffffffffL;
            int head = ownerKeyHeads.get(hash);
            for (int index = head; index >= 0; index = nextOwnerKey[index]) {
                if (owners[index] != owner || !keys[index].equals(what)) continue;
                long previous = amounts[index];
                if (amount <= previous) return 0;
                amounts[index] = amount;
                return amount - previous;
            }
            if (used == owners.length) {
                int size = used * 2;
                owners = Arrays.copyOf(owners, size);
                keys = Arrays.copyOf(keys, size);
                amounts = Arrays.copyOf(amounts, size);
                nextOwnerKey = Arrays.copyOf(nextOwnerKey, size);
                nextKey = Arrays.copyOf(nextKey, size);
            }
            owners[used] = owner;
            keys[used] = what;
            amounts[used] = amount;
            nextOwnerKey[used] = head;
            nextKey[used] = keyHeads.getInt(what);
            ownerKeyHeads.put(hash, used);
            keyHeads.put(what, used++);
            return amount;
        }

        public void clear() {
            ownerKeyHeads.clear();
            keyHeads.clear();
            Arrays.fill(owners, 0, used, null);
            Arrays.fill(keys, 0, used, null);
            used = 0;
        }
    }

    private static final class QueryStack {
        private Frame[] frames = new Frame[2];
        private Frame[] active = new Frame[2];
        private int depth;

        void begin(int kind, AEKey what, KeyCounter out) {
            if (depth == frames.length) {
                frames = Arrays.copyOf(frames, frames.length * 2);
                active = Arrays.copyOf(active, frames.length);
            }
            Frame parent = current();
            if (kind != LIST && parent != null && parent.kind == kind && parent.what.equals(what)) {
                active[depth++] = parent;
                return;
            }
            Frame frame = frames[depth];
            if (frame == null) frames[depth] = frame = new Frame();
            frame.kind = kind;
            frame.what = what;
            frame.out = out;
            active[depth++] = frame;
        }

        Frame current() {
            return depth == 0 ? null : active[depth - 1];
        }

        void end() {
            assert depth > 0;
            Frame frame = active[--depth];
            active[depth] = null;
            if (frame == frames[depth]) frame.clear();
        }
    }

    private static final class Frame {
        private int kind;
        private AEKey what;
        private KeyCounter out;
        private BlockEntity[] owners;
        private int[] ownerIndices;
        private int[] ownerMarks;
        private int[] occupiedSlots;
        private int generation = 1;
        private long[] quotas;
        private BlockEntity sourceOwner;
        private Object2LongOpenHashMap<AEKey> sourceAmounts;
        private boolean sourceOnly;
        private KeyCounter filteredSource;
        private int used;

        int index(BlockEntity owner) {
            if (owners == null) {
                owners = new BlockEntity[8];
                ownerIndices = new int[8];
                ownerMarks = new int[8];
                occupiedSlots = new int[4];
                quotas = new long[4];
            }
            int slot = slot(owner);
            if (ownerMarks[slot] == generation) return ownerIndices[slot];
            if (used * 2 == owners.length) {
                growOwners();
                slot = slot(owner);
            }
            if (used == quotas.length) {
                quotas = Arrays.copyOf(quotas, quotas.length * 2);
                occupiedSlots = Arrays.copyOf(occupiedSlots, occupiedSlots.length * 2);
            }
            int index = used++;
            owners[slot] = owner;
            ownerMarks[slot] = generation;
            ownerIndices[slot] = index;
            occupiedSlots[index] = slot;
            quotas[index] = -1;
            return index;
        }

        private int slot(BlockEntity owner) {
            int mask = owners.length - 1;
            int slot = HashCommon.mix(System.identityHashCode(owner)) & mask;
            while (ownerMarks[slot] == generation && owners[slot] != owner)
                slot = (slot + 1) & mask;
            return slot;
        }

        private void growOwners() {
            BlockEntity[] previous = owners;
            owners = new BlockEntity[previous.length * 2];
            ownerIndices = new int[owners.length];
            ownerMarks = new int[owners.length];
            for (int index = 0; index < used; index++) {
                BlockEntity owner = previous[occupiedSlots[index]];
                int slot = slot(owner);
                owners[slot] = owner;
                ownerMarks[slot] = generation;
                ownerIndices[slot] = index;
                occupiedSlots[index] = slot;
            }
        }

        void clear() {
            if (used > 0) {
                for (int index = 0; index < used; index++) {
                    owners[occupiedSlots[index]] = null;
                }
                used = 0;
                if (++generation == 0) {
                    Arrays.fill(ownerMarks, 0);
                    generation = 1;
                }
            }
            what = null;
            out = null;
            sourceOwner = null;
            sourceOnly = false;
            filteredSource = null;
            if (sourceAmounts != null) sourceAmounts.clear();
        }
    }
}
