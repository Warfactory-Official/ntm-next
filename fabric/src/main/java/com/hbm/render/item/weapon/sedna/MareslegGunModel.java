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

package com.hbm.render.item.weapon.sedna;

import com.hbm.main.ResourceManager;
import com.mojang.math.Axis;
import java.util.function.Predicate;
import net.minecraft.resources.Identifier;

final class MareslegGunModel {

    static final GunAnimationChannels RIG = new GunAnimationChannels();
    private static final GunAnimationChannels.Scalar DRAW = RIG.x("EQUIP");
    private static final GunAnimationChannels.Vec3 KICK = RIG.vec3("RECOIL");
    private static final GunAnimationChannels.Scalar CYCLE = RIG.x("LEVER");
    private static final GunAnimationChannels.Scalar ROLL = RIG.z("TURN");
    private static final GunAnimationChannels.Scalar TWIRL = RIG.x("FLIP");
    private static final GunAnimationChannels.Scalar TILT = RIG.x("LIFT");
    private static final GunAnimationChannels.Vec3 EJECT = RIG.vec3("SHELL");
    private static final GunAnimationChannels.Scalar SPARE = RIG.x("FLAG");

    static {
        RIG.carries("HAMMER", "SIGHT");
    }

    private MareslegGunModel() {}

    static void gun(
            GunRenderDefinition.Builder b,
            Identifier sheet,
            Predicate<GunRenderDefinition.Reading> sawn) {
        b.mesh(ResourceManager.maresleg)
                .skin(sheet)
                .shift(r -> r.x(KICK) * 2, r -> r.y(KICK), r -> r.z(KICK))
                .spin(Axis.XP, r -> r.z(KICK) * 5)
                .spin(Axis.ZP, ROLL)
                .hinge(0, 0, -4, Axis.XP, TILT)
                .hinge(0, 0, -4, Axis.XN, DRAW)
                .hinge(0, 0, -2, Axis.XN, TWIRL)
                .group(
                        smoke ->
                                muzzle(smoke, sawn)
                                        .spin(Axis.ZN, ROLL)
                                        .spin(Axis.XP, TWIRL)
                                        .turn(Axis.YP, 90)
                                        .smoke(0.25))
                .part("Gun")
                .when(sawn.negate(), full -> full.part("Stock").part("Barrel"))
                .group(lever -> lever.hinge(0, 0.125, -2.875, Axis.XP, CYCLE).part("Lever"))
                .group(
                        shell ->
                                shell.shift(
                                                r -> r.x(EJECT),
                                                r -> r.y(EJECT) - 0.75,
                                                r -> r.z(EJECT))
                                        .part("Shell"))
                .when(
                        r -> r.get(SPARE) != 0,
                        spare -> spare.group(shell -> shell.shift(0, -0.5, 0).part("Shell")))
                .group(
                        flash ->
                                muzzle(flash, sawn)
                                        .turn(Axis.YP, 90)
                                        .roll(90)
                                        .flash(MuzzleFlash.NORMAL, 75, 5));
    }

    private static GunRenderDefinition.Builder muzzle(
            GunRenderDefinition.Builder b, Predicate<GunRenderDefinition.Reading> sawn) {
        return b.either(sawn, end -> end.shift(0, 1, 3.75), end -> end.shift(0, 1, 8));
    }
}
