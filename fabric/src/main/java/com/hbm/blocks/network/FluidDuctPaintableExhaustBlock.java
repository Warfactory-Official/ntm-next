// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.network;

import com.hbm.api.fluidmk2.FluidPipeGraph;
import com.hbm.api.fluidmk2.PipeData;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.tileentity.network.BlockEntityPipePaintable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public class FluidDuctPaintableExhaustBlock extends FluidDuctPaintableBlock {

    public FluidDuctPaintableExhaustBlock(Properties props) {
        super(props);
    }

    @Override
    public boolean retypable() {
        return false;
    }

    @Override
    public boolean canConnectFluid(Level level, BlockPos pos, Direction side, Fluid fluid) {
        return FluidPipeGraph.isSmoke(fluid);
    }

    @Override
    protected void createNodes(ServerLevel level, BlockPos pos, BlockState state) {
        for (Fluid smoke : FluidPipeGraph.smokes()) {
            FluidPipeGraph.get(level, smoke).addNode(pos.asLong(), new PipeData(smoke), OPEN_ALL);
        }
    }

    @Override
    protected void destroyNodes(ServerLevel level, BlockPos pos) {
        for (Fluid smoke : FluidPipeGraph.smokes()) {
            FluidPipeGraph.get(level, smoke).removeNode(pos.asLong());
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityPipePaintable(pos, state);
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, ILookOverlay.LookInfo info) {
        info.title(getName().getString(), 0xffff00, 0x404000);
        for (Fluid smoke : FluidPipeGraph.smokes()) {
            info.line(NTMFluidProperties.getDisplayName(smoke).getString());
        }
    }
}
