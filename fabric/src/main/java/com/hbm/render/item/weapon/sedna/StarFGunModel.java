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

import static com.hbm.items.weapon.sedna.mods.XWeaponModManager.ID_SILENCER;

final class StarFGunModel {

    static final GunAnimationChannels RIG = new GunAnimationChannels();
    private static final GunAnimationChannels.Scalar DRAW = RIG.x("EQUIP");
    private static final GunAnimationChannels.Scalar ROLL = RIG.z("TURN");
    private static final GunAnimationChannels.Scalar PITCH = RIG.x("TILT");
    private static final GunAnimationChannels.Scalar KICK = RIG.z("RECOIL");
    private static final GunAnimationChannels.Scalar COCK = RIG.x("HAMMER");
    private static final GunAnimationChannels.Scalar BOLT = RIG.z("SLIDE");
    private static final GunAnimationChannels.Vec3 CLIP = RIG.vec3("MAG");
    private static final GunAnimationChannels.Vec3 ROUND = RIG.vec3("BULLET");

    private StarFGunModel() {}

    static void gun(GunRenderDefinition.Builder b, Identifier sheet, int side, boolean shrunk) {
        b.mesh(ResourceManager.star_f)
                .skin(sheet)
                .hinge(0, -2, -8, Axis.XP, DRAW)
                .about(0, 1, -3, a -> a.spin(Axis.ZP, r -> r.get(ROLL) * side).spin(Axis.XP, PITCH))
                .slideZ(KICK)
                .part("Gun")
                .group(
                        hammer ->
                                hammer.hinge(0, 1.75, -4.25, Axis.XP, r -> 60 * (r.get(COCK) - 1))
                                        .part("Hammer"))
                .group(bolt -> bolt.slideZ(r -> r.get(BOLT) * 2.3125).part("Slide"))
                .group(feed -> feed.move(CLIP).part("Mag").move(ROUND).part("Bullet"))
                .either(
                        GunRenderDefinition.fitted(ID_SILENCER),
                        silenced ->
                                silenced.group(
                                        can ->
                                                can.shift(0, 2.375, -0.25)
                                                        .mesh(ResourceManager.uzi)
                                                        .skin(ResourceManager.uzi_tex)
                                                        .part("Silencer")),
                        open ->
                                open.group(
                                                smoke ->
                                                        smoke.shift(0, 3, 6.125)
                                                                .turn(Axis.YP, 90)
                                                                .size(0.5)
                                                                .smoke(0.75))
                                        .group(
                                                flash -> {
                                                    flash.shift(0, 3, 6.125);
                                                    if (shrunk) flash.size(0.75);
                                                    flash.turn(Axis.YP, 90)
                                                            .roll(90)
                                                            .flash(MuzzleFlash.NORMAL, 75, 7.5);
                                                }));
    }
}
