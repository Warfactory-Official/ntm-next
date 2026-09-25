// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.mob;

import com.hbm.entity.ModEntities;
import com.hbm.entity.projectile.EntityBullet;
import com.hbm.entity.projectile.EntityChopperMine;
import com.hbm.interfaces.IRadiationImmune;
import com.hbm.inventory.material.Mats;
import com.hbm.items.ModItems;
import com.hbm.lib.ModDamageTypes;
import com.hbm.particle.HbmParticles;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import com.hbm.sound.ModSounds;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class EntityHunterChopper extends Mob implements Enemy, IRadiationImmune {

    private static final EntityDataAccessor<Byte> ATTACKING =
            SynchedEntityData.defineId(EntityHunterChopper.class, EntityDataSerializers.BYTE);

    private static final EntityDataAccessor<Byte> DYING =
            SynchedEntityData.defineId(EntityHunterChopper.class, EntityDataSerializers.BYTE);

    private static final List<ResourceKey<DamageType>> FULL_DAMAGE =
            List.of(
                    ModDamageTypes.SHRAPNEL,
                    ModDamageTypes.NUCLEAR_BLAST,
                    ModDamageTypes.BLACKHOLE,
                    ModDamageTypes.TAU,
                    ModDamageTypes.SUBATOMIC);

    private static final double TARGET_RANGE = 250D;
    private static final double GUN_RANGE = 64D;

    private final ServerBossEvent bossEvent =
            new ServerBossEvent(
                    getUUID(),
                    getDisplayName(),
                    BossEvent.BossBarColor.RED,
                    BossEvent.BossBarOverlay.PROGRESS);

    private double waypointX;
    private double waypointY;
    private double waypointZ;

    private int courseChangeCooldown;
    private int attackCounter;
    private int mineDropCounter;
    private @Nullable Entity targetedEntity;
    private @Nullable AudioWrapper flyingLoop;
    private @Nullable AudioWrapper crashingLoop;

    public EntityHunterChopper(EntityType<? extends EntityHunterChopper> type, Level level) {
        super(type, level);
        setNoGravity(true);
        this.xpReward = 500;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 750.0D)
                .add(Attributes.FOLLOW_RANGE, 250.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);

        builder.define(ATTACKING, (byte) 0);
        builder.define(DYING, (byte) 0);
    }

    @Override
    protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {}

    @Override
    public void travel(Vec3 input) {

        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().scale(0.91D));
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    public boolean isDying() {
        return entityData.get(DYING) == 1;
    }

    private void setDying(boolean dying) {
        entityData.set(DYING, (byte) (dying ? 1 : 0));
    }

    public boolean isAttacking() {
        return entityData.get(ATTACKING) != 0;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean exotic =
                source.is(DamageTypeTags.IS_EXPLOSION) || FULL_DAMAGE.stream().anyMatch(source::is);

        if (!exotic) amount *= 0.1F;

        if (isInvulnerableTo(level, source)
                || source.getDirectEntity() != null
                || getHealth() <= 0.1F) {
            return false;
        }

        if (amount >= getHealth()) {
            initDeath(level);
            setDying(true);
            setHealth(0.1F);
            return false;
        }

        if (!isDying() && random.nextInt(15) == 0) {
            level.explode(this, getX(), getY(), getZ(), 5F, Level.ExplosionInteraction.BLOCK);
            dropDamageItem(level);
        }

        for (int i = 0; i < 8; i++) {
            level.sendParticles(
                    ParticleTypes.FIREWORK,
                    getX(),
                    getY(),
                    getZ(),
                    3,
                    i * 0.05D,
                    i * 0.05D,
                    i * 0.05D,
                    0.05D);
        }

        return super.hurtServer(level, source, amount);
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            discard();
            return;
        }

        if (isDying()) {
            crashStep(level);
        } else {
            flyStep(level);
        }

        aimAt(this.targetedEntity);
    }

    private void flyStep(ServerLevel level) {
        double dx = this.waypointX - getX();
        double dy = this.waypointY - getY();
        double dz = this.waypointZ - getZ();
        double distSq = dx * dx + dy * dy + dz * dz;

        if (distSq < 1.0D || distSq > 3600.0D) pickWaypoint(this.targetedEntity);

        if (this.courseChangeCooldown-- <= 0) {
            this.courseChangeCooldown += random.nextInt(5) + 2;
            double dist = Math.sqrt(distSq);

            if (isCourseTraversable(dist)) {
                setDeltaMovement(
                        getDeltaMovement()
                                .add(dx / dist * 0.1D, dy / dist * 0.1D, dz / dist * 0.1D));
            } else {
                pickWaypoint(null);
            }
        }

        if (this.targetedEntity != null && !this.targetedEntity.isAlive())
            this.targetedEntity = null;
        if (this.targetedEntity == null || this.attackCounter <= 0)
            this.targetedEntity = findTarget();

        if (this.targetedEntity != null
                && this.targetedEntity.distanceToSqr(this) < GUN_RANGE * GUN_RANGE) {
            engage(level, this.targetedEntity);
        } else if (this.attackCounter > 0) {
            this.attackCounter = 0;
        }

        entityData.set(ATTACKING, (byte) (this.attackCounter > 10 ? 1 : 0));
    }

    private void engage(ServerLevel level, Entity target) {
        Vec3 look = getLookAngle();
        double startX = getX() + look.x * 2D;
        double startY = getY() - 0.5D;
        double startZ = getZ() + look.z * 2D;

        if (++this.attackCounter >= 200) this.attackCounter -= 200;

        if (this.attackCounter == 80) {
            level.playSound(
                    null, this, ModSounds.CHOPPER_CHARGE.get(), SoundSource.HOSTILE, 5.0F, 1.0F);
        }

        if (this.attackCounter >= 120 && this.attackCounter % 2 == 0) {

            Vec3 heading =
                    new Vec3(
                                    target.getX() - startX - 1 + random.nextInt(3),
                                    target.getBoundingBox().minY
                                            + target.getBbHeight() / 2F
                                            - startY
                                            - 1
                                            + random.nextInt(3),
                                    target.getZ() - startZ - 1 + random.nextInt(3))
                            .normalize()
                            .scale(3D);

            EntityBullet bullet =
                    new EntityBullet(
                            ModEntities.BULLET.get(), level, this, 3.0F, 35, 45, false, "chopper");
            bullet.setPos(startX, startY, startZ);
            bullet.setDeltaMovement(heading);
            bullet.setDamage(3 + random.nextInt(5));
            level.addFreshEntity(bullet);
        }

        if (++this.mineDropCounter > 100 && random.nextInt(15) == 0) {
            this.mineDropCounter = 0;
            level.playSound(
                    null, this, ModSounds.CHOPPER_DROP.get(), SoundSource.HOSTILE, 15.0F, 1.0F);
            dropMine(level, 0, 0);

            if (random.nextInt(3) == 0) {
                dropMine(level, 1, 0);
                dropMine(level, -1, 0);
                dropMine(level, 0, 1);
                dropMine(level, 0, -1);
            }
        }
    }

    private void dropMine(ServerLevel level, double motionX, double motionZ) {
        level.addFreshEntity(
                new EntityChopperMine(
                        level, getX(), getY() - 0.5D, getZ(), motionX, -0.3D, motionZ, this));
    }

    private void crashStep(ServerLevel level) {
        setDeltaMovement(getDeltaMovement().add(0D, -0.08D, 0D));

        Vec3 motion = getDeltaMovement();
        if (Math.sqrt(motion.x * motion.x + motion.z * motion.z) * 1.2D < 1.8D) {
            setDeltaMovement(motion.x * 1.2D, motion.y, motion.z * 1.2D);
        }

        if (random.nextInt(20) == 0) {
            level.explode(this, getX(), getY(), getZ(), 5F, Level.ExplosionInteraction.BLOCK);
        }

        level.sendParticles(
                HbmParticles.LAUNCH_SMOKE.get(), getX(), getY(), getZ(), 10, 1D, 1D, 1D, 0.05D);

        setYRot(getYRot() + 20F);

        if (onGround()) {
            level.explode(this, getX(), getY(), getZ(), 15F, Level.ExplosionInteraction.BLOCK);
            dropItems(level);
            discard();
        }
    }

    private @Nullable Entity findTarget() {
        AABB box = getBoundingBox().inflate(TARGET_RANGE, TARGET_RANGE / 2D, TARGET_RANGE);
        Entity closest = null;
        double closestSq = -1D;

        for (LivingEntity candidate : level().getEntitiesOfClass(LivingEntity.class, box)) {
            if (!candidate.isAlive() || candidate instanceof EntityHunterChopper) continue;
            if (candidate instanceof Player player && player.getAbilities().invulnerable) continue;

            double distSq = candidate.distanceToSqr(this);
            double range = candidate.isShiftKeyDown() ? TARGET_RANGE * 0.8D : TARGET_RANGE;

            if (distSq < range * range && (closestSq == -1D || distSq < closestSq)) {
                closestSq = distSq;
                closest = candidate;
            }
        }

        return closest;
    }

    private void pickWaypoint(@Nullable Entity around) {
        double originX = around != null ? around.getX() : getX();
        double originZ = around != null ? around.getZ() : getZ();

        this.waypointX = originX + (random.nextFloat() * 2.0F - 1.0F) * 16.0F;
        this.waypointZ = originZ + (random.nextFloat() * 2.0F - 1.0F) * 16.0F;
        this.waypointY =
                level().getHeight(
                                        Heightmap.Types.WORLD_SURFACE,
                                        (int) this.waypointX,
                                        (int) this.waypointZ)
                        + 10
                        + random.nextInt(15);
    }

    private boolean isCourseTraversable(double distance) {
        Vec3 step =
                new Vec3(this.waypointX - getX(), this.waypointY - getY(), this.waypointZ - getZ())
                        .scale(1 / distance);
        AABB box = getBoundingBox();

        for (int i = 1; i < distance; i++) {
            box = box.move(step);
            if (!level().noCollision(this, box)) return false;
        }

        return true;
    }

    private void aimAt(@Nullable Entity target) {
        Vec3 motion = getDeltaMovement();
        double bearing =
                target != null
                        ? Math.atan2(getX() - target.getX(), getZ() - target.getZ())
                        : Math.atan2(motion.x, motion.z);
        float wanted = (float) (bearing * 180D / Math.PI);
        float delta = getYRot() - wanted;

        this.yRotO = getYRot();

        if (delta >= 10F) setYRot(getYRot() - 10F);
        else if (delta <= -10F) setYRot(getYRot() + 10F);
        this.yRotO = getYRot();

        float horizontal = (float) Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        setXRot((float) (Math.atan2(motion.y, horizontal) * 180D / Math.PI));
        this.xRotO = getXRot();

        if (getXRot() <= 330F && getXRot() >= 30F) setXRot(getXRot() < 180F ? 30F : 330F);
    }

    private void initDeath(ServerLevel level) {
        level.explode(this, getX(), getY(), getZ(), 10F, Level.ExplosionInteraction.BLOCK);
        if (!isDying()) {
            level.playSound(
                    null, this, ModSounds.CHOPPER_DAMAGE.get(), SoundSource.HOSTILE, 10.0F, 1.0F);
        }
    }

    private void dropDamageItem(ServerLevel level) {
        spawnAtLocation(
                level,
                new ItemStack(
                        random.nextInt(10) < 6
                                ? ModItems.COMBINE_SCRAP.get()
                                : ModItems.plate(Mats.MAT_CMB)));
    }

    private void dropItems(ServerLevel level) {
        spawnAtLocation(level, new ItemStack(ModItems.COMBINE_SCRAP.get(), random.nextInt(8) + 1));
        spawnAtLocation(level, new ItemStack(ModItems.plate(Mats.MAT_CMB), random.nextInt(5) + 1));
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide()) {
            this.flyingLoop =
                    updateLoop(this.flyingLoop, ModSounds.CHOPPER_FLYING_LOOP.get(), !isDying());
            this.crashingLoop =
                    updateLoop(this.crashingLoop, ModSounds.CHOPPER_CRASHING_LOOP.get(), isDying());
            return;
        }

        bossEvent.setProgress(getHealth() / getMaxHealth());
    }

    private @Nullable AudioWrapper updateLoop(
            @Nullable AudioWrapper audio, SoundEvent sound, boolean wanted) {
        if (!wanted) {
            if (audio != null) audio.stopSound();
            return null;
        }

        if (audio == null || !audio.isPlaying()) {
            audio =
                    AudioSystem.getLoopedSound(
                            sound,
                            SoundSource.HOSTILE,
                            (float) getX(),
                            (float) getY(),
                            (float) getZ(),
                            10F,
                            160F,
                            1F,
                            20);
            if (audio != null) audio.startSound();
        }

        if (audio != null) {
            audio.keepAlive();
            audio.updatePosition((float) getX(), (float) getY(), (float) getZ());
        }

        return audio;
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);

        if (this.flyingLoop != null) this.flyingLoop.stopSound();
        if (this.crashingLoop != null) this.crashingLoop.stopSound();
        this.flyingLoop = null;
        this.crashingLoop = null;
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource source) {
        return null;
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return null;
    }

    @Override
    protected float getSoundVolume() {
        return 10.0F;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 25000D;
    }
}
