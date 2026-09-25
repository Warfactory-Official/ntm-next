// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toserver;

import com.hbm.config.InteractionConfig;
import com.hbm.extprop.HbmPlayerProps;
import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import com.hbm.platform.Services;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

public final class CrateOpenHeldPayload extends ThreadedPayload {

    public static final Type<CrateOpenHeldPayload> TYPE = new Type<>(Library.id("crate_open_held"));
    public static final StreamCodec<ByteBuf, CrateOpenHeldPayload> STREAM_CODEC =
            streamCodec(CrateOpenHeldPayload::decode);

    private final boolean openHeld;

    private CrateOpenHeldPayload(boolean openHeld) {
        this.openHeld = openHeld;
    }

    public static void report() {
        Services.NETWORK.sendToServer(new CrateOpenHeldPayload(InteractionConfig.crateOpensHeld));
    }

    private static CrateOpenHeldPayload decode(ByteBuf buf) {
        return new CrateOpenHeldPayload(ByteBufCodecs.BOOL.decode(buf));
    }

    public static void handleServer(CrateOpenHeldPayload payload, IPayloadHandlerContext ctx) {
        if (ctx.player() instanceof ServerPlayer player) {
            HbmPlayerProps.getData(player).crateOpenHeld = payload.openHeld;
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufCodecs.BOOL.encode(buf, openHeld);
    }

    @Override
    public @NotNull Type<CrateOpenHeldPayload> type() {
        return TYPE;
    }
}
