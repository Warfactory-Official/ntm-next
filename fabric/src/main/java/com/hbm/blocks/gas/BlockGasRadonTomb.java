// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.gas;

import com.hbm.blocks.ModBlocks;
import com.hbm.extprop.HbmLivingProps;
import com.hbm.particle.AshRevealParticleOptions;
import com.hbm.potion.HbmPotion;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import com.hbm.util.ContaminationUtil;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class BlockGasRadonTomb extends BlockGasBase {

    public static final MapCodec<BlockGasRadonTomb> CODEC = simpleCodec(BlockGasRadonTomb::new);

    public BlockGasRadonTomb(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends BlockGasRadonTomb> codec() {
        return CODEC;
    }

    @Override
    protected void entityInside(
            BlockState state,
            Level level,
            BlockPos pos,
            Entity entity,
            InsideBlockEffectApplier applier,
            boolean isPrecise) {
        if (level.isClientSide() || !(entity instanceof LivingEntity living)) return;

        living.removeEffect(HbmPotion.radaway());
        living.removeEffect(HbmPotion.radx());
        ContaminationUtil.contaminate(
                living, HazardType.RADIATION, ContaminationType.RAD_BYPASS, 0.5F);
        HbmLivingProps.incrementAsbestos(living, 10);
    }

    @Override
    public Direction getFirstDirection(Level level, BlockPos pos, RandomSource rand) {
        return rand.nextInt(3) == 0 ? Direction.UP : Direction.DOWN;
    }

    @Override
    public Direction getSecondDirection(Level level, BlockPos pos, RandomSource rand) {
        return randomHorizontal(rand);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(10) == 0) {
            BlockPos below = pos.below();
            BlockState belowState = level.getBlockState(below);
            if (belowState.is(Blocks.GRASS_BLOCK)) {

                level.setBlockAndUpdate(
                        below,
                        random.nextInt(5) == 0
                                ? Blocks.COARSE_DIRT.defaultBlockState()
                                : ModBlocks.WASTE_EARTH.get().defaultBlockState());
            }

            if (belowState.getBlock() instanceof VegetationBlock
                    || belowState.getBlock() instanceof VineBlock
                    || belowState.is(BlockTags.LEAVES)) {
                level.removeBlock(below, false);
            }
        }
        if (random.nextInt(600) == 0) {
            level.removeBlock(pos, false);
            return;
        }
        super.tick(state, level, pos, random);
    }

    @Override
    protected AshRevealParticleOptions revealTint() {
        return new AshRevealParticleOptions(0.1F, 0.3F, 0.1F);
    }
}
