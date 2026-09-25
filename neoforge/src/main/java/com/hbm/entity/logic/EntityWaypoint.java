// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.logic;

import com.hbm.entity.mob.glyphid.EntityGlyphid;
import com.hbm.lib.Library;
import com.hbm.platform.Services;
import com.hbm.registration.IRegistrar;
import com.hbm.registration.Reg;
import java.util.List;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public class EntityWaypoint extends Entity {

    public static Reg.@Nullable EntityHandle<EntityWaypoint> TYPE;

    public static void register(IRegistrar r) {
        TYPE =
                Reg.entity(
                        "entity_waypoint",
                        () ->
                                EntityType.Builder.<EntityWaypoint>of(
                                                EntityWaypoint::new, MobCategory.MISC)
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(0.1F, 0.1F)
                                        .clientTrackingRange(16)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_waypoint"))));
    }

    private static final EntityDataAccessor<Integer> WAYPOINT_TYPE =
            SynchedEntityData.defineId(EntityWaypoint.class, EntityDataSerializers.INT);

    public int maxAge = 2400;
    public int radius = 3;
    public boolean highPriority = false;
    protected EntityWaypoint additional;
    private boolean hasSpawned = false;

    public EntityWaypoint(EntityType<? extends EntityWaypoint> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(WAYPOINT_TYPE, 0);
    }

    public int getWaypointType() {
        return entityData.get(WAYPOINT_TYPE);
    }

    public void setWaypointType(int waypointType) {
        entityData.set(WAYPOINT_TYPE, waypointType);
    }

    public void setHighPriority() {
        highPriority = true;
    }

    public void setAdditionalWaypoint(EntityWaypoint waypoint) {
        additional = waypoint;
    }

    public int getColor() {
        int type = getWaypointType();
        if (type == EntityGlyphid.TASK_RETREAT_FOR_REINFORCEMENTS) return 0x5FA6E8;
        if (type == EntityGlyphid.TASK_BUILD_HIVE || type == EntityGlyphid.TASK_INITIATE_RETREAT)
            return 0x127766;
        return 0x566573;
    }

    @Override
    public void tick() {
        super.tick();

        if (tickCount >= maxAge) {
            discard();
        }

        AABB bb = new AABB(getX(), getY(), getZ(), getX(), getY(), getZ()).inflate(radius);

        if (!level().isClientSide()) {

            if (tickCount % 40 == 0) {

                List<Entity> targets = level().getEntities(this, bb);

                for (Entity e : targets) {
                    if (e instanceof EntityGlyphid bug) {

                        if (additional != null && !hasSpawned) {
                            level().addFreshEntity(additional);
                            hasSpawned = true;
                        }

                        boolean exceptions =
                                bug.getWaypoint() != this
                                        || bug.isScoutType()
                                        || bug.isNuclearType();

                        if (!exceptions) bug.setCurrentTask(getWaypointType(), additional);

                        if (getWaypointType() == EntityGlyphid.TASK_BUILD_HIVE) {
                            if (bug.isScoutType()) discard();
                        } else {
                            discard();
                        }
                    }
                }
            }
        } else if (Services.CONFIG.runtime().waypointDebug()) {

            double x = bb.minX + (random.nextDouble() - 0.5) * (bb.maxX - bb.minX);
            double y = bb.minY + random.nextDouble() * (bb.maxY - bb.minY);
            double z = bb.minZ + (random.nextDouble() - 0.5) * (bb.maxZ - bb.minZ);

            level().addParticle(new DustParticleOptions(getColor(), 1.0F), x, y, z, 0D, 0D, 0D);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        setWaypointType(input.getIntOr("type", 0));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("type", getWaypointType());
    }
}
