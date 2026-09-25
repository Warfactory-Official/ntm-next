// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.bomb.BlockBarrelExplosive;
import com.hbm.blocks.bomb.BlockTaint;
import com.hbm.entity.item.EntityTntNtm;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class BlockTaintBarrel extends BlockBarrelExplosive {

    private static final int SPLASHES = 100;
    private static final int SPREAD = 4;

    public BlockTaintBarrel(BlockBehaviour.Properties props) {

        super(props, 100, false);
    }

    @Override
    public void explodeEntity(
            Level level, double x, double y, double z, @Nullable EntityTntNtm entity) {
        level.explode(entity, x, y, z, 1F, false, Level.ExplosionInteraction.NONE);
        if (!(level instanceof ServerLevel server)) return;

        BlockPos centre = BlockPos.containing(x, y, z);
        RandomSource rand = server.getRandom();
        BlockState taint = ModBlocks.TAINT.get().defaultBlockState();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int i = 0; i < SPLASHES; i++) {
            cursor.set(
                    centre.getX() + rand.nextInt(9) - SPREAD,
                    centre.getY() + rand.nextInt(9) - SPREAD,
                    centre.getZ() + rand.nextInt(9) - SPREAD);

            if (!server.getBlockState(cursor).isSolidRender()) continue;
            server.setBlock(
                    cursor,
                    taint.setValue(BlockTaint.AGE, rand.nextInt(3) + 4),
                    Block.UPDATE_CLIENTS);
        }
    }
}
