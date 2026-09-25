// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toserver;

import com.hbm.api.fluidmk2.FluidPipeGraph;
import com.hbm.api.fluidmk2.FluidPipeTintData;
import com.hbm.api.fluidmk2.PipeData;
import com.hbm.blocks.network.FluidDuctBlockBase;
import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.NotNull;

public final class FluidPipeTintRequestPayload extends ThreadedPayload {

    public static final Type<FluidPipeTintRequestPayload> TYPE =
            new Type<>(Library.id("fluid_pipe_tint_request"));
    public static final StreamCodec<ByteBuf, FluidPipeTintRequestPayload> STREAM_CODEC =
            streamCodec(FluidPipeTintRequestPayload::decode);

    private final long[] posKeys;

    public FluidPipeTintRequestPayload(long[] posKeys) {
        this.posKeys = posKeys;
    }

    private static FluidPipeTintRequestPayload decode(ByteBuf buf) {
        int count = buf.readInt();

        if (count < 0 || count > buf.readableBytes() / Long.BYTES) {
            throw new DecoderException("fluid_pipe_tint_request count " + count);
        }
        long[] posKeys = new long[count];
        for (int i = 0; i < count; i++) posKeys[i] = buf.readLong();
        return new FluidPipeTintRequestPayload(posKeys);
    }

    public static void handle(FluidPipeTintRequestPayload payload, IPayloadHandlerContext ctx) {
        ServerPlayer player = (ServerPlayer) ctx.player();
        ServerLevel level = player.level();
        Reference2ObjectOpenHashMap<Fluid, LongArrayList> byFluid =
                new Reference2ObjectOpenHashMap<>();
        for (long key : payload.posKeys) {
            BlockPos pos = BlockPos.of(key);
            if (!(level.getBlockState(pos).getBlock() instanceof FluidDuctBlockBase)) continue;
            PipeData data = FluidPipeGraph.dataAt(level, key);
            if (data == null) continue;
            byFluid.computeIfAbsent(data.fluid(), f -> new LongArrayList()).add(key);
        }
        for (Reference2ObjectMap.Entry<Fluid, LongArrayList> entry :
                byFluid.reference2ObjectEntrySet()) {
            FluidPipeTintData.syncTo(player, entry.getValue().toLongArray(), entry.getKey());
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(posKeys.length);
        for (long posKey : posKeys) buf.writeLong(posKey);
    }

    @Override
    public @NotNull Type<FluidPipeTintRequestPayload> type() {
        return TYPE;
    }
}
