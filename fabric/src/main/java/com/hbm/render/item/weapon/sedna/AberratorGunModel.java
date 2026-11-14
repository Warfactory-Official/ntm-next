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

final class AberratorGunModel {

    static final GunAnimationChannels RIG = new GunAnimationChannels();
    static final GunAnimationChannels.Scalar DRAW = RIG.x("EQUIP");
    private static final GunAnimationChannels.Scalar RISE = RIG.y("RISE");
    static final GunAnimationChannels.Scalar KICK = RIG.x("RECOIL");
    private static final GunAnimationChannels.Scalar CYCLE = RIG.z("SLIDE");
    private static final GunAnimationChannels.Vec3 ROUND = RIG.vec3("BULLET");
    private static final GunAnimationChannels.Scalar COCK = RIG.x("HAMMER");
    static final GunAnimationChannels.Scalar ROLL = RIG.z("ROLL");
    private static final GunAnimationChannels.Vec3 CLIP = RIG.vec3("MAG");
    private static final GunAnimationChannels.Scalar CLIP_ROLL = RIG.z("MAGROLL");
    private static final GunAnimationChannels.Scalar SIGHT = RIG.x("SIGHT");

    static {
        RIG.carries("NULL");
    }

    private AberratorGunModel() {}

    static void gun(GunRenderDefinition.Builder b, Identifier sheet, int side) {
        b.mesh(ResourceManager.aberrator)
                .skin(sheet)
                .slideY(RISE)
                .hinge(0, 1, -2.25, Axis.XP, DRAW)
                .hinge(0, -1, -4, Axis.XP, KICK)
                .hinge(0, 1, 0, Axis.ZP, r -> r.get(ROLL) * side)
                .part("Gun")
                .group(sight -> sight.hinge(0, 2.4375, -1.9375, Axis.XP, SIGHT).part("Sight"))
                .group(
                        mag ->
                                mag.shift(r -> r.x(CLIP) * side, r -> r.y(CLIP), r -> r.z(CLIP))
                                        .hinge(0, 1, 0, Axis.ZP, r -> r.get(CLIP_ROLL) * side)
                                        .part("Magazine")
                                        .move(ROUND)
                                        .part("Bullet"))
                .group(slide -> slide.slideZ(CYCLE).part("Slide"))
                .group(
                        hammer ->
                                hammer.hinge(0, 1.25, -3.625, Axis.XP, r -> -45 + r.get(COCK))
                                        .part("Hammer"))
                .group(
                        smoke ->
                                smoke.shift(0, 2, 4)
                                        .spin(Axis.XN, KICK)
                                        .spin(Axis.ZN, r -> r.get(ROLL) * side)
                                        .turn(Axis.YP, 90)
                                        .size(0.5)
                                        .smoke(0.5))
                .group(
                        flash ->
                                flash.shift(0, 2, 4)
                                        .turn(Axis.YP, 90)
                                        .roll(90)
                                        .size(0.75)
                                        .flash(MuzzleFlash.NORMAL, 75, 7.5))
                .group(
                        fireball ->
                                fireball.shift(0, 2, -1.5)
                                        .size(0.5)
                                        .flash(MuzzleFlash.FIREBALL, 0, 0));
    }
}
