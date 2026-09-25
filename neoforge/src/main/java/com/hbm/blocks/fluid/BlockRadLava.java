// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.fluid;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.BlockSellafieldSlaked;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import com.hbm.util.ContaminationUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class BlockRadLava extends BlockVolcanicLava {

    public BlockRadLava(ClassicFluid fluid, Properties props) {
        super(fluid, props);
    }

    @Override
    protected Block basaltForCheck() {
        return ModBlocks.SELLAFIELD_SLAKED.get();
    }

    @Override
    protected void entityInside(
            BlockState state,
            Level level,
            BlockPos pos,
            Entity entity,
            InsideBlockEffectApplier effectApplier,
            boolean isPrecise) {
        if (level.isClientSide() || !(entity instanceof LivingEntity living)) return;
        ContaminationUtil.contaminate(living, HazardType.RADIATION, ContaminationType.CREATIVE, 5F);
    }

    @Override
    protected @Nullable Block getReaction(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (isWater(state)) return Blocks.STONE;
        if (state.is(BlockTags.LOGS)) return ModBlocks.WASTE_LOG.get();
        if (state.is(BlockTags.PLANKS)) return wastePlanks();
        if (state.is(BlockTags.LEAVES)) return Blocks.FIRE;
        if (state.is(Blocks.DIAMOND_ORE)) return ModBlocks.ORE_SELLAFIELD_RADGEM.get();
        if (state.is(ModBlocks.ORE_URANIUM.get()) || state.is(requireBlock("ore_gneiss_uranium"))) {
            return level.getRandom().nextInt(5) == 0
                    ? ModBlocks.ORE_SELLAFIELD_SCHRABIDIUM.get()
                    : ModBlocks.ORE_SELLAFIELD_URANIUM_SCORCHED.get();
        }
        return null;
    }

    @Override
    protected void onSolidify(
            ServerLevel level, BlockPos pos, int lavaCount, int basaltCount, RandomSource rand) {
        int r = rand.nextInt(400);
        Block slaked = basaltForCheck();
        Block above = level.getBlockState(pos.above(10)).getBlock();
        boolean canMakeGem =
                lavaCount + basaltCount == 6 && lavaCount < 3 && (above == slaked || above == this);

        Block result;
        if (r < 2) result = ModBlocks.ORE_SELLAFIELD_DIAMOND.get();
        else if (r == 2) result = ModBlocks.ORE_SELLAFIELD_EMERALD.get();
        else if (r < 20 && canMakeGem) result = ModBlocks.ORE_SELLAFIELD_RADGEM.get();
        else result = slaked;

        level.setBlockAndUpdate(
                pos,
                result.defaultBlockState()
                        .setValue(BlockSellafieldSlaked.SHADE, 5 + rand.nextInt(3)));
    }
}
