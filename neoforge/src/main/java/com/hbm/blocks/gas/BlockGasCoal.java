// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.gas;

import com.hbm.extprop.HbmLivingProps;
import com.hbm.hazard.HazardClass;
import com.hbm.particle.AshRevealParticleOptions;
import com.hbm.util.ArmorRegistry;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class BlockGasCoal extends BlockGasBase {

    public static final MapCodec<BlockGasCoal> CODEC = simpleCodec(BlockGasCoal::new);

    public BlockGasCoal(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends BlockGasCoal> codec() {
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
        if (!ArmorRegistry.hasProtection(living, EquipmentSlot.HEAD, HazardClass.PARTICLE_COARSE)) {
            HbmLivingProps.incrementBlackLung(living, 10);
        }
    }

    @Override
    public Direction getFirstDirection(Level level, BlockPos pos, RandomSource rand) {
        return rand.nextInt(5) == 0 ? Direction.DOWN : Direction.from3DDataValue(rand.nextInt(6));
    }

    @Override
    public Direction getSecondDirection(Level level, BlockPos pos, RandomSource rand) {
        return randomHorizontal(rand);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        super.animateTick(state, level, pos, rand);

        level.addParticle(
                ParticleTypes.SMOKE,
                pos.getX() + rand.nextFloat(),
                pos.getY() + rand.nextFloat(),
                pos.getZ() + rand.nextFloat(),
                0.0,
                0.0,
                0.0);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(20) == 0) {
            level.removeBlock(pos, false);
            return;
        }
        super.tick(state, level, pos, random);
    }

    @Override
    protected AshRevealParticleOptions revealTint() {
        return new AshRevealParticleOptions(0.2F, 0.2F, 0.2F);
    }
}
