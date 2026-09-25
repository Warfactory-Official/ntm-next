// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.explosion.vanillant.interfaces;

import com.hbm.explosion.vanillant.ExplosionVNT;
import java.util.HashMap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public interface IPlayerProcessor {

    void process(
            ExplosionVNT explosion,
            Level world,
            double x,
            double y,
            double z,
            HashMap<Player, Vec3> affectedPlayers);
}
