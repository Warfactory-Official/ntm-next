// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toclient;

import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public final class BlockDestroyPayload extends ThreadedPayload {
    public static final Type<BlockDestroyPayload> TYPE = new Type<>(Library.id("block_destroy"));
    public static final StreamCodec<ByteBuf, BlockDestroyPayload> STREAM_CODEC =
            streamCodec(BlockDestroyPayload::decode);

    final int x, y, z, stateId;

    public BlockDestroyPayload(BlockState state, BlockPos pos) {
        this(pos.getX(), pos.getY(), pos.getZ(), Block.BLOCK_STATE_REGISTRY.getId(state));
    }

    private BlockDestroyPayload(int x, int y, int z, int stateId) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.stateId = stateId;
    }

    private static BlockDestroyPayload decode(ByteBuf buf) {
        return new BlockDestroyPayload(buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt());
    }

    public static void handleClient(BlockDestroyPayload payload, IPayloadHandlerContext ctx) {
        ClientParticlePayloadHandlers.handle(payload);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        buf.writeInt(stateId);
    }

    @Override
    public @NotNull Type<BlockDestroyPayload> type() {
        return TYPE;
    }
}
