// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toclient;

import com.hbm.handler.ImpactWorldHandler;
import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

public final class ImpactSyncPayload extends ThreadedPayload {

    public static final Type<ImpactSyncPayload> TYPE = new Type<>(Library.id("impact_sync"));
    public static final StreamCodec<ByteBuf, ImpactSyncPayload> STREAM_CODEC =
            streamCodec(buf -> new ImpactSyncPayload(buf.readFloat(), buf.readFloat()));

    private final float fire;
    private final float dust;

    public ImpactSyncPayload(float fire, float dust) {
        this.fire = fire;
        this.dust = dust;
    }

    public static void handle(ImpactSyncPayload payload, IPayloadHandlerContext context) {
        Player player = context.playerOrNull();
        if (player != null) ImpactWorldHandler.receive(player, payload.fire, payload.dust);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeFloat(fire);
        buf.writeFloat(dust);
    }

    @Override
    public Type<ImpactSyncPayload> type() {
        return TYPE;
    }
}
