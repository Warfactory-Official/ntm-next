// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.bomb;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.particle.helper.ExplosionCreator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class BlockDetCharge extends BlockExplosiveCharge {

    public BlockDetCharge(Properties props) {
        super(props);
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, @Nullable Entity detonator) {
        if (level.isClientSide()) return BombReturnCode.DETONATED;
        if (!TntRule.explodes(level)) return BombReturnCode.ERROR_TNT_DISABLED;
        level.removeBlock(pos, false);

        double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
        ExplosionVNT xnt = new ExplosionVNT(level, x, y, z, 15F);
        xnt.setBlockAllocator(new BlockAllocatorStandard(64));
        xnt.setBlockProcessor(new BlockProcessorStandard());
        xnt.setEntityProcessor(new EntityProcessorCrossSmooth(1D, 15F * 7F));
        xnt.setPlayerProcessor(new PlayerProcessorStandard());
        xnt.explode();
        ExplosionCreator.composeEffectStandard(level, x, pos.getY() + 1.0D, z);

        return BombReturnCode.DETONATED;
    }
}
