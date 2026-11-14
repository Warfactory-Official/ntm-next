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
import com.hbm.data.MobData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class ChimneyBrickBlockEntity extends ChimneyBaseBlockEntity {

    public ChimneyBrickBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHIMNEY_BRICK.get(), pos, state);
    }

    @Override
    public int plumeHeight() {
        return 12;
    }

    @Override
    public float plumeBaseScale() {
        return 0.5F;
    }

    @Override
    public double getPollutionMod() {
        return MobData.RAMPANT_MODE.get() ? MobData.RAMPANT_SMOKE_STACK_OVERRIDE.get() : 0.25D;
    }
}
