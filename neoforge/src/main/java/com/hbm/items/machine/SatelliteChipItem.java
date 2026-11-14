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

package com.hbm.items.machine;

import com.hbm.items.SatelliteFrequencyProvider;
import com.hbm.items.special.SatelliteItem;
import com.hbm.saveddata.satellites.SatelliteType;
import com.hbm.util.TooltipStyle;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class SatelliteChipItem extends Item implements SatelliteFrequencyProvider {
    private final @Nullable String description;

    public SatelliteChipItem(Properties properties) {
        this(properties, null);
    }

    public SatelliteChipItem(Properties properties, @Nullable String description) {
        super(properties);
        this.description = description;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        Runnable ntm =
                () -> {
                    adder.accept(
                            Component.translatable("satchip.frequency")
                                    .append(": " + SatelliteFrequencyProvider.frequency(stack))
                                    .withStyle(ChatFormatting.AQUA));
                    if (description == null) return;
                    for (Component line : TooltipStyle.lines(Component.translatable(description)))
                        adder.accept(line);
                };
        SatelliteType type = SatelliteType.fromStack(stack);
        if (type == null) ntm.run();
        else SatelliteItem.DESCRIBE.get().describe(stack, type, context, flag, adder, ntm);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        SatelliteType type = SatelliteType.fromStack(stack);
        return type == null
                ? super.use(level, player, hand)
                : SatelliteItem.USE.get().use(stack, type, level, player, hand);
    }
}
