// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toclient;

import com.hbm.api.fluidmk2.FluidPipeTintData;
import com.hbm.blocks.network.FluidPipeBlock;
import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;

public final class FluidPipeTintPayload extends ThreadedPayload {

    public static final Type<FluidPipeTintPayload> TYPE = new Type<>(Library.id("fluid_pipe_tint"));
    public static final StreamCodec<ByteBuf, FluidPipeTintPayload> STREAM_CODEC =
            streamCodec(FluidPipeTintPayload::decode);

    private final BlockPos pos;
    private final int fluidId;

    public FluidPipeTintPayload(BlockPos pos, Fluid fluid) {
        this.pos = pos;
        this.fluidId = BuiltInRegistries.FLUID.getId(fluid);
    }

    private FluidPipeTintPayload(BlockPos pos, int fluidId) {
        this.pos = pos;
        this.fluidId = fluidId;
    }

    private static FluidPipeTintPayload decode(ByteBuf buf) {
        return new FluidPipeTintPayload(
                new BlockPos(buf.readInt(), buf.readInt(), buf.readInt()), buf.readInt());
    }

    public static void handle(FluidPipeTintPayload payload, IPayloadHandlerContext ctx) {
        var player = ctx.playerOrNull();
        if (player == null) return;
        Level level = player.level();
        Fluid fluid = BuiltInRegistries.FLUID.byId(payload.fluidId);
        FluidPipeTintData.set(
                level.dimension(), payload.pos.asLong(), fluid == null ? Fluids.EMPTY : fluid);
        BlockState state = level.getBlockState(payload.pos);
        if (state.getBlock() instanceof FluidPipeBlock)
            level.sendBlockUpdated(payload.pos, state, state, 0);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(pos.getX());
        buf.writeInt(pos.getY());
        buf.writeInt(pos.getZ());
        buf.writeInt(fluidId);
    }

    @Override
    public @NotNull Type<FluidPipeTintPayload> type() {
        return TYPE;
    }
}
