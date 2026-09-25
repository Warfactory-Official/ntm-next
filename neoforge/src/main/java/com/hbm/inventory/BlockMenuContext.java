// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record BlockMenuContext(BlockPos pos, int dispatchId) {

    public static final StreamCodec<ByteBuf, BlockMenuContext> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    BlockMenuContext::pos,
                    ByteBufCodecs.VAR_INT,
                    BlockMenuContext::dispatchId,
                    BlockMenuContext::new);
}
