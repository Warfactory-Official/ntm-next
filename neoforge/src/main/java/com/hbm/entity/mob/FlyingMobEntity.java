/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.entity.mob;

import java.util.function.IntBinaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public abstract class FlyingMobEntity extends Mob {

    private BlockPos waypoint = BlockPos.ZERO;

    protected FlyingMobEntity(EntityType<? extends FlyingMobEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        moveControl = new Ghast.GhastMoveControl<>(this, false, () -> false);
    }

    @Override
    public void travel(Vec3 input) {
        travelFlying(input, 0.02F);
    }

    @Override
    public boolean onClimbable() {
        return false;
    }

    @Override
    protected void checkFallDamage(
            double distance, boolean onGround, BlockState state, BlockPos pos) {}

    public void setWaypoint(int x, int y, int z) {
        waypoint = new BlockPos(x, y, z);
    }

    public int getWaypointX() {
        return waypoint.getX();
    }

    public int getWaypointY() {
        return waypoint.getY();
    }

    public int getWaypointZ() {
        return waypoint.getZ();
    }

    protected int surfaceAt(int x, int z) {
        return level().getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
    }

    protected final Vec3 offsetFrom(Entity prey) {
        return new Vec3(getX() - prey.getX(), 0, getZ() - prey.getZ());
    }

    protected final void setWaypointPast(
            Entity prey, Vec3 bearing, double overshoot, IntBinaryOperator altitude) {
        double length = bearing.length();
        int x = Mth.floor(prey.getX() - bearing.x / length * overshoot);
        int z = Mth.floor(prey.getZ() - bearing.z / length * overshoot);
        setWaypoint(x, altitude.applyAsInt(surfaceAt(x, z), (int) prey.getY()), z);
    }

    protected final void setWaypointOver(double x, double z, int clearance) {
        int column = Mth.floor(x);
        int row = Mth.floor(z);
        setWaypoint(column, surfaceAt(column, row) + clearance, row);
    }

    protected final boolean steerToWaypoint(double speed) {
        double dx = waypoint.getX() - getX();
        double dy = waypoint.getY() - getY();
        double dz = waypoint.getZ() - getZ();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance <= 5) return true;
        if (!clearCourse(dx, dy, dz, distance)) return false;
        setDeltaMovement(dx * speed / distance, dy * speed / distance, dz * speed / distance);
        return true;
    }

    protected final boolean canReach(Vec3 travel) {
        return clearCourse(travel.x, travel.y, travel.z, travel.length());
    }

    private boolean clearCourse(double dx, double dy, double dz, double distance) {
        double sx = dx / distance;
        double sy = dy / distance;
        double sz = dz / distance;
        AABB box = getBoundingBox();
        for (int step = 1; step < distance; step++) {
            box = box.move(sx, sy, sz);
            if (!level().noCollision(this, box)) return false;
        }
        return true;
    }
}
