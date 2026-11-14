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

import com.hbm.entity.ModEntities;
import com.hbm.entity.item.TntNtmEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public final class ChainDetonation {

    private ChainDetonation() {}

    public static void spawn(
            Level level,
            BlockPos pos,
            @Nullable LivingEntity igniter,
            BlockState bomb,
            int popFuse) {
        if (!TntRule.explodes(level)) return;
        TntNtmEntity tnt =
                new TntNtmEntity(
                        ModEntities.TNT_NTM.get(),
                        level,
                        pos.getX() + 0.5D,
                        pos.getY(),
                        pos.getZ() + 0.5D,
                        igniter,
                        bomb);
        tnt.setFuse(popFuse <= 0 ? 0 : level.getRandom().nextInt(popFuse) + popFuse / 2);
        level.addFreshEntity(tnt);
    }
}
