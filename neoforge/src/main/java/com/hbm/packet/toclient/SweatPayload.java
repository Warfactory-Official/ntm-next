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

public final class SweatPayload extends ThreadedPayload {

    public static final Type<SweatPayload> TYPE = new Type<>(Library.id("sweat"));
    public static final StreamCodec<ByteBuf, SweatPayload> STREAM_CODEC =
            streamCodec(SweatPayload::decode);

    final int entityId;
    final int stateId;
    final int count;

    public SweatPayload(int entityId, BlockState state, int count) {
        this(entityId, Block.BLOCK_STATE_REGISTRY.getId(state), count);
    }

    private SweatPayload(int entityId, int stateId, int count) {
        this.entityId = entityId;
        this.stateId = stateId;
        this.count = count;
    }

    private static SweatPayload decode(ByteBuf buf) {
        return new SweatPayload(
                ByteBufCodecs.VAR_INT.decode(buf),
                ByteBufCodecs.VAR_INT.decode(buf),
                ByteBufCodecs.VAR_INT.decode(buf));
    }

    public static void handleClient(SweatPayload payload, IPayloadHandlerContext ctx) {
        ClientParticlePayloadHandlers.handle(payload);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufCodecs.VAR_INT.encode(buf, entityId);
        ByteBufCodecs.VAR_INT.encode(buf, stateId);
        ByteBufCodecs.VAR_INT.encode(buf, count);
    }

    @Override
    public @NotNull Type<SweatPayload> type() {
        return TYPE;
    }
}
