/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.blocks.network;

import com.hbm.api.fluidmk2.FluidPipeGraph;
import com.hbm.api.fluidmk2.PipeData;
import com.hbm.interfaces.LookOverlay;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.uninos.graph.PhysicalGraph;
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
    public boolean hasFluidContact(
            Level level, BlockPos pos, BlockState state, Direction side, Fluid fluid) {
        return FluidPipeGraph.isSmoke(fluid);
    }

    @Override
    protected boolean connectsVisually(
            LevelReader level, BlockPos pos, Direction dir, Fluid selfFluid) {
        for (Fluid smoke : FluidPipeGraph.smokes()) {
            if (PhysicalGraph.fluidContact(level, pos.relative(dir), dir.getOpposite(), smoke))
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
    public void buildLookOverlay(Level level, BlockPos pos, LookOverlay info) {
        info.title(getName());
        for (Fluid smoke : FluidPipeGraph.smokes()) {
            info.row(NTMFluidProperties.getDisplayName(smoke));
        }
    }
}
