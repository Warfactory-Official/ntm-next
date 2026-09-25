// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.particle;

import com.hbm.entity.ModEntities;
import com.hbm.explosion.ExplosionChaos;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class EntityPinkCloudFX extends Entity {

    private int particleAge;
    private int maxAge;

    public EntityPinkCloudFX(EntityType<? extends EntityPinkCloudFX> type, Level level) {
        super(type, level);
        noPhysics = false;
    }

    public EntityPinkCloudFX(
            Level level,
            double x,
            double y,
            double z,
            double motionX,
            double motionY,
            double motionZ) {
        this(ModEntities.PINK_CLOUD_FX.get(), level);
        setPos(x, y, z);
        setDeltaMovement(ModFXMotion.spawn(motionX, motionY, motionZ));
    }

    public int particleAge() {
        return particleAge;
    }

    public int maxAge() {
        return maxAge;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {}

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        discard();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 25000D;
    }

    @Override
    public void tick() {
        xo = getX();
        yo = getY();
        zo = getZ();
        super.tick();

        if (maxAge < 900) maxAge = random.nextInt(301) + 900;

        if (!level().isClientSide() && random.nextInt(50) == 0)
            ExplosionChaos.pinkCloud(level(), getX(), getY(), getZ(), 2D);

        if (++particleAge >= maxAge) {
            discard();
            return;
        }

        double motionX = getDeltaMovement().x * 0.76D;
        double motionY = getDeltaMovement().y * 0.76D;
        double motionZ = getDeltaMovement().z * 0.76D;

        if (onGround()) {
            motionX *= 0.7D;
            motionZ *= 0.7D;
        }

        if (level().isRaining() && level().canSeeSky(blockPosition())) motionY -= 0.01D;

        setDeltaMovement(motionX, motionY, motionZ);

        for (int i = 0; i < 4; i++) {
            setPos(getX() + motionX / 4D, getY() + motionY / 4D, getZ() + motionZ / 4D);

            BlockPos pos = new BlockPos((int) getX(), (int) getY(), (int) getZ());
            if (!level().getBlockState(pos).isSolidRender()) continue;

            if (!level().isClientSide() && random.nextInt(5) != 0) discard();

            setPos(getX() - motionX / 4D, getY() - motionY / 4D, getZ() - motionZ / 4D);
            setDeltaMovement(0D, 0D, 0D);
            motionX = motionY = motionZ = 0D;
        }
    }
}
