// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.mob;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class EntityUFOBase extends Mob implements Enemy {

    private static final EntityDataAccessor<Integer> WAYPOINT_X =
            SynchedEntityData.defineId(EntityUFOBase.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> WAYPOINT_Y =
            SynchedEntityData.defineId(EntityUFOBase.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> WAYPOINT_Z =
            SynchedEntityData.defineId(EntityUFOBase.class, EntityDataSerializers.INT);

    protected int scanCooldown;
    protected int courseChangeCooldown;
    protected @Nullable Entity target;

    protected EntityUFOBase(EntityType<? extends EntityUFOBase> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    @Override
    protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {}

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(WAYPOINT_X, 0);
        builder.define(WAYPOINT_Y, 0);
        builder.define(WAYPOINT_Z, 0);
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            discard();
            return;
        }

        setDeltaMovement(Vec3.ZERO);

        if (this.target != null && !this.target.isAlive()) this.target = null;

        scanForTarget();

        if (this.courseChangeCooldown <= 0) setCourse();
    }

    protected void scanForTarget() {
        if (this.scanCooldown > 0) return;

        int range = getScanRange();
        AABB box = getBoundingBox().inflate(range, range / 2D, range);
        this.target = null;

        for (Player player : level().getEntitiesOfClass(Player.class, box)) {
            if (!player.isAlive() || !canTarget(player)) continue;
            if (player.isCreative()) continue;
            if (player.hasEffect(MobEffects.INVISIBILITY)) continue;

            if (this.target == null || distanceToSqr(player) < distanceToSqr(this.target)) {
                this.target = player;
            }
        }

        this.scanCooldown = getScanDelay();
    }

    protected int getScanRange() {
        return 50;
    }

    protected int getScanDelay() {
        return 100;
    }

    protected boolean isCourseTraversable(double distance) {
        Vec3 step =
                new Vec3(getWaypointX() - getX(), getWaypointY() - getY(), getWaypointZ() - getZ())
                        .scale(1 / distance);
        AABB box = getBoundingBox();

        for (int i = 1; i < distance; i++) {
            box = box.move(step);
            if (!level().noCollision(this, box)) return false;
        }

        return true;
    }

    protected void approachPosition(double speed) {
        Vec3 delta =
                new Vec3(getWaypointX() - getX(), getWaypointY() - getY(), getWaypointZ() - getZ());
        double len = delta.length();

        if (len <= 5) return;

        if (isCourseTraversable(len)) {
            setDeltaMovement(delta.scale(speed / len));
        } else {
            this.courseChangeCooldown = 0;
        }
    }

    protected void setCourse() {
        if (this.target != null) {
            setCourseForTarget();
            this.courseChangeCooldown = 20 + random.nextInt(20);
        } else {
            setCourseWithoutTarget();
            this.courseChangeCooldown = 60 + random.nextInt(20);
        }
    }

    protected void setCourseForTarget() {
        Vec3 vec =
                new Vec3(getX() - this.target.getX(), 0, getZ() - this.target.getZ())
                        .yRot((float) Math.PI * 2 * random.nextFloat());

        double length = vec.length();
        double overshoot = 10 + random.nextDouble() * 10;

        int wX = (int) Math.floor(this.target.getX() - vec.x / length * overshoot);
        int wZ = (int) Math.floor(this.target.getZ() - vec.z / length * overshoot);

        setWaypoint(
                wX,
                Math.max(surfaceAt(wX, wZ), (int) this.target.getY()) + targetHeightOffset(),
                wZ);
    }

    protected int targetHeightOffset() {
        return 2 + random.nextInt(2);
    }

    protected int wanderHeightOffset() {
        return 2 + random.nextInt(3);
    }

    protected void setCourseWithoutTarget() {
        int x = (int) Math.floor(getX() + random.nextGaussian() * 5);
        int z = (int) Math.floor(getZ() + random.nextGaussian() * 5);
        setWaypoint(x, surfaceAt(x, z) + wanderHeightOffset(), z);
    }

    protected int surfaceAt(int x, int z) {
        return level().getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
    }

    public void setWaypoint(int x, int y, int z) {
        entityData.set(WAYPOINT_X, x);
        entityData.set(WAYPOINT_Y, y);
        entityData.set(WAYPOINT_Z, z);
    }

    public int getWaypointX() {
        return entityData.get(WAYPOINT_X);
    }

    public int getWaypointY() {
        return entityData.get(WAYPOINT_Y);
    }

    public int getWaypointZ() {
        return entityData.get(WAYPOINT_Z);
    }

    protected boolean canTarget(Entity entity) {
        return !(entity instanceof EntityUFOBase);
    }
}
