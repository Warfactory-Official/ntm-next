// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.network;

import com.hbm.api.fluidmk2.FluidPipeGraph;
import com.hbm.api.fluidmk2.PipeData;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.FoldedCoreResident;
import com.hbm.tileentity.GraphResident;
import com.hbm.tileentity.Synced;
import com.hbm.uninos.graph.GraphNode;
import com.hbm.uninos.graph.LevelNodeGraph;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public class BlockEntityPipeAnchor extends BlockEntity
        implements Synced, GraphResident, FoldedCoreResident, SyncUnitSchema {

    @SyncField(units = 1L)
    public long[] connections = new long[0];

    @SyncField(units = 1L << 1)
    public Fluid fluid = Fluids.EMPTY;

    public BlockEntityPipeAnchor(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PIOE_ANCHOR.get(), pos, state);
    }

    public void tickServer() {
        ServerLevel sl = (ServerLevel) level;
        LevelNodeGraph<PipeData> graph = FluidPipeGraph.graphAt(sl, worldPosition.asLong());
        GraphNode<PipeData> node = graph == null ? null : graph.getNode(worldPosition.asLong());
        if (node != null) {
            this.fluid = node.data.fluid();
            this.connections = node.hasRemoteLinks() ? node.remoteLinks.toLongArray() : new long[0];
        } else {

            PipeData data = graph == null ? null : graph.dataAt(worldPosition.asLong());
            this.fluid = data == null ? Fluids.EMPTY : data.fluid();
            this.connections = new long[0];
        }
        networkPackNTTracking();
    }

    private void writeConnections(ByteBuf output) {
        output.writeInt(connections.length);
        for (long connection : connections) output.writeLong(connection);
    }

    private void readConnections(ByteBuf input) {
        long[] values = new long[input.readInt()];
        for (int i = 0; i < values.length; i++) values[i] = input.readLong();
        connections = values;
    }

    private void writeFluid(ByteBuf output) {
        output.writeInt(BuiltInRegistries.FLUID.getId(fluid));
    }

    private void readFluid(ByteBuf input) {
        fluid = BuiltInRegistries.FLUID.byId(input.readInt());
    }

    @Override
    public long syncUnitMask() {
        return 0x3L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> writeConnections(output);
            case 1 -> writeFluid(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> readConnections(input);
            case 1 -> readFluid(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
