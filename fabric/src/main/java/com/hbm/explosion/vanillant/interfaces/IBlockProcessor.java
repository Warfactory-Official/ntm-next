// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.explosion.vanillant.interfaces;

import com.hbm.explosion.vanillant.ExplosionVNT;
import java.util.HashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;

public interface IBlockProcessor {

    void process(
            ExplosionVNT explosion,
            Level world,
            double x,
            double y,
            double z,
            HashSet<BlockPos> affectedBlocks);

    default Explosion.BlockInteraction interaction() {
        return Explosion.BlockInteraction.DESTROY_WITH_DECAY;
    }
}
