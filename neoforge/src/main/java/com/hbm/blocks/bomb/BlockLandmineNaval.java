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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlockLandmineNaval extends BlockLandmine {

    public BlockLandmineNaval(BlockBehaviour.Properties props) {
        super(props, 2.5D, 1D);
    }

    @Override
    protected VoxelShape shape() {
        return Shapes.block();
    }

    @Override
    protected void explodeVariant(Level level, BlockPos pos) {
        float damage = ExplosionData.MINE_NAVAL_DAMAGE.get().floatValue();
        ExplosionVNT vnt =
                new ExplosionVNT(level, pos.getX() + 5, pos.getY() + 5, pos.getZ() + 5, 25F);
        vnt.setBlockAllocator(new BlockAllocatorWater(32));
        vnt.setBlockProcessor(new BlockProcessorStandard());
        vnt.setEntityProcessor(new EntityProcessorCrossSmooth(0.5, damage).setupPiercing(5F, 0.2F));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.setSFX(new ExplosionEffectWeapon(10, 1F, 0.5F));
        vnt.explode();

        ExplosionLarge.spawnParticlesRadial(
                level, pos.getX() + 0.5, pos.getY() + 2, pos.getZ() + 0.5, 30);
        ExplosionLarge.spawnRubble(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 5);
        if (isWaterAbove(level, pos)) {
            ExplosionLarge.spawnFoam(
                    level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 60);
        }
    }

    private static boolean isWaterAbove(Level level, BlockPos pos) {
        for (int xo = -1; xo <= 1; xo++) {
            for (int zo = -1; zo <= 1; zo++) {
                if (level.getBlockState(pos.offset(xo, 1, zo)).is(Blocks.WATER)) return true;
            }
        }
        return false;
    }
}
