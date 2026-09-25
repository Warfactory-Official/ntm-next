// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.bomb;

import com.hbm.data.ExplosionData;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.*;
import com.hbm.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlockLandmineHE extends BlockLandmine {

    private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 2, 12);

    public BlockLandmineHE(BlockBehaviour.Properties props) {
        super(props, 2D, 5D);
    }

    @Override
    protected VoxelShape shape() {
        return SHAPE;
    }

    @Override
    protected void explodeVariant(Level level, BlockPos pos) {
        float damage = ExplosionData.MINE_HE_DAMAGE.get().floatValue();
        ExplosionVNT vnt =
                new ExplosionVNT(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4F);
        vnt.setBlockAllocator(new BlockAllocatorStandard());
        vnt.setBlockProcessor(new BlockProcessorStandard());
        vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, damage).setupPiercing(15F, 0.2F));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.setSFX(new ExplosionEffectWeapon(15, 3.5F, 1.25F));
        vnt.explode();
    }
}
