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

public final class ProperJoltPayload extends ThreadedPayload {

    public static final Type<ProperJoltPayload> TYPE = new Type<>(Library.id("proper_jolt"));
    public static final StreamCodec<ByteBuf, ProperJoltPayload> STREAM_CODEC =
            streamCodec(ProperJoltPayload::decode);

    final int time;
    final int maxTime;

    public ProperJoltPayload(int time, int maxTime) {
        this.time = time;
        this.maxTime = maxTime;
    }

    private static ProperJoltPayload decode(ByteBuf buf) {
        return new ProperJoltPayload(
                ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf));
    }

    public static void handleClient(ProperJoltPayload payload, IPayloadHandlerContext ctx) {
        ClientParticlePayloadHandlers.handle(payload);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufCodecs.VAR_INT.encode(buf, time);
        ByteBufCodecs.VAR_INT.encode(buf, maxTime);
    }

    @Override
    public @NotNull Type<ProperJoltPayload> type() {
        return TYPE;
    }
}
