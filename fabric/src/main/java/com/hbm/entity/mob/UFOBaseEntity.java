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

import com.hbm.api.environment.SuffocationImmune;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class UFOBaseEntity extends FlyingMobEntity implements Enemy, SuffocationImmune {

    private static final TargetingConditions PREY =
            TargetingConditions.forNonCombat()
                    .ignoreLineOfSight()
                    .selector(
                            (candidate, _) ->
                                    candidate instanceof Player player
                                            && !player.isCreative()
                                            && !player.hasEffect(MobEffects.INVISIBILITY));

    protected int scanCooldown;
    protected int courseChangeCooldown;
    protected @Nullable Entity target;

    protected UFOBaseEntity(EntityType<? extends UFOBaseEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            discard();
            return;
        }

        setDeltaMovement(Vec3.ZERO);
        if (this.target != null && (!this.target.isAlive() || this.target.isSpectator()))
            this.target = null;

        if (this.scanCooldown <= 0) {
            int range = getScanRange();
            this.target =
                    level.getNearestEntity(
                            Player.class,
                            PREY,
                            this,
                            getX(),
                            getY(),
                            getZ(),
                            getBoundingBox().inflate(range, range / 2, range));
            this.scanCooldown = getScanDelay();
        }

        if (this.courseChangeCooldown <= 0) replot();
    }

    private void replot() {
        Entity prey = this.target;
        if (prey != null) {
            float yaw = (float) Math.PI * 2F * random.nextFloat();
            double overshoot = 10 + random.nextDouble() * 10;
            setWaypointPast(
                    prey,
                    offsetFrom(prey).yRot(yaw),
                    overshoot,
                    (surface, preyY) -> Math.max(surface, preyY) + targetHeightOffset());
        } else {
            setCourseWithoutTarget();
        }
        this.courseChangeCooldown = (prey != null ? 20 : 60) + random.nextInt(20);
    }

    protected void setCourseWithoutTarget() {
        setWaypointOver(
                getX() + random.nextGaussian() * 5,
                getZ() + random.nextGaussian() * 5,
                wanderHeightOffset());
    }

    protected int getScanRange() {
        return 50;
    }

    protected int getScanDelay() {
        return 100;
    }

    protected int targetHeightOffset() {
        return 2 + random.nextInt(2);
    }

    protected int wanderHeightOffset() {
        return 2 + random.nextInt(3);
    }

    protected void approachPosition(double speed) {
        if (!steerToWaypoint(speed)) this.courseChangeCooldown = 0;
    }
}
