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
import com.hbm.items.tool.WiringItem;
import com.hbm.items.tool.WrenchItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public final class WiringReadout {
    public static final int ID_CABLE = 3;
    private static final int ID_WRENCH = 13;
    public static final int MILLIS = 1_000;

    private WiringReadout() {}

    public static void clientTick() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        for (ItemStack stack : player.getInventory()) {
            if (stack.getItem() instanceof WiringItem) {
                push(player, stack, stack.get(ModDataComponents.WIRE_TARGET.get()), ID_CABLE);
            } else if (stack.getItem() instanceof WrenchItem) {
                push(player, stack, stack.get(ModDataComponents.WRENCH_TARGET.get()), ID_WRENCH);
            }
        }
    }

    private static void push(LocalPlayer player, ItemStack stack, @Nullable Long stored, int id) {
        if (stored == null) return;
        BlockPos start = BlockPos.of(stored);
        double dx = player.getX() - start.getX();
        double dy = player.getY() - start.getY();
        double dz = player.getZ() - start.getZ();
        int metres = (int) Math.sqrt(dx * dx + dy * dy + dz * dz);
        InfoSystem.push(
                new InfoSystem.InfoEntry(
                        Component.translatable(
                                "desc.item.wiring.distance", stack.getHoverName(), metres),
                        MILLIS),
                id);
    }
}
