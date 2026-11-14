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

package com.hbm.items.weapon.sedna.cue;

import com.hbm.items.weapon.sedna.GunBaseNTItem;
import com.hbm.particle.helper.CasingCreator;
import java.util.function.Predicate;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public record CasingEjection(
        Point hip,
        Point aimed,
        Predicate<ItemStack> aiming,
        Point kick,
        RandomFloatProvider pitchSpin,
        RandomFloatProvider yawSpin,
        Smoke smoke,
        @Nullable Double legacyFront) {

    private static final double KICK_SPREAD = 0.01;

    public static CasingEjection from(double front, double up, double side) {
        var point = new Point(front, up, side);
        return new CasingEjection(
                point,
                point,
                GunBaseNTItem::getIsAiming,
                new Point(0, 0, 0),
                RandomFloatProvider.fixed(5F),
                RandomFloatProvider.fixed(10F),
                Smoke.NONE,
                null);
    }

    public CasingEjection aimed(double front, double up, double side) {
        return new CasingEjection(
                hip,
                new Point(front, up, side),
                aiming,
                kick,
                pitchSpin,
                yawSpin,
                smoke,
                legacyFront);
    }

    public CasingEjection aimedWhile(Predicate<ItemStack> aiming) {
        return new CasingEjection(hip, aimed, aiming, kick, pitchSpin, yawSpin, smoke, legacyFront);
    }

    public CasingEjection kick(double front, double up, double side) {
        return new CasingEjection(
                hip,
                aimed,
                aiming,
                new Point(front, up, side),
                pitchSpin,
                yawSpin,
                smoke,
                legacyFront);
    }

    public CasingEjection spin(RandomFloatProvider pitch, RandomFloatProvider yaw) {
        return new CasingEjection(hip, aimed, aiming, kick, pitch, yaw, smoke, legacyFront);
    }

    public CasingEjection smoke(int life, double lift, int nodeLife) {
        return smoke(new Smoke(true, life, lift, nodeLife));
    }

    public CasingEjection smoke(Smoke smoke) {
        return new CasingEjection(hip, aimed, aiming, kick, pitchSpin, yawSpin, smoke, legacyFront);
    }

    public CasingEjection legacyFront(double front) {
        return new CasingEjection(hip, aimed, aiming, kick, pitchSpin, yawSpin, smoke, front);
    }

    void toss(LivingEntity shooter, ItemStack stack, String casing) {
        var from = aiming.test(stack) ? aimed : hip;
        var random = shooter.getRandom();
        float pitch = pitchSpin.sample(random);
        float yaw = yawSpin.sample(random);
        CasingCreator.composeEffect(
                shooter.level(),
                shooter,
                from.front,
                from.up,
                from.side,
                kick.front,
                kick.up,
                kick.side,
                KICK_SPREAD,
                pitch,
                yaw,
                casing,
                smoke.lit,
                smoke.life,
                smoke.lift,
                smoke.nodeLife,
                legacyFront != null ? legacyFront : from.front);
    }

    public record Point(double front, double up, double side) {}

    public record Smoke(boolean lit, int life, double lift, int nodeLife) {
        public static final Smoke NONE = new Smoke(false, 0, 0, 0);
    }
}
