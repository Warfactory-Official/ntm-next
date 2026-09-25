// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.feature;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.storage.BlockSafe;
import com.hbm.itempool.ComponentLoot;
import com.hbm.tileentity.machine.storage.BlockEntityCrate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public final class LockedSafeFeature extends Feature<NoneFeatureConfiguration> {

    public LockedSafeFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos pos = context.origin();
        if (!canPlaceTorchOnTop(level.getBlockState(pos.below()))) return false;

        BlockState state =
                ModBlocks.SAFE
                        .get()
                        .defaultBlockState()
                        .setValue(
                                BlockSafe.FACING, Direction.from3DDataValue(random.nextInt(4) + 2));
        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
        if (!(level.getBlockEntity(pos) instanceof BlockEntityCrate safe)) return false;

        ComponentLoot loot;
        switch (random.nextInt(10)) {
            case 0, 1, 2, 3 -> {
                safe.setMod(1);
                loot = ComponentLoot.VAULT_RUSTY_3_6_UNSCALED;
            }
            case 4, 5, 6 -> {
                safe.setMod(0.1);
                loot = ComponentLoot.VAULT_STANDARD_2_4_UNSCALED;
            }
            case 7, 8 -> {
                safe.setMod(0.02);
                loot = ComponentLoot.VAULT_REINFORCED_1_3_UNSCALED;
            }
            default -> {
                safe.setMod(0.0);
                loot = ComponentLoot.VAULT_UNBREAKABLE_1_2_UNSCALED;
            }
        }
        safe.setLootTable(loot.table(), random.nextLong());
        safe.setPins(random.nextInt(999) + 1);
        safe.lock();
        if (random.nextInt(10) < 3) safe.fillWithSpiders();
        return true;
    }

    static boolean canPlaceTorchOnTop(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof SlabBlock) return state.getValue(SlabBlock.TYPE) != SlabType.BOTTOM;
        if (block instanceof FarmlandBlock) return false;
        if (block instanceof StairBlock) return state.getValue(StairBlock.HALF) == Half.TOP;
        if (block instanceof SnowLayerBlock)
            return state.getValue(SnowLayerBlock.LAYERS) == SnowLayerBlock.MAX_HEIGHT;
        if (block instanceof HopperBlock || state.is(Blocks.REDSTONE_BLOCK)) return true;
        if (state.isSolidRender() && !state.isSignalSource()) return true;
        return state.is(BlockTags.WOODEN_FENCES)
                || state.is(Blocks.NETHER_BRICK_FENCE)
                || state.is(Blocks.GLASS)
                || state.is(Blocks.COBBLESTONE_WALL)
                || state.is(Blocks.MOSSY_COBBLESTONE_WALL);
    }
}
