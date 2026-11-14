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

package com.hbm.blocks.machine.pile;

import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.MarkerLayout;
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
            PacketWire.sendTo(MarkerLayout.encode(0xff0000, 5000, 128.0, markers), sp);
        }
        return true;
    }
}
