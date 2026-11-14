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

import com.hbm.inventory.container.HeldItemMenu;
import com.hbm.inventory.container.ModMenus;
import com.hbm.items.ModDataComponents;
import com.hbm.items.SatelliteFrequencyProvider;
import com.hbm.items.machine.SatelliteChipItem;
import com.hbm.saveddata.SatelliteSavedData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class SatelliteInterfaceItem extends SatelliteChipItem {

    public static final String KEY_CONNECTED = "connected";

    public SatelliteInterfaceItem(Properties properties) {
        super(properties);
    }

    public void setCoordinates(ServerPlayer player, ItemStack stack, int x, int y, int z) {
        var satellite =
                SatelliteSavedData.at(player.level(), player.getBlockX(), player.getBlockZ())
                        .getSatFromFreq(SatelliteFrequencyProvider.frequency(stack));
        if (satellite != null) satellite.designate(player.level(), player, x, y, z);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        HeldItemMenu.open(
                player,
                hand,
                ModMenus.SATELLITE_INTERFACE,
                Component.translatable("item.hbm.sat_coord"));
        return InteractionResult.CONSUME;
    }

    @Override
    public void inventoryTick(
            ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        if (!(entity instanceof ServerPlayer player)) return;
        if (player.getMainHandItem() != stack
                && player.getItemInHand(InteractionHand.OFF_HAND) != stack) return;
        boolean connected =
                SatelliteSavedData.at(level, player.getBlockX(), player.getBlockZ())
                                .getSatFromFreq(SatelliteFrequencyProvider.frequency(stack))
                        != null;
        CustomData data = stack.get(ModDataComponents.PERSISTENT_DATA.get());
        if (data != null && data.copyTag().getBooleanOr(KEY_CONNECTED, false) == connected) return;
        if (data == null && !connected) return;
        CustomData.update(
                ModDataComponents.PERSISTENT_DATA.get(),
                stack,
                tag -> tag.putBoolean(KEY_CONNECTED, connected));
    }
}
