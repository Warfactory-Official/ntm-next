// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.bomb;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.BlockFissure;
import com.hbm.entity.item.EntityTntNtm;
import com.hbm.explosion.ExplosionNukeSmall;
import com.hbm.util.BiomeUtil;
import com.hbm.world.biome.NtmBiomes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class BlockFissureBomb extends BlockTNTBase {

    private static final int RANGE = 5;

    public BlockFissureBomb(Properties props) {
        super(props, 20);
    }

    @Override
    public void explodeEntity(Level level, double x, double y, double z, EntityTntNtm entity) {
        ExplosionNukeSmall.explode(level, x, y, z, ExplosionNukeSmall.PARAMS_MEDIUM);

        BlockPos centre = BlockPos.containing(x, y, z);
        boolean crater =
                NtmBiomes.isCrater(
                        level instanceof ServerLevel server
                                ? BiomeUtil.biomeAt(server, centre)
                                : level.getBiome(centre));
        BlockState fissure =
                ModBlocks.ORE_VOLCANO
                        .get()
                        .defaultBlockState()
                        .setValue(BlockFissure.CRATER, crater);

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int i = -RANGE; i <= RANGE; i++) {
            for (int j = -RANGE; j <= RANGE; j++) {
                for (int k = -RANGE; k <= RANGE; k++) {
                    cursor.set(centre.getX() + i, centre.getY() + j, centre.getZ() + k);
                    BlockState state = level.getBlockState(cursor);
                    if (state.is(ModBlocks.ORE_BEDROCK.get())) {
                        level.setBlock(cursor, fissure, Block.UPDATE_ALL);
                    } else if (state.is(ModBlocks.ORE_BEDROCK_OIL.get())) {
                        level.setBlock(
                                cursor, Blocks.BEDROCK.defaultBlockState(), Block.UPDATE_ALL);
                    }
                }
            }
        }
    }
}
