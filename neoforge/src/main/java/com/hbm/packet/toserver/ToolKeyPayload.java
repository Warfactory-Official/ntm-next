// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toserver;

import com.hbm.extprop.HbmPlayerProps;
import com.hbm.handler.EnumToolKey;
import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

public final class ToolKeyPayload extends ThreadedPayload {

    public static final Type<ToolKeyPayload> TYPE = new Type<>(Library.id("tool_key"));
    public static final StreamCodec<ByteBuf, ToolKeyPayload> STREAM_CODEC =
            streamCodec(ToolKeyPayload::decode);

    private final int key;
    private final boolean pressed;

    public ToolKeyPayload(int key, boolean pressed) {
        this.key = key;
        this.pressed = pressed;
    }

    private static ToolKeyPayload decode(ByteBuf buf) {
        return new ToolKeyPayload(
                ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.BOOL.decode(buf));
    }

    public static void handleServer(ToolKeyPayload payload, IPayloadHandlerContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) return;
        if (payload.key < 0 || payload.key >= EnumToolKey.VALUES.length) return;
        HbmPlayerProps.getData(player)
                .setToolKeyPressed(EnumToolKey.VALUES[payload.key], payload.pressed);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufCodecs.VAR_INT.encode(buf, key);
        ByteBufCodecs.BOOL.encode(buf, pressed);
    }

    @Override
    public @NotNull Type<ToolKeyPayload> type() {
        return TYPE;
    }
}
