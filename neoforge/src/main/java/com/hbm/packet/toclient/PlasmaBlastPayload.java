// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toclient;

import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

public final class PlasmaBlastPayload extends ThreadedPayload {

    public static final Type<PlasmaBlastPayload> TYPE = new Type<>(Library.id("plasma_blast"));
    public static final StreamCodec<ByteBuf, PlasmaBlastPayload> STREAM_CODEC =
            streamCodec(PlasmaBlastPayload::decode);

    final double x, y, z;
    final float r, g, b;
    final float pitch, yaw;
    final float scale;

    public PlasmaBlastPayload(
            double x,
            double y,
            double z,
            float r,
            float g,
            float b,
            float pitch,
            float yaw,
            float scale) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.r = r;
        this.g = g;
        this.b = b;
        this.pitch = pitch;
        this.yaw = yaw;
        this.scale = scale;
    }

    private static PlasmaBlastPayload decode(ByteBuf buf) {
        return new PlasmaBlastPayload(
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat());
    }

    public static void handleClient(PlasmaBlastPayload payload, IPayloadHandlerContext ctx) {
        ClientParticlePayloadHandlers.handle(payload);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeFloat(r);
        buf.writeFloat(g);
        buf.writeFloat(b);
        buf.writeFloat(pitch);
        buf.writeFloat(yaw);
        buf.writeFloat(scale);
    }

    @Override
    public @NotNull Type<PlasmaBlastPayload> type() {
        return TYPE;
    }
}
