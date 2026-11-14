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

import com.hbm.config.RadiationConfig;
import com.hbm.hazard.modifier.HazardModifier;
import com.hbm.util.ContaminationUtil;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class HazardTypeDigamma implements HazardKind {

    @Override
    public void onUpdate(LivingEntity target, float base, ItemStack stack) {
        double level = base;
        ContaminationUtil.applyDigammaData(target, (level / 20D) * RadiationConfig.hazardRate);
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
        double displayLevel = Math.floor(level * 10000D) / 10D;
        list.add(
                Component.literal("[")
                        .append(Component.translatable("trait.digamma"))
                        .append("]")
                        .withStyle(ChatFormatting.RED));
        list.add(Component.literal(displayLevel + "mDRX/s").withStyle(ChatFormatting.DARK_RED));
        if (stack.getCount() > 1) {
            double stackLevel = Math.floor(level * 10000D * stack.getCount()) / 10D;
            list.add(
                    Component.translatable(
                                    "desc.misc.hazardTypeDigamma.stack", stackLevel + "mDRX/s")
                            .withStyle(ChatFormatting.DARK_RED));
        }
    }
}
