// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.gas;

import com.hbm.blocks.ModBlocks;
import com.hbm.extprop.HbmLivingProps;
import com.hbm.handler.ArmorUtil;
import com.hbm.handler.radiation.RadiationSystemNT;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class BlockGasMeltdown extends BlockGasBase {

    public static final MapCodec<BlockGasMeltdown> CODEC = simpleCodec(BlockGasMeltdown::new);

    public BlockGasMeltdown(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends BlockGasMeltdown> codec() {
        return CODEC;
    }

    @Override
    public Direction getFirstDirection(Level level, BlockPos pos, RandomSource rand) {
        return rand.nextInt(2) == 0 ? Direction.UP : Direction.DOWN;
    }

    @Override
    public Direction getSecondDirection(Level level, BlockPos pos, RandomSource rand) {
        return randomHorizontal(rand);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.canSeeSky(pos)) RadiationSystemNT.incrementRad(level, pos, 5.0D, 5120.0D);

        Direction dir = Direction.from3DDataValue(random.nextInt(6));
        if (random.nextInt(7) == 0 && level.getBlockState(pos.relative(dir)).isAir()) {
            level.setBlockAndUpdate(
                    pos.relative(dir), ModBlocks.GAS_RADON_DENSE.get().defaultBlockState());
        }

        if (random.nextInt(350) == 0) {
            level.removeBlock(pos, false);
            return;
        }

        super.tick(state, level, pos, random);
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
        ContaminationUtil.contaminate(
                living, HazardType.RADIATION, ContaminationType.CREATIVE, 0.5D);
        living.addEffect(new MobEffectInstance(HbmPotion.radiation(), 60 * 20, 2));

        if (ArmorRegistry.hasProtection(living, EquipmentSlot.HEAD, HazardClass.PARTICLE_FINE)) {
            ArmorUtil.damageGasMaskFilter(living, 1);
        } else {
            HbmLivingProps.incrementAsbestos(living, 5);
        }
    }

    @Override
    protected AshRevealParticleOptions revealTint() {
        return new AshRevealParticleOptions(0.1F, 0.4F, 0.1F);
    }
}
