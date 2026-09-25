// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.gas;

import com.hbm.blocks.ModBlocks;
import com.hbm.extprop.HbmLivingProps;
import com.hbm.handler.ArmorUtil;
import com.hbm.hazard.HazardClass;
import com.hbm.particle.AshRevealParticleOptions;
import com.hbm.potion.HbmPotion;
import com.hbm.util.ArmorRegistry;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import com.hbm.util.ContaminationUtil;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class BlockGasRadonDense extends BlockGasBase {

    public static final MapCodec<BlockGasRadonDense> CODEC = simpleCodec(BlockGasRadonDense::new);

    public BlockGasRadonDense(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends BlockGasRadonDense> codec() {
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
        if (ArmorRegistry.hasProtection(living, EquipmentSlot.HEAD, HazardClass.PARTICLE_FINE)) {
            ArmorUtil.damageGasMaskFilter(living, 1);
        } else {
            ContaminationUtil.contaminate(
                    living, HazardType.RADIATION, ContaminationType.CREATIVE, 0.5F);
            living.addEffect(new MobEffectInstance(HbmPotion.radiation(), 15 * 20, 0));
            HbmLivingProps.incrementAsbestos(living, 5);
        }
    }

    @Override
    public Direction getFirstDirection(Level level, BlockPos pos, RandomSource rand) {
        return rand.nextInt(5) == 0 ? Direction.UP : Direction.DOWN;
    }

    @Override
    public Direction getSecondDirection(Level level, BlockPos pos, RandomSource rand) {
        return randomHorizontal(rand);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(20) == 0) {
            BlockPos below = pos.below();
            if (level.getBlockState(below).is(Blocks.GRASS_BLOCK)) {
                level.setBlockAndUpdate(below, ModBlocks.WASTE_EARTH.get().defaultBlockState());
            }
        }
        if (random.nextInt(30) == 0) {
            BlockState fallout = ModBlocks.FALLOUT.get().defaultBlockState();
            if (fallout.canSurvive(level, pos)) level.setBlockAndUpdate(pos, fallout);
            else level.removeBlock(pos, false);
            return;
        }
        super.tick(state, level, pos, random);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        super.animateTick(state, level, pos, rand);

        level.addParticle(
                ParticleTypes.MYCELIUM,
                pos.getX() + rand.nextFloat(),
                pos.getY() + rand.nextFloat(),
                pos.getZ() + rand.nextFloat(),
                0.0,
                0.0,
                0.0);
    }

    @Override
    protected AshRevealParticleOptions revealTint() {
        return new AshRevealParticleOptions(0.1F, 0.5F, 0.1F);
    }
}
