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
import com.hbm.items.weapon.sedna.GunBaseNTItem;
import com.hbm.main.ResourceManager;
import com.mojang.math.Axis;
import net.minecraft.world.item.ItemStack;

public class Stg77ItemRenderer extends AnimatedGunItemRenderer {

    public Stg77ItemRenderer() {
        super(
                GunRenderDefinition.on(Rig.RIG)
                        .shift(0, 0, 0.875)
                        .aim(-1.5F * 0.8F, -1F * 0.8F, 2.5F * 0.8F, 0, -5.75 / 8D, 2)
                        .body()
                        .when(r -> !r.steadyAim(), Stg77ItemRenderer::gun)
                        .build());
        Rig.RIG.requireParts("stg77", ResourceManager.stg77_anim);
    }

    private static void gun(GunRenderDefinition.Builder b) {
        b.size(0.5)
                .mesh(ResourceManager.stg77)
                .skin(ResourceManager.stg77_tex)
                .hinge(0, -1, -4, Axis.XP, Rig.DRAW)
                .hinge(0, 0, -4, Axis.XP, Rig.TILT)
                .slideZ(Rig.KICK)
                .group(
                        action ->
                                action.spin(Axis.ZP, r -> r.z(Rig.INSPECT_TURN))
                                        .spin(Axis.XP, r -> r.x(Rig.INSPECT_TURN))
                                        .apply(Rig.GUN)
                                        .part("Gun")
                                        .group(mag -> mag.apply(Rig.MAGAZINE).part("Magazine"))
                                        .group(
                                                lever ->
                                                        lever.spin(Axis.ZP, Rig.INSPECT_LEVER)
                                                                .apply(Rig.LEVER)
                                                                .part("Lever"))
                                        .group(
                                                bolt ->
                                                        bolt.slideZ(Rig.CYCLE)
                                                                .group(
                                                                        breech ->
                                                                                breech.apply(
                                                                                                Rig
                                                                                                        .BREECH)
                                                                                        .part(
                                                                                                "Breech"))
                                                                .hinge(
                                                                        0.125, 0, 0, Axis.ZP,
                                                                        Rig.LATCH)
                                                                .apply(Rig.HANDLE)
                                                                .part("Handle"))
                                        .group(
                                                safety ->
                                                        safety.slideX(Rig.SAFE)
                                                                .apply(Rig.SAFETY)
                                                                .part("Safety")))
                .group(
                        barrel ->
                                barrel.move(Rig.INSPECT_SHIFT)
                                        .spin(Axis.XP, r -> r.x(Rig.INSPECT_BARREL))
                                        .spin(Axis.ZP, r -> r.z(Rig.INSPECT_BARREL))
                                        .apply(Rig.GUN)
                                        .apply(Rig.BARREL)
                                        .part("Barrel"))
                .group(smoke -> smoke.shift(0, 0, 8).turn(Axis.YP, 90).size(0.75).smoke(0.5))
                .group(
                        flash ->
                                flash.shift(0, 0, 7.5)
                                        .turn(Axis.YP, 90)
                                        .size(0.25)
                                        .spin(Axis.XP, r -> -5 + r.shotRandom() * 10)
                                        .flash(MuzzleFlash.GAP, 0, 0));
    }

    @Override
    protected float aimZoom(ItemStack stack) {
        return 0.66F;
    }

    @Override
    protected float getBaseFOV(ItemStack stack) {
        float aimingProgress = GunClientState.getHandAim();
        return 70F - aimingProgress * 65;
    }

    @Override
    protected float getTurnMagnitude(ItemStack stack) {
        return GunBaseNTItem.getIsAiming(stack) ? 0.5F : -0.25F;
    }

    private static final class Rig {
        static final GunAnimationChannels RIG = new GunAnimationChannels();
        static final GunAnimationChannels.Scalar DRAW = RIG.x("EQUIP");
        static final GunAnimationChannels.Scalar TILT = RIG.x("LIFT");
        static final GunAnimationChannels.Scalar KICK = RIG.z("RECOIL");
        static final GunAnimationChannels.Scalar CYCLE = RIG.z("BOLT");
        static final GunAnimationChannels.Scalar LATCH = RIG.z("HANDLE");
        static final GunAnimationChannels.Scalar SAFE = RIG.x("SAFETY");
        static final GunAnimationChannels.Vec3 INSPECT_TURN = RIG.vec3("INSPECT_GUN");
        static final GunAnimationChannels.Vec3 INSPECT_BARREL = RIG.vec3("INSPECT_BARREL");
        static final GunAnimationChannels.Vec3 INSPECT_SHIFT = RIG.vec3("INSPECT_MOVE");
        static final GunAnimationChannels.Scalar INSPECT_LEVER = RIG.z("INSPECT_LEVER");
        static final GunAnimationChannels.Part GUN = RIG.part("Gun");
        static final GunAnimationChannels.Part MAGAZINE = RIG.part("Magazine");
        static final GunAnimationChannels.Part LEVER = RIG.part("Lever");
        static final GunAnimationChannels.Part BREECH = RIG.part("Breech");
        static final GunAnimationChannels.Part HANDLE = RIG.part("Handle");
        static final GunAnimationChannels.Part SAFETY = RIG.part("Safety");
        static final GunAnimationChannels.Part BARREL = RIG.part("Barrel");
    }
}
