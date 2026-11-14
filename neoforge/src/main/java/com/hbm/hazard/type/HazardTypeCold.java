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

package com.hbm.hazard.type;

import com.hbm.data.RadiationData;
import com.hbm.handler.ArmorUtil;
import com.hbm.hazard.helper.HazardHelper;
import com.hbm.hazard.modifier.HazardModifier;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class HazardTypeCold implements HazardKind {

    @Override
    public void onUpdate(LivingEntity target, float base, ItemStack stack) {
        double level = base;
        boolean reacher = HazardHelper.isHoldingReacher(target);
        if (RadiationData.DISABLE_COLD.get() || reacher) return;
        if (target instanceof Player && ArmorUtil.checkForHazmat(target)) return;

        int baseLevel = (int) level - 1;
        int witherLevel = (int) level - 3;

        target.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 110, baseLevel));
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 110, Math.min(4, baseLevel)));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 110, baseLevel));

        if (level > 4) {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 110, witherLevel));
        }
    }

    @Override
    public void onDropped(ItemEntity item, float level) {}

    @Override
    public void appendTooltip(
            Player player,
            List<Component> list,
            float level,
            ItemStack stack,
            List<HazardModifier> modifiers) {
        list.add(
                Component.literal("[")
                        .append(Component.translatable("trait.cryogenic"))
                        .append("]")
                        .withStyle(ChatFormatting.AQUA));
    }
}
