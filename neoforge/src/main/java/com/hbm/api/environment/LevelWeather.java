// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.environment;

import java.util.Arrays;
import net.minecraft.server.level.ServerLevel;

public final class LevelWeather {

    public interface Owner {
        boolean advance(ServerLevel level);

        default void sleptThrough(ServerLevel level) {}
    }

    private static Owner[] owners = new Owner[0];

    private LevelWeather() {}

    public static void register(Owner owner) {
        owners = Arrays.copyOf(owners, owners.length + 1);
        owners[owners.length - 1] = owner;
    }

    public static boolean advance(ServerLevel level) {
        for (Owner owner : owners) {
            if (owner.advance(level)) return true;
        }
        return false;
    }

    public static void sleptThrough(ServerLevel level) {
        for (Owner owner : owners) owner.sleptThrough(level);
    }
}
