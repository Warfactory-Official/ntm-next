// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toserver;

import com.hbm.interfaces.injected.FarViewer;
import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.PacketWire;
import com.hbm.packet.threading.ThreadedPayload;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

public final class FarViewPayload extends ThreadedPayload {

    public static final Type<FarViewPayload> TYPE = new Type<>(Library.id("far_view"));
    public static final StreamCodec<ByteBuf, FarViewPayload> STREAM_CODEC =
            streamCodec(FarViewPayload::decode);

    private final int blocks;

    private FarViewPayload(int blocks) {
        this.blocks = blocks;
    }

    public static void report(int blocks) {
        PacketWire.sendToServer(new FarViewPayload(blocks));
    }

    private static FarViewPayload decode(ByteBuf buf) {
        return new FarViewPayload(ByteBufCodecs.VAR_INT.decode(buf));
    }

    public static void handleServer(FarViewPayload payload, IPayloadHandlerContext ctx) {
        if (ctx.player() instanceof ServerPlayer player) {
            ((FarViewer) player.connection).hbm$farView(Math.max(payload.blocks, 0));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufCodecs.VAR_INT.encode(buf, blocks);
    }

    @Override
    public @NotNull Type<FarViewPayload> type() {
        return TYPE;
    }
}
