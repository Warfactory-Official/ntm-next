// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toserver;

import com.hbm.lib.Library;
import com.hbm.packet.ChunkTrackerIndex;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import com.hbm.packet.toclient.RBMKDiagPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKBase;
import com.hbm.util.ChunkUtil;
import io.netty.buffer.ByteBuf;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public final class RBMKDiagRequestPayload extends ThreadedPayload {

    public static final Type<RBMKDiagRequestPayload> TYPE =
            new Type<>(Library.id("rbmk_diag_request"));
    public static final StreamCodec<ByteBuf, RBMKDiagRequestPayload> STREAM_CODEC =
            streamCodec(buf -> new RBMKDiagRequestPayload(BlockPos.of(buf.readLong())));

    private static final Map<ServerPlayer, Sent> SENT = new WeakHashMap<>();

    private final BlockPos pos;

    private record Sent(ResourceKey<Level> dimension, BlockPos pos, CompoundTag diagnostics) {}

    public RBMKDiagRequestPayload(BlockPos pos) {
        this.pos = pos;
    }

    public static void handle(RBMKDiagRequestPayload payload, IPayloadHandlerContext context) {
        if (!(context.playerOrNull() instanceof ServerPlayer player)
                || !ChunkTrackerIndex.canRepair(
                        player, ChunkPos.pack(payload.pos.getX() >> 4, payload.pos.getZ() >> 4)))
            return;
        if (!(ChunkUtil.blockEntityIfLoaded(player.level(), payload.pos)
                instanceof BlockEntityRBMKBase column)) return;
        CompoundTag diagnostics = new CompoundTag();
        column.writeDiagnostics(diagnostics);
        Sent sent = new Sent(player.level().dimension(), payload.pos, diagnostics);
        if (sent.equals(SENT.put(player, sent))) return;
        Services.NETWORK.sendTo(new RBMKDiagPayload(payload.pos, diagnostics), player);
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
    public @NotNull Type<RBMKDiagRequestPayload> type() {
        return TYPE;
    }
}
