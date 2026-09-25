// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toclient;

import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

public final class SparkBurstPayload extends ThreadedPayload {

    public static final Type<SparkBurstPayload> TYPE = new Type<>(Library.id("spark_burst"));
    public static final StreamCodec<ByteBuf, SparkBurstPayload> STREAM_CODEC =
            streamCodec(SparkBurstPayload::decode);

    final double x, y, z;
    final byte count;
    final boolean small;

    public SparkBurstPayload(double x, double y, double z, int count, boolean small) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.count = (byte) count;
        this.small = small;
    }

    private static SparkBurstPayload decode(ByteBuf buf) {
        return new SparkBurstPayload(
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readByte(),
                buf.readBoolean());
    }

    public static void handleClient(SparkBurstPayload payload, IPayloadHandlerContext ctx) {
        ClientParticlePayloadHandlers.handle(payload);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeByte(count);
        buf.writeBoolean(small);
    }

    @Override
    public @NotNull Type<SparkBurstPayload> type() {
        return TYPE;
    }
}
