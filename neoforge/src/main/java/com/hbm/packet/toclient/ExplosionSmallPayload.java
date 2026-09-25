// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toclient;

import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

public final class ExplosionSmallPayload extends ThreadedPayload {

    public static final Type<ExplosionSmallPayload> TYPE =
            new Type<>(Library.id("explosion_small"));
    public static final StreamCodec<ByteBuf, ExplosionSmallPayload> STREAM_CODEC =
            streamCodec(ExplosionSmallPayload::decode);

    public static final double SPEED_OF_SOUND = 17.15D * 0.5D;

    final double x, y, z;
    final int cloudCount;
    final float cloudScale;
    final float cloudSpeedMult;
    final int debris;

    public ExplosionSmallPayload(
            double x,
            double y,
            double z,
            int cloudCount,
            float cloudScale,
            float cloudSpeedMult,
            int debris) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.cloudCount = cloudCount;
        this.cloudScale = cloudScale;
        this.cloudSpeedMult = cloudSpeedMult;
        this.debris = debris;
    }

    private static ExplosionSmallPayload decode(ByteBuf buf) {
        return new ExplosionSmallPayload(
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readInt(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readInt());
    }

    public static void handleClient(ExplosionSmallPayload payload, IPayloadHandlerContext ctx) {
        ClientParticlePayloadHandlers.handle(payload);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeInt(cloudCount);
        buf.writeFloat(cloudScale);
        buf.writeFloat(cloudSpeedMult);
        buf.writeInt(debris);
    }

    @Override
    public @NotNull Type<ExplosionSmallPayload> type() {
        return TYPE;
    }
}
