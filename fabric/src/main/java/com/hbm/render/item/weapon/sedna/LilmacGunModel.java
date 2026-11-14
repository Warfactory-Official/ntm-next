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
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;

final class LilmacGunModel {

    static final GunAnimationChannels RIG = new GunAnimationChannels();
    static final GunAnimationChannels.Scalar FLICK = RIG.x("ROTATE");
    static final GunAnimationChannels.Scalar TWIRL = RIG.x("SPIN");
    private static final GunAnimationChannels.Scalar KICK = RIG.z("RECOIL");
    private static final GunAnimationChannels.Scalar OPEN_LIFT = RIG.x("RELOAD_LIFT");
    private static final GunAnimationChannels.Scalar OPEN_JOLT = RIG.x("RELOAD_JOLT");
    private static final GunAnimationChannels.Scalar OPEN_TILT = RIG.x("RELAOD_TILT");
    private static final GunAnimationChannels.Scalar SWING_OUT = RIG.x("RELOAD_CYLINDER");
    private static final GunAnimationChannels.Vec3 LOADER = RIG.vec3("RELOAD_BULLETS");
    private static final GunAnimationChannels.Scalar CYLINDER = RIG.z("DRUM");
    private static final GunAnimationChannels.Scalar LOADER_SPENT = RIG.x("RELOAD_BULLETS_CON");
    private static final GunAnimationChannels.Scalar COCK = RIG.z("HAMMER");

    private LilmacGunModel() {}

    static void gun(
            GunRenderDefinition.Builder b,
            Identifier sheet,
            Consumer<GunRenderDefinition.Builder> fittings) {
        b.mesh(ResourceManager.lilmac)
                .skin(sheet)
                .aim(
                        _ -> 0,
                        _ -> 0,
                        KICK,
                        r -> -r.get(KICK),
                        _ -> 0,
                        _ -> 0)
                .spin(Axis.ZP, r -> r.get(KICK) * 10)
                .group(
                        smoke ->
                                smoke.shift(-9, 2.5, 0)
                                        .spin(Axis.ZP, r -> r.get(KICK) * -10)
                                        .smoke(0.5))
                .spin(Axis.ZP, OPEN_LIFT)
                .slideX(OPEN_JOLT)
                .spin(Axis.XP, OPEN_TILT)
                .part("Gun")
                .group(
                        swing ->
                                swing.spin(Axis.XP, SWING_OUT)
                                        .part("Pivot")
                                        .hinge(0, 1.75, 0, Axis.XN, r -> r.get(CYLINDER) * 60)
                                        .part("Cylinder")
                                        .move(LOADER)
                                        .when(
                                                r -> r.get(LOADER_SPENT) != 1,
                                                rounds -> rounds.part("Bullets"))
                                        .part("Casings"))
                .group(
                        hammer ->
                                hammer.hinge(4, 1.25, 0, Axis.ZP, r -> -30 + 30 * r.get(COCK))
                                        .part("Hammer"));
        fittings.accept(b);
        b.group(flash -> flash.shift(0.125, 2.5, 0).flash(MuzzleFlash.GAP, 0, 0));
    }
}
