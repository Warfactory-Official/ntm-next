// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toclient;

import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

public final class HazePayload extends ThreadedPayload {

    public static final Type<HazePayload> TYPE = new Type<>(Library.id("haze"));
    public static final StreamCodec<ByteBuf, HazePayload> STREAM_CODEC =
            streamCodec(HazePayload::decode);

    final double x, y, z;

    public HazePayload(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    private static HazePayload decode(ByteBuf buf) {
        return new HazePayload(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    public static void handleClient(HazePayload payload, IPayloadHandlerContext ctx) {
        ClientParticlePayloadHandlers.handle(payload);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
    }

    @Override
    public @NotNull Type<HazePayload> type() {
        return TYPE;
    }
}
