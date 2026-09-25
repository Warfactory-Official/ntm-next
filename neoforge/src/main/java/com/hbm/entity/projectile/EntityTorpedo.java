// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.projectile;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.particle.helper.ExplosionCreator;
import com.hbm.particle.helper.FlameCreator;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public class EntityTorpedo extends Entity {

    private final InterpolationHandler interpolation = new InterpolationHandler(this);

    public EntityTorpedo(EntityType<? extends EntityTorpedo> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    protected void readAdditionalSaveData(ValueInput input) {}

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {}

    @Override
    public InterpolationHandler getInterpolation() {
        return this.interpolation;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 25000;
    }

    @Override
    public void tick() {
        this.interpolation.interpolate();
        super.tick();

        if (level().isClientSide()) return;

        if (this.tickCount == 1) {
            for (int i = 0; i < 15; i++) {
                FlameCreator.composeEffect(
                        level(),
                        getX() + (random.nextDouble() - 0.5) * 2,
                        getY() + (random.nextDouble() - 0.5) * 1,
                        getZ() + (random.nextDouble() - 0.5) * 2,
                        FlameCreator.META_BALEFIRE);
            }
        }

        Vec3 motion = getDeltaMovement();
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);

        double mY = motion.y - 0.04;
        if (mY < -2.5) mY = -2.5;
        setDeltaMovement(motion.x, mY, motion.z);

        if (!level().getBlockState(blockPosition()).isAir()) {
            discard();
            ExplosionCreator.composeEffectStandard(level(), getX(), getY() + 1, getZ());
            ExplosionVNT vnt = new ExplosionVNT(level(), getX(), getY(), getZ(), 20F);
            vnt.makeStandard();
            vnt.explode();
        }
    }
}
