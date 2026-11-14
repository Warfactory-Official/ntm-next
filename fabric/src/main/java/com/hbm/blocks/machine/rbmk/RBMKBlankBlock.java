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

package com.hbm.blocks.machine.rbmk;

import com.hbm.blockentity.machine.rbmk.RBMKBaseBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKBlankBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class RBMKBlankBlock extends RBMKBlock {

    public RBMKBlankBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntityType<? extends RBMKBaseBlockEntity> beType() {
        return ModBlockEntities.RBMK_BLANK.get();
    }

    @Override
    protected RBMKBaseBlockEntity createCore(BlockPos pos, BlockState state) {
        return new RBMKBlankBlockEntity(pos, state);
    }
}
