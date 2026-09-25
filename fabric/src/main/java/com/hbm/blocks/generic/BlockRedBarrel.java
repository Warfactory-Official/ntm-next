// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.blocks.bomb.BlockBarrelExplosive;
import com.hbm.blocks.bomb.BlockDetonatable;
import com.hbm.blocks.bomb.TntRule;
import com.hbm.entity.item.EntityTntNtm;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.jspecify.annotations.Nullable;

public class BlockRedBarrel extends BlockBarrelExplosive implements BlockDetonatable {

    public BlockRedBarrel(BlockBehaviour.Properties props) {

        super(props, 100, true);
    }

    @Override
    public void explodeEntity(
            Level level, double x, double y, double z, @Nullable EntityTntNtm entity) {

        level.explode(entity, x, y, z, 2.5F, true, Level.ExplosionInteraction.TNT);
    }

    @Override
    public void onShot(Level level, BlockPos pos) {
        if (!TntRule.explodes(level)) return;
        level.removeBlock(pos, false);
        explodeEntity(level, pos.getX(), pos.getY(), pos.getZ(), null);
    }
}
