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
import net.minecraft.resources.Identifier;

final class BioRevolverGunModel {

    static final GunAnimationChannels RIG = new GunAnimationChannels();
    private static final GunAnimationChannels.Vec3 KICK = RIG.vec3("RECOIL");
    private static final GunAnimationChannels.Vec3 OPEN_SHIFT = RIG.vec3("RELOAD_MOVE");
    private static final GunAnimationChannels.Vec3 OPEN_TURN = RIG.vec3("RELOAD_ROT");
    private static final GunAnimationChannels.Scalar DRAW = RIG.x("EQUIP");
    private static final GunAnimationChannels.Scalar BREAK = RIG.z("FRONT");
    private static final GunAnimationChannels.Scalar CATCH = RIG.z("LATCH");
    private static final GunAnimationChannels.Scalar CYLINDER = RIG.z("DRUM");
    private static final GunAnimationChannels.Scalar CYLINDER_SLIDE = RIG.z("DRUM_PUSH");
    private static final GunAnimationChannels.Scalar COCK = RIG.z("HAMMER");

    private BioRevolverGunModel() {}

    static void gun(
            GunRenderDefinition.Builder b, Identifier sheet, int side, double drawY, double drawZ) {
        b.mesh(ResourceManager.bio_revolver)
                .skin(sheet)
                .move(KICK)
                .spin(Axis.XP, r -> r.z(KICK) * 10)
                .hinge(0, drawY, drawZ, Axis.XN, DRAW)
                .group(
                        smoke ->
                                smoke.shift(0, 1.5, 9.25)
                                        .spin(Axis.XP, r -> -r.z(KICK) * 10)
                                        .turn(Axis.YP, 90)
                                        .smoke(0.5))
                .move(OPEN_SHIFT)
                .spin(Axis.XP, r -> r.x(OPEN_TURN))
                .spin(Axis.ZP, r -> r.z(OPEN_TURN) * side)
                .spin(Axis.YP, r -> r.y(OPEN_TURN) * side)
                .part("Grip")
                .group(
                        front ->
                                front.spin(Axis.XP, BREAK)
                                        .part("Barrel")
                                        .group(
                                                latch ->
                                                        latch.hinge(
                                                                        0, 2.3125, -0.875, Axis.XP,
                                                                        CATCH)
                                                                .part("Latch"))
                                        .group(
                                                drum ->
                                                        drum.hinge(
                                                                        0,
                                                                        1,
                                                                        0,
                                                                        Axis.ZP,
                                                                        r -> r.get(CYLINDER) * 60)
                                                                .slideZ(CYLINDER_SLIDE)
                                                                .part("Drum")))
                .group(
                        hammer ->
                                hammer.hinge(0, 0, -4.5, Axis.XP, r -> -45 + 45 * r.get(COCK))
                                        .part("Hammer"))
                .group(
                        flash ->
                                flash.shift(0, 1.5, 9.25)
                                        .turn(Axis.YP, 90)
                                        .flash(MuzzleFlash.NORMAL, 75, 7.5));
    }
}
