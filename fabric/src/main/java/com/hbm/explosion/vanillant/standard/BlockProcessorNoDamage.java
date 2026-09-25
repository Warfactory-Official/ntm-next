// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.explosion.vanillant.standard;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.interfaces.IBlockMutator;
import com.hbm.explosion.vanillant.interfaces.IBlockProcessor;
import java.util.HashSet;
import java.util.Iterator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class BlockProcessorNoDamage implements IBlockProcessor {

    protected IBlockMutator convert;

    public BlockProcessorNoDamage() {}

    public BlockProcessorNoDamage withBlockEffect(IBlockMutator convert) {
        this.convert = convert;
        return this;
    }

    @Override
    public void process(
            ExplosionVNT explosion,
            Level world,
            double x,
            double y,
            double z,
            HashSet<BlockPos> affectedBlocks) {

        Iterator<BlockPos> iterator = affectedBlocks.iterator();

        while (iterator.hasNext()) {
            BlockPos pos = iterator.next();
            BlockState state = world.getBlockState(pos);

            if (!state.isAir()) {
                if (convert != null) convert.mutatePre(explosion, state, pos);
            }
        }

        if (convert != null) {
            for (BlockPos pos : affectedBlocks) {
                if (world.getBlockState(pos).isAir()) {
                    convert.mutatePost(explosion, pos);
                }
            }
        }

        affectedBlocks.clear();
    }
}
