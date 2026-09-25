// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.projectile;

import com.hbm.entity.effect.EntityCloudFleijaRainbow;
import com.hbm.entity.logic.EntityNukeExplosionMK3;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class EntityB92Beam extends Entity {

    private static final EntityDataAccessor<Byte> CRITICAL =
            SynchedEntityData.defineId(EntityB92Beam.class, EntityDataSerializers.BYTE);

    private int xTile = -1;
    private int yTile = -1;
    private int zTile = -1;

    public double gravity = 0.0D;

    private BlockState inTile;
    private boolean inGround;

    public int canBePickedUp;

    public int arrowShake;

    public Entity shootingEntity;
    private int ticksInGround;
    private int ticksInAir;
    private double damage = 2.0D;

    public EntityB92Beam(EntityType<? extends EntityB92Beam> type, Level level) {
        super(type, level);
    }

    public EntityB92Beam(
            EntityType<? extends EntityB92Beam> type, Level level, double x, double y, double z) {
        this(type, level);
        this.setPos(x, y, z);
    }

    public EntityB92Beam(
            EntityType<? extends EntityB92Beam> type,
            LivingEntity shooter,
            LivingEntity target,
            float velocity,
            float inaccuracy) {
        this(type, shooter.level());
        this.shootingEntity = shooter;

        if (shooter instanceof Player) {
            this.canBePickedUp = 1;
        }

        double posY = shooter.getY() + shooter.getEyeHeight() - 0.10000000149011612D;
        double d0 = target.getX() - shooter.getX();
        double d1 = target.getY() + target.getBbHeight() / 3.0F - posY;
        double d2 = target.getZ() - shooter.getZ();
        double d3 = (float) Math.sqrt(d0 * d0 + d2 * d2);

        if (d3 >= 1.0E-7D) {
            float f2 = (float) (Math.atan2(d2, d0) * 180.0D / Math.PI) - 90.0F;
            float f3 = (float) (-(Math.atan2(d1, d3) * 180.0D / Math.PI));
            double d4 = d0 / d3;
            double d5 = d2 / d3;
            this.snapTo(shooter.getX() + d4, posY, shooter.getZ() + d5, f2, f3);
            float f4 = (float) d3 * 0.2F;
            this.setThrowableHeading(d0, d1 + f4, d2, velocity, inaccuracy);
        }
    }

    public EntityB92Beam(
            EntityType<? extends EntityB92Beam> type, LivingEntity shooter, float velocity) {
        this(type, shooter.level());
        this.shootingEntity = shooter;

        this.snapTo(
                shooter.getX(),
                shooter.getY() + shooter.getEyeHeight(),
                shooter.getZ(),
                shooter.getYRot(),
                shooter.getXRot());

        double x = this.getX() - Mth.cos(this.getYRot() / 180.0F * (float) Math.PI) * 0.16F;
        double y = this.getY() - 0.10000000149011612D;
        double z = this.getZ() - Mth.sin(this.getYRot() / 180.0F * (float) Math.PI) * 0.16F;
        this.setPos(x, y, z);

        double motionX =
                -Mth.sin(this.getYRot() / 180.0F * (float) Math.PI)
                        * Mth.cos(this.getXRot() / 180.0F * (float) Math.PI);
        double motionZ =
                Mth.cos(this.getYRot() / 180.0F * (float) Math.PI)
                        * Mth.cos(this.getXRot() / 180.0F * (float) Math.PI);
        double motionY = -Mth.sin(this.getXRot() / 180.0F * (float) Math.PI);
        this.setDeltaMovement(motionX, motionY, motionZ);

        Vec3 motion = this.getDeltaMovement();
        this.setThrowableHeading(motion.x, motion.y, motion.z, velocity * 1.5F, 1.0F);
    }

    public EntityB92Beam(
            EntityType<? extends EntityB92Beam> type,
            Level level,
            int x,
            int y,
            int z,
            double mx,
            double my,
            double mz,
            double grav) {
        this(type, level);
        this.setPos(x + 0.5F, y + 0.5F, z + 0.5F);

        this.setDeltaMovement(mx, my, mz);

        this.gravity = grav;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(CRITICAL, (byte) 0);
    }

    public void setThrowableHeading(
            double x, double y, double z, float velocity, float inaccuracy) {
        float f2 = (float) Math.sqrt(x * x + y * y + z * z);
        x /= f2;
        y /= f2;
        z /= f2;
        x +=
                this.random.nextGaussian()
                        * (this.random.nextBoolean() ? -1 : 1)
                        * 0.002499999832361937D
                        * inaccuracy;
        y +=
                this.random.nextGaussian()
                        * (this.random.nextBoolean() ? -1 : 1)
                        * 0.002499999832361937D
                        * inaccuracy;
        z +=
                this.random.nextGaussian()
                        * (this.random.nextBoolean() ? -1 : 1)
                        * 0.002499999832361937D
                        * inaccuracy;
        x *= velocity;
        y *= velocity;
        z *= velocity;
        this.setDeltaMovement(x, y, z);
        float f3 = (float) Math.sqrt(x * x + z * z);
        float yaw = (float) (Math.atan2(x, z) * 180.0D / Math.PI);
        float pitch = (float) (Math.atan2(y, f3) * 180.0D / Math.PI);
        this.yRotO = yaw;
        this.setYRot(yaw);
        this.xRotO = pitch;
        this.setXRot(pitch);
        this.ticksInGround = 0;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.tickCount > 100) this.discard();

        if (this.xRotO == 0.0F && this.yRotO == 0.0F) {

            float yaw =
                    (float)
                            (Math.atan2(this.getDeltaMovement().x, this.getDeltaMovement().z)
                                    * 180.0D
                                    / Math.PI);
            this.yRotO = yaw;
            this.setYRot(yaw);
        }

        if (this.xTile != -1 || this.yTile != -1 || this.zTile != -1) {
            BlockState state =
                    this.level().getBlockState(new BlockPos(this.xTile, this.yTile, this.zTile));
            if (!state.isAir()) {

                this.discard();
                explode();
            }
        }

        if (this.arrowShake > 0) {
            --this.arrowShake;
        } else {
            ++this.ticksInAir;
            Vec3 vec31 = new Vec3(this.getX(), this.getY(), this.getZ());
            Vec3 motion = this.getDeltaMovement();
            Vec3 vec3 =
                    new Vec3(
                            this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);
            HitResult clip =
                    this.level()
                            .clip(
                                    new ClipContext(
                                            vec31,
                                            vec3,
                                            ClipContext.Block.COLLIDER,
                                            ClipContext.Fluid.NONE,
                                            this));
            HitResult movingobjectposition = clip.getType() == HitResult.Type.MISS ? null : clip;
            vec31 = new Vec3(this.getX(), this.getY(), this.getZ());
            vec3 = new Vec3(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);

            if (movingobjectposition != null) {
                vec3 = movingobjectposition.getLocation();
            }

            Entity entity = null;
            List<Entity> list =
                    this.level()
                            .getEntities(
                                    this,
                                    this.getBoundingBox()
                                            .expandTowards(motion.x, motion.y, motion.z)
                                            .inflate(1.0D, 1.0D, 1.0D));
            double d0 = 0.0D;
            Vec3 hitVec = null;

            for (Entity entity1 : list) {
                if (entity1.isPickable()
                        && (entity1 != this.shootingEntity || this.ticksInAir >= 5)) {
                    float f1 = 0.3F;
                    AABB axisalignedbb1 = entity1.getBoundingBox().inflate(f1, f1, f1);
                    Optional<Vec3> intercept = axisalignedbb1.clip(vec31, vec3);

                    if (intercept.isPresent()) {
                        double d1 = vec31.distanceTo(intercept.get());

                        if (d1 < d0 || d0 == 0.0D) {
                            entity = entity1;
                            d0 = d1;
                            hitVec = intercept.get();
                        }
                    }
                }
            }

            if (entity != null) {
                movingobjectposition = new EntityHitResult(entity, hitVec);
            }

            if (movingobjectposition instanceof EntityHitResult entityHit
                    && entityHit.getEntity() instanceof Player entityplayer) {
                if (entityplayer.getAbilities().invulnerable
                        || this.shootingEntity instanceof Player
                                && !((Player) this.shootingEntity).canHarmPlayer(entityplayer)) {
                    movingobjectposition = null;
                }
            }

            float f2;
            if (movingobjectposition != null) {
                if (movingobjectposition.getType() == HitResult.Type.ENTITY) {
                    Vec3 vel = this.getDeltaMovement();
                    f2 = (float) Math.sqrt(vel.x * vel.x + vel.y * vel.y + vel.z * vel.z);
                    int k = Mth.ceil(f2 * this.damage);

                    if (this.getIsCritical()) {
                        k += this.random.nextInt(k / 2 + 2);
                    }

                    explode();
                } else if (movingobjectposition instanceof BlockHitResult blockHit) {
                    BlockPos blockpos = blockHit.getBlockPos();
                    this.xTile = blockpos.getX();
                    this.yTile = blockpos.getY();
                    this.zTile = blockpos.getZ();
                    this.inTile = this.level().getBlockState(blockpos);
                }
            }

            motion = this.getDeltaMovement();
            this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);
            this.setYRot((float) (Math.atan2(motion.x, motion.z) * 180.0D / Math.PI));

            if (this.isInWater()) {
                this.discard();
                explode();
            }

            if (this.isInWaterOrRain()) {
                this.extinguishFire();
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("xTile", this.xTile);
        output.putInt("yTile", this.yTile);
        output.putInt("zTile", this.zTile);
        output.putInt("life", this.ticksInGround);
        output.storeNullable("inTile", BlockState.CODEC, this.inTile);

        output.putByte("shake", (byte) this.arrowShake);
        output.putBoolean("inGround", this.inGround);
        output.putByte("pickup", (byte) this.canBePickedUp);
        output.putDouble("damage", this.damage);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.xTile = input.getIntOr("xTile", -1);
        this.yTile = input.getIntOr("yTile", -1);
        this.zTile = input.getIntOr("zTile", -1);
        this.ticksInGround = input.getIntOr("life", 0);
        this.inTile = input.read("inTile", BlockState.CODEC).orElse(null);
        this.arrowShake = input.getByteOr("shake", (byte) 0) & 255;
        this.inGround = input.getBooleanOr("inGround", false);

        this.damage = input.getDoubleOr("damage", this.damage);

        Optional<Integer> pickup = input.getInt("pickup");
        if (pickup.isPresent()) {
            this.canBePickedUp = pickup.get();
        } else {
            this.canBePickedUp = input.getBooleanOr("player", false) ? 1 : 0;
        }
    }

    public void setDamage(double p_70239_1_) {
        this.damage = p_70239_1_;
    }

    public double getDamage() {
        return this.damage;
    }

    public void setKnockbackStrength(int p_70240_1_) {}

    public void setIsCritical(boolean p_70243_1_) {
        byte b0 = this.entityData.get(CRITICAL);

        if (p_70243_1_) {
            this.entityData.set(CRITICAL, (byte) (b0 | 1));
        } else {
            this.entityData.set(CRITICAL, (byte) (b0 & -2));
        }
    }

    public boolean getIsCritical() {
        byte b0 = this.entityData.get(CRITICAL);
        return (b0 & 1) != 0;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distSq) {
        double perimeter = this.getBoundingBox().getSize() * 10.0D * 64.0D;
        return distSq < perimeter * perimeter;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    private void explode() {
        if (!level().isClientSide()) {
            EntityNukeExplosionMK3 ex =
                    EntityNukeExplosionMK3.statFacFleija(level(), getX(), getY(), getZ(), 10);
            if (!ex.isRemoved()) {
                this.level()
                        .playSound(
                                null,
                                this.getX(),
                                this.getY(),
                                this.getZ(),
                                SoundEvents.GENERIC_EXPLODE,
                                SoundSource.BLOCKS,
                                100.0F,
                                this.level().getRandom().nextFloat() * 0.1F + 0.9F);
                level().addFreshEntity(ex);

                level().addFreshEntity(
                                EntityCloudFleijaRainbow.statFac(
                                        level(), 10, this.getX(), this.getY(), this.getZ()));
            }
        }
    }
}
