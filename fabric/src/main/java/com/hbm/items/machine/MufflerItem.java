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

import com.hbm.blockentity.AudioLoop;
import com.hbm.blockentity.Mufflable;
import com.hbm.blockentity.machine.LPW2BlockEntity;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class MufflerItem extends Item {

    public MufflerItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos target = MultiblockSurface.coreOfAny(level, ctx.getClickedPos());
        if (target == null) target = ctx.getClickedPos();

        BlockEntity be = level.getBlockEntity(target);
        if (be instanceof LPW2BlockEntity && !(be instanceof AudioLoop))
            return InteractionResult.PASS;
        if (!(be instanceof Mufflable machine) || machine.isMuffled())
            return InteractionResult.PASS;

        if (!level.isClientSide()) {
            machine.setMuffled();
            Player player = ctx.getPlayer();
            level.playSound(
                    null,
                    player != null ? player.blockPosition() : target,
                    ModSounds.UPGRADE_PLUG.get(),
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F);
            ctx.getItemInHand().consume(1, player);
        }
        return InteractionResult.SUCCESS;
    }
}
