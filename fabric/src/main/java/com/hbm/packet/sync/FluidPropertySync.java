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

package com.hbm.packet.sync;

import com.hbm.blocks.network.ChunkDuctNodes;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.FluidPropertySyncLayout;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class FluidPropertySync {
    private FluidPropertySync() {}

    public static void resendIfPending(MinecraftServer server) {
        if (!NTMFluidProperties.takeResyncPending()) return;
        ChunkDuctNodes.scheduleReload(server);
        List<ServerPlayer> remote = new ArrayList<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!server.isSingleplayerOwner(player.nameAndId())) remote.add(player);
        }
        if (!remote.isEmpty()) {
            PacketWire.sendToPlayers(
                    FluidPropertySyncLayout.encode(NTMFluidProperties.snapshot()), remote);
        }
    }
}
