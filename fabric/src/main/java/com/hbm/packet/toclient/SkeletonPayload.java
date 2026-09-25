// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toclient;

import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

public final class SkeletonPayload extends ThreadedPayload {

    public static final Type<SkeletonPayload> TYPE = new Type<>(Library.id("skeleton"));
    public static final StreamCodec<ByteBuf, SkeletonPayload> STREAM_CODEC =
            streamCodec(SkeletonPayload::decode);

    final double x, y, z;
    final int entityId;
    final float brightness, force;
    final boolean gib;

    private SkeletonPayload(
            double x,
            double y,
            double z,
            int entityId,
            float brightness,
            float force,
            boolean gib) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.entityId = entityId;
        this.brightness = brightness;
        this.force = force;
        this.gib = gib;
    }

    public static SkeletonPayload skeletonize(
            double x, double y, double z, int entityId, float brightness) {
        return new SkeletonPayload(x, y, z, entityId, brightness, 0F, false);
    }

    public static SkeletonPayload gib(double x, double y, double z, int entityId, float force) {
        return new SkeletonPayload(x, y, z, entityId, 1F, force, true);
    }

    private static SkeletonPayload decode(ByteBuf buf) {
        return new SkeletonPayload(
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readInt(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readBoolean());
    }

    public static void handleClient(SkeletonPayload payload, IPayloadHandlerContext ctx) {
        ClientParticlePayloadHandlers.handle(payload);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeInt(entityId);
        buf.writeFloat(brightness);
        buf.writeFloat(force);
        buf.writeBoolean(gib);
    }

    @Override
    public @NotNull Type<SkeletonPayload> type() {
        return TYPE;
    }
}
