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

import com.hbm.extprop.PlayerState;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.Level;

public class FlaskItem extends Item {

    public static final Consumable CONSUMABLE =
            Consumable.builder()
                    .consumeSeconds(1.6F)
                    .animation(ItemUseAnimation.DRINK)
                    .sound(SoundEvents.GENERIC_DRINK)
                    .hasConsumeParticles(false)
                    .build();

    public final EnumInfusion type;

    public FlaskItem(Properties properties, EnumInfusion type) {
        super(properties);
        this.type = type;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (type == EnumInfusion.SHIELD
                && PlayerState.getOrCreate(player).maxShield >= PlayerState.SHIELD_CAP) {
            return InteractionResult.PASS;
        }
        return super.use(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide()
                && entity instanceof Player player
                && type == EnumInfusion.SHIELD) {
            PlayerState props = PlayerState.getOrCreate(player);
            props.maxShield = Math.min(PlayerState.SHIELD_CAP, props.maxShield + 5F);
            props.shield = Math.min(props.shield + 5F, props.shieldCapacity(player));
        }
        return result;
    }

    public enum EnumInfusion {
        SHIELD
    }
}
