// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.network;

import com.hbm.api.fluidmk2.FluidPipeGraph;
import com.hbm.api.fluidmk2.PipeData;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.lib.Library;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public class FluidDuctBoxExhaustBlock extends FluidDuctBoxBlock {

    public FluidDuctBoxExhaustBlock(BlockBehaviour.Properties props, int size) {
        super(props, size, false);
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
    protected boolean connectsVisually(
            LevelReader level, BlockPos pos, Direction dir, Fluid selfFluid) {
        for (Fluid smoke : FluidPipeGraph.smokes()) {
            if (Library.canConnectFluid(level, pos.relative(dir), dir.getOpposite(), smoke))
                return true;
        }
        return false;
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
    public void setPlacedBy(
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable LivingEntity by,
            ItemStack stack) {}

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, ILookOverlay.LookInfo info) {
        info.title(getName().getString(), 0xffff00, 0x404000);
        for (Fluid smoke : FluidPipeGraph.smokes()) {
            info.line(NTMFluidProperties.getDisplayName(smoke).getString());
        }
    }
}
