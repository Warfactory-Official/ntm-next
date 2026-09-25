// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toclient;

import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

public final class RbmkJetPayload extends ThreadedPayload {

    public static final Type<RbmkJetPayload> TYPE = new Type<>(Library.id("rbmk_jet"));
    public static final StreamCodec<ByteBuf, RbmkJetPayload> STREAM_CODEC =
            streamCodec(RbmkJetPayload::decode);

    final double x, y, z;
    final int maxAge;

    private RbmkJetPayload(double x, double y, double z, int maxAge) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.maxAge = maxAge;
    }

    public static RbmkJetPayload flame(double x, double y, double z, int maxAge) {
        return new RbmkJetPayload(x, y, z, maxAge);
    }

    public static RbmkJetPayload steam(double x, double y, double z) {
        return new RbmkJetPayload(x, y, z, 0);
    }

    private static RbmkJetPayload decode(ByteBuf buf) {
        return new RbmkJetPayload(
                buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readInt());
    }

    public static void handleClient(RbmkJetPayload payload, IPayloadHandlerContext ctx) {
        ClientParticlePayloadHandlers.handle(payload);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeInt(maxAge);
    }

    @Override
    public @NotNull Type<RbmkJetPayload> type() {
        return TYPE;
    }
}
