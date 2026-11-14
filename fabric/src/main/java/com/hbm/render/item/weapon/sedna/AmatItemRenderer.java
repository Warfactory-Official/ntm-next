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
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import com.hbm.main.ResourceManager;
import com.mojang.math.Axis;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public class AmatItemRenderer extends AnimatedGunItemRenderer {

    public AmatItemRenderer(Identifier texture) {
        super(
                GunRenderDefinition.on(Rig.RIG)
                        .shift(0, 0, 0.875)
                        .aim(-1F * 0.8F, -1F * 0.8F, 3.25F * 0.8F, 0, -4.875 / 8D, 1.875)
                        .body()
                        .when(
                                r -> !r.steadyAim(),
                                gun ->
                                        gun.size(0.375)
                                                .mesh(ResourceManager.amat)
                                                .skin(texture)
                                                .slideZ(Rig.KICK)
                                                .about(
                                                        0,
                                                        -3,
                                                        -8,
                                                        a ->
                                                                a.spin(Axis.XP, Rig.DRAW)
                                                                        .spin(Axis.XP, Rig.TILT))
                                                .part("Gun")
                                                .group(
                                                        scope ->
                                                                scope.move(Rig.SCOPE_THROW)
                                                                        .hinge(
                                                                                0,
                                                                                1.5,
                                                                                -4.5,
                                                                                Axis.XP,
                                                                                Rig.SCOPE_SPIN)
                                                                        .part("Scope"))
                                                .group(
                                                        bolt ->
                                                                bolt.hinge(
                                                                                0,
                                                                                0.625,
                                                                                0,
                                                                                Axis.ZP,
                                                                                Rig.BOLT_TURN)
                                                                        .slideZ(Rig.BOLT_PULL)
                                                                        .part("Bolt"))
                                                .group(mag -> mag.move(Rig.CLIP).part("Magazine"))
                                                .group(
                                                        left ->
                                                                left.hinge(
                                                                                0.3125,
                                                                                -0.625,
                                                                                -1,
                                                                                Axis.ZP,
                                                                                r ->
                                                                                        folded(r)
                                                                                                ? r
                                                                                                        .y(
                                                                                                                Rig
                                                                                                                        .LEGS)
                                                                                                : 25)
                                                                        .part("BipodHingeLeft")
                                                                        .hinge(
                                                                                0.3125,
                                                                                -0.625,
                                                                                -1,
                                                                                Axis.XP,
                                                                                r ->
                                                                                        folded(r)
                                                                                                ? r
                                                                                                        .x(
                                                                                                                Rig
                                                                                                                        .LEGS)
                                                                                                : 80)
                                                                        .part("BipodLeft"))
                                                .group(
                                                        right ->
                                                                right.hinge(
                                                                                -0.3125,
                                                                                -0.625,
                                                                                -1,
                                                                                Axis.ZP,
                                                                                r ->
                                                                                        folded(r)
                                                                                                ? -r
                                                                                                        .y(
                                                                                                                Rig
                                                                                                                        .LEGS)
                                                                                                : -25)
                                                                        .part("BipodHingeRight")
                                                                        .hinge(
                                                                                -0.3125,
                                                                                -0.625,
                                                                                -1,
                                                                                Axis.XP,
                                                                                r ->
                                                                                        folded(r)
                                                                                                ? r
                                                                                                        .x(
                                                                                                                Rig
                                                                                                                        .LEGS)
                                                                                                : 80)
                                                                        .part("BipodRight"))
                                                .either(
                                                        r ->
                                                                r.stack().getItem()
                                                                                == ModItems
                                                                                        .GUN_AMAT_PENANCE
                                                                                        .get()
                                                                        || XWeaponModManager
                                                                                .hasUpgrade(
                                                                                        r.stack(),
                                                                                        0,
                                                                                        XWeaponModManager
                                                                                                .ID_SILENCER),
                                                        can ->
                                                                can.shift(0, 0.625, -4.3125)
                                                                        .size(1.25)
                                                                        .mesh(ResourceManager.g3)
                                                                        .skin(
                                                                                ResourceManager
                                                                                        .g3_attachments)
                                                                        .part("Silencer"),
                                                        open ->
                                                                open.part("MuzzleBrake")
                                                                        .group(
                                                                                smoke ->
                                                                                        smoke.shift(
                                                                                                        0,
                                                                                                        0.625,
                                                                                                        12)
                                                                                                .turn(
                                                                                                        Axis
                                                                                                                .YP,
                                                                                                        90)
                                                                                                .size(
                                                                                                        0.5)
                                                                                                .smoke(
                                                                                                        1))
                                                                        .group(
                                                                                flash ->
                                                                                        flash.shift(
                                                                                                        0,
                                                                                                        0.5,
                                                                                                        11)
                                                                                                .turn(
                                                                                                        Axis
                                                                                                                .YP,
                                                                                                        90)
                                                                                                .size(
                                                                                                        0.75)
                                                                                                .flash(
                                                                                                        MuzzleFlash
                                                                                                                .GAP,
                                                                                                        0,
                                                                                                        0))))
                        .build());
    }

    private static boolean folded(GunRenderDefinition.Reading r) {
        return r.carries(Rig.LEGS);
    }

    @Override
    protected float idleTurnMagnitude() {
        return -0.5F;
    }

    @Override
    public float getViewFOV(ItemStack stack, float fov) {
        float aimingProgress = GunClientState.getHandAim();
        return fov * (1 - aimingProgress * 0.8F);
    }

    private static final class Rig {
        static final GunAnimationChannels RIG = new GunAnimationChannels();
        static final GunAnimationChannels.Vec3 LEGS = RIG.vec3("BIPOD");
        static final GunAnimationChannels.Scalar DRAW = RIG.x("EQUIP");
        static final GunAnimationChannels.Scalar TILT = RIG.x("LIFT");
        static final GunAnimationChannels.Scalar KICK = RIG.z("RECOIL");
        static final GunAnimationChannels.Scalar BOLT_TURN = RIG.z("BOLT_TURN");
        static final GunAnimationChannels.Scalar BOLT_PULL = RIG.z("BOLT_PULL");
        static final GunAnimationChannels.Vec3 CLIP = RIG.vec3("MAG");
        static final GunAnimationChannels.Vec3 SCOPE_THROW = RIG.vec3("SCOPE_THROW");
        static final GunAnimationChannels.Scalar SCOPE_SPIN = RIG.x("SCOPE_SPIN");
    }
}
