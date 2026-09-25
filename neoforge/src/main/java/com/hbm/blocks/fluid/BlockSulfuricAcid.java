// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.fluid;

import com.hbm.advancement.AwardRegions;
import com.hbm.advancement.HbmCriteria;
import com.hbm.lib.ModDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class BlockSulfuricAcid extends BlockFluidClassicBase {

    private static final float DAMAGE = 5.0F;

    public BlockSulfuricAcid(ClassicFluid fluid, Properties props) {
        super(fluid, props);
    }

    @Override
    protected void entityInside(
            BlockState state,
            Level level,
            BlockPos pos,
            Entity entity,
            InsideBlockEffectApplier effectApplier,
            boolean isPrecise) {
        if (entity instanceof ItemEntity item) {
            entity.setDeltaMovement(Vec3.ZERO);
            if (entity.tickCount % 20 == 0 && level instanceof ServerLevel server) {
                ItemStack stack = item.getItem().copy();
                entity.hurt(level.damageSources().source(ModDamageTypes.ACID), DAMAGE * 0.1F);
                if (entity.isRemoved()) {
                    AwardRegions.nearby(
                            server,
                            entity.getBoundingBox().inflate(10),
                            player -> HbmCriteria.itemDissolved(player, stack));
                }
            }
            if (entity.tickCount % 5 == 0 && level.isClientSide()) {
                level.addParticle(
                        ParticleTypes.CLOUD,
                        entity.getX(),
                        entity.getY(),
                        entity.getZ(),
                        0D,
                        0D,
                        0D);
            }
        } else {
            Vec3 motion = entity.getDeltaMovement();
            if (motion.y < -0.2D) entity.setDeltaMovement(motion.x, motion.y * 0.5D, motion.z);
            if (!level.isClientSide()) {
                entity.hurt(level.damageSources().source(ModDamageTypes.ACID), DAMAGE);
            }
        }

        if (entity.tickCount % 5 == 0 && !level.isClientSide()) {
            level.playSound(
                    null,
                    entity.getX(),
                    entity.getY(),
                    entity.getZ(),
                    SoundEvents.FIRE_EXTINGUISH,
                    SoundSource.BLOCKS,
                    0.2F,
                    1.0F);
        }
    }
}
