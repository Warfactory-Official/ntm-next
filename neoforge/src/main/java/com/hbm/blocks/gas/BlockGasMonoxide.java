// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.gas;

import com.hbm.handler.ArmorUtil;
import com.hbm.hazard.HazardClass;
import com.hbm.lib.ModDamageTypes;
import com.hbm.particle.AshRevealParticleOptions;
import com.hbm.util.ArmorRegistry;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class BlockGasMonoxide extends BlockGasBase {

    public static final MapCodec<BlockGasMonoxide> CODEC = simpleCodec(BlockGasMonoxide::new);

    public BlockGasMonoxide(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends BlockGasMonoxide> codec() {
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
        if (ArmorRegistry.hasProtection(living, EquipmentSlot.HEAD, HazardClass.GAS_MONOXIDE)) {
            ArmorUtil.damageGasMaskFilter(living, 1);
        } else {
            living.hurt(level.damageSources().source(ModDamageTypes.MONOXIDE), 1);
        }
    }

    @Override
    public Direction getFirstDirection(Level level, BlockPos pos, RandomSource rand) {
        return Direction.DOWN;
    }

    @Override
    public Direction getSecondDirection(Level level, BlockPos pos, RandomSource rand) {
        return randomHorizontal(rand);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(100) == 0) {
            level.removeBlock(pos, false);
            return;
        }
        super.tick(state, level, pos, random);
    }

    @Override
    protected AshRevealParticleOptions revealTint() {
        return new AshRevealParticleOptions(0.1F, 0.1F, 0.1F);
    }
}
