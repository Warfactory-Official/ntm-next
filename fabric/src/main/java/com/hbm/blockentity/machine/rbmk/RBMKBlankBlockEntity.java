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

package com.hbm.blockentity.machine.rbmk;

import com.hbm.blockentity.machine.rbmk.RBMKBaseBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKColumnType;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.entity.projectile.RBMKDebrisEntity;
import com.hbm.packet.SyncUnitSchema;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class RBMKBlankBlockEntity extends RBMKBaseBlockEntity implements SyncUnitSchema {

    public RBMKBlankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_BLANK.get(), pos, state, 0);
    }

    @Override
    public void onMelt(int reduce) {
        int count = 1 + level.getRandom().nextInt(2);
        for (int i = 0; i < count; i++) spawnDebris(RBMKDebrisEntity.DebrisType.BLANK);
        super.onMelt(reduce);
    }

    @Override
    public RBMKColumnType getConsoleType() {
        return RBMKColumnType.BLANK;
    }
}
