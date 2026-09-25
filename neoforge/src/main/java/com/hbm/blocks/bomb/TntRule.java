// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.bomb;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;

public final class TntRule {

    private TntRule() {}

    public static boolean explodes(Level level) {
        return level instanceof ServerLevel server
                && server.getGameRules().get(GameRules.TNT_EXPLODES);
    }
}
