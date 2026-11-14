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

import com.hbm.api.item.TargetDesignator;
import com.hbm.blocks.bomb.LaunchPadBlock;
import com.hbm.items.ModDataComponents;
import com.hbm.sound.EntitySounds;
import com.hbm.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class DesignatorRangeItem extends DesignatorItem {

    public DesignatorRangeItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        HitResult hit = player.pick(300.0D, 1.0F, false);
        if (hit instanceof BlockHitResult bhr
                && bhr.getType() == HitResult.Type.BLOCK
                && !(level.getBlockState(bhr.getBlockPos()).getBlock() instanceof LaunchPadBlock)) {
            if (!level.isClientSide()) {
                BlockPos pos = bhr.getBlockPos();
                player.getItemInHand(hand)
                        .set(
                                ModDataComponents.TARGET_DESIGNATOR.get(),
                                TargetDesignator.pack(pos.getX(), pos.getZ()));
                EntitySounds.play(
                        player,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        ModSounds.TECH_BLEEP.get(),
                        SoundSource.PLAYERS,
                        1.0F,
                        1.0F);
                player.sendSystemMessage(
                        Component.translatable(
                                "item.hbm.designator_range.set", pos.getX(), pos.getZ()));
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
