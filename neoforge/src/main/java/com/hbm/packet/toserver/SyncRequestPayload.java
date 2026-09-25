// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toserver;

import com.hbm.lib.Library;
import com.hbm.packet.ChunkTrackerIndex;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import com.hbm.tileentity.Synced;
import com.hbm.util.ChunkUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

public final class SyncRequestPayload extends ThreadedPayload {
    public static final Type<SyncRequestPayload> TYPE = new Type<>(Library.id("sync_request"));
    public static final StreamCodec<ByteBuf, SyncRequestPayload> STREAM_CODEC =
            streamCodec(buf -> new SyncRequestPayload(BlockPos.of(buf.readLong())));
    private final BlockPos pos;

    public SyncRequestPayload(BlockPos pos) {
        this.pos = pos;
    }

    public static void handle(SyncRequestPayload payload, IPayloadHandlerContext context) {
        if (!(context.playerOrNull() instanceof ServerPlayer player)
                || !ChunkTrackerIndex.canRepair(
                        player, ChunkPos.pack(payload.pos.getX() >> 4, payload.pos.getZ() >> 4)))
            return;
        if (ChunkUtil.blockEntityIfLoaded(player.level(), payload.pos) instanceof Synced synced)
            synced.syncTo(player);
    }

    @Override
    protected int bodyCapacity() {
        return Long.BYTES;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(pos.asLong());
    }

    @Override
    public Type<SyncRequestPayload> type() {
        return TYPE;
    }
}
