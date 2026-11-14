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

import com.hbm.NuclearTech;
import com.hbm.config.HbmConfig;
import com.hbm.config.RuntimeConfig;
import com.hbm.extprop.LivingHazards;
import com.hbm.handler.EntityEffectHandler;
import com.hbm.sound.EntitySounds;
import com.hbm.sound.ModSounds;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class SyringeMkuItem extends Item {

    public SyringeMkuItem(Properties props) {
        super(props);
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!(target.level() instanceof ServerLevel level)) return;
        LivingHazards.getOrCreate(target).setContagion(EntityEffectHandler.CONTAGION_DURATION);
        EntitySounds.play(
                target,
                target.getX(),
                target.getY(),
                target.getZ(),
                ModSounds.ITEM_SYRINGE.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F);
        stack.shrink(1);
        if (((RuntimeConfig) HbmConfig.RUNTIME).extendedLogging()) {
            NuclearTech.LOGGER.info(
                    "[MKU] {} used an MKU syringe!", attacker.getName().getString());
        }
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag tooltipFlag) {
        adder.accept(Component.literal("?").withStyle(ChatFormatting.RED));
    }
}
