/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.entity.logic;

import com.hbm.config.HbmConfig;
import com.hbm.config.RuntimeConfig;
import com.hbm.entity.mob.glyphid.GlyphidEntity;
import com.hbm.registration.Reg;
import com.hbm.util.RegistryUtil;
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

public class WaypointEntity extends Entity {

    public static Reg.@Nullable EntityHandle<WaypointEntity> TYPE;

    public static void register() {
        TYPE =
                Reg.entity(
                        "entity_waypoint",
                        () ->
                                EntityType.Builder.<WaypointEntity>of(
                                                WaypointEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(0.1F, 0.1F)
                                        .clientTrackingRange(16)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_waypoint"))));
    }

    private static final EntityDataAccessor<Integer> WAYPOINT_TYPE =
            SynchedEntityData.defineId(WaypointEntity.class, EntityDataSerializers.INT);
    private static final int VISIT_PERIOD = 40;

    public int maxAge = 2400;
    public int radius = 3;
    public boolean highPriority = false;
    protected @Nullable WaypointEntity additional;
    private boolean additionalReleased;

    public WaypointEntity(EntityType<? extends WaypointEntity> type, Level level) {
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

    public void setAdditionalWaypoint(WaypointEntity waypoint) {
        additional = waypoint;
    }

    public int getColor() {
        return switch (getWaypointType()) {
            case GlyphidEntity.TASK_RETREAT_FOR_REINFORCEMENTS -> 0x5FA6E8;
            case GlyphidEntity.TASK_BUILD_HIVE, GlyphidEntity.TASK_INITIATE_RETREAT -> 0x127766;
            default -> 0x566573;
        };
    }

    private AABB area() {
        return new AABB(position(), position()).inflate(radius);
    }

    @Override
    public void tick() {
        super.tick();
        if (tickCount >= maxAge) discard();

        if (level() instanceof ServerLevel server) {
            if (tickCount % VISIT_PERIOD == 0) guide(server);
        } else if (((RuntimeConfig) HbmConfig.RUNTIME).waypointDebug()) {
            showArea();
        }
    }

    private void guide(ServerLevel server) {
        int task = getWaypointType();
        for (var bug : server.getEntitiesOfClass(GlyphidEntity.class, area())) {
            releaseAdditional(server);
            boolean routedHere =
                    bug.getWaypoint() == this && !bug.isScoutType() && !bug.isNuclearType();
            if (routedHere) bug.setCurrentTask(task, additional);
            if (task != GlyphidEntity.TASK_BUILD_HIVE || bug.isScoutType()) discard();
        }
    }

    private void releaseAdditional(ServerLevel server) {
        if (additional == null || additionalReleased) return;
        additionalReleased = true;
        server.addFreshEntity(additional);
    }

    private void showArea() {
        var box = area();
        double x = box.minX + (random.nextDouble() - 0.5) * box.getXsize();
        double y = box.minY + random.nextDouble() * box.getYsize();
        double z = box.minZ + (random.nextDouble() - 0.5) * box.getZsize();
        level().addParticle(new DustParticleOptions(getColor(), 1.0F), x, y, z, 0D, 0D, 0D);
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
