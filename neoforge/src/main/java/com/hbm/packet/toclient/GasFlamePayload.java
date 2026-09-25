// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toclient;

import com.hbm.client.ClientEffects;
import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

public final class GasFlamePayload extends ThreadedPayload {

    public static final Type<GasFlamePayload> TYPE = new Type<>(Library.id("gas_flame"));
    public static final StreamCodec<ByteBuf, GasFlamePayload> STREAM_CODEC =
            streamCodec(GasFlamePayload::decode);

    public static final float DEFAULT_SCALE = 6.5F;

    private final double x;
    private final double y;
    private final double z;
    private final double motionX;
    private final double motionY;
    private final double motionZ;
    private final float scale;

    public GasFlamePayload(
            double x,
            double y,
            double z,
            double motionX,
            double motionY,
            double motionZ,
            float scale) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.motionX = motionX;
        this.motionY = motionY;
        this.motionZ = motionZ;
        this.scale = scale;
    }

    private static GasFlamePayload decode(ByteBuf buf) {
        return new GasFlamePayload(
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readFloat());
    }

    public static void handleClient(GasFlamePayload payload, IPayloadHandlerContext ctx) {
        var player = ctx.playerOrNull();
        if (player == null) return;
        ClientEffects.spawnGasFlame(
                player.level(),
                payload.x,
                payload.y,
                payload.z,
                payload.motionX,
                payload.motionY,
                payload.motionZ,
                payload.scale);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeDouble(motionX);
        buf.writeDouble(motionY);
        buf.writeDouble(motionZ);
        buf.writeFloat(scale);
    }

    @Override
    public @NotNull Type<GasFlamePayload> type() {
        return TYPE;
    }
}
