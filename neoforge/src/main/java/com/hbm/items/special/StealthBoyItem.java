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

package com.hbm.items.special;

import com.hbm.sound.EntitySounds;
import com.hbm.sound.ModSounds;
import net.minecraft.SharedConstants;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class StealthBoyItem extends Item {

    public StealthBoyItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level instanceof ServerLevel server) {
            ItemStack stack = player.getItemInHand(hand);
            player.addEffect(
                    new MobEffectInstance(
                            MobEffects.INVISIBILITY,
                            30 * SharedConstants.TICKS_PER_SECOND,
                            1,
                            true,
                            true));
            EntitySounds.play(
                    player,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    ModSounds.ITEM_UNPACK.get(),
                    SoundSource.PLAYERS,
                    1F,
                    1F);
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }
}
