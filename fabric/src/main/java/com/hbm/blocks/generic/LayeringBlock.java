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

package com.hbm.blocks.generic;

import com.hbm.blocks.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;

public class LayeringBlock extends SnowLayerBlock {

    private final boolean alwaysReplaceable;

    public LayeringBlock(Properties properties, boolean alwaysReplaceable) {
        super(properties.noOcclusion());
        this.alwaysReplaceable = alwaysReplaceable;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return false;
    }

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        if (alwaysReplaceable) return true;
        if (!context.getItemInHand().is(this.asItem())) return false;
        return super.canBeReplaced(state, context);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        var support = level.getBlockState(pos.below()).getBlock();
        return support == ModBlocks.RBMK_DEBRIS.get()
                || support == ModBlocks.RBMK_DEBRIS_BURNING.get()
                || support == ModBlocks.RBMK_DEBRIS_RADIATING.get()
                || support == ModBlocks.RBMK_DEBRIS_DIGAMMA.get()
                || support == ModBlocks.ZIRNOX_DESTROYED.get()
                || super.canSurvive(state, level, pos);
    }
}
