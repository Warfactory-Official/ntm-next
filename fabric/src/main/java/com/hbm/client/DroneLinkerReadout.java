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

package com.hbm.client;

import com.hbm.items.ModDataComponents;
import com.hbm.items.tool.DroneLinkerItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class DroneLinkerReadout {
    private static final int ID_DRONE = 4;
    private static final int MILLIS = 1_000;

    private DroneLinkerReadout() {}

    public static void clientTick() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof DroneLinkerItem)) return;

        Long stored = stack.get(ModDataComponents.DRONE_LINK_TARGET.get());
        if (stored == null) return;

        BlockPos previous = BlockPos.of(stored);
        InfoSystem.push(
                new InfoSystem.InfoEntry(
                        Component.translatable(
                                "desc.item.droneLinker.prevPos",
                                previous.getX(),
                                previous.getY(),
                                previous.getZ()),
                        MILLIS),
                ID_DRONE);
    }
}
