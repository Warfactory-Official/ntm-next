// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toserver;

import com.hbm.handler.radiation.RadVisServer;
import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

public final class RadVisWatchPayload extends ThreadedPayload {

    public static final Type<RadVisWatchPayload> TYPE = new Type<>(Library.id("radvis_watch"));
    public static final StreamCodec<ByteBuf, RadVisWatchPayload> STREAM_CODEC =
            streamCodec(buf -> new RadVisWatchPayload(buf.readByte()));

    private final int radius;

    public RadVisWatchPayload(int radius) {
        this.radius = radius;
    }

    public static void handle(RadVisWatchPayload payload, IPayloadHandlerContext context) {
        if (context.playerOrNull() instanceof ServerPlayer player)
            RadVisServer.watch(player, payload.radius);
    }

    @Override
    protected int bodyCapacity() {
        return 1;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(radius);
    }

    @Override
    public @NotNull Type<RadVisWatchPayload> type() {
        return TYPE;
    }
}
