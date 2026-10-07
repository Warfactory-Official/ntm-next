// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.entity;

import java.util.Arrays;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.TeleportTransition;
import org.jspecify.annotations.Nullable;

public final class PlayerRespawns {

    @FunctionalInterface
    public interface Rule {
        @Nullable TeleportTransition respawn(
                ServerPlayer player, TeleportTransition.PostTeleportTransition then);
    }

    private static Rule[] rules = new Rule[0];

    private PlayerRespawns() {}

    public static void register(Rule rule) {
        Rule[] out = Arrays.copyOf(rules, rules.length + 1);
        out[rules.length] = rule;
        rules = out;
    }

    public static @Nullable TeleportTransition respawn(
            ServerPlayer player, TeleportTransition.PostTeleportTransition then) {
        for (Rule rule : rules) {
            TeleportTransition transition = rule.respawn(player, then);
            if (transition != null) return transition;
        }
        return null;
    }
}
