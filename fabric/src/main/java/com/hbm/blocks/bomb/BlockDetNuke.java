// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.bomb;

import com.hbm.config.BombConfig;
import com.hbm.data.ExplosionData;
import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class BlockDetNuke extends BlockExplosiveCharge {

    public BlockDetNuke(Properties props) {
        super(props);
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, @Nullable Entity detonator) {
        if (level.isClientSide()) return BombReturnCode.DETONATED;
        if (!TntRule.explodes(level)) return BombReturnCode.ERROR_TNT_DISABLED;
        level.removeBlock(pos, false);

        double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
        level.addFreshEntity(
                EntityNukeExplosionMK5.statFac(level, ExplosionData.MISSILE_RADIUS.get(), x, y, z));
        EntityNukeTorex.statFac(level, x, y, z, ExplosionData.MISSILE_RADIUS.get());

        return BombReturnCode.DETONATED;
    }
}
