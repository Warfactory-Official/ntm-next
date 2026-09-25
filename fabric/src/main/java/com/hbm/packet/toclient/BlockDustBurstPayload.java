// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toclient;

import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public final class BlockDustBurstPayload extends ThreadedPayload {

    public static final Type<BlockDustBurstPayload> TYPE =
            new Type<>(Library.id("block_dust_burst"));
    public static final StreamCodec<ByteBuf, BlockDustBurstPayload> STREAM_CODEC =
            streamCodec(BlockDustBurstPayload::decode);

    final double x, y, z;
    final int stateId, count;
    final double motion;

    public BlockDustBurstPayload(
            BlockState state, double x, double y, double z, int count, double motion) {
        this(Block.BLOCK_STATE_REGISTRY.getId(state), x, y, z, count, motion);
    }

    private BlockDustBurstPayload(
            int stateId, double x, double y, double z, int count, double motion) {
        this.stateId = stateId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.count = count;
        this.motion = motion;
    }

    private static BlockDustBurstPayload decode(ByteBuf buf) {
        return new BlockDustBurstPayload(
                ByteBufCodecs.VAR_INT.decode(buf),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                ByteBufCodecs.VAR_INT.decode(buf),
                buf.readDouble());
    }

    public static void handleClient(BlockDustBurstPayload payload, IPayloadHandlerContext ctx) {
        ClientParticlePayloadHandlers.handle(payload);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufCodecs.VAR_INT.encode(buf, stateId);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        ByteBufCodecs.VAR_INT.encode(buf, count);
        buf.writeDouble(motion);
    }

    @Override
    public @NotNull Type<BlockDustBurstPayload> type() {
        return TYPE;
    }
}
