// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.projectile;

import com.hbm.entity.ModEntities;
import com.hbm.items.weapon.sedna.BulletConfig.ProjectileType;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.XFactoryTool;
import com.hbm.util.BobMathUtil;
import com.hbm.util.ChunkUtil;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class EntityBulletBaseMK4 extends EntityThrowableInterp {

    private static final EntityDataAccessor<Integer> CONFIG_ID =
            SynchedEntityData.defineId(EntityBulletBaseMK4.class, EntityDataSerializers.INT);

    public BulletConfig config;

    public double velocity;
    public double prevVelocity;
    public double accel;
    public float damage;
    public int ricochets = 0;
    public Entity lockonTarget = null;

    public boolean ignoreFrustum;

    public EntityBulletBaseMK4(EntityType<? extends EntityBulletBaseMK4> type, Level level) {
        super(type, level);
    }

    public EntityBulletBaseMK4(
            Level level,
            LivingEntity entity,
            BulletConfig config,
            float damage,
            float gunSpread,
            double posX,
            double posY,
            double posZ,
            double motionX,
            double motionY,
            double motionZ) {
        this(ModEntities.BULLET_MK4.get(), level);

        this.setThrower(entity);
        this.setBulletConfig(config);

        this.damage = damage;

        this.snapTo(posX, posY, posZ, 0, 0);

        this.setDeltaMovement(motionX, motionY, motionZ);
        this.setThrowableHeading(motionX, motionY, motionZ, 1.0F, this.config.spread + gunSpread);
    }

    public EntityBulletBaseMK4(
            LivingEntity entity,
            BulletConfig config,
            float baseDamage,
            float gunSpread,
            double sideOffset,
            double heightOffset,
            double frontOffset) {
        this(ModEntities.BULLET_MK4.get(), entity.level());

        this.setThrower(entity);
        this.setBulletConfig(config);

        this.damage = baseDamage * this.config.damageMult;

        this.snapTo(
                entity.getX(),
                entity.getY() + entity.getEyeHeight(),
                entity.getZ(),
                entity.getYRot(),
                entity.getXRot());

        Vec3 offset =
                new Vec3(sideOffset, heightOffset, frontOffset)
                        .xRot(-this.getXRot() / 180F * (float) Math.PI)
                        .yRot(-this.getYRot() / 180F * (float) Math.PI);

        this.setPos(this.getX() + offset.x, this.getY() + offset.y, this.getZ() + offset.z);

        double motionX =
                -Mth.sin(this.getYRot() / 180.0F * (float) Math.PI)
                        * Mth.cos(this.getXRot() / 180.0F * (float) Math.PI);
        double motionZ =
                Mth.cos(this.getYRot() / 180.0F * (float) Math.PI)
                        * Mth.cos(this.getXRot() / 180.0F * (float) Math.PI);
        double motionY = -Mth.sin(this.getXRot() / 180.0F * (float) Math.PI);
        this.setDeltaMovement(motionX, motionY, motionZ);

        this.setThrowableHeading(motionX, motionY, motionZ, 1.0F, gunSpread);
    }

    public EntityBulletBaseMK4(
            Level level,
            BulletConfig config,
            float baseDamage,
            float gunSpread,
            float yaw,
            float pitch) {
        this(ModEntities.BULLET_MK4.get(), level);

        this.setBulletConfig(config);
        this.damage = baseDamage * this.config.damageMult;

        float yawDeg = yaw * 180F / (float) Math.PI;
        float pitchDeg = -pitch * 180F / (float) Math.PI;
        this.setYRot(yawDeg);
        this.yRotO = yawDeg;
        this.setXRot(pitchDeg);
        this.xRotO = pitchDeg;

        double motionX =
                -Mth.sin(this.getYRot() / 180.0F * (float) Math.PI)
                        * Mth.cos(this.getXRot() / 180.0F * (float) Math.PI);
        double motionZ =
                Mth.cos(this.getYRot() / 180.0F * (float) Math.PI)
                        * Mth.cos(this.getXRot() / 180.0F * (float) Math.PI);
        double motionY = -Mth.sin(this.getXRot() / 180.0F * (float) Math.PI);
        this.setDeltaMovement(motionX, motionY, motionZ);
        this.setThrowableHeading(motionX, motionY, motionZ, 1.0F, gunSpread);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CONFIG_ID, 0);
    }

    public BulletConfig getBulletConfig() {
        int id = this.entityData.get(CONFIG_ID);
        if (id < 0 || id > BulletConfig.configs.size()) return null;
        return BulletConfig.configs.get(id);
    }

    public void setBulletConfig(BulletConfig config) {
        this.config = config;
        this.entityData.set(CONFIG_ID, config.id);
    }

    @Override
    public void tick() {

        if (config == null) config = this.getBulletConfig();

        if (config == null) {
            this.discard();
            return;
        }

        double prevX = this.getX();
        double prevY = this.getY();
        double prevZ = this.getZ();

        super.tick();

        double dX = this.getX() - prevX;
        double dY = this.getY() - prevY;
        double dZ = this.getZ() - prevZ;

        if (!this.inGround && this.lockonTarget != null && this.lockonTarget.isAlive()) {
            Vec3 motion = this.getDeltaMovement();
            double vel = motion.length();
            Vec3 delta =
                    new Vec3(
                            lockonTarget.getX() - getX(),
                            lockonTarget.getY() + lockonTarget.getBbHeight() / 2D - getY(),
                            lockonTarget.getZ() - getZ());
            float turn = Math.min(0.005F * this.tickCount, 1F);
            Vec3 newVec =
                    new Vec3(
                                    BobMathUtil.interp(motion.x, delta.x, turn),
                                    BobMathUtil.interp(motion.y, delta.y, turn),
                                    BobMathUtil.interp(motion.z, delta.z, turn))
                            .normalize()
                            .scale(vel);
            this.setDeltaMovement(newVec);
        }

        this.prevVelocity = this.velocity;
        this.velocity = Math.sqrt(dX * dX + dY * dY + dZ * dZ);

        if ((this.config != XFactoryTool.ct_hook || !this.level().isClientSide())
                && !this.inGround
                && !this.onGround()
                && velocity > 0) {

            double hyp = Math.sqrt(dX * dX + dZ * dZ);
            float yaw = (float) (Math.atan2(dX, dZ) * 180.0D / Math.PI);
            float pitch = (float) (Math.atan2(dY, hyp) * 180.0D / Math.PI);

            for (; pitch - this.xRotO < -180.0F; this.xRotO -= 360.0F)
                ;
            while (pitch - this.xRotO >= 180.0F) this.xRotO += 360.0F;
            while (yaw - this.yRotO < -180.0F) this.yRotO -= 360.0F;
            while (yaw - this.yRotO >= 180.0F) this.yRotO += 360.0F;
            this.setYRot(yaw);
            this.setXRot(pitch);
        }

        if (config.pType == ProjectileType.BULLET_CHUNKLOADING) ChunkUtil.holdOwnChunk(this);

        if (!this.level().isClientSide() && this.tickCount > config.expires) this.discard();

        if (this.config.onUpdate != null) this.config.onUpdate.accept(this);
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);

        if (reason.shouldDestroy()) this.sendTeleport();
    }

    @Override
    protected void onImpact(HitResult mop) {
        if (!this.level().isClientSide()) {

            if (this.config.onImpact != null) this.config.onImpact.accept(this, mop);
            if (this.isRemoved() || this.inGround) return;
            if (this.config.onRicochet != null) this.config.onRicochet.accept(this, mop);
            if (this.config.onEntityHit != null) this.config.onEntityHit.accept(this, mop);
        }
    }

    @Override
    protected double headingForceMult() {
        return 1D;
    }

    @Override
    public double getGravityVelocity() {
        return this.config.gravity;
    }

    @Override
    protected double motionMult() {
        return this.config.velocity + this.accel;
    }

    @Override
    protected float getAirDrag() {
        return 1F;
    }

    @Override
    protected float getWaterDrag() {
        return 1F;
    }

    @Override
    public boolean doesImpactEntities() {
        return this.config.impactsEntities;
    }

    @Override
    public boolean doesPenetrate() {
        return this.config.doesPenetrate;
    }

    @Override
    public boolean isSpectral() {
        return this.config.isSpectral;
    }

    @Override
    public int selfDamageDelay() {
        return this.config.selfDamageDelay;
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }
}
