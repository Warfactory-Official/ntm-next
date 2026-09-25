// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine.pile;

import com.hbm.packet.toclient.MarkerPayload;
import com.hbm.platform.Services;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class PileError {

    private final Map<BlockPos, Component> markers = new LinkedHashMap<>();

    public static void send(BlockPos target, Component message, Player player) {
        PileError error = new PileError();
        error.add(target, message);
        error.send(player);
    }

    public void add(BlockPos pos, Component message) {
        markers.put(pos, message);
    }

    public boolean send(Player player) {
        if (markers.isEmpty()) return false;
        if (player instanceof ServerPlayer sp) {
            Services.NETWORK.sendTo(new MarkerPayload(0xff0000, 5000, 128.0, markers), sp);
        }
        return true;
    }
}
