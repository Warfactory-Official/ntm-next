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

package com.hbm.items.machine;

import com.hbm.blocks.machine.rbmk.RBMKBlock;
import com.hbm.blocks.multiblock.MultiblockSurface;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

public class RBMKLidItem extends Item {

    public final RBMKBlock.Lid lid;

    public RBMKLidItem(Properties properties, RBMKBlock.Lid lid) {
        super(properties);
        this.lid = lid;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        if (!(level.getBlockState(pos).getBlock() instanceof RBMKBlock rbmk) || rbmk.hasOwnLid()) {
            return InteractionResult.PASS;
        }
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core == null) return InteractionResult.PASS;
        BlockState coreState = level.getBlockState(core);
        if (RBMKBlock.lidOf(coreState).present()) return InteractionResult.PASS;

        if (level instanceof ServerLevel server) {
            SoundType sound = lid == RBMKBlock.Lid.GLASS ? SoundType.GLASS : SoundType.STONE;
            level.playSound(
                    null,
                    core,
                    sound.getPlaceSound(),
                    SoundSource.BLOCKS,
                    (sound.getVolume() + 1.0F) / 2.0F,
                    sound.getPitch() * 0.8F);
            rbmk.writeLid(server, core, coreState, lid);
            ctx.getItemInHand().consume(1, ctx.getPlayer());
        }
        return InteractionResult.SUCCESS;
    }
}
