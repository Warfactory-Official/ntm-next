/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
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
import com.hbm.items.weapon.sedna.GunBaseNTItem;
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import com.hbm.main.ResourceManager;
import com.hbm.registration.RegistryHandle;
import com.mojang.math.Axis;
import java.util.function.Predicate;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public class G3ItemRenderer extends AnimatedGunItemRenderer {

    public G3ItemRenderer(Identifier texture) {
        super(body(texture));
    }

    private static Predicate<GunRenderDefinition.Reading> zebraOr(
            RegistryHandle<XWeaponModManager.Declaration> mod) {
        return r ->
                r.stack().getItem() == ModItems.GUN_G3_ZEBRA.get()
                        || XWeaponModManager.hasUpgrade(r.stack(), 0, mod);
    }

    private static GunRenderDefinition body(Identifier texture) {
        var scoped = zebraOr(XWeaponModManager.ID_SCOPE);
        var silenced = zebraOr(XWeaponModManager.ID_SILENCER);
        return GunRenderDefinition.on(Rig.RIG)
                .shift(0, 0, 0.875)
                .either(
                        scoped,
                        s ->
                                s.aim(
                                        -1.25F * 0.8F,
                                        -1F * 0.8F,
                                        2.75F * 0.8F,
                                        0,
                                        -5.53125 / 8D,
                                        1.46875),
                        s -> s.aim(-1.25F * 0.8F, -1F * 0.8F, 2.75F * 0.8F, 0, -3.5625 / 8D, 1.75))
                .body()
                .when(
                        scoped.and(GunRenderDefinition.Reading::steadyAim).negate(),
                        gun ->
                                gun.mesh(ResourceManager.g3)
                                        .skin(
                                                r ->
                                                        XWeaponModManager.hasUpgrade(
                                                                        r.stack(),
                                                                        0,
                                                                        XWeaponModManager
                                                                                .ID_FURNITURE_GREEN)
                                                                ? ResourceManager
                                                                        .g3_polymer_green_tex
                                                                : XWeaponModManager.hasUpgrade(
                                                                                r.stack(),
                                                                                0,
                                                                                XWeaponModManager
                                                                                        .ID_FURNITURE_BLACK)
                                                                        ? ResourceManager
                                                                                .g3_polymer_black_tex
                                                                        : texture)
                                        .size(0.375)
                                        .hinge(0, -2, -6, Axis.XP, Rig.DRAW)
                                        .hinge(0, 0, -4, Axis.XP, Rig.TILT)
                                        .slideZ(Rig.KICK)
                                        .part("Rifle")
                                        .when(
                                                GunRenderDefinition.fitted(
                                                                XWeaponModManager.ID_NO_STOCK)
                                                        .negate(),
                                                stock -> stock.part("Stock"))
                                        .when(silenced.negate(), hider -> hider.part("Flash_Hider"))
                                        .part("Trigger")
                                        .group(
                                                mag ->
                                                        mag.move(Rig.CLIP)
                                                                .about(
                                                                        0,
                                                                        -1.75,
                                                                        -0.5,
                                                                        a ->
                                                                                a.spin(
                                                                                                Axis
                                                                                                        .ZP,
                                                                                                r ->
                                                                                                        r
                                                                                                                .z(
                                                                                                                        Rig
                                                                                                                                .CLIP_TURN))
                                                                                        .spin(
                                                                                                Axis
                                                                                                        .YP,
                                                                                                r ->
                                                                                                        r
                                                                                                                .y(
                                                                                                                        Rig
                                                                                                                                .CLIP_TURN)))
                                                                .part("Magazine")
                                                                .when(
                                                                        r -> r.get(Rig.ROUND) == 0,
                                                                        round ->
                                                                                round.part(
                                                                                        "Bullet")))
                                        .group(
                                                bolt ->
                                                        bolt.slideZ(Rig.CYCLE)
                                                                .part("Guide_And_Bolt"))
                                        .group(
                                                plug ->
                                                        plug.shift(
                                                                        _ ->
                                                                                0,
                                                                        _ ->
                                                                                0.625,
                                                                        Rig.PLUG)
                                                                .spin(Axis.ZP, r -> r.z(Rig.HANDLE))
                                                                .shift(0, -0.625, 0)
                                                                .part("Plug")
                                                                .about(
                                                                        0,
                                                                        0.625,
                                                                        5.25,
                                                                        a ->
                                                                                a.turn(
                                                                                                Axis
                                                                                                        .ZP,
                                                                                                22.5)
                                                                                        .spin(
                                                                                                Axis
                                                                                                        .YP,
                                                                                                r ->
                                                                                                        r
                                                                                                                .y(
                                                                                                                        Rig
                                                                                                                                .HANDLE))
                                                                                        .turn(
                                                                                                Axis
                                                                                                        .ZN,
                                                                                                22.5))
                                                                .part("Handle"))
                                        .group(
                                                selector ->
                                                        selector.hinge(
                                                                        0,
                                                                        -0.875,
                                                                        -3.5,
                                                                        Axis.XN,
                                                                        r ->
                                                                                30
                                                                                        * (1
                                                                                                - GunBaseNTItem
                                                                                                        .getMode(
                                                                                                                r
                                                                                                                        .stack(),
                                                                                                                0)))
                                                                .part("Selector"))
                                        .group(
                                                fitted ->
                                                        fitted.skin(ResourceManager.g3_attachments)
                                                                .when(
                                                                        silenced,
                                                                        can -> can.part("Silencer"))
                                                                .when(
                                                                        scoped,
                                                                        optic ->
                                                                                optic.part(
                                                                                        "Scope")))
                                        .when(
                                                silenced.negate(),
                                                open ->
                                                        open.group(
                                                                        smoke ->
                                                                                smoke.shift(
                                                                                                0,
                                                                                                0,
                                                                                                13)
                                                                                        .turn(
                                                                                                Axis
                                                                                                        .YP,
                                                                                                90)
                                                                                        .size(0.75)
                                                                                        .smoke(0.5))
                                                                .group(
                                                                        flash ->
                                                                                flash.shift(
                                                                                                0,
                                                                                                0,
                                                                                                12)
                                                                                        .turn(
                                                                                                Axis
                                                                                                        .YP,
                                                                                                90)
                                                                                        .spin(
                                                                                                Axis
                                                                                                        .XP,
                                                                                                r ->
                                                                                                        -25
                                                                                                                + r
                                                                                                                                .shotRandom()
                                                                                                                        * 10)
                                                                                        .size(0.75)
                                                                                        .flash(
                                                                                                MuzzleFlash
                                                                                                        .NORMAL,
                                                                                                75,
                                                                                                10))))
                .build();
    }

    @Override
    public float getViewFOV(ItemStack stack, float fov) {
        float aimingProgress = GunClientState.getHandAim();
        return fov
                * (1
                        - aimingProgress
                                * ((stack.getItem() == ModItems.GUN_G3_ZEBRA.get()
                                                || XWeaponModManager.hasUpgrade(
                                                        stack, 0, XWeaponModManager.ID_SCOPE))
                                        ? 0.66F
                                        : 0.33F));
    }

    private static final class Rig {
        static final GunAnimationChannels RIG = new GunAnimationChannels();
        static final GunAnimationChannels.Scalar DRAW = RIG.x("EQUIP");
        static final GunAnimationChannels.Scalar TILT = RIG.x("LIFT");
        static final GunAnimationChannels.Scalar KICK = RIG.z("RECOIL");
        static final GunAnimationChannels.Vec3 CLIP = RIG.vec3("MAG");
        static final GunAnimationChannels.Vec3 CLIP_TURN = RIG.vec3("SPEEN");
        static final GunAnimationChannels.Scalar CYCLE = RIG.z("BOLT");
        static final GunAnimationChannels.Scalar PLUG = RIG.z("PLUG");
        static final GunAnimationChannels.Vec3 HANDLE = RIG.vec3("HANDLE");
        static final GunAnimationChannels.Scalar ROUND = RIG.x("BULLET");
    }
}
