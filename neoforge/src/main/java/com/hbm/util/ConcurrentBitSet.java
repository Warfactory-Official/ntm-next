// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.util;

import com.hbm.interfaces.BitMask;
import java.nio.ByteBuffer;
import org.jspecify.annotations.NonNull;

import static com.hbm.lib.internal.UnsafeHolder.U;
import static com.hbm.lib.internal.UnsafeHolder.offLong;

public class ConcurrentBitSet implements BitMask, Cloneable {

    private final long[] words;
    private final int wordCount;
    private final int logicalSize;

    public ConcurrentBitSet(int logicalSize) {
        if (logicalSize < 0)
            throw new NegativeArraySizeException("logicalSize < 0: " + logicalSize);
        this.logicalSize = logicalSize;
        this.wordCount = (int) (((long) logicalSize + 63L) >>> 6);
        this.words = new long[wordCount];
    }

    public ConcurrentBitSet(@NonNull ConcurrentBitSet other) {
        this.logicalSize = other.logicalSize;
        this.wordCount = other.wordCount;
        this.words = new long[wordCount];
        for (int i = 0; i < wordCount; i++) {
            this.words[i] = U.getLongVolatile(other.words, offLong(i));
        }
    }

    public static @NonNull ConcurrentBitSet fromLongArray(long[] data, int logicalSize) {
        ConcurrentBitSet bitSet = new ConcurrentBitSet(logicalSize);
        if (logicalSize == 0) return bitSet;

        int wordsToCopy = Math.min(data.length, bitSet.wordCount);
        int lastIdx = (logicalSize - 1) >>> 6;
        int rem = logicalSize & 63;
        long tailMask = rem == 0 ? -1L : ((1L << rem) - 1L);

        for (int i = 0; i < wordsToCopy; i++) {
            long w = data[i];
            if (i > lastIdx) w = 0L;
            else if (i == lastIdx && rem != 0) w &= tailMask;
            bitSet.words[i] = w;
        }
        return bitSet;
    }

    public static @NonNull ConcurrentBitSet valueOf(long @NonNull [] data) {
        int len = 0;
        for (int i = data.length - 1; i >= 0; i--) {
            long w = data[i];
            if (w != 0) {
                long computedLength = (((long) i) << 6) + (64 - Long.numberOfLeadingZeros(w));
                if (computedLength > Integer.MAX_VALUE) {
                    throw new IllegalArgumentException(
                            "bit set length exceeds Integer.MAX_VALUE: " + computedLength);
                }
                len = (int) computedLength;
                break;
            }
        }
        return fromLongArray(data, len);
    }

    public static ConcurrentBitSet fromWords(@NonNull ByteBuffer buf, int logicalSize) {
        ConcurrentBitSet bitSet = new ConcurrentBitSet(logicalSize);
        int wc = bitSet.wordCount;
        int need = wc << 3;
        if (buf.remaining() < need) {
            throw new IllegalArgumentException(
                    "Buffer underflow: need " + need + " bytes for words, have " + buf.remaining());
        }
        if (wc == 0) return bitSet;
        int last = wc - 1;
        long tailMask = bitSet.lastWordMask();
        for (int i = 0; i < last; i++) {
            bitSet.words[i] = buf.getLong();
        }
        long wLast = buf.getLong() & tailMask;
        bitSet.words[last] = wLast;
        return bitSet;
    }

    public void toWords(@NonNull ByteBuffer buf) {
        int need = wordCount << 3;
        if (buf.remaining() < need) {
            throw new IllegalArgumentException(
                    "Buffer too small: need " + need + " bytes remaining, have " + buf.remaining());
        }
        if (wordCount == 0) return;

        int last = wordCount - 1;
        long tailMask = lastWordMask();

        for (int i = 0; i < last; i++) {
            buf.putLong(U.getLongVolatile(words, offLong(i)));
        }
        buf.putLong(U.getLongVolatile(words, offLong(last)) & tailMask);
    }

    @Override
    public boolean get(int bit) {
        if (bit < 0) throw new IndexOutOfBoundsException("bit < 0: " + bit);
        if (bit >= logicalSize) return false;
        int wordIndex = bit >>> 6;
        long mask = 1L << (bit & 63);
        long word = U.getLongVolatile(words, offLong(wordIndex));
        return (word & mask) != 0;
    }

    @Override
    public void set(int bit) {
        if (bit < 0 || bit >= logicalSize) return;
        int wordIndex = bit >>> 6;
        long offset = offLong(wordIndex);
        long mask = 1L << (bit & 63);
        while (true) {
            long oldWord = U.getLongVolatile(words, offset);
            long newWord = oldWord | mask;
            if (oldWord == newWord) return;
            if (U.compareAndSetLong(words, offset, oldWord, newWord)) {
                return;
            }
        }
    }

    @Override
    public boolean getAndSet(int bit) {
        if (bit < 0 || bit >= logicalSize)
            throw new IndexOutOfBoundsException("bit index out of bounds: " + bit);
        int wordIndex = bit >>> 6;
        long offset = offLong(wordIndex);
        long mask = 1L << (bit & 63);
        while (true) {
            long oldWord = U.getLongVolatile(words, offset);
            if ((oldWord & mask) != 0) return true;
            long newWord = oldWord | mask;
            if (U.compareAndSetLong(words, offset, oldWord, newWord)) {
                return false;
            }
        }
    }

    public void set(int fromIndex, int toIndex) {
        if (fromIndex < 0 || toIndex > logicalSize || fromIndex > toIndex)
            throw new IndexOutOfBoundsException();
        if (fromIndex == toIndex) return;
        int startWord = fromIndex >>> 6;
        int endWord = (toIndex - 1) >>> 6;
        long startMask = -1L << (fromIndex & 63);
        long endMask = (toIndex & 63) == 0 ? -1L : ((1L << (toIndex & 63)) - 1L);
        if (startWord == endWord) {
            setBits(startWord, startMask & endMask);
        } else {
            setBits(startWord, startMask);
            for (int i = startWord + 1; i < endWord; i++) setBits(i, -1L);
            if (endMask != 0) setBits(endWord, endMask);
        }
    }

    private void setBits(int wi, long mask) {
        if (mask == 0) return;
        long offset = offLong(wi);
        while (true) {
            long oldWord = U.getLongVolatile(words, offset);
            long newWord = oldWord | mask;
            if (oldWord == newWord) return;
            if (U.compareAndSetLong(words, offset, oldWord, newWord)) {
                return;
            }
        }
    }

    public void clear(int bit) {
        if (bit < 0 || bit >= logicalSize) return;
        int wordIndex = bit >>> 6;
        long offset = offLong(wordIndex);
        long mask = ~(1L << (bit & 63));
        while (true) {
            long oldWord = U.getLongVolatile(words, offset);
            long newWord = oldWord & mask;
            if (oldWord == newWord) return;
            if (U.compareAndSetLong(words, offset, oldWord, newWord)) {
                return;
            }
        }
    }

    public boolean getAndClear(int bit) {
        if (bit < 0 || bit >= logicalSize)
            throw new IndexOutOfBoundsException("bit index out of bounds: " + bit);
        int wordIndex = bit >>> 6;
        long offset = offLong(wordIndex);
        long mask = 1L << (bit & 63);
        while (true) {
            long oldWord = U.getLongVolatile(words, offset);
            if ((oldWord & mask) == 0) return false;
            long newWord = oldWord & ~mask;
            if (U.compareAndSetLong(words, offset, oldWord, newWord)) {
                return true;
            }
        }
    }

    public void clear(int fromIndex, int toIndex) {
        if (fromIndex < 0 || toIndex > logicalSize || fromIndex > toIndex)
            throw new IndexOutOfBoundsException();
        if (fromIndex == toIndex) return;
        int startWord = fromIndex >>> 6;
        int endWord = (toIndex - 1) >>> 6;
        long startMask = -1L << (fromIndex & 63);
        long endMask = (toIndex & 63) == 0 ? -1L : ((1L << (toIndex & 63)) - 1L);
        if (startWord == endWord) {
            clearBits(startWord, startMask & endMask);
        } else {
            clearBits(startWord, startMask);
            for (int i = startWord + 1; i < endWord; i++) clearBits(i, -1L);
            if (endMask != 0) clearBits(endWord, endMask);
        }
    }

    private void clearBits(int wi, long mask) {
        if (mask == 0) return;
        long offset = offLong(wi);
        while (true) {
            long oldWord = U.getLongVolatile(words, offset);
            long newWord = oldWord & ~mask;
            if (oldWord == newWord) return;
            if (U.compareAndSetLong(words, offset, oldWord, newWord)) {
                return;
            }
        }
    }

    public void clear() {
        clear(0, logicalSize);
    }

    public void flip(int bit) {
        if (bit < 0 || bit >= logicalSize) return;
        int wordIndex = bit >>> 6;
        long offset = offLong(wordIndex);
        long mask = 1L << (bit & 63);
        while (true) {
            long oldWord = U.getLongVolatile(words, offset);
            long newWord = oldWord ^ mask;
            if (U.compareAndSetLong(words, offset, oldWord, newWord)) {
                return;
            }
        }
    }

    public boolean getAndFlip(int bit) {
        if (bit < 0 || bit >= logicalSize)
            throw new IndexOutOfBoundsException("bit index out of bounds: " + bit);
        int wordIndex = bit >>> 6;
        long offset = offLong(wordIndex);
        long mask = 1L << (bit & 63);
        while (true) {
            long oldWord = U.getLongVolatile(words, offset);
            boolean wasSet = (oldWord & mask) != 0;
            long newWord = oldWord ^ mask;
            if (U.compareAndSetLong(words, offset, oldWord, newWord)) {
                return wasSet;
            }
        }
    }

    public void flip(int fromIndex, int toIndex) {
        if (fromIndex < 0 || toIndex > logicalSize || fromIndex > toIndex)
            throw new IndexOutOfBoundsException();
        if (fromIndex == toIndex) return;
        int startWord = fromIndex >>> 6;
        int endWord = (toIndex - 1) >>> 6;
        long startMask = -1L << (fromIndex & 63);
        long endMask = (toIndex & 63) == 0 ? -1L : ((1L << (toIndex & 63)) - 1L);
        if (startWord == endWord) {
            flipBits(startWord, startMask & endMask);
        } else {
            flipBits(startWord, startMask);
            for (int i = startWord + 1; i < endWord; i++) flipBits(i, -1L);
            if (endMask != 0) flipBits(endWord, endMask);
        }
    }

    private void flipBits(int wi, long mask) {
        if (mask == 0) return;
        long offset = offLong(wi);
        while (true) {
            long oldWord = U.getLongVolatile(words, offset);
            long newWord = oldWord ^ mask;
            if (U.compareAndSetLong(words, offset, oldWord, newWord)) {
                return;
            }
        }
    }

    @Override
    public int nextSetBit(int from) {
        if (from < 0) from = 0;
        int wordIndex = from >>> 6;
        if (wordIndex >= wordCount) return -1;
        long word = U.getLongVolatile(words, offLong(wordIndex)) & (~0L << (from & 63));
        while (true) {
            if (word != 0) {
                int idx = (wordIndex << 6) + Long.numberOfTrailingZeros(word);
                return (idx < logicalSize) ? idx : -1;
            }
            wordIndex++;
            if (wordIndex >= wordCount) return -1;
            word = U.getLongVolatile(words, offLong(wordIndex));
        }
    }

    @Override
    public int nextClearBit(int from) {
        if (from < 0) throw new IndexOutOfBoundsException("from < 0: " + from);
        if (from >= logicalSize) return from;
        int wordIndex = from >>> 6;
        if (wordIndex >= wordCount) return from;
        long word = ~U.getLongVolatile(words, offLong(wordIndex)) & (-1L << (from & 63));
        while (true) {
            if (word != 0) {
                int idx = (wordIndex << 6) + Long.numberOfTrailingZeros(word);
                return Math.min(idx, logicalSize);
            }
            wordIndex++;
            if (wordIndex >= wordCount) return logicalSize;
            word = ~U.getLongVolatile(words, offLong(wordIndex));
        }
    }

    @Override
    public int previousSetBit(int from) {
        if (from < 0) return -1;
        if (from >= logicalSize) from = logicalSize - 1;
        if (from < 0) return -1;
        int wordIndex = from >>> 6;
        long mask = ~0L >>> (63 - (from & 63));
        long word = U.getLongVolatile(words, offLong(wordIndex)) & mask;
        while (true) {
            if (word != 0) return (wordIndex << 6) + (63 - Long.numberOfLeadingZeros(word));
            wordIndex--;
            if (wordIndex < 0) return -1;
            word = U.getLongVolatile(words, offLong(wordIndex));
        }
    }

    @Override
    public int previousClearBit(int from) {
        if (from < 0) return -1;
        if (from >= logicalSize) from = logicalSize - 1;
        if (from < 0) return -1;
        int wordIndex = from >>> 6;
        long mask = ~0L >>> (63 - (from & 63));
        long word = ~U.getLongVolatile(words, offLong(wordIndex)) & mask;
        while (true) {
            if (word != 0) return (wordIndex << 6) + (63 - Long.numberOfLeadingZeros(word));
            wordIndex--;
            if (wordIndex < 0) return -1;
            word = ~U.getLongVolatile(words, offLong(wordIndex));
        }
    }

    private void requireSameSize(@NonNull ConcurrentBitSet set) {
        if (this.logicalSize != set.logicalSize) {
            throw new IllegalArgumentException(
                    "Expected size: " + logicalSize + ", actual size: " + set.logicalSize);
        }
    }

    @Override
    public boolean isEmpty() {
        return isEmptyExact();
    }

    public boolean isEmptyExact() {
        if (wordCount == 0) return true;
        int last = wordCount - 1;
        long tailMask = lastWordMask();

        for (int i = 0; i < last; i++) {
            if (U.getLongVolatile(words, offLong(i)) != 0L) return false;
        }
        return (U.getLongVolatile(words, offLong(last)) & tailMask) == 0L;
    }

    @Override
    public long cardinality() {
        return cardinalityExact();
    }

    public long cardinalityExact() {
        if (wordCount == 0) return 0L;
        int last = wordCount - 1;
        long tailMask = lastWordMask();

        long total = 0L;
        for (int i = 0; i < last; i++) {
            total += Long.bitCount(U.getLongVolatile(words, offLong(i)));
        }
        total += Long.bitCount(U.getLongVolatile(words, offLong(last)) & tailMask);
        return total;
    }

    public boolean isFullExact() {
        if (logicalSize == 0) return true;
        int last = wordCount - 1;
        if ((logicalSize & 63) == 0) {
            for (int i = 0; i <= last; i++) {
                if (U.getLongVolatile(words, offLong(i)) != -1L) return false;
            }
            return true;
        }
        for (int i = 0; i < last; i++) {
            if (U.getLongVolatile(words, offLong(i)) != -1L) return false;
        }
        long tailMask = lastWordMask();
        long wLast = U.getLongVolatile(words, offLong(last)) & tailMask;
        return wLast == tailMask;
    }

    @Override
    public int length() {
        if (logicalSize == 0) return 0;
        int maxWord = (logicalSize - 1) >>> 6;
        long mask = lastWordMask();
        for (int i = maxWord; i >= 0; i--) {
            long w = U.getLongVolatile(words, offLong(i));
            if (i == maxWord) w &= mask;
            if (w != 0L) {
                return (i << 6) + (64 - Long.numberOfLeadingZeros(w));
            }
        }
        return 0;
    }

    @Override
    public int size() {
        return (int) Math.min(((long) wordCount) << 6, Integer.MAX_VALUE);
    }

    @Override
    public int logicalSize() {
        return logicalSize;
    }

    public boolean intersects(@NonNull ConcurrentBitSet set) {
        requireSameSize(set);
        for (int i = 0; i < wordCount; i++) {
            long a = U.getLongVolatile(words, offLong(i));
            long b = U.getLongVolatile(set.words, offLong(i));
            if ((a & b) != 0) return true;
        }
        return false;
    }

    public void and(@NonNull ConcurrentBitSet set) {
        requireSameSize(set);
        for (int i = 0; i < wordCount; i++) {
            long offset = offLong(i);
            long other = U.getLongVolatile(set.words, offset);
            while (true) {
                long oldWord = U.getLongVolatile(words, offset);
                long newWord = oldWord & other;
                if (oldWord == newWord) break;
                if (U.compareAndSetLong(words, offset, oldWord, newWord)) {
                    break;
                }
            }
        }
    }

    public void or(@NonNull ConcurrentBitSet set) {
        requireSameSize(set);
        for (int i = 0; i < wordCount; i++) {
            long offset = offLong(i);
            long other = U.getLongVolatile(set.words, offset);
            while (true) {
                long oldWord = U.getLongVolatile(words, offset);
                long newWord = oldWord | other;
                if (oldWord == newWord) break;
                if (U.compareAndSetLong(words, offset, oldWord, newWord)) {
                    break;
                }
            }
        }
    }

    public void xor(@NonNull ConcurrentBitSet set) {
        requireSameSize(set);
        for (int i = 0; i < wordCount; i++) {
            long offset = offLong(i);
            long otherWord = U.getLongVolatile(set.words, offset);
            while (true) {
                long oldWord = U.getLongVolatile(words, offset);
                long newWord = oldWord ^ otherWord;
                if (oldWord == newWord) break;
                if (U.compareAndSetLong(words, offset, oldWord, newWord)) {
                    break;
                }
            }
        }
    }

    public void andNot(@NonNull ConcurrentBitSet set) {
        requireSameSize(set);
        for (int i = 0; i < wordCount; i++) {
            long offset = offLong(i);
            long otherWord = U.getLongVolatile(set.words, offset);
            while (true) {
                long oldWord = U.getLongVolatile(words, offset);
                long newWord = oldWord & ~otherWord;
                if (oldWord == newWord) break;
                if (U.compareAndSetLong(words, offset, oldWord, newWord)) {
                    break;
                }
            }
        }
    }

    @Override
    public long[] toLongArray() {
        int len = length();
        if (len == 0) return new long[0];

        int used = (int) (((long) len + 63L) >>> 6);
        long[] out = new long[used];
        int last = used - 1;
        int rem = len & 63;
        long tailMask = rem == 0 ? -1L : ((1L << rem) - 1L);

        for (int i = 0; i < used; i++) {
            long v = U.getLongVolatile(words, offLong(i));
            if (i == last && rem != 0) v &= tailMask;
            out[i] = v;
        }
        return out;
    }

    public void toLongArray(long @NonNull [] out) {
        if (out.length != wordCount) {
            throw new IllegalArgumentException(
                    "Expected long[" + wordCount + "], got long[" + out.length + "]");
        }
        if (wordCount == 0) return;
        int last = wordCount - 1;
        long tailMask = lastWordMask();
        for (int i = 0; i < last; i++) {
            out[i] = U.getLongVolatile(words, offLong(i));
        }
        out[last] = U.getLongVolatile(words, offLong(last)) & tailMask;
    }

    private long lastWordMask() {
        int r = logicalSize & 63;
        return r == 0 ? -1L : ((1L << r) - 1L);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConcurrentBitSet other)) return false;
        if (logicalSize != other.logicalSize) return false;
        for (int i = 0; i < wordCount; i++) {
            if (U.getLongVolatile(words, offLong(i)) != U.getLongVolatile(other.words, offLong(i)))
                return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        long h = 1234;
        int used = (int) (((long) length() + 63L) >>> 6);
        for (int i = used - 1; i >= 0; i--) {
            h ^= U.getLongVolatile(words, offLong(i)) * (i + 1L);
        }
        return Long.hashCode(h);
    }

    @Override
    public @NonNull String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        int i = nextSetBit(0);
        if (i != -1) {
            sb.append(i);
            while (true) {
                i = nextSetBit(i + 1);
                if (i == -1) break;
                sb.append(", ").append(i);
            }
        }
        sb.append('}');
        return sb.toString();
    }

    @Override
    public @NonNull ConcurrentBitSet clone() {
        return new ConcurrentBitSet(this);
    }
}
