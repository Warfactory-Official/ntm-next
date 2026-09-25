// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.gas;

import com.hbm.handler.ArmorUtil;
import com.hbm.hazard.HazardClass;
import com.hbm.particle.AshRevealParticleOptions;
import com.hbm.util.ArmorRegistry;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class BlockGasChlorine extends BlockGasBase {

    public static final MapCodec<BlockGasChlorine> CODEC = simpleCodec(BlockGasChlorine::new);

    public BlockGasChlorine(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends BlockGasChlorine> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
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
        if (ArmorRegistry.hasProtection(living, EquipmentSlot.HEAD, HazardClass.GAS_LUNG)) {
            ArmorUtil.damageGasMaskFilter(living, 1);
        } else {
            living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 5 * 20, 0));
            living.addEffect(new MobEffectInstance(MobEffects.POISON, 20 * 20, 2));
            living.addEffect(new MobEffectInstance(MobEffects.WITHER, 1 * 20, 1));
            living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30 * 20, 1));
            living.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 30 * 20, 2));
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
        if (random.nextInt(10) == 0) {
            level.removeBlock(pos, false);
            return;
        }
        super.tick(state, level, pos, random);
    }

    @Override
    protected AshRevealParticleOptions revealTint() {
        return new AshRevealParticleOptions(0.7F, 0.8F, 0.6F);
    }
}
