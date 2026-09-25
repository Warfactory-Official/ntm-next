// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toclient;

import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

public final class MukePayload extends ThreadedPayload {

    public static final Type<MukePayload> TYPE = new Type<>(Library.id("muke"));
    public static final StreamCodec<ByteBuf, MukePayload> STREAM_CODEC =
            streamCodec(MukePayload::decode);

    final double x, y, z;
    final boolean tinytot;
    final boolean balefire;

    public MukePayload(double x, double y, double z, boolean tinytot, boolean balefire) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.tinytot = tinytot;
        this.balefire = balefire;
    }

    private static MukePayload decode(ByteBuf buf) {
        return new MukePayload(
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readBoolean(),
                buf.readBoolean());
    }

    public static void handleClient(MukePayload payload, IPayloadHandlerContext ctx) {
        ClientParticlePayloadHandlers.handle(payload);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeBoolean(tinytot);
        buf.writeBoolean(balefire);
    }

    @Override
    public @NotNull Type<MukePayload> type() {
        return TYPE;
    }
}
