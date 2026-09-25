// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.util;

import com.hbm.lib.internal.UnsafeHolder;
import io.netty.buffer.ByteBuf;

public final class HashUtil {

    private static final long FNV_OFFSET = 0xcbf29ce484222325L;
    private static final long FNV_PRIME = 0x100000001b3L;

    private HashUtil() {}

    public static long fnv1a64(ByteBuf buf) {
        long hash = FNV_OFFSET;
        int len = buf.readableBytes();
        if (buf.hasMemoryAddress()) {
            long addr = buf.memoryAddress() + buf.readerIndex();
            long end = addr + len;
            for (; addr < end; addr++) {
                hash ^= (UnsafeHolder.U.getByte(addr) & 0xffL);
                hash *= FNV_PRIME;
            }
        } else if (buf.hasArray()) {
            byte[] arr = buf.array();
            long offset = UnsafeHolder.BA_BASE + buf.arrayOffset() + buf.readerIndex();
            long end = offset + len;
            for (; offset < end; offset++) {
                hash ^= (UnsafeHolder.U.getByte(arr, offset) & 0xffL);
                hash *= FNV_PRIME;
            }
        } else {
            int start = buf.readerIndex();
            for (int i = 0; i < len; i++) {
                hash ^= (buf.getByte(start + i) & 0xffL);
                hash *= FNV_PRIME;
            }
        }
        return hash;
    }
}
