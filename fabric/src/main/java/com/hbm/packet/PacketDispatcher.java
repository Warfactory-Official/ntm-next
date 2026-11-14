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

package com.hbm.packet;

import com.hbm.config.ModeStatus;
import com.hbm.packet.layout.PacketLayoutRegistry;
import com.hbm.packet.sync.FluidPropertySync;
import com.hbm.platform.ServerEvents;
import com.hbm.qmaw.QMAWCatalog;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public final class PacketDispatcher {
    private PacketDispatcher() {}

    public static void register() {
        PacketLayoutRegistry.registerAll();
        final Consumer<ServerPlayer> callback = ModeStatus::sendTo;
        ServerEvents.JOIN.add(callback);
        final Consumer<MinecraftServer> callback2 = FluidPropertySync::resendIfPending;
        ServerEvents.TICK_PRE.add(callback2);
        final Consumer<MinecraftServer> callback3 = server -> QMAWCatalog.clearServer();
        ServerEvents.STOPPING.add(callback3);
        final BiConsumer<Entity, ServerPlayer> callback4 = PlayerAppearanceSync::sendInitial;
        EntityTrackingEvents.START_TRACKING.register(callback4::accept);
    }
}
