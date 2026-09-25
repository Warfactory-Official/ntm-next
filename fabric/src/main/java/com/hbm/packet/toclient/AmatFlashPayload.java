// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toclient;

import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

public final class AmatFlashPayload extends ThreadedPayload {

    public static final Type<AmatFlashPayload> TYPE = new Type<>(Library.id("amat_flash"));
    public static final StreamCodec<ByteBuf, AmatFlashPayload> STREAM_CODEC =
            streamCodec(AmatFlashPayload::decode);

    final double x, y, z;
    final float scale;

    public AmatFlashPayload(double x, double y, double z, float scale) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.scale = scale;
    }

    private static AmatFlashPayload decode(ByteBuf buf) {
        return new AmatFlashPayload(
                buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat());
    }

    public static void handleClient(AmatFlashPayload payload, IPayloadHandlerContext ctx) {
        ClientParticlePayloadHandlers.handle(payload);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeFloat(scale);
    }

    @Override
    public @NotNull Type<AmatFlashPayload> type() {
        return TYPE;
    }
}
