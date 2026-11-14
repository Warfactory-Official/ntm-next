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

package com.hbm.items.tool;

import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.PlayerInformLayout;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public class PollutionDetectorItem extends Item {
    private static final int SOOT_ID = 100, POISON_ID = 101, METAL_ID = 102;
    private static final int LIFETIME = 4000;
    private static final int CADENCE = 10;

    public PollutionDetectorItem(Properties props) {
        super(props);
    }

    @Override
    public void inventoryTick(
            ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        if (!(owner instanceof ServerPlayer player) || level.getGameTime() % CADENCE != 0) return;

        float[] values = new float[PollutionType.COUNT];
        PollutionHandler.sample(level, player.blockPosition(), values);

        line(player, SOOT_ID, PollutionType.SOOT, values[PollutionType.SOOT.ordinal()]);
        line(player, POISON_ID, PollutionType.POISON, values[PollutionType.POISON.ordinal()]);
        line(
                player,
                METAL_ID,
                PollutionType.HEAVYMETAL,
                values[PollutionType.HEAVYMETAL.ordinal()]);
    }

    private static void line(ServerPlayer player, int id, PollutionType type, float value) {
        PacketWire.sendTo(PlayerInformLayout.encode(readout(type, value), id, LIFETIME), player);
    }

    public static Component readout(PollutionType type, float value) {
        float shown = ((int) (value * 100F)) / 100F;
        String key =
                switch (type) {
                    case SOOT -> "pollution.soot";
                    case POISON -> "pollution.poison";
                    case HEAVYMETAL -> "pollution.heavymetal";
                    default -> throw new IllegalArgumentException("No detector line for " + type);
                };
        return Component.translatable(key)
                .append(Component.translatable("desc.item.pollutionDetector.value", shown))
                .withStyle(ChatFormatting.YELLOW);
    }
}
