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

public class CarbineItemRenderer extends AnimatedGunItemRenderer {

    public CarbineItemRenderer() {
        super(body());
    }

    private static GunRenderDefinition body() {
        var scoped = GunRenderDefinition.fitted(XWeaponModManager.ID_SCOPE);
        return GunRenderDefinition.on(Rig.RIG)
                .shift(0, 0, 0.875)
                .either(
                        scoped,
                        s -> s.aim(-1.5F * 0.8F, -1.5F * 0.8F, 0.875F * 0.8F, 0, -8 / 8D, 0.25),
                        s -> s.aim(-1.5F * 0.8F, -1.5F * 0.8F, 0.875F * 0.8F, 0, -6.25 / 8D, 0.25))
                .body()
                .when(
                        scoped.and(GunRenderDefinition.Reading::steadyAim).negate(),
                        gun ->
                                gun.size(0.5)
                                        .mesh(ResourceManager.carbine)
                                        .skin(ResourceManager.carbine_tex)
                                        .hinge(0, -1, -2, Axis.XP, Rig.DRAW)
                                        .hinge(0, 0, -2, Axis.XP, Rig.TILT)
                                        .move(Rig.STAB)
                                        .slideZ(Rig.KICK)
                                        .part("Gun")
                                        .group(slide -> slide.slideZ(Rig.CYCLE).part("Slide"))
                                        .group(
                                                mag ->
                                                        mag.move(Rig.CLIP)
                                                                .part("Magazine")
                                                                .move(Rig.LOAD)
                                                                .when(
                                                                        r -> r.get(Rig.ROUND) != 1,
                                                                        round ->
                                                                                round.part(
                                                                                        "Bullet")))
                                        .either(
                                                scoped,
                                                optic ->
                                                        optic.skin(
                                                                        ResourceManager
                                                                                .carbine_scope_tex)
                                                                .part("Scope"),
                                                irons -> irons.part("IronSight"))
                                        .when(
                                                GunRenderDefinition.fitted(
                                                        XWeaponModManager.ID_CARBINE_BAYONET),
                                                blade ->
                                                        blade.skin(
                                                                        ResourceManager
                                                                                .carbine_bayonet_tex)
                                                                .part("Bayonet"))
                                        .group(
                                                smoke ->
                                                        smoke.shift(0, 1, 8)
                                                                .turn(Axis.YP, 90)
                                                                .smoke(0.25))
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
        static final GunAnimationChannels.Scalar KICK = RIG.z("RECOIL");
        static final GunAnimationChannels.Scalar CYCLE = RIG.z("SLIDE");
        static final GunAnimationChannels.Vec3 CLIP = RIG.vec3("MAG");
        static final GunAnimationChannels.Scalar TILT = RIG.x("LIFT");
        static final GunAnimationChannels.Scalar ROUND = RIG.x("BULLET");
        static final GunAnimationChannels.Vec3 LOAD = RIG.vec3("REL");
        static final GunAnimationChannels.Vec3 STAB = RIG.vec3("STAB");

        static {
            RIG.carries("NULL");
        }
    }
}
