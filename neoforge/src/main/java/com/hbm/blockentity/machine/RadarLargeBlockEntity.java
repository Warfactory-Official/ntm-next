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

package com.hbm.blockentity.machine;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.data.MachineData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class RadarLargeBlockEntity extends RadarBlockEntity {

    public RadarLargeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RADAR_LARGE.get(), pos, state);
    }

    @Override
    public int getRange() {
        return MachineData.RADAR_LARGE_RANGE.get();
    }

    @Override
    protected void notifyRedstone() {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos cell = worldPosition.relative(dir);
            level.updateNeighborsAt(cell, level.getBlockState(cell).getBlock());
        }
    }
}
