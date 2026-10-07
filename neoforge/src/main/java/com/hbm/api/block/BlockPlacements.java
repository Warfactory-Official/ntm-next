// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.block;

import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class BlockPlacements {

    @FunctionalInterface
    public interface Listener {
        void at(ServerLevel level, BlockPos pos);
    }

    @FunctionalInterface
    public interface PlayerListener {
        void at(ServerPlayer player, BlockPos pos);
    }

    private static Listener[] placed = new Listener[0];
    private static PlayerListener[] placedBy = new PlayerListener[0];
    private static Listener[] repaired = new Listener[0];

    private BlockPlacements() {}

    public static void onPlaced(Listener listener) {
        placed = append(placed, listener);
    }

    public static void onPlacedBy(PlayerListener listener) {
        placedBy = append(placedBy, listener);
    }

    public static void onRepaired(Listener listener) {
        repaired = append(repaired, listener);
    }

    public static void placed(ServerLevel level, BlockPos pos) {
        for (Listener listener : placed) listener.at(level, pos);
    }

    public static void placedBy(ServerPlayer player, BlockPos pos) {
        placed(player.level(), pos);
        for (PlayerListener listener : placedBy) listener.at(player, pos);
    }

    public static void repaired(ServerLevel level, BlockPos pos) {
        for (Listener listener : repaired) listener.at(level, pos);
    }

    private static <L> L[] append(L[] listeners, L listener) {
        L[] out = Arrays.copyOf(listeners, listeners.length + 1);
        out[listeners.length] = listener;
        return out;
    }
}
