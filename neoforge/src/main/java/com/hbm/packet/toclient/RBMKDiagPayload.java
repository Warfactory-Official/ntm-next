// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toclient;

import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import com.hbm.tileentity.machine.rbmk.RBMKDiagnostics;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

public final class RBMKDiagPayload extends ThreadedPayload {

    public static final Type<RBMKDiagPayload> TYPE = new Type<>(Library.id("rbmk_diag"));
    public static final StreamCodec<ByteBuf, RBMKDiagPayload> STREAM_CODEC =
            streamCodec(
                    buf ->
                            new RBMKDiagPayload(
                                    BlockPos.of(buf.readLong()),
                                    ByteBufCodecs.COMPOUND_TAG.decode(buf)));

    private final BlockPos pos;
    private final CompoundTag diagnostics;

    public RBMKDiagPayload(BlockPos pos, CompoundTag diagnostics) {
        this.pos = pos;
        this.diagnostics = diagnostics;
    }

    public static void handle(RBMKDiagPayload payload, IPayloadHandlerContext context) {
        RBMKDiagnostics.received(payload.pos, payload.diagnostics);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(pos.asLong());
        ByteBufCodecs.COMPOUND_TAG.encode(buf, diagnostics);
    }

    @Override
    public @NotNull Type<RBMKDiagPayload> type() {
        return TYPE;
    }
}
