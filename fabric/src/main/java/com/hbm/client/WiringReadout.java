// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.items.ModDataComponents;
import com.hbm.items.tool.ItemWiring;
import com.hbm.items.tool.ItemWrench;
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
            if (stack.getItem() instanceof ItemWiring) {
                push(player, stack, stack.get(ModDataComponents.WIRE_TARGET.get()), ID_CABLE);
            } else if (stack.getItem() instanceof ItemWrench) {
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
