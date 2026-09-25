// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.uninos.graph;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import org.jspecify.annotations.Nullable;

public final class GraphSegment<D> {

    private static final long[] EMPTY_CELLS = new long[0];
    private static final long[] EMPTY_BITS = new long[0];

    private static final int INDEX_THRESHOLD = 64;
    public final D data;
    public GraphNode<D> endA;
    public GraphNode<D> endB;

    public int dirA;
    public int dirB;
    public @Nullable NodeNetwork<D> net;

    long[] backing;
    int from;
    int to;

    int ownedFrom;
    int ownedTo;

    long[] probedBits;

    int unprobedSideFaces;

    @Nullable GraphSegment<D> pendingPeel;
    @Nullable GraphSegment<D> peelOrigin;
    @Nullable GraphSegment<D> peelNext;

    @Nullable GraphSegment<D> forwardTo;

    int structMod;
    int floodStamp;
    int floodOwner;
    private @Nullable Long2IntOpenHashMap offsets;

    public GraphSegment(
            GraphNode<D> endA, GraphNode<D> endB, int dirA, int dirB, long[] interior, D data) {
        this.endA = endA;
        this.endB = endB;
        this.dirA = dirA;
        this.dirB = dirB;
        this.data = data;
        this.backing = interior;
        this.from = 0;
        this.to = interior.length;
        this.ownedFrom = 0;
        this.ownedTo = interior.length;
        this.probedBits = new long[bitWords(interior.length)];
        this.unprobedSideFaces = interior.length * 4;
    }

    private GraphSegment(GraphNode<D> endA, GraphNode<D> endB, int dirA, int dirB, D data) {
        this.endA = endA;
        this.endB = endB;
        this.dirA = dirA;
        this.dirB = dirB;
        this.data = data;
    }

    static <D> GraphSegment<D> sharedView(
            GraphNode<D> endA,
            GraphNode<D> endB,
            int dirA,
            int dirB,
            GraphSegment<D> host,
            int viewFrom,
            int viewTo,
            int ownedFrom,
            int ownedTo,
            int inheritedUnprobed) {
        GraphSegment<D> s = new GraphSegment<>(endA, endB, dirA, dirB, host.data);
        s.backing = host.backing;
        s.probedBits = host.probedBits;
        s.from = viewFrom;
        s.to = viewTo;
        s.ownedFrom = ownedFrom;
        s.ownedTo = ownedTo;
        s.unprobedSideFaces = Math.min(inheritedUnprobed, (viewTo - viewFrom) * 4);
        return s;
    }

    static <D> GraphSegment<D> compiled(
            GraphNode<D> endA,
            GraphNode<D> endB,
            int dirA,
            int dirB,
            long[] cells,
            long[] bits,
            int unprobedSideFaces,
            D data) {
        GraphSegment<D> s = new GraphSegment<>(endA, endB, dirA, dirB, cells, data);
        s.probedBits = bits;
        s.unprobedSideFaces = unprobedSideFaces;
        return s;
    }

    private static int bitWords(int slots) {
        return (slots * 6 + 63) / 64;
    }

    private static void copyGroup(long[] srcBits, int srcAbs, long[] dstBits, int dstAbs) {
        for (int f = 0; f < 6; f++) {
            int sb = srcAbs * 6 + f;
            if ((srcBits[sb >> 6] & (1L << (sb & 63))) == 0) continue;
            int db = dstAbs * 6 + f;
            dstBits[db >> 6] |= 1L << (db & 63);
        }
    }

    void dropArrays() {
        backing = EMPTY_CELLS;
        probedBits = EMPTY_BITS;
        from = 0;
        to = 0;
        ownedFrom = 0;
        ownedTo = 0;
        offsets = null;
    }

    public int cellCount() {
        return to - from;
    }

    public long cellAt(int i) {
        return backing[from + i];
    }

    public GraphNode<D> other(GraphNode<D> end) {
        return endA == end ? endB : endA;
    }

    public int slotAt(GraphNode<D> end) {
        return endA == end ? dirA : dirB;
    }

    public boolean probed(int i, int faceOrdinal) {
        return bit(from + i, faceOrdinal);
    }

    public void markProbed(int i, int faceOrdinal) {
        int b = (from + i) * 6 + faceOrdinal;
        long word = probedBits[b >> 6];
        long mask = 1L << (b & 63);
        if ((word & mask) == 0) {
            probedBits[b >> 6] = word | mask;
            if (unprobedSideFaces > 0) unprobedSideFaces--;
        }
    }

    public void clearProbed(int i, int faceOrdinal) {
        int b = (from + i) * 6 + faceOrdinal;
        long word = probedBits[b >> 6];
        long mask = 1L << (b & 63);
        if ((word & mask) != 0) {
            probedBits[b >> 6] = word & ~mask;
            unprobedSideFaces++;
            structMod++;
        }
    }

    public boolean hasUnprobed() {
        return unprobedSideFaces > 0;
    }

    void rebaseUnprobed(int exact) {
        unprobedSideFaces = exact;
    }

    private boolean bit(int abs, int f) {
        int b = abs * 6 + f;
        return (probedBits[b >> 6] & (1L << (b & 63))) != 0;
    }

    int groupAt(int i) {
        int abs = from + i;
        int g = 0;
        for (int f = 0; f < 6; f++) if (bit(abs, f)) g |= 1 << f;
        return g;
    }

    void appendAtB(long cell, int group, int unprobedSides) {
        structMod++;
        if (to == ownedTo) regrow(1);
        backing[to] = cell;
        writeGroup(to, group);
        to++;
        unprobedSideFaces += unprobedSides;
        if (offsets != null) offsets.put(cell, to - 1);
    }

    void appendAtA(long cell, int group, int unprobedSides) {
        structMod++;
        if (from == ownedFrom) regrow(-1);
        from--;
        backing[from] = cell;
        writeGroup(from, group);
        unprobedSideFaces += unprobedSides;
        if (offsets != null) offsets.put(cell, from);
    }

    long shrinkAtB(int runA, int runB) {
        structMod++;
        to--;
        long cell = backing[to];
        unprobedSideFaces -= 4 - probedSideCountAbs(to, runA, runB);
        if (unprobedSideFaces < 0) unprobedSideFaces = 0;
        if (offsets != null) offsets.remove(cell);
        return cell;
    }

    long shrinkAtA(int runA, int runB) {
        structMod++;
        long cell = backing[from];
        unprobedSideFaces -= 4 - probedSideCountAbs(from, runA, runB);
        if (unprobedSideFaces < 0) unprobedSideFaces = 0;
        if (offsets != null) offsets.remove(cell);
        from++;
        return cell;
    }

    void narrowTo(int newFrom, int newTo, int newOwnedFrom, int newOwnedTo) {
        structMod++;
        this.from = newFrom;
        this.to = newTo;
        this.ownedFrom = newOwnedFrom;
        this.ownedTo = newOwnedTo;
        this.unprobedSideFaces = Math.min(unprobedSideFaces, (newTo - newFrom) * 4);
    }

    private int probedSideCountAbs(int abs, int runA, int runB) {
        int g = 0;
        for (int f = 0; f < 6; f++) if (bit(abs, f)) g |= 1 << f;
        return Integer.bitCount(g & ~((1 << runA) | (1 << runB)));
    }

    private void writeGroup(int abs, int group) {
        for (int f = 0; f < 6; f++) {
            int b = abs * 6 + f;
            long mask = 1L << (b & 63);
            if ((group & (1 << f)) != 0) probedBits[b >> 6] |= mask;
            else probedBits[b >> 6] &= ~mask;
        }
    }

    private void regrow(int side) {
        int len = to - from;
        int cap = Math.max(8, len + Math.max(4, len >> 1));
        long[] cells = new long[cap];
        long[] bits = new long[bitWords(cap)];
        int slackTotal = cap - len;
        int slackA = side < 0 ? slackTotal - (slackTotal >> 2) : slackTotal >> 2;
        System.arraycopy(backing, from, cells, slackA, len);
        for (int i = 0; i < len; i++) copyGroup(probedBits, from + i, bits, slackA + i);
        this.backing = cells;
        this.probedBits = bits;
        this.from = slackA;
        this.to = slackA + len;
        this.ownedFrom = 0;
        this.ownedTo = cap;
        this.offsets = null;
    }

    public int offsetOf(long posKey) {
        int len = to - from;
        if (len < INDEX_THRESHOLD) {
            for (int i = from; i < to; i++) if (backing[i] == posKey) return i - from;
            return -1;
        }
        Long2IntOpenHashMap map = offsets;
        if (map == null) {
            map = new Long2IntOpenHashMap(len);
            map.defaultReturnValue(-1);
            for (int i = from; i < to; i++) map.put(backing[i], i);
            offsets = map;
        }
        int abs = map.get(posKey);
        if (abs < from || abs >= to || backing[abs] != posKey) return -1;
        return abs - from;
    }
}
