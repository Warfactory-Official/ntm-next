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
import com.hbm.hazard.modifier.HazardModifier;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class HazardTypeHydroactive implements HazardKind {

    @Override
    public void onUpdate(LivingEntity target, float base, ItemStack stack) {
        double level = base;
        if (RadiationData.DISABLE_HYDRO.get()) return;
        if (target.isInWaterOrRain() && stack.getCount() > 0) {
            stack.setCount(0);
            target.level()
                    .explode(
                            null,
                            target.getX(),
                            target.getEyeY(),
                            target.getZ(),
                            (float) level,
                            false,
                            Level.ExplosionInteraction.BLOCK);
        }
    }

    @Override
    public void onDropped(ItemEntity item, float base) {
        double level = base;
        if (RadiationData.DISABLE_HYDRO.get()) return;
        if (item.isInWaterOrRain()) {
            item.discard();
            item.level()
                    .explode(
                            null,
                            item.getX(),
                            item.getY() + item.getBbHeight() * 0.5,
                            item.getZ(),
                            (float) level,
                            false,
                            Level.ExplosionInteraction.BLOCK);
        }
    }

    @Override
    public void appendTooltip(
            Player player,
            List<Component> list,
            float level,
            ItemStack stack,
            List<HazardModifier> modifiers) {
        list.add(
                Component.literal("[")
                        .append(Component.translatable("trait.hydro"))
                        .append("]")
                        .withStyle(ChatFormatting.RED));
    }
}
