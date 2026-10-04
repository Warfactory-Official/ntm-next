// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blockentity.machine.storage;

import com.hbm.api.fluidmk2.PipeData;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.uninos.graph.LevelNodeGraph;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

public class BlockEntityMachineOrbus extends BlockEntityBarrel {

    public static final int CAPACITY = 512_000;

    public BlockEntityMachineOrbus(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ORBUS.get(), pos, state, CAPACITY);
    }

    @Override
    protected void addBufferNodes(LevelNodeGraph<PipeData> graph, PipeData data) {
        long core = worldPosition.asLong();
        BlockState state = getBlockState();
        var coreNode = graph.addMachineNode(core, data, 0, true);
        MultiblockSurface.forEachActiveFace(
                (BlockMultiblockCore) state.getBlock(),
                worldPosition,
                state.getValue(BlockMultiblockCore.FACING),
                null,
                (cell, side) -> {
                    long key = cell.asLong();
                    var node = graph.addMachineNode(key, data, 1 << side.ordinal(), false);
                    if (key != core) graph.addRemoteLink(coreNode, node);
                });
    }

    @Override
    protected void removeBufferNodes(LevelNodeGraph<PipeData> graph, BlockState state) {
        graph.removeNode(worldPosition.asLong());
        MultiblockSurface.forEachActiveFace(
                (BlockMultiblockCore) state.getBlock(),
                worldPosition,
                state.getValue(BlockMultiblockCore.FACING),
                null,
                (cell, side) -> graph.removeNode(cell.asLong()));
    }

    @Override
    public long getReceiverSpeed(Fluid type, int pressure) {
        if (getMode() != MODE_RECEIVE && getMode() != MODE_BUFFER) return 0L;
        return Math.max(1_000L, (long) (tank.getMaxFill() - tank.getFill()) / 100);
    }

    @Override
    public long getProviderSpeed(Fluid type, int pressure) {
        if (getMode() != MODE_SEND && getMode() != MODE_BUFFER) return 0L;
        return Math.max(1_000L, (long) tank.getFill() / 100);
    }

    @Override
    protected void checkFluidInteraction() {}

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.orbus");
    }
}
