// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.bomb;

import com.hbm.data.ExplosionData;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.*;
import com.hbm.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlockLandmineShrap extends BlockLandmine {

    private static final VoxelShape SHAPE = Block.box(5, 0, 5, 11, 1, 11);

    public BlockLandmineShrap(BlockBehaviour.Properties props) {
        super(props, 1.5D, 1D);
    }

    @Override
    protected VoxelShape shape() {
        return SHAPE;
    }

    @Override
    protected void explodeVariant(Level level, BlockPos pos) {
        float damage = ExplosionData.MINE_SHRAP_DAMAGE.get().floatValue();
        ExplosionVNT vnt =
                new ExplosionVNT(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3F);
        vnt.setEntityProcessor(new EntityProcessorCrossSmooth(0.5, damage));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.setSFX(new ExplosionEffectWeapon(5, 1F, 0.5F));
        vnt.explode();

        ExplosionLarge.spawnShrapnelShower(
                level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0, 1D, 0, 45, 0.2D);
        ExplosionLarge.spawnShrapnels(
                level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 5);
    }
}
