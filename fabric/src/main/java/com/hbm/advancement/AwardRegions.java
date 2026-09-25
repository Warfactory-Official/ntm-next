// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.advancement;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
        for (ServerPlayer player : level.players()) award.accept(player);
    }
}
