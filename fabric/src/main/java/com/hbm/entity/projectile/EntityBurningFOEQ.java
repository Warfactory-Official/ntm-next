// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.projectile;

import com.hbm.explosion.ExplosionLarge;
import com.hbm.explosion.ExplosionNukeGeneric;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class EntityBurningFOEQ extends ThrowableProjectile {

    public float renderPitch;
    public float renderPitchO;

    public EntityBurningFOEQ(EntityType<? extends EntityBurningFOEQ> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    public void tick() {

        this.renderPitchO = this.renderPitch;
        this.xo = getX();
        this.yo = getY();
        this.zo = getZ();
        this.xOld = this.xo;
        this.yOld = this.yo;
        this.zOld = this.zo;

        Vec3 motion = this.getDeltaMovement();
        this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);

        if (motion.y > -4D) {
            this.setDeltaMovement(motion.x, motion.y - 0.1D, motion.z);
        }

        this.rotation();

        BlockPos pos = new BlockPos((int) getX(), (int) getY(), (int) getZ());

        if (!level().getBlockState(pos).isAir()) {
            if (!level().isClientSide()) {
                for (int i = 0; i < 25; i++)
                    ExplosionLarge.explode(
                            level(),
                            getX() + 0.5F + random.nextGaussian() * 5,
                            getY() + 0.5F + random.nextGaussian() * 5,
                            getZ() + 0.5F + random.nextGaussian() * 5,
                            10.0F,
                            random.nextBoolean(),
                            false,
                            false);
                ExplosionNukeGeneric.waste(level(), pos, 35, random);
            }
            this.discard();
        }
    }

    public void rotation() {
        Vec3 motion = this.getDeltaMovement();
        float f2 = (float) Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        this.setYRot((float) (Math.atan2(motion.x, motion.z) * 180.0D / Math.PI));

        this.renderPitch = (float) (Math.atan2(motion.y, f2) * 180.0D / Math.PI) - 90F;

        for (; this.renderPitch - this.renderPitchO < -180.0F; this.renderPitchO -= 360.0F)
            ;
        while (this.renderPitch - this.renderPitchO >= 180.0F) this.renderPitchO += 360.0F;

        while (this.getYRot() - this.yRotO < -180.0F) this.yRotO -= 360.0F;
        while (this.getYRot() - this.yRotO >= 180.0F) this.yRotO += 360.0F;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 100000;
    }
}
