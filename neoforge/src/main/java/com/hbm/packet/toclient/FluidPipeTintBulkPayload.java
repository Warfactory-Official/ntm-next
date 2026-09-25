// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toclient;

import com.hbm.api.fluidmk2.FluidPipeTintData;
import com.hbm.blocks.network.FluidPipeBlock;
import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;

public final class FluidPipeTintBulkPayload extends ThreadedPayload {

    public static final Type<FluidPipeTintBulkPayload> TYPE =
            new Type<>(Library.id("fluid_pipe_tint_bulk"));
    public static final StreamCodec<ByteBuf, FluidPipeTintBulkPayload> STREAM_CODEC =
            streamCodec(FluidPipeTintBulkPayload::decode);

    private final long[] posKeys;
    private final int fluidId;

    public FluidPipeTintBulkPayload(long[] posKeys, Fluid fluid) {
        this.posKeys = posKeys;
        this.fluidId = BuiltInRegistries.FLUID.getId(fluid);
    }

    private FluidPipeTintBulkPayload(long[] posKeys, int fluidId) {
        this.posKeys = posKeys;
        this.fluidId = fluidId;
    }

    private static FluidPipeTintBulkPayload decode(ByteBuf buf) {
        int count = buf.readInt();
        long[] posKeys = new long[count];
        for (int i = 0; i < count; i++) posKeys[i] = buf.readLong();
        return new FluidPipeTintBulkPayload(posKeys, buf.readInt());
    }

    public static void handle(FluidPipeTintBulkPayload payload, IPayloadHandlerContext ctx) {
        var player = ctx.playerOrNull();
        if (player == null) return;
        Level level = player.level();
        Fluid fluid = BuiltInRegistries.FLUID.byId(payload.fluidId);
        FluidPipeTintData.setAll(
                level.dimension(), payload.posKeys, fluid == null ? Fluids.EMPTY : fluid);
        LongOpenHashSet dirtySections = new LongOpenHashSet();
        for (long key : payload.posKeys) {
            BlockPos pos = BlockPos.of(key);
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof FluidPipeBlock
                    && dirtySections.add(SectionPos.asLong(pos))) {
                level.sendBlockUpdated(pos, state, state, 0);
            }
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(posKeys.length);
        for (long posKey : posKeys) buf.writeLong(posKey);
        buf.writeInt(fluidId);
    }

    @Override
    public @NotNull Type<FluidPipeTintBulkPayload> type() {
        return TYPE;
    }
}
