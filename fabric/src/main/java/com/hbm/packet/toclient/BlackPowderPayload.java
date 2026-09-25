// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toclient;

import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

public final class BlackPowderPayload extends ThreadedPayload {

    public static final Type<BlackPowderPayload> TYPE = new Type<>(Library.id("black_powder"));
    public static final StreamCodec<ByteBuf, BlackPowderPayload> STREAM_CODEC =
            streamCodec(BlackPowderPayload::decode);

    final double x, y, z;
    final double hx, hy, hz;
    final int cloudCount, sparkCount;
    final float cloudScale, cloudSpeedMult, sparkSpeedMult;

    public BlackPowderPayload(
            double x,
            double y,
            double z,
            double hx,
            double hy,
            double hz,
            int cloudCount,
            float cloudScale,
            float cloudSpeedMult,
            int sparkCount,
            float sparkSpeedMult) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.hx = hx;
        this.hy = hy;
        this.hz = hz;
        this.cloudCount = cloudCount;
        this.cloudScale = cloudScale;
        this.cloudSpeedMult = cloudSpeedMult;
        this.sparkCount = sparkCount;
        this.sparkSpeedMult = sparkSpeedMult;
    }

    private static BlackPowderPayload decode(ByteBuf buf) {
        return new BlackPowderPayload(
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readInt(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readInt(),
                buf.readFloat());
    }

    public static void handleClient(BlackPowderPayload payload, IPayloadHandlerContext ctx) {
        ClientParticlePayloadHandlers.handle(payload);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeDouble(hx);
        buf.writeDouble(hy);
        buf.writeDouble(hz);
        buf.writeInt(cloudCount);
        buf.writeFloat(cloudScale);
        buf.writeFloat(cloudSpeedMult);
        buf.writeInt(sparkCount);
        buf.writeFloat(sparkSpeedMult);
    }

    @Override
    public @NotNull Type<BlackPowderPayload> type() {
        return TYPE;
    }
}
