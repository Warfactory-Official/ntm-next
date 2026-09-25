// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.mob;

import com.hbm.advancement.BossKilledTrigger;
import com.hbm.advancement.HbmCriteria;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.explosion.ExplosionNukeSmall;
import com.hbm.interfaces.IRadiationImmune;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.factory.XFactoryNPC;
import com.hbm.lib.ModDamageTypes;
import com.hbm.sound.ModSounds;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import com.hbm.util.ContaminationUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
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

public class EntityUFO extends Mob implements Enemy, IRadiationImmune {

    private static final EntityDataAccessor<Boolean> BEAM =
            SynchedEntityData.defineId(EntityUFO.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> WAYPOINT_X =
            SynchedEntityData.defineId(EntityUFO.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> WAYPOINT_Y =
            SynchedEntityData.defineId(EntityUFO.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> WAYPOINT_Z =
            SynchedEntityData.defineId(EntityUFO.class, EntityDataSerializers.INT);

    private final ServerBossEvent bossEvent =
            new ServerBossEvent(
                    getUUID(),
                    getDisplayName(),
                    BossEvent.BossBarColor.GREEN,
                    BossEvent.BossBarOverlay.PROGRESS);

    private final List<Entity> secondaries = new ArrayList<>();

    public int courseChangeCooldown;
    public int scanCooldown;
    public int hurtCooldown;
    public int beamTimer;

    private @Nullable Entity target;

    public EntityUFO(EntityType<? extends EntityUFO> type, Level level) {
        super(type, level);
        setNoGravity(true);
        this.xpReward = 500;

        this.deathTime = -30;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20000.0D)
                .add(Attributes.FOLLOW_RANGE, 150.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BEAM, false);
        builder.define(WAYPOINT_X, 0);
        builder.define(WAYPOINT_Y, 0);
        builder.define(WAYPOINT_Z, 0);
    }

    @Override
    protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {}

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 500000D;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {

        if (this.hurtCooldown > 0) return false;

        boolean hit = super.hurtServer(level, source, amount);
        if (hit) this.hurtCooldown = 5;
        return hit;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {

        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            discard();
            return;
        }

        if (this.hurtCooldown > 0) this.hurtCooldown--;
        if (this.courseChangeCooldown > 0) this.courseChangeCooldown--;
        if (this.scanCooldown > 0) this.scanCooldown--;

        if (this.target != null && !this.target.isAlive()) this.target = null;

        if (this.scanCooldown <= 0) scanForTargets();
        if (this.target != null && this.courseChangeCooldown <= 0) setCourse();

        tickBeam(level);
        tickWeapons();
        approachWaypoint();
    }

    private void scanForTargets() {

        this.secondaries.clear();
        this.target = null;

        for (Entity entity : level().getEntities(this, getBoundingBox().inflate(100D, 50D, 100D))) {

            if (!entity.isAlive() || !canTarget(entity)) continue;

            if (entity instanceof Player player) {
                if (player.isCreative()) continue;
                if (player.hasEffect(MobEffects.INVISIBILITY)) continue;

                if (this.target == null || distanceToSqr(player) < distanceToSqr(this.target))
                    this.target = player;
            }

            if (entity instanceof LivingEntity living
                    && distanceToSqr(living) < 100D * 100D
                    && getSensing().hasLineOfSight(living)
                    && living != this.target) {
                this.secondaries.add(living);
            }
        }

        if (this.target == null && !this.secondaries.isEmpty()) {
            this.target = this.secondaries.get(this.random.nextInt(this.secondaries.size()));
        }

        this.scanCooldown = 50;
    }

    private void setCourse() {

        Vec3 vec = new Vec3(getX() - this.target.getX(), 0D, getZ() - this.target.getZ());

        if (this.random.nextInt(3) > 0)
            vec = vec.yRot((float) Math.PI * 2F * this.random.nextFloat());

        double length = vec.length();
        double overshoot = 35D;

        int wX = (int) Math.floor(this.target.getX() - vec.x / length * overshoot);
        int wZ = (int) Math.floor(this.target.getZ() - vec.z / length * overshoot);

        int surface =
                level().getHeight(Heightmap.Types.WORLD_SURFACE, wX, wZ)
                        + 20
                        + this.random.nextInt(15);
        setWaypoint(wX, Math.max(surface, (int) this.target.getY() + 15), wZ);

        this.courseChangeCooldown = 40 + this.random.nextInt(20);
    }

    private void tickBeam(ServerLevel level) {

        if (this.beamTimer <= 0 && getBeam()) setBeam(false);

        if (this.target != null) {
            double dist =
                    Math.abs(this.target.getX() - getX()) + Math.abs(this.target.getZ() - getZ());
            if (dist < 25D) this.beamTimer = 30;
        }

        if (this.beamTimer <= 0) return;
        this.beamTimer--;

        if (!getBeam()) {
            level.playSound(null, this, ModSounds.UFO_BEAM.get(), SoundSource.HOSTILE, 10F, 1F);
            setBeam(true);
        }

        int groundY = groundBelow();
        if (groundY >= getY()) return;

        AABB column = new AABB(getX(), groundY, getZ(), getX(), getY(), getZ()).inflate(5D, 0D, 5D);

        for (Entity entity : level.getEntities(this, column)) {

            if (!canTarget(entity)) continue;

            entity.hurtServer(
                    level,
                    level.damageSources().source(ModDamageTypes.COMBINE, this, entity),
                    1000F);
            entity.igniteForSeconds(5);

            if (entity instanceof LivingEntity living) {
                ContaminationUtil.contaminate(
                        living, HazardType.RADIATION, ContaminationType.CREATIVE, 5F);
            }
        }
    }

    private void tickWeapons() {

        boolean laserPhase = this.tickCount % 300 < 200;
        int period = laserPhase ? 4 : 20;

        if (this.tickCount % period == 0) {

            if (!this.secondaries.isEmpty()) {
                Entity victim = this.secondaries.get(this.random.nextInt(this.secondaries.size()));
                if (!victim.isAlive()) this.secondaries.remove(victim);
                else shoot(victim, laserPhase);

            } else if (this.target != null) {
                shoot(this.target, laserPhase);
            }

        } else if (this.tickCount % period == period / 2 && this.target != null) {
            shoot(this.target, laserPhase);
        }
    }

    private void shoot(Entity victim, boolean laser) {
        if (laser) laserAttack(victim);
        else rocketAttack(victim);
    }

    private void laserAttack(Entity victim) {

        Vec3 vec =
                new Vec3(getX() - victim.getX(), 0D, getZ() - victim.getZ())
                        .yRot((float) Math.toRadians(-80 + this.random.nextInt(160)))
                        .normalize();

        double pivotX = getX() - vec.x * 10D;
        double pivotY = getY() + 0.5D;
        double pivotZ = getZ() - vec.z * 10D;

        Vec3 heading =
                new Vec3(
                                victim.getX() - pivotX,
                                victim.getY() + victim.getBbHeight() / 2D - pivotY,
                                victim.getZ() - pivotZ)
                        .normalize();

        EntityBulletBaseMK4 bullet =
                new EntityBulletBaseMK4(
                        level(),
                        this,
                        XFactoryNPC.worm_laser,
                        XFactoryNPC.WORM_LASER_DAMAGE,
                        0.02F,
                        pivotX,
                        pivotY,
                        pivotZ,
                        heading.x,
                        heading.y,
                        heading.z);

        bullet.accel = 1F;
        level().addFreshEntity(bullet);

        level().playSound(
                        null,
                        getX(),
                        getY(),
                        getZ(),
                        ModSounds.BALLS_LASER.get(),
                        SoundSource.HOSTILE,
                        5F,
                        1F);
    }

    private void rocketAttack(Entity victim) {

        Vec3 heading =
                new Vec3(
                                victim.getX() - getX(),
                                victim.getY() + victim.getBbHeight() / 2D - getY() - 0.5D,
                                victim.getZ() - getZ())
                        .normalize();

        EntityBulletBaseMK4 bullet =
                new EntityBulletBaseMK4(
                        level(),
                        this,
                        XFactoryNPC.ufo_rocket,
                        XFactoryNPC.UFO_ROCKET_DAMAGE,
                        0.02F,
                        getX(),
                        getY() - 0.5D,
                        getZ(),
                        heading.x,
                        heading.y,
                        heading.z);
        bullet.lockonTarget = victim;
        level().addFreshEntity(bullet);

        level().playSound(
                        null,
                        getX(),
                        getY(),
                        getZ(),
                        ModSounds.RICHARD_FIRE.get(),
                        SoundSource.HOSTILE,
                        5F,
                        1F);
    }

    private void approachWaypoint() {

        setDeltaMovement(Vec3.ZERO);

        if (this.courseChangeCooldown <= 0) return;

        Vec3 delta =
                new Vec3(getWaypointX() - getX(), getWaypointY() - getY(), getWaypointZ() - getZ());
        double len = delta.length();

        if (len <= 5D) return;

        double speed = this.target instanceof Player ? 5D : 2D;

        if (isCourseTraversable(len)) {
            setDeltaMovement(delta.scale(speed / len));
        } else {
            this.courseChangeCooldown = 0;
        }
    }

    private boolean isCourseTraversable(double distance) {

        Vec3 step =
                new Vec3(getWaypointX() - getX(), getWaypointY() - getY(), getWaypointZ() - getZ())
                        .scale(1D / distance);
        AABB box = getBoundingBox();

        for (int i = 1; i < distance; i++) {
            box = box.move(step);
            if (!level().noCollision(this, box)) return false;
        }

        return true;
    }

    private int groundBelow() {

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int x = Mth.floor(getX());
        int z = Mth.floor(getZ());

        for (int y = (int) Math.ceil(getY()); y >= level().getMinY(); y--) {
            if (!level().getBlockState(pos.set(x, y, z)).isAir()) return y;
        }

        return level().getMinY();
    }

    private boolean canTarget(Entity entity) {
        return !(entity instanceof EntityUFO) && !(entity instanceof EntityBulletBaseMK4);
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide()) bossEvent.setProgress(getHealth() / getMaxHealth());
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
    protected void tickDeath() {

        if (getBeam()) setBeam(false);

        setDeltaMovement(getDeltaMovement().subtract(0D, 0.05D, 0D));

        if (this.deathTime == -10) {
            level().playSound(
                            null,
                            this,
                            ModSounds.CHOPPER_DAMAGE.get(),
                            SoundSource.HOSTILE,
                            10F,
                            1F);
        }

        if (this.deathTime == 19 && level() instanceof ServerLevel server) {

            server.explode(
                    this, getX(), getY(), getZ(), 10F, true, Level.ExplosionInteraction.BLOCK);
            ExplosionNukeSmall.explode(
                    server, getX(), getY(), getZ(), ExplosionNukeSmall.PARAMS_MEDIUM);

            HbmCriteria.bossKilled(this, BossKilledTrigger.Kind.UFO, 200D);

            for (Player player : server.getPlayers(p -> p.distanceToSqr(this) <= 200D * 200D)) {
                player.getInventory()
                        .placeItemBackInInventory(new ItemStack(ModItems.COIN_UFO.get()));
            }
        }

        super.tickDeath();
    }

    @Override
    protected float getSoundVolume() {
        return 10F;
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BLAZE_HURT;
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return null;
    }

    public void setBeam(boolean beam) {
        this.entityData.set(BEAM, beam);
    }

    public boolean getBeam() {
        return this.entityData.get(BEAM);
    }

    public void setWaypoint(int x, int y, int z) {
        this.entityData.set(WAYPOINT_X, x);
        this.entityData.set(WAYPOINT_Y, y);
        this.entityData.set(WAYPOINT_Z, z);
    }

    public int getWaypointX() {
        return this.entityData.get(WAYPOINT_X);
    }

    public int getWaypointY() {
        return this.entityData.get(WAYPOINT_Y);
    }

    public int getWaypointZ() {
        return this.entityData.get(WAYPOINT_Z);
    }
}
