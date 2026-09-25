// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toclient;

import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

public final class SmokeShockPayload extends ThreadedPayload {

    public static final Type<SmokeShockPayload> TYPE = new Type<>(Library.id("smoke_shock"));
    public static final StreamCodec<ByteBuf, SmokeShockPayload> STREAM_CODEC =
            streamCodec(SmokeShockPayload::decode);

    public static final byte MODE_SHOCK = 0;
    public static final byte MODE_CLOUD = 1;
    public static final byte MODE_BURST = 2;
    public static final byte MODE_RADIAL = 3;
    public static final byte MODE_FOAM = 4;

    final double x, y, z;
    final int count;
    final double strength;
    final byte mode;

    public SmokeShockPayload(double x, double y, double z, int count, double strength, byte mode) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.count = count;
        this.strength = strength;
        this.mode = mode;
    }

    private static SmokeShockPayload decode(ByteBuf buf) {
        return new SmokeShockPayload(
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readInt(),
                buf.readDouble(),
                buf.readByte());
    }

    public static void handleClient(SmokeShockPayload payload, IPayloadHandlerContext ctx) {
        ClientParticlePayloadHandlers.handle(payload);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeInt(count);
        buf.writeDouble(strength);
        buf.writeByte(mode);
    }

    @Override
    public @NotNull Type<SmokeShockPayload> type() {
        return TYPE;
    }
}
