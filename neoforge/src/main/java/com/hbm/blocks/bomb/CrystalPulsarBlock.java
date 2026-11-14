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

package com.hbm.blocks.bomb;

import com.hbm.blocks.ModBlocks;
import com.hbm.explosion.ExplosionChaos;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

public class CrystalPulsarBlock extends Block {

    public CrystalPulsarBlock(Properties props) {
        super(props);
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block block,
            @Nullable Orientation orientation,
            boolean movedByPiston) {
        if (!level.isClientSide()
                || level.getBlockState(pos.east()).is(ModBlocks.CRYSTAL_VIRUS.get())
                || level.getBlockState(pos.west()).is(ModBlocks.CRYSTAL_VIRUS.get())
                || level.getBlockState(pos.above()).is(ModBlocks.CRYSTAL_VIRUS.get())
                || level.getBlockState(pos.below()).is(ModBlocks.CRYSTAL_VIRUS.get())
                || level.getBlockState(pos.south()).is(ModBlocks.CRYSTAL_VIRUS.get())
                || level.getBlockState(pos.north()).is(ModBlocks.CRYSTAL_VIRUS.get())) {
            ExplosionChaos.hardenVirus(level, pos, 10);
        }
    }
}
