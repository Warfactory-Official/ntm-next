// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toclient;

import com.hbm.client.SootFog;
import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

public final class PollutionSyncPayload extends ThreadedPayload {

    public static final Type<PollutionSyncPayload> TYPE = new Type<>(Library.id("pollution_sync"));
    public static final StreamCodec<ByteBuf, PollutionSyncPayload> STREAM_CODEC =
            streamCodec(buf -> new PollutionSyncPayload(buf.readFloat()));

    private final float soot;

    public PollutionSyncPayload(float soot) {
        this.soot = soot;
    }

    public static void handle(PollutionSyncPayload payload, IPayloadHandlerContext context) {
        SootFog.receive(payload.soot);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeFloat(soot);
    }

    @Override
    public Type<PollutionSyncPayload> type() {
        return TYPE;
    }
}
