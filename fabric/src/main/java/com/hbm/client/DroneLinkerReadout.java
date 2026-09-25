// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.items.ModDataComponents;
import com.hbm.items.tool.ItemDroneLinker;
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
        if (!(stack.getItem() instanceof ItemDroneLinker)) return;

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
