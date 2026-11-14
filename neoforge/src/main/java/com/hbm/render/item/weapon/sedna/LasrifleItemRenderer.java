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

public class LasrifleItemRenderer extends AnimatedGunItemRenderer {

    public LasrifleItemRenderer() {
        super(body());
    }

    private static GunRenderDefinition body() {
        var scoped = GunRenderDefinition.fitted(XWeaponModManager.ID_LAS_AUTO).negate();
        var shotgun = GunRenderDefinition.fitted(XWeaponModManager.ID_LAS_SHOTGUN);
        return GunRenderDefinition.on(Rig.RIG)
                .shift(0, 0, 0.875)
                .either(
                        scoped,
                        s -> s.aim(-1.5F * 0.8F, -1.5F * 0.8F, 2.5F * 0.8F, 0, -7.375 / 8D, 0.75),
                        s -> s.aim(-1.5F * 0.8F, -1.5F * 0.8F, 2.5F * 0.8F, 0, -5.25 / 8D, 1))
                .body()
                .when(
                        scoped.and(GunRenderDefinition.Reading::steadyAim).negate(),
                        gun ->
                                gun.size(0.3125)
                                        .mesh(ResourceManager.lasrifle)
                                        .skin(ResourceManager.lasrifle_tex)
                                        .hinge(0, -1, -6, Axis.XP, Rig.DRAW)
                                        .slideZ(Rig.KICK)
                                        .part("Gun")
                                        .part("Stock")
                                        .when(scoped, optic -> optic.part("Scope"))
                                        .group(
                                                lever ->
                                                        lever.hinge(
                                                                        0, -0.375, 2.375, Axis.XP,
                                                                        Rig.LEVER)
                                                                .part("Lever"))
                                        .group(cell -> cell.move(Rig.CELL).part("Battery"))
                                        .when(shotgun.negate(), rifle -> rifle.part("Barrel"))
                                        .group(
                                                mods ->
                                                        mods.mesh(ResourceManager.lasrifle_mods)
                                                                .skin(
                                                                        ResourceManager
                                                                                .lasrifle_mods_tex)
                                                                .when(
                                                                        shotgun,
                                                                        s ->
                                                                                s.part(
                                                                                        "BarrelShotgun"))
                                                                .when(
                                                                        GunRenderDefinition.fitted(
                                                                                XWeaponModManager
                                                                                        .ID_LAS_CAPACITOR),
                                                                        c -> c.part("UnderBarrel")))
                                        .group(
                                                flash ->
                                                        flash.shift(0, 1.5, 12)
                                                                .turn(Axis.YP, 90)
                                                                .flash(
                                                                        MuzzleFlash.LASER,
                                                                        150,
                                                                        1.5,
                                                                        0xff0000)
                                                                .shift(0, 0, -0.25)
                                                                .flash(
                                                                        MuzzleFlash.LASER,
                                                                        150,
                                                                        0.75,
                                                                        0xff8000)))
                .build();
    }

    @Override
    public float getViewFOV(ItemStack stack, float fov) {
        float aimingProgress = GunClientState.getHandAim();
        return fov
                * (1
                        - aimingProgress
                                * (!XWeaponModManager.hasUpgrade(
                                                stack, 0, XWeaponModManager.ID_LAS_AUTO)
                                        ? 0.75F
                                        : 0.66F));
    }

    private static final class Rig {
        static final GunAnimationChannels RIG = new GunAnimationChannels();
        static final GunAnimationChannels.Scalar DRAW = RIG.x("EQUIP");
        static final GunAnimationChannels.Scalar KICK = RIG.z("RECOIL");
        static final GunAnimationChannels.Scalar LEVER = RIG.x("LEVER");
        static final GunAnimationChannels.Vec3 CELL = RIG.vec3("MAG");
    }
}
