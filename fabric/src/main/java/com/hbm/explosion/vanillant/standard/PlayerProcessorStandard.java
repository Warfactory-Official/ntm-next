// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.explosion.vanillant.standard;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.interfaces.IPlayerProcessor;
import java.util.HashMap;
import java.util.Map.Entry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class PlayerProcessorStandard implements IPlayerProcessor {

    @Override
    public void process(
            ExplosionVNT explosion,
            Level world,
            double x,
            double y,
            double z,
            HashMap<Player, Vec3> affectedPlayers) {

        for (Entry<Player, Vec3> entry : affectedPlayers.entrySet()) {
            if (entry.getKey() instanceof ServerPlayer player) {
                player.hurtMarked = true;
            }
        }
    }
}
