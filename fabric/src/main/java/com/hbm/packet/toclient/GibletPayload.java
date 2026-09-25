// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toclient;

import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

public final class GibletPayload extends ThreadedPayload {

    public static final Type<GibletPayload> TYPE = new Type<>(Library.id("giblets"));
    public static final StreamCodec<ByteBuf, GibletPayload> STREAM_CODEC =
            streamCodec(GibletPayload::decode);

    final double x, y, z;
    final int entityId, gibType, countDivisor;

    public GibletPayload(
            double x, double y, double z, int entityId, int gibType, int countDivisor) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.entityId = entityId;
        this.gibType = gibType;
        this.countDivisor = countDivisor;
    }

    private static GibletPayload decode(ByteBuf buf) {
        return new GibletPayload(
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt());
    }

    public static void handleClient(GibletPayload payload, IPayloadHandlerContext ctx) {
        ClientParticlePayloadHandlers.handle(payload);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeInt(entityId);
        buf.writeInt(gibType);
        buf.writeInt(countDivisor);
    }

    @Override
    public @NotNull Type<GibletPayload> type() {
        return TYPE;
    }
}
