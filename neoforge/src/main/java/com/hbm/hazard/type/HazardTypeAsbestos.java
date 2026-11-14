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
import com.hbm.data.RadiationData;
import com.hbm.extprop.LivingHazards;
import com.hbm.handler.ArmorUtil;
import com.hbm.hazard.Exposure;
import com.hbm.hazard.modifier.HazardModifier;
import com.hbm.util.ArmorRegistry;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class HazardTypeAsbestos implements HazardKind {

    @Override
    public void onUpdate(LivingEntity target, float base, ItemStack stack) {
        double level = base;
        if (RadiationData.DISABLE_ASBESTOS.get()) return;
        if (ArmorRegistry.hasProtection(target, EquipmentSlot.HEAD, Exposure.PARTICLE_FINE)) {
            ArmorUtil.damageGasMaskFilter(target, (int) level * RadiationConfig.hazardRate);
        } else {
            LivingHazards.addAsbestos(
                    target, (int) Math.min(level, 10) * RadiationConfig.hazardRate);
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
                        .append(Component.translatable("trait.asbestos"))
                        .append("]")
                        .withStyle(ChatFormatting.WHITE));
    }
}
