// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.compat;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.buffer.UnpooledDirectByteBuf;
import io.netty.util.Recycler;

public final class DecodeBuffers extends UnpooledDirectByteBuf {
    private static final Recycler<DecodeBuffers>[] POOLS = pools();
    private final Recycler.Handle<DecodeBuffers> handle;

    private DecodeBuffers(Recycler.Handle<DecodeBuffers> handle, int capacity) {
        super(UnpooledByteBufAllocator.DEFAULT, capacity, capacity);
        this.handle = handle;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Recycler<DecodeBuffers>[] pools() {
        Recycler<DecodeBuffers>[] pools = new Recycler[13];
        for (int i = 0; i < pools.length; i++) {
            int capacity = 256 << i;
            pools[i] =
                    new Recycler<>(8, false) {
                        @Override
                        protected DecodeBuffers newObject(Handle<DecodeBuffers> handle) {
                            return new DecodeBuffers(handle, capacity);
                        }
                    };
        }
        return pools;
    }

    public static ByteBuf acquire(int size) {
        if (size <= 0 || size > 1_048_576)
            return PooledByteBufAllocator.DEFAULT.directBuffer(size, size);
        int shift = 32 - Integer.numberOfLeadingZeros(Math.max(255, size - 1));
        DecodeBuffers buffer = POOLS[shift - 8].get();
        buffer.setRefCnt(1);
        buffer.capacity(buffer.maxCapacity());
        buffer.clear();
        return buffer;
    }

    @Override
    protected void deallocate() {
        handle.recycle(this);
    }
}
