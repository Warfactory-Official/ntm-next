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

package com.hbm.advancement;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.phys.AABB;

public final class AwardRegions {

    private AwardRegions() {}

    public static void nearby(ServerLevel level, AABB box, Consumer<ServerPlayer> award) {
        List<ServerPlayer> players = level.getEntitiesOfClass(ServerPlayer.class, box);
        for (ServerPlayer player : players) award.accept(player);
    }

    public static void within(
            ServerLevel level,
            double x,
            double y,
            double z,
            double radius,
            Consumer<ServerPlayer> award) {
        nearby(level, new AABB(x, y, z, x, y, z).inflate(radius), award);
    }

    public static void inLevel(ServerLevel level, Consumer<ServerPlayer> award) {
        for (ServerPlayer player : level.getPlayers(EntitySelector.NO_SPECTATORS))
            award.accept(player);
    }
}
