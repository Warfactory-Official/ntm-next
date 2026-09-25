// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.synthetic;

import com.hbm.NuclearTech;
import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import com.hbm.platform.Services;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

public final class PingPayload extends ThreadedPayload {

    public static final Type<PingPayload> C2S_TYPE = new Type<>(Library.id("ping_c2s"));
    public static final Type<PingPayload> S2C_TYPE = new Type<>(Library.id("ping_s2c"));
    public static final StreamCodec<ByteBuf, PingPayload> C2S_STREAM_CODEC =
            streamCodec(buf -> new PingPayload(buf.readLong(), false));
    public static final StreamCodec<ByteBuf, PingPayload> S2C_STREAM_CODEC =
            streamCodec(buf -> new PingPayload(buf.readLong(), true));

    private final long nonce;
    private final boolean echo;

    public PingPayload(long nonce) {
        this(nonce, false);
    }

    private PingPayload(long nonce, boolean echo) {
        this.nonce = nonce;
        this.echo = echo;
    }

    public static void handleServer(PingPayload payload, IPayloadHandlerContext ctx) {
        NuclearTech.LOGGER.info("PingPayload C2S received: nonce={}", payload.nonce);
        if (ctx.player() instanceof ServerPlayer sp) {
            Services.NETWORK.sendTo(new PingPayload(payload.nonce, true), sp);
        }
    }

    public static void handleClient(PingPayload payload, IPayloadHandlerContext ctx) {
        NuclearTech.LOGGER.info("PingPayload S2C echo received: nonce={}", payload.nonce);
    }

    public long nonce() {
        return nonce;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(nonce);
    }

    @Override
    public @NotNull Type<PingPayload> type() {
        return echo ? S2C_TYPE : C2S_TYPE;
    }
}
