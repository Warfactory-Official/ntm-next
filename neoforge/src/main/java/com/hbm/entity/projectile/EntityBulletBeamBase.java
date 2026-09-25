// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.projectile;

import com.hbm.entity.ModEntities;
import com.hbm.items.weapon.sedna.BulletConfig;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.*;

public class EntityBulletBeamBase extends Entity {

    private static final EntityDataAccessor<Integer> CONFIG_ID =
            SynchedEntityData.defineId(EntityBulletBeamBase.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> BEAM_LENGTH =
            SynchedEntityData.defineId(EntityBulletBeamBase.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> EXACT_YAW =
            SynchedEntityData.defineId(EntityBulletBeamBase.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> EXACT_PITCH =
            SynchedEntityData.defineId(EntityBulletBeamBase.class, EntityDataSerializers.FLOAT);
    public LivingEntity thrower;
    public BulletConfig config;
    public float damage;
    public double headingX;
    public double headingY;
    public double headingZ;

    public EntityBulletBeamBase(EntityType<? extends EntityBulletBeamBase> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public EntityBulletBeamBase(Level level, BulletConfig config, float baseDamage) {
        this(ModEntities.BULLET_BEAM.get(), level);
        this.setBulletConfig(config);
        this.damage = baseDamage * this.config.damageMult;
    }

    public EntityBulletBeamBase(LivingEntity entity, BulletConfig config, float baseDamage) {
        this(entity.level(), config, baseDamage);
        this.thrower = entity;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distSq) {
        double range = getBoundingBox().getSize() * 64.0D * 10.0D * getViewScale();
        return distSq < range * range;
    }

    public EntityBulletBeamBase(
            LivingEntity entity,
            BulletConfig config,
            float baseDamage,
            float angularInaccuracy,
            double sideOffset,
            double heightOffset,
            double frontOffset) {
        this(ModEntities.BULLET_BEAM.get(), entity.level());

        this.thrower = entity;
        this.setBulletConfig(config);

        this.damage = baseDamage * this.config.damageMult;

        this.snapTo(
                entity.getX(),
                entity.getY() + entity.getEyeHeight(),
                entity.getZ(),
                entity.getYRot() + (float) this.random.nextGaussian() * angularInaccuracy,
                entity.getXRot() + (float) this.random.nextGaussian() * angularInaccuracy);

        Vec3 offset =
                new Vec3(sideOffset, heightOffset, frontOffset)
                        .xRot(-this.getXRot() / 180F * (float) Math.PI)
                        .yRot(-this.getYRot() / 180F * (float) Math.PI);

        this.setPos(this.getX() + offset.x, this.getY() + offset.y, this.getZ() + offset.z);

        this.headingX =
                -Mth.sin(this.getYRot() / 180.0F * (float) Math.PI)
                        * Mth.cos(this.getXRot() / 180.0F * (float) Math.PI);
        this.headingZ =
                Mth.cos(this.getYRot() / 180.0F * (float) Math.PI)
                        * Mth.cos(this.getXRot() / 180.0F * (float) Math.PI);
        this.headingY = -Mth.sin(this.getXRot() / 180.0F * (float) Math.PI);

        double range = 250D;
        this.headingX *= range;
        this.headingY *= range;
        this.headingZ *= range;

        this.entityData.set(EXACT_YAW, this.getYRot());
        this.entityData.set(EXACT_PITCH, this.getXRot());

        performHitscan();
    }

    public LivingEntity getThrower() {
        return this.thrower;
    }

    public void setRotationsFromVector(Vec3 delta) {
        this.setXRot((float) (-Math.asin(delta.y / delta.length()) * 180D / Math.PI));
        this.setYRot((float) (-Math.atan2(delta.x, delta.z) * 180D / Math.PI));

        this.headingX =
                -Mth.sin(this.getYRot() / 180.0F * (float) Math.PI)
                        * Mth.cos(this.getXRot() / 180.0F * (float) Math.PI);
        this.headingZ =
                Mth.cos(this.getYRot() / 180.0F * (float) Math.PI)
                        * Mth.cos(this.getXRot() / 180.0F * (float) Math.PI);
        this.headingY = -Mth.sin(this.getXRot() / 180.0F * (float) Math.PI);

        this.entityData.set(EXACT_YAW, this.getYRot());
        this.entityData.set(EXACT_PITCH, this.getXRot());
    }

    public void performHitscanExternal(double range) {
        this.headingX *= range;
        this.headingY *= range;
        this.headingZ *= range;
        performHitscan();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(CONFIG_ID, 0);
        builder.define(BEAM_LENGTH, 0F);
        builder.define(EXACT_YAW, 0F);
        builder.define(EXACT_PITCH, 0F);
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

    public double getBeamLength() {
        return this.entityData.get(BEAM_LENGTH);
    }

    public float getExactYaw() {
        return this.entityData.get(EXACT_YAW);
    }

    public float getExactPitch() {
        return this.entityData.get(EXACT_PITCH);
    }

    @Override
    public void tick() {

        if (config == null) config = this.getBulletConfig();

        if (config == null) {
            this.discard();
            return;
        }

        if (config.onUpdate != null) config.onUpdate.accept(this);

        super.tick();

        if (!this.level().isClientSide() && this.tickCount > config.expires) this.discard();
    }

    protected void performHitscan() {

        Vec3 pos = this.position();
        Vec3 nextPos = pos.add(this.headingX, this.headingY, this.headingZ);
        HitResult mop = null;
        if (!this.isSpectral()) {
            BlockHitResult blockHit =
                    this.level()
                            .clip(
                                    new ClipContext(
                                            pos,
                                            nextPos,
                                            ClipContext.Block.COLLIDER,
                                            ClipContext.Fluid.NONE,
                                            this));
            if (blockHit.getType() != HitResult.Type.MISS) mop = blockHit;
        }

        if (mop != null) {
            nextPos = mop.getLocation();
        }

        if (!this.level().isClientSide() && this.doesImpactEntities()) {

            Entity hitEntity = null;
            List<Entity> list =
                    this.level()
                            .getEntities(
                                    this,
                                    this.getBoundingBox()
                                            .expandTowards(
                                                    this.headingX, this.headingY, this.headingZ)
                                            .inflate(1.0D, 1.0D, 1.0D));
            double nearest = 0.0D;
            Vec3 nonPenImpact = null;
            Vec3 coinHit = null;

            double closestCoin = 0;
            EntityCoin hitCoin = null;

            for (Entity entity : list) {
                if (!entity.isAlive()) continue;
                if (entity instanceof EntityCoin coin) {
                    double hitbox = 0.3F;
                    AABB aabb = entity.getBoundingBox().inflate(hitbox, hitbox, hitbox);
                    Vec3 hitVec = aabb.clip(pos, nextPos).orElse(null);
                    if (hitVec != null) {
                        double dist = pos.distanceTo(hitVec);
                        if (closestCoin == 0 || dist < closestCoin) {
                            closestCoin = dist;
                            hitCoin = coin;
                            coinHit = hitVec;
                        }
                    }
                }
            }

            for (int j = 0; j < list.size(); ++j) {
                Entity entity = list.get(j);

                if (entity.isPickable() && entity != thrower && entity.isAlive()) {
                    double hitbox = 0.3F;
                    AABB aabb = entity.getBoundingBox().inflate(hitbox, hitbox, hitbox);
                    Vec3 hitVec = aabb.clip(pos, nextPos).orElse(null);

                    if (hitVec != null) {

                        double dist = pos.distanceTo(hitVec);

                        if (this.doesPenetrate()) {
                            if (hitCoin == null || dist < closestCoin) {
                                this.onImpact(new EntityHitResult(entity, hitVec));
                            }
                        } else {
                            if (dist < nearest || nearest == 0.0D) {
                                hitEntity = entity;
                                nearest = dist;
                                nonPenImpact = hitVec;
                            }
                        }
                    }
                }
            }

            if (!this.doesPenetrate() && hitEntity != null) {
                mop = new EntityHitResult(hitEntity, nonPenImpact);
            }

            if (hitCoin != null) {
                this.entityData.set(BEAM_LENGTH, (float) coinHit.distanceTo(pos));

                double range = 50;
                List<Entity> targets =
                        this.level()
                                .getEntities(
                                        (Entity) null,
                                        new AABB(
                                                        coinHit.x, coinHit.y, coinHit.z, coinHit.x,
                                                        coinHit.y, coinHit.z)
                                                .inflate(range, range, range),
                                        e -> true);
                Entity nearestCoin = null;
                Entity nearestPlayer = null;
                Entity nearestMob = null;
                Entity nearestOther = null;
                double coinDist = 0;
                double playerDist = 0;
                double mobDist = 0;
                double otherDist = 0;

                hitCoin.discard();

                for (Entity entity : targets) {
                    if (entity == this.thrower) continue;
                    if (!entity.isAlive()) continue;
                    double dist = entity.distanceTo(hitCoin);
                    if (dist > range) continue;

                    if (entity instanceof EntityCoin) {
                        if (coinDist == 0 || dist < coinDist) {
                            coinDist = dist;
                            nearestCoin = entity;
                        }
                    } else if (entity instanceof Player) {
                        if (playerDist == 0 || dist < playerDist) {
                            playerDist = dist;
                            nearestPlayer = entity;
                        }
                    } else if (entity instanceof Monster) {
                        if (mobDist == 0 || dist < mobDist) {
                            mobDist = dist;
                            nearestMob = entity;
                        }
                    } else if (entity instanceof LivingEntity) {
                        if (otherDist == 0 || dist < otherDist) {
                            otherDist = dist;
                            nearestOther = entity;
                        }
                    }
                }

                Entity target =
                        nearestCoin != null
                                ? nearestCoin
                                : nearestPlayer != null
                                        ? nearestPlayer
                                        : nearestMob != null ? nearestMob : nearestOther;

                EntityBulletBeamBase newBeam =
                        new EntityBulletBeamBase(
                                hitCoin.getThrower() != null ? hitCoin.getThrower() : this.thrower,
                                this.config,
                                this.damage * 1.25F);
                newBeam.setPos(coinHit);
                if (target != null) {
                    Vec3 delta =
                            new Vec3(
                                    target.getX() - newBeam.getX(),
                                    (target.getY() + target.getBbHeight() / 2D) - newBeam.getY(),
                                    target.getZ() - newBeam.getZ());
                    newBeam.setRotationsFromVector(delta);
                } else {
                    newBeam.setRotationsFromVector(
                            new Vec3(
                                    this.random.nextGaussian() * 0.5,
                                    -1,
                                    this.random.nextGaussian() * 0.5));
                }
                newBeam.performHitscanExternal(250D);
                this.level().addFreshEntity(newBeam);

                if (this.level() instanceof ServerLevel server) {
                    server.sendParticles(
                            ParticleTypes.EXPLOSION,
                            coinHit.x,
                            coinHit.y,
                            coinHit.z,
                            1,
                            0,
                            0,
                            0,
                            0);
                }

                return;
            }
        }

        if (mop != null) {

            if (!(mop instanceof BlockHitResult blockHit
                    && this.level()
                            .getBlockState(blockHit.getBlockPos())
                            .is(Blocks.NETHER_PORTAL))) {
                this.onImpact(mop);
            }
            this.entityData.set(BEAM_LENGTH, (float) mop.getLocation().distanceTo(pos));
        } else {
            this.entityData.set(BEAM_LENGTH, (float) nextPos.distanceTo(pos));
        }
    }

    protected void onImpact(HitResult mop) {
        if (!this.level().isClientSide()) {
            if (this.config.onImpactBeam != null) this.config.onImpactBeam.accept(this, mop);
        }
    }

    public boolean doesImpactEntities() {
        return this.config.impactsEntities;
    }

    public boolean doesPenetrate() {
        return this.config.doesPenetrate;
    }

    public boolean isSpectral() {
        return this.config.isSpectral;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {}

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.discard();
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }
}
