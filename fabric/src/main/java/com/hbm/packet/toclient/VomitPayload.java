// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toclient;

import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

public final class VomitPayload extends ThreadedPayload {

    public static final Type<VomitPayload> TYPE = new Type<>(Library.id("vomit"));
    public static final StreamCodec<ByteBuf, VomitPayload> STREAM_CODEC =
            streamCodec(VomitPayload::decode);

    public static final byte MODE_NORMAL = 0;
    public static final byte MODE_BLOOD = 1;
    public static final byte MODE_SMOKE = 2;

    final int entityId;
    final byte mode;
    final int count;

    public VomitPayload(int entityId, byte mode, int count) {
        this.entityId = entityId;
        this.mode = mode;
        this.count = count;
    }

    private static VomitPayload decode(ByteBuf buf) {
        return new VomitPayload(
                ByteBufCodecs.VAR_INT.decode(buf),
                buf.readByte(),
                ByteBufCodecs.VAR_INT.decode(buf));
    }

    public static void handleClient(VomitPayload payload, IPayloadHandlerContext ctx) {
        ClientParticlePayloadHandlers.handle(payload);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufCodecs.VAR_INT.encode(buf, entityId);
        buf.writeByte(mode);
        ByteBufCodecs.VAR_INT.encode(buf, count);
    }

    @Override
    public @NotNull Type<VomitPayload> type() {
        return TYPE;
    }
}
