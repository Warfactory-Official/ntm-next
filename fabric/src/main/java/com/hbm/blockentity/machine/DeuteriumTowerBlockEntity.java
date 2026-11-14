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
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class DeuteriumTowerBlockEntity extends DeuteriumExtractorBlockEntity {

    public static final long MAX_POWER = 100_000L;

    public DeuteriumTowerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DEUTERIUM_TOWER.get(), pos, state, 50_000, 5_000);
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }
}
