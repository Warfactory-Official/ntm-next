// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.blocks.bomb.BlockBarrelExplosive;
import com.hbm.entity.item.EntityTntNtm;
import com.hbm.explosion.ExplosionThermo;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.jspecify.annotations.Nullable;

public class BlockLoxBarrel extends BlockBarrelExplosive {

    private static final int FREEZER_RADIUS = 7;

    public BlockLoxBarrel(BlockBehaviour.Properties props) {

        super(props, 100, false);
    }

    @Override
    public void explodeEntity(
            Level level, double x, double y, double z, @Nullable EntityTntNtm entity) {
        level.explode(entity, x, y, z, 1F, false, Level.ExplosionInteraction.NONE);
        ExplosionThermo.freezer(level, BlockPos.containing(x, y, z), FREEZER_RADIUS);
    }
}
