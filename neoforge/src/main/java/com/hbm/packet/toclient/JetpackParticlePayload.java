// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toclient;

import com.hbm.client.ClientEffects;
import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

public final class JetpackParticlePayload extends ThreadedPayload {

    public static final int DNS = 0;
    public static final int BJ = 1;
    public static final int REGULAR = 2;
    public static final int VECTOR = 3;

    public static final Type<JetpackParticlePayload> TYPE =
            new Type<>(Library.id("jetpack_particle"));
    public static final StreamCodec<ByteBuf, JetpackParticlePayload> STREAM_CODEC =
            streamCodec(JetpackParticlePayload::decode);

    private final int player;
    private final int mode;

    public JetpackParticlePayload(int player, int mode) {
        this.player = player;
        this.mode = mode;
    }

    private static JetpackParticlePayload decode(ByteBuf buf) {
        return new JetpackParticlePayload(
                ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf));
    }

    public static void handle(JetpackParticlePayload payload, IPayloadHandlerContext ctx) {
        var viewer = ctx.playerOrNull();
        if (viewer == null) return;
        ClientEffects.spawnJetpackPlume(viewer.level(), payload.player, payload.mode);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufCodecs.VAR_INT.encode(buf, player);
        ByteBufCodecs.VAR_INT.encode(buf, mode);
    }

    @Override
    public @NotNull Type<JetpackParticlePayload> type() {
        return TYPE;
    }
}
