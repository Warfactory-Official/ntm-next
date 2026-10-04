// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blockentity.machine.storage;

import com.hbm.api.fluidmk2.PipeData;
import com.hbm.blockentity.Tiltable;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.storage.MachineBigAssTank;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.trait.FluidTraitSimple.FT_Amat;
import com.hbm.uninos.graph.LevelNodeGraph;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

public class BlockEntityMachineBigAssTank extends BlockEntityBarrel implements Tiltable {

    public static final int CAPACITY = 16_000_000;

    private int lastComparator;

    public BlockEntityMachineBigAssTank(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BIGASSTANK.get(), pos, state, CAPACITY);
    }

    @Override
    public void tickServer() {
        checkTilt(Tiltable.TiltType.UNAVOIDABLE, true);
        int comparator = getComparatorPower();
        if (comparator != lastComparator) {
            setChanged();
            ((MachineBigAssTank) getBlockState().getBlock())
                    .updateComparatorOutput((ServerLevel) level, worldPosition);
        }
        lastComparator = comparator;
        super.tickServer();
    }

    @Override
    protected void addBufferNodes(LevelNodeGraph<PipeData> graph, PipeData data) {
        long core = worldPosition.asLong();
        Direction facing = getBlockState().getValue(BlockMultiblockCore.FACING);
        long front = worldPosition.relative(facing, 6).asLong();
        long back = worldPosition.relative(facing.getOpposite(), 6).asLong();
        var coreNode = graph.addMachineNode(core, data, 0, true);
        graph.addRemoteLink(
                coreNode, graph.addMachineNode(front, data, 1 << facing.ordinal(), false));
        graph.addRemoteLink(
                coreNode,
                graph.addMachineNode(back, data, 1 << facing.getOpposite().ordinal(), false));
    }

    @Override
    protected void removeBufferNodes(LevelNodeGraph<PipeData> graph, BlockState state) {
        Direction facing = state.getValue(BlockMultiblockCore.FACING);
        graph.removeNode(worldPosition.relative(facing, 6).asLong());
        graph.removeNode(worldPosition.relative(facing.getOpposite(), 6).asLong());
        graph.removeNode(worldPosition.asLong());
    }

    @Override
    public long getReceiverSpeed(Fluid type, int pressure) {
        if (isTilted() || getMode() != MODE_RECEIVE && getMode() != MODE_BUFFER) return 0L;
        return Math.max(50_000L, (long) (tank.getMaxFill() - tank.getFill()) / 100);
    }

    @Override
    public long getProviderSpeed(Fluid type, int pressure) {
        if (getMode() == MODE_BUFFER) return Math.max(50_000L, (long) tank.getFill() / 100);
        if (isTilted() || getMode() != MODE_SEND) return 0L;
        return Math.max(50_000L, (long) tank.getFill() / 100);
    }

    @Override
    protected void checkFluidInteraction() {
        Fluid type = tank.getTankType();
        if (type != null && NTMFluidProperties.hasTrait(type, FT_Amat.class)) {
            level.destroyBlock(worldPosition, false);
            level.explode(
                    null,
                    worldPosition.getX() + 0.5,
                    worldPosition.getY() + 0.5,
                    worldPosition.getZ() + 0.5,
                    10F,
                    true,
                    Level.ExplosionInteraction.BLOCK);
        }
    }

    @Override
    public int getFloorCount() {
        return 4 * 4;
    }

    @Override
    public BlockPos getFloorPosFromIndex(int index) {
        return standardFloor7x7(index);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.bigAssTank");
    }
}
