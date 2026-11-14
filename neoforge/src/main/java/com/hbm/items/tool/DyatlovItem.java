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

package com.hbm.items.tool;

import com.hbm.blockentity.machine.ReactorZirnoxBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKBaseBlockEntity;
import com.hbm.blocks.multiblock.MultiblockSurface;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class DyatlovItem extends Item {

    public DyatlovItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        BlockPos pos = ctx.getClickedPos();

        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core == null) return InteractionResult.PASS;

        BlockEntity be = level.getBlockEntity(core);
        if (be instanceof RBMKBaseBlockEntity rbmk) {
            rbmk.meltdown();
            return InteractionResult.SUCCESS;
        }
        if (be instanceof ReactorZirnoxBlockEntity zirnox) {
            zirnox.hbm$syncSet$ReactorZirnoxBlockEntity$heat(200000);
            zirnox.markChanged();
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }
}
