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

package com.hbm.blocks.network.pneumatic;

import com.hbm.blockentity.network.pneumatic.PneumoStorageExporterBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class PneumoStorageExporterBlock extends PneumaticStorageBlock implements CapabilityBlock {

    public PneumoStorageExporterBlock(Properties props) {
        super(props);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PneumoStorageExporterBlockEntity(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.PNEUMATIC_STORAGE_EXPORTER).items();
    }

    @java.lang.Override
    protected void neighborChanged(
            net.minecraft.world.level.block.state.BlockState state,
            net.minecraft.world.level.Level level,
            net.minecraft.core.BlockPos pos,
            net.minecraft.world.level.block.Block neighborBlock,
            net.minecraft.world.level.redstone.Orientation orientation,
            boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
        net.minecraft.world.level.block.entity.BlockEntity hbm$be = level.getBlockEntity(pos);
        if (hbm$be instanceof PneumoStorageExporterBlockEntity)
            ((PneumoStorageExporterBlockEntity) hbm$be).refreshRedstone();
    }

    @java.lang.Override
    protected void onPlace(
            net.minecraft.world.level.block.state.BlockState state,
            net.minecraft.world.level.Level level,
            net.minecraft.core.BlockPos pos,
            net.minecraft.world.level.block.state.BlockState oldState,
            boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        net.minecraft.world.level.block.entity.BlockEntity hbm$be = level.getBlockEntity(pos);
        if (hbm$be instanceof PneumoStorageExporterBlockEntity)
            ((PneumoStorageExporterBlockEntity) hbm$be).refreshRedstone();
    }
}
