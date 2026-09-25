// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toclient;

import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

public final class AshesPayload extends ThreadedPayload {

    public static final Type<AshesPayload> TYPE = new Type<>(Library.id("ashes"));
    public static final StreamCodec<ByteBuf, AshesPayload> STREAM_CODEC =
            streamCodec(AshesPayload::decode);

    final double x, y, z;
    final int entityId, count;
    final float scale;

    public AshesPayload(double x, double y, double z, int entityId, int count, float scale) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.entityId = entityId;
        this.count = count;
        this.scale = scale;
    }

    private static AshesPayload decode(ByteBuf buf) {
        return new AshesPayload(
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readInt(),
                buf.readInt(),
                buf.readFloat());
    }

    public static void handleClient(AshesPayload payload, IPayloadHandlerContext ctx) {
        ClientParticlePayloadHandlers.handle(payload);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeInt(entityId);
        buf.writeInt(count);
        buf.writeFloat(scale);
    }

    @Override
    public @NotNull Type<AshesPayload> type() {
        return TYPE;
    }
}
