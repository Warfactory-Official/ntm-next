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

import static com.hbm.items.weapon.sedna.mods.XWeaponModManager.ID_SILENCER;
import static com.hbm.items.weapon.sedna.mods.XWeaponModManager.ID_UZI_SATURN;

final class UziGunModel {

    static final GunAnimationChannels RIG = new GunAnimationChannels();
    private static final GunAnimationChannels.Vec3 THROW = RIG.vec3("YEET");
    private static final GunAnimationChannels.Scalar ROLL = RIG.x("SPEEN");
    private static final GunAnimationChannels.Scalar DRAW = RIG.x("EQUIP");
    private static final GunAnimationChannels.Scalar TILT = RIG.x("LIFT");
    private static final GunAnimationChannels.Scalar KICK = RIG.z("RECOIL");
    private static final GunAnimationChannels.Scalar FRONT_STOCK = RIG.x("STOCKFRONT");
    private static final GunAnimationChannels.Scalar BACK_STOCK = RIG.x("STOCKBACK");
    private static final GunAnimationChannels.Scalar BOLT = RIG.z("SLIDE");
    private static final GunAnimationChannels.Vec3 CLIP = RIG.vec3("MAG");
    private static final GunAnimationChannels.Scalar ROUND = RIG.x("BULLET");

    private UziGunModel() {}

    static void gun(GunRenderDefinition.Builder b, boolean mirrored) {
        var silenced = GunRenderDefinition.fitted(ID_SILENCER);
        b.mesh(ResourceManager.uzi)
                .skin(
                        GunRenderDefinition.fitted(ID_UZI_SATURN),
                        ResourceManager.uzi_saturnite_tex,
                        ResourceManager.uzi_tex)
                .move(THROW)
                .spin(mirrored ? Axis.ZN : Axis.ZP, ROLL)
                .hinge(0, -2, -4, Axis.XP, DRAW)
                .hinge(0, 0, -6, Axis.XP, TILT)
                .slideZ(KICK)
                .part(mirrored ? "GunMirror" : "Gun")
                .when(silenced, s -> s.part("Silencer"))
                .group(
                        stock ->
                                stock.hinge(
                                                0,
                                                0.3125,
                                                -5.75,
                                                Axis.XP,
                                                r -> 180 - r.get(FRONT_STOCK))
                                        .part("StockFront")
                                        .hinge(
                                                0,
                                                -0.3125,
                                                -3,
                                                Axis.XP,
                                                r -> -200 - r.get(BACK_STOCK))
                                        .part("StockBack"))
                .group(bolt -> bolt.slideZ(BOLT).part("Slide"))
                .group(
                        feed ->
                                feed.move(CLIP)
                                        .part("Magazine")
                                        .when(
                                                r -> r.get(ROUND) == 1,
                                                round -> round.part("Bullet")))
                .when(
                        silenced.negate(),
                        open ->
                                open.group(smoke -> muzzle(smoke).size(0.5).smoke(0.75))
                                        .group(
                                                flash ->
                                                        muzzle(flash)
                                                                .roll(90)
                                                                .flash(
                                                                        MuzzleFlash.NORMAL,
                                                                        75,
                                                                        7.5)));
    }

    private static GunRenderDefinition.Builder muzzle(GunRenderDefinition.Builder b) {
        return b.shift(0, 0.75, 8.5).turn(Axis.YP, 90);
    }
}
