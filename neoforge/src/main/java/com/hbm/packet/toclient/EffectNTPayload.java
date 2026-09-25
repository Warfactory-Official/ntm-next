// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toclient;

import com.hbm.client.ClientEffects;
import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import com.hbm.particle.HbmEffectNT;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

public final class EffectNTPayload extends ThreadedPayload {

    public static final Type<EffectNTPayload> TYPE = new Type<>(Library.id("effect_nt"));
    public static final StreamCodec<ByteBuf, EffectNTPayload> STREAM_CODEC =
            streamCodec(EffectNTPayload::decode);
    private static final HbmEffectNT[] EFFECTS = HbmEffectNT.values();

    private final int effect;
    private final double x, y, z;

    private final float scale;

    public EffectNTPayload(HbmEffectNT effect, double x, double y, double z) {
        this(effect.ordinal(), x, y, z, 0F);
    }

    public EffectNTPayload(HbmEffectNT effect, double x, double y, double z, float scale) {
        this(effect.ordinal(), x, y, z, scale);
    }

    private EffectNTPayload(int effect, double x, double y, double z, float scale) {
        this.effect = effect;
        this.x = x;
        this.y = y;
        this.z = z;
        this.scale = scale;
    }

    private static EffectNTPayload decode(ByteBuf buf) {
        return new EffectNTPayload(
                buf.readByte(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readFloat());
    }

    public static void handle(EffectNTPayload payload, IPayloadHandlerContext ctx) {
        if (payload.effect < 0 || payload.effect >= EFFECTS.length) return;
        var player = ctx.playerOrNull();
        if (player == null) return;
        ClientEffects.spawn(
                EFFECTS[payload.effect],
                player.level(),
                payload.x,
                payload.y,
                payload.z,
                payload.scale);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(this.effect);
        buf.writeDouble(this.x);
        buf.writeDouble(this.y);
        buf.writeDouble(this.z);
        buf.writeFloat(this.scale);
    }

    @Override
    public @NotNull Type<EffectNTPayload> type() {
        return TYPE;
    }
}
