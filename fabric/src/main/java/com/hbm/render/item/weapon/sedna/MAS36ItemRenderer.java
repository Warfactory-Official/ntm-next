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

package com.hbm.render.item.weapon.sedna;

import com.hbm.client.weapon.GunClientState;
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import com.hbm.main.ResourceManager;
import com.mojang.math.Axis;
import net.minecraft.world.item.ItemStack;

public class MAS36ItemRenderer extends AnimatedGunItemRenderer {

    public MAS36ItemRenderer() {
        super(body());
    }

    private static GunRenderDefinition body() {
        var scoped = GunRenderDefinition.fitted(XWeaponModManager.ID_SCOPE);
        int rounds = ResourceManager.mas36.partId("Bullets");
        return GunRenderDefinition.on(Rig.RIG)
                .shift(0, 0, 0.875)
                .either(
                        scoped,
                        s ->
                                s.aim(
                                        -1.5F * 0.8F,
                                        -1.25F * 0.8F,
                                        1.75F * 0.8F,
                                        -0.2,
                                        -5.875 / 8D,
                                        1.125),
                        s ->
                                s.aim(
                                        -1.5F * 0.8F,
                                        -1.25F * 0.8F,
                                        1.75F * 0.8F,
                                        0,
                                        -4.6825 / 8D,
                                        0.75))
                .body()
                .when(
                        scoped.and(GunRenderDefinition.Reading::steadyAim).negate(),
                        gun ->
                                gun.size(0.375)
                                        .mesh(ResourceManager.mas36)
                                        .skin(ResourceManager.mas36_tex)
                                        .about(
                                                0,
                                                -3,
                                                -3,
                                                a ->
                                                        a.spin(Axis.XP, Rig.DRAW)
                                                                .spin(Axis.XP, Rig.TILT))
                                        .move(Rig.STAB)
                                        .slideZ(Rig.KICK)
                                        .part("Gun")
                                        .when(
                                                GunRenderDefinition.fitted(
                                                        XWeaponModManager.ID_MAS_BAYONET),
                                                blade -> blade.part("Bayonet"))
                                        .group(
                                                stock ->
                                                        stock.hinge(
                                                                        0, 0.3125, -2.125, Axis.XP,
                                                                        Rig.STOCK)
                                                                .part("Stock"))
                                        .group(
                                                bolt ->
                                                        bolt.hinge(
                                                                        0,
                                                                        0.0625 * 18.5,
                                                                        0,
                                                                        Axis.ZP,
                                                                        Rig.BOLT_TURN)
                                                                .slideZ(Rig.BOLT_PULL)
                                                                .part("Bolt"))
                                        .group(round -> round.move(Rig.ROUND).part("Bullet"))
                                        .when(scoped, optic -> optic.part("Scope"))
                                        .when(
                                                r -> r.get(Rig.CLIP_SHOWN) != 0,
                                                clip ->
                                                        clip.group(
                                                                        strip ->
                                                                                strip.move(Rig.CLIP)
                                                                                        .part(
                                                                                                "Clip"))
                                                                .group(
                                                                        loaded ->
                                                                                loaded.move(
                                                                                                Rig
                                                                                                        .CLIP_ROUNDS)
                                                                                        .either(
                                                                                                r ->
                                                                                                        r
                                                                                                                        .x(
                                                                                                                                Rig
                                                                                                                                        .CLIP_ROUNDS)
                                                                                                                == 0,
                                                                                                sinking ->
                                                                                                        sinking
                                                                                                                .custom(
                                                                                                                        d -> {
                                                                                                                            float
                                                                                                                                    plane =
                                                                                                                                            (float)
                                                                                                                                                    (0.5
                                                                                                                                                            - d
                                                                                                                                                                    .y(
                                                                                                                                                                            Rig
                                                                                                                                                                                    .CLIP_ROUNDS));
                                                                                                                            int
                                                                                                                                    light =
                                                                                                                                            d
                                                                                                                                                    .light();
                                                                                                                            d.collector()
                                                                                                                                    .submitCustomGeometry(
                                                                                                                                            d
                                                                                                                                                    .pose(),
                                                                                                                                            d
                                                                                                                                                    .skin(),
                                                                                                                                            (p,
                                                                                                                                                    buffer) ->
                                                                                                                                                    ResourceManager
                                                                                                                                                            .mas36
                                                                                                                                                            .renderPartClipped(
                                                                                                                                                                    p,
                                                                                                                                                                    buffer,
                                                                                                                                                                    light,
                                                                                                                                                                    -1,
                                                                                                                                                                    rounds,
                                                                                                                                                                    0F,
                                                                                                                                                                    1F,
                                                                                                                                                                    0F,
                                                                                                                                                                    plane));
                                                                                                                        },
                                                                                                                        null),
                                                                                                whole ->
                                                                                                        whole
                                                                                                                .part(
                                                                                                                        "Bullets"))))
                                        .group(
                                                smoke ->
                                                        smoke.shift(0, 1.125, 8)
                                                                .turn(Axis.YP, 90)
                                                                .size(0.25)
                                                                .smoke(1))
                                        .group(
                                                flash ->
                                                        flash.shift(0, 1, 8)
                                                                .turn(Axis.YP, 90)
                                                                .roll(90)
                                                                .size(0.5)
                                                                .flash(
                                                                        MuzzleFlash.NORMAL,
                                                                        75,
                                                                        7.5)))
                .build();
    }

    @Override
    protected float idleTurnMagnitude() {
        return -0.5F;
    }

    @Override
    public float getViewFOV(ItemStack stack, float fov) {
        float aimingProgress = GunClientState.getHandAim();
        return fov
                * (1
                        - aimingProgress
                                * (XWeaponModManager.hasUpgrade(
                                                stack, 0, XWeaponModManager.ID_SCOPE)
                                        ? 0.66F
                                        : 0.33F));
    }

    private static final class Rig {
        static final GunAnimationChannels RIG = new GunAnimationChannels();
        static final GunAnimationChannels.Scalar DRAW = RIG.x("EQUIP");
        static final GunAnimationChannels.Scalar TILT = RIG.x("LIFT");
        static final GunAnimationChannels.Scalar STOCK = RIG.x("STOCK");
        static final GunAnimationChannels.Scalar KICK = RIG.z("RECOIL");
        static final GunAnimationChannels.Scalar BOLT_TURN = RIG.z("BOLT_TURN");
        static final GunAnimationChannels.Scalar BOLT_PULL = RIG.z("BOLT_PULL");
        static final GunAnimationChannels.Vec3 ROUND = RIG.vec3("BULLET");
        static final GunAnimationChannels.Scalar CLIP_SHOWN = RIG.x("SHOW_CLIP");
        static final GunAnimationChannels.Vec3 CLIP = RIG.vec3("CLIP");
        static final GunAnimationChannels.Vec3 CLIP_ROUNDS = RIG.vec3("BULLETS");
        static final GunAnimationChannels.Vec3 STAB = RIG.vec3("STAB");
    }
}
