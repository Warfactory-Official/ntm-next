// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.storage;

import com.hbm.api.fluidmk2.FluidPipeGraph;
import com.hbm.api.fluidmk2.PipeData;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.uninos.graph.GraphNode;
import com.hbm.uninos.graph.LevelNodeGraph;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public class BlockEntityMachineOrbus extends BlockEntityBarrel {

    public static final int CAPACITY = 512_000;

    public BlockEntityMachineOrbus(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ORBUS.get(), pos, state, CAPACITY);
    }

    private static void removeNode(ServerLevel level, long key) {
        LevelNodeGraph<PipeData> graph = FluidPipeGraph.graphAt(level, key);
        if (graph != null) graph.removeNode(key);
    }

    @Override
    protected void updateBufferNode(ServerLevel server) {
        long core = worldPosition.asLong();
        Fluid type = tank.getDeclaredFluid();
        BlockState state = getBlockState();
        LevelNodeGraph<PipeData> current = FluidPipeGraph.graphAt(server, core);

        if (mode != MODE_BUFFER
                || type == null
                || type == Fluids.EMPTY
                || !(state.getBlock() instanceof BlockMultiblockCore block)) {
            if (current != null) removeNodes(server);
            return;
        }

        LevelNodeGraph<PipeData> target = FluidPipeGraph.get(server, type);

        if (current != null && (current != target || current.getNode(core).data.fluid() != type)) {
            removeNodes(server);
            current = null;
        }
        if (current == null) {
            target.addNode(core, new PipeData(type), 0);

            MultiblockSurface.forEachActiveFace(
                    block,
                    worldPosition,
                    state.getValue(BlockMultiblockCore.FACING),
                    null,
                    (cell, side) -> {
                        long key = cell.asLong();
                        int bit = 1 << side.ordinal();
                        GraphNode<PipeData> node = target.getNode(key);
                        if (node == null) {
                            target.addNode(key, new PipeData(type), bit);
                        } else {
                            target.updateConnections(key, node.openConnections | bit);
                        }
                        if (key != core) target.addRemoteLink(core, key);
                    });
        }
        target.setSelfEndpoint(core, true);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);

        if (level instanceof ServerLevel server
                && FluidPipeGraph.graphAt(server, worldPosition.asLong()) != null) {
            removeNodes(server);
        }
    }

    public void removeNodes(ServerLevel server) {
        BlockState state = getBlockState();
        removeNode(server, worldPosition.asLong());
        if (!(state.getBlock() instanceof BlockMultiblockCore block)) return;
        MultiblockSurface.forEachActiveFace(
                block,
                worldPosition,
                state.getValue(BlockMultiblockCore.FACING),
                null,
                (cell, side) -> removeNode(server, cell.asLong()));
    }

    @Override
    public long getReceiverSpeed(Fluid type, int pressure) {
        if (mode != MODE_RECEIVE && mode != MODE_BUFFER) return 0L;
        return Math.max(1_000L, (long) (tank.getMaxFill() - tank.getFill()) / 100);
    }

    @Override
    public long getProviderSpeed(Fluid type, int pressure) {
        if (mode != MODE_SEND && mode != MODE_BUFFER) return 0L;
        return Math.max(1_000L, (long) tank.getFill() / 100);
    }

    @Override
    protected void checkFluidInteraction() {}

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.orbus");
    }
}
