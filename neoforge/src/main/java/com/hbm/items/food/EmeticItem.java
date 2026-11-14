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

package com.hbm.items.food;

import com.hbm.items.special.CustomLoreItem;
import com.hbm.sound.EntitySounds;
import com.hbm.sound.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class EmeticItem extends CustomLoreItem {
    private static final int HUNGER_TICKS = 50;
    private static final int HUNGER_AMPLIFIER = 49;

    public EmeticItem(Properties props) {
        super(props);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack rest = super.finishUsingItem(stack, level, entity);
        entity.addEffect(new MobEffectInstance(MobEffects.HUNGER, HUNGER_TICKS, HUNGER_AMPLIFIER));
        if (level instanceof ServerLevel server) {
            EntitySounds.play(
                    entity,
                    entity.getX(),
                    entity.getY(),
                    entity.getZ(),
                    ModSounds.PLAYER_VOMIT.get(),
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F);
        }
        return rest;
    }
}
