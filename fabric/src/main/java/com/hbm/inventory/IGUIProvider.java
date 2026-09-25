// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory;

import com.hbm.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

public interface IGUIProvider extends MenuProvider {

    static void openBlockMenu(Player player, MenuProvider provider, BlockPos pos) {
        openBlockMenu(player, provider, pos, 0);
    }

    static void openBlockMenu(Player player, MenuProvider provider, BlockPos pos, int dispatchId) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        if (provider instanceof RandomizableContainer container) {
            if (container.getLootTable() != null && serverPlayer.isSpectator()) return;
            container.unpackLootTable(serverPlayer);
        }
        Services.PLATFORM.openMenu(serverPlayer, provider, new BlockMenuContext(pos, dispatchId));
    }

    static AbstractContainerMenu createDispatchedMenu(
            MenuProvider provider,
            int containerId,
            Inventory inventory,
            Player player,
            int dispatchId) {
        if (provider instanceof IGUIProvider dispatched) {
            return dispatched.createMenu(containerId, inventory, player, dispatchId);
        }
        if (dispatchId != 0) {
            throw new IllegalArgumentException(
                    "Unsupported GUI dispatch ID "
                            + dispatchId
                            + " for "
                            + provider.getClass().getName());
        }
        return provider.createMenu(containerId, inventory, player);
    }

    default AbstractContainerMenu createMenu(
            int containerId, Inventory inventory, Player player, int dispatchId) {
        if (dispatchId != 0) {
            throw new IllegalArgumentException(
                    "Unsupported GUI dispatch ID " + dispatchId + " for " + getClass().getName());
        }
        return createMenu(containerId, inventory, player);
    }

    default void openMenu(Player player, BlockPos pos) {
        openMenu(player, pos, 0);
    }

    default void openMenu(Player player, BlockPos pos, int dispatchId) {
        openBlockMenu(player, this, pos, dispatchId);
    }
}
