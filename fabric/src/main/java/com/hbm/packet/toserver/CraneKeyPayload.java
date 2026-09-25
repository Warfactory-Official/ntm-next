// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toserver;

import com.hbm.extprop.HbmPlayerProps;
import com.hbm.handler.EnumCraneKey;
import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

public final class CraneKeyPayload extends ThreadedPayload {

    public static final Type<CraneKeyPayload> TYPE = new Type<>(Library.id("crane_key"));
    public static final StreamCodec<ByteBuf, CraneKeyPayload> STREAM_CODEC =
            streamCodec(CraneKeyPayload::decode);

    private final int key;
    private final boolean pressed;

    public CraneKeyPayload(int key, boolean pressed) {
        this.key = key;
        this.pressed = pressed;
    }

    private static CraneKeyPayload decode(ByteBuf buf) {
        int key = ByteBufCodecs.VAR_INT.decode(buf);
        boolean pressed = ByteBufCodecs.BOOL.decode(buf);
        return new CraneKeyPayload(key, pressed);
    }

    public static void handleServer(CraneKeyPayload payload, IPayloadHandlerContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer sp)) return;
        if (payload.key < 0 || payload.key >= EnumCraneKey.VALUES.length) return;
        HbmPlayerProps.getData(sp)
                .setCraneKeyPressed(EnumCraneKey.VALUES[payload.key], payload.pressed);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufCodecs.VAR_INT.encode(buf, key);
        ByteBufCodecs.BOOL.encode(buf, pressed);
    }

    @Override
    public @NotNull Type<CraneKeyPayload> type() {
        return TYPE;
    }
}
