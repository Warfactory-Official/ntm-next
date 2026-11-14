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

import com.hbm.config.BalanceConfig;
import com.hbm.data.RadiationData;
import com.hbm.hazard.helper.HazardHelper;
import com.hbm.hazard.modifier.HazardModifier;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class HazardTypeHot implements HazardKind {

    @Override
    public void onUpdate(LivingEntity target, float base, ItemStack stack) {
        double level = base;
        boolean wetOrReacher =
                (!BalanceConfig.enable528 && HazardHelper.isHoldingReacher(target))
                        || target.isInWaterOrRain();
        if (RadiationData.DISABLE_HOT.get() || wetOrReacher || level <= 0) return;
        if (target.getRemainingFireTicks() > 0) return;
        target.igniteForSeconds((int) Math.ceil(level));
    }

    @Override
    public void onDropped(ItemEntity item, float level) {}

    @Override
    public void appendTooltip(
            Player player,
            List<Component> list,
            float base,
            ItemStack stack,
            List<HazardModifier> modifiers) {
        double level = HazardModifier.evalAllModifiers(stack, player, base, modifiers);
        if (level > 0) {
            list.add(
                    Component.literal("[")
                            .append(Component.translatable("trait.hot"))
                            .append("]")
                            .withStyle(ChatFormatting.GOLD));
        }
    }
}
