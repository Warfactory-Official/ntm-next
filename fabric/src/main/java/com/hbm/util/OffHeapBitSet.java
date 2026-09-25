// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.util;

import com.hbm.interfaces.BitMask;
import org.jspecify.annotations.NonNull;

import static com.hbm.lib.internal.UnsafeHolder.U;

public final class OffHeapBitSet implements BitMask, Cloneable, AutoCloseable {
    private final int logicalSizeLocal;
    private final int wordCountLocal;
    private long addr;
    private long bitCount;
    private boolean closed;

    public OffHeapBitSet(int logicalSize) {
        if (logicalSize < 0)
            throw new NegativeArraySizeException("logicalSize < 0: " + logicalSize);
        this.logicalSizeLocal = logicalSize;
        this.wordCountLocal = (int) (((long) logicalSize + 63L) >>> 6);
        long bytes = ((long) wordCountLocal) << 3;
        long p = U.allocateMemory(bytes);
        try {
            if (bytes != 0L) U.setMemory(p, bytes, (byte) 0);
        } catch (RuntimeException | Error failure) {
            if (p != 0L) U.freeMemory(p);
            throw failure;
        }
        this.addr = p;
        this.bitCount = 0L;
    }

    @Override
    public void free() {
        close();
    }

    @Override
    public void close() {
        if (closed) return;
        long p = addr;
        addr = 0L;
        closed = true;
        if (p != 0L) U.freeMemory(p);
    }

    private void ensureOpen() {
        if (closed) throw new IllegalStateException("OffHeapBitSet is closed");
    }

    private long wordAddr(int index) {
        ensureOpen();
        return addr + (((long) index) << 3);
    }

    private long getWord(int index) {
        return U.getLong(wordAddr(index));
    }

    private void setWord(int index, long v) {
        U.putLong(wordAddr(index), v);
    }

    @Override
    public boolean get(int bit) {
        ensureOpen();
        if (bit < 0 || bit >= logicalSizeLocal) return false;
        int wi = bit >>> 6;
        long mask = 1L << (bit & 63);
        return (getWord(wi) & mask) != 0L;
    }

    @Override
    public void set(int bit) {
        ensureOpen();
        if (bit < 0 || bit >= logicalSizeLocal) return;
        int wi = bit >>> 6;
        long mask = 1L << (bit & 63);
        long old = getWord(wi);
        if ((old & mask) != 0L) return;
        setWord(wi, old | mask);
        bitCount++;
    }

    @Override
    public boolean getAndSet(int bit) {
        ensureOpen();
        if (bit < 0 || bit >= logicalSizeLocal)
            throw new IndexOutOfBoundsException("bit index out of bounds: " + bit);
        int wi = bit >>> 6;
        long mask = 1L << (bit & 63);
        long old = getWord(wi);
        if ((old & mask) != 0L) return true;
        setWord(wi, old | mask);
        bitCount++;
        return false;
    }

    public void clear(int bit) {
        ensureOpen();
        if (bit < 0 || bit >= logicalSizeLocal) return;
        int wi = bit >>> 6;
        long mask = 1L << (bit & 63);
        long old = getWord(wi);
        if ((old & mask) == 0L) return;
        setWord(wi, old & ~mask);
        bitCount--;
    }

    public boolean getAndClear(int bit) {
        ensureOpen();
        if (bit < 0 || bit >= logicalSizeLocal)
            throw new IndexOutOfBoundsException("bit index out of bounds: " + bit);
        int wi = bit >>> 6;
        long mask = 1L << (bit & 63);
        long old = getWord(wi);
        if ((old & mask) == 0L) return false;
        setWord(wi, old & ~mask);
        bitCount--;
        return true;
    }

    public void set(int fromIndex, int toIndex) {
        updateRange(fromIndex, toIndex, 1);
    }

    public void clear(int fromIndex, int toIndex) {
        updateRange(fromIndex, toIndex, 0);
    }

    public void flip(int fromIndex, int toIndex) {
        updateRange(fromIndex, toIndex, -1);
    }

    public void clear() {
        ensureOpen();
        long bytes = ((long) wordCountLocal) << 3;
        if (bytes != 0L) U.setMemory(addr, bytes, (byte) 0);
        bitCount = 0L;
    }

    public void flip(int bit) {
        getAndFlip(bit);
    }

    public boolean getAndFlip(int bit) {
        ensureOpen();
        if (bit < 0 || bit >= logicalSizeLocal)
            throw new IndexOutOfBoundsException("bit index out of bounds: " + bit);
        int wi = bit >>> 6;
        long old = getWord(wi);
        long mask = 1L << (bit & 63);
        setWord(wi, old ^ mask);
        boolean wasSet = (old & mask) != 0L;
        bitCount += wasSet ? -1L : 1L;
        return wasSet;
    }

    private void updateRange(int fromIndex, int toIndex, int operation) {
        ensureOpen();
        if (fromIndex < 0 || toIndex > logicalSizeLocal || fromIndex > toIndex) {
            throw new IndexOutOfBoundsException(
                    "range: " + fromIndex + ".." + toIndex + ", size: " + logicalSizeLocal);
        }
        if (fromIndex == toIndex) return;
        int firstWord = fromIndex >>> 6;
        int lastWord = (toIndex - 1) >>> 6;
        long firstMask = -1L << (fromIndex & 63);
        long lastMask = (toIndex & 63) == 0 ? -1L : (1L << (toIndex & 63)) - 1L;
        if (firstWord == lastWord) {
            updateWord(firstWord, firstMask & lastMask, operation);
            return;
        }
        updateWord(firstWord, firstMask, operation);
        for (int word = firstWord + 1; word < lastWord; word++) updateWord(word, -1L, operation);
        updateWord(lastWord, lastMask, operation);
    }

    private void updateWord(int wordIndex, long mask, int operation) {
        long old = getWord(wordIndex);
        long updated = operation > 0 ? old | mask : operation == 0 ? old & ~mask : old ^ mask;
        if (updated == old) return;
        setWord(wordIndex, updated);
        bitCount += Long.bitCount(updated) - Long.bitCount(old);
    }

    @Override
    public int nextSetBit(int from) {
        ensureOpen();
        if (from < 0) from = 0;
        int wi = from >>> 6;
        if (wi >= wordCountLocal) return -1;
        long word = getWord(wi) & (~0L << (from & 63));
        while (true) {
            if (word != 0L) {
                int idx = (wi << 6) + Long.numberOfTrailingZeros(word);
                return (idx < logicalSizeLocal) ? idx : -1;
            }
            wi++;
            if (wi >= wordCountLocal) return -1;
            word = getWord(wi);
        }
    }

    @Override
    public int nextClearBit(int from) {
        ensureOpen();
        if (from < 0) throw new IndexOutOfBoundsException("from < 0: " + from);
        if (from >= logicalSizeLocal) return from;
        int wi = from >>> 6;
        if (wi >= wordCountLocal) return from;
        long word = ~getWord(wi) & (-1L << (from & 63));
        while (true) {
            if (word != 0L) {
                int idx = (wi << 6) + Long.numberOfTrailingZeros(word);
                return Math.min(idx, logicalSizeLocal);
            }
            wi++;
            if (wi >= wordCountLocal) return logicalSizeLocal;
            word = ~getWord(wi);
        }
    }

    @Override
    public int previousSetBit(int from) {
        ensureOpen();
        if (from < 0) return -1;
        if (from >= logicalSizeLocal) from = logicalSizeLocal - 1;
        if (from < 0) return -1;
        int wi = from >>> 6;
        long mask = ~0L >>> (63 - (from & 63));
        long word = getWord(wi) & mask;
        while (true) {
            if (word != 0L) return (wi << 6) + (63 - Long.numberOfLeadingZeros(word));
            wi--;
            if (wi < 0) return -1;
            word = getWord(wi);
        }
    }

    @Override
    public int previousClearBit(int from) {
        ensureOpen();
        if (from < 0) return -1;
        if (from >= logicalSizeLocal) from = logicalSizeLocal - 1;
        if (from < 0) return -1;
        int wi = from >>> 6;
        long mask = ~0L >>> (63 - (from & 63));
        long word = ~getWord(wi) & mask;
        while (true) {
            if (word != 0L) return (wi << 6) + (63 - Long.numberOfLeadingZeros(word));
            wi--;
            if (wi < 0) return -1;
            word = ~getWord(wi);
        }
    }

    @Override
    public boolean isEmpty() {
        ensureOpen();
        return bitCount == 0L;
    }

    @Override
    public long cardinality() {
        ensureOpen();
        return bitCount;
    }

    @Override
    public int length() {
        ensureOpen();
        if (logicalSizeLocal == 0) return 0;
        int maxWord = (logicalSizeLocal - 1) >>> 6;
        long mask = lastWordMask();
        for (int i = maxWord; i >= 0; i--) {
            long w = getWord(i);
            if (i == maxWord) w &= mask;
            if (w != 0L) return (i << 6) + (64 - Long.numberOfLeadingZeros(w));
        }
        return 0;
    }

    @Override
    public int size() {
        ensureOpen();
        return (int) Math.min(((long) wordCountLocal) << 6, Integer.MAX_VALUE);
    }

    @Override
    public int logicalSize() {
        ensureOpen();
        return logicalSizeLocal;
    }

    private long lastWordMask() {
        int r = logicalSizeLocal & 63;
        return r == 0 ? -1L : ((1L << r) - 1L);
    }

    @Override
    public long[] toLongArray() {
        ensureOpen();
        int len = length();
        if (len == 0) return new long[0];
        int used = (int) (((long) len + 63L) >>> 6);
        long[] out = new long[used];
        int last = used - 1;
        int rem = len & 63;
        long tailMask = rem == 0 ? -1L : ((1L << rem) - 1L);
        for (int i = 0; i < used; i++) {
            long v = getWord(i);
            if (i == last && rem != 0) v &= tailMask;
            out[i] = v;
        }
        return out;
    }

    @Override
    public @NonNull OffHeapBitSet clone() {
        ensureOpen();
        OffHeapBitSet b = new OffHeapBitSet(this.logicalSizeLocal);
        long bytes = ((long) this.wordCountLocal) << 3;
        if (bytes != 0L) U.copyMemory(null, this.addr, null, b.addr, bytes);
        b.bitCount = this.bitCount;
        return b;
    }
}
