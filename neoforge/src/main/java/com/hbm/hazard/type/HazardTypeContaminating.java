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
import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.hazard.modifier.HazardModifier;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class HazardTypeContaminating implements HazardKind {

    private static final int MAX_RADIUS = 500;

    private static int computeRadius(double level) {
        return (int) Math.min(Math.sqrt(level) + 0.5D, MAX_RADIUS);
    }

    @Override
    public void onUpdate(LivingEntity target, float level, ItemStack stack) {}

    @Override
    public void onDropped(ItemEntity item, float base) {
        double level = base;
        if (!RadiationData.ENABLE_CONTAMINATION_ON_GROUND.get()) return;
        Level world = item.level();
        if (!(world instanceof ServerLevel server)) return;
        if (!item.onGround()) return;

        int radius = computeRadius(level);
        if (radius > 1) {
            BlockPos pos = item.blockPosition();
            RadiationSystemNT.incrementRad(server, pos, level, level * 1024D);
        }
        item.discard();
    }

    @Override
    public void appendTooltip(
            Player player,
            List<Component> list,
            float base,
            ItemStack stack,
            List<HazardModifier> modifiers) {
        double level = base;
        if (!RadiationData.ENABLE_CONTAMINATION_ON_GROUND.get()) return;
        int radius = computeRadius(level);
        if (radius > 1) {
            list.add(
                    Component.literal("[")
                            .append(Component.translatable("trait.contaminating"))
                            .append("]")
                            .withStyle(ChatFormatting.DARK_GREEN));
            list.add(
                    Component.literal(" ")
                            .append(Component.translatable("trait.contaminating.radius", radius))
                            .withStyle(ChatFormatting.GREEN));
        }
    }
}
