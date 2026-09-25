// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.compat;

import io.netty.buffer.ByteBuf;

public interface NebCodec {
    void hbm$compress(ByteBuf input, ByteBuf output);

    ByteBuf hbm$decompress(ByteBuf input, int size);
}
