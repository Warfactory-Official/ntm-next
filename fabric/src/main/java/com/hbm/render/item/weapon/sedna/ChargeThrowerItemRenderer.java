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
import com.hbm.items.weapon.sedna.factory.XFactoryTool;
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import com.hbm.main.ResourceManager;
import com.mojang.math.Axis;
import java.util.function.Predicate;
import net.minecraft.world.item.ItemStack;

public class ChargeThrowerItemRenderer extends AnimatedGunItemRenderer {

    public ChargeThrowerItemRenderer() {
        super(body());
    }

    private static GunRenderDefinition body() {
        var scoped = GunRenderDefinition.fitted(XWeaponModManager.ID_SCOPE);
        var sighted = scoped.and(GunRenderDefinition.Reading::steadyAim);
        return GunRenderDefinition.on(Rig.RIG)
                .shift(0, 0, 0.875)
                .either(
                        scoped,
                        s ->
                                s.aim(
                                        -1.5F * 0.8F,
                                        -1.25F * 0.8F,
                                        3.5F * 0.8F,
                                        -0.15625,
                                        -6.5 / 8D,
                                        1.6875),
                        s ->
                                s.aim(
                                        -1.5F * 0.8F,
                                        -1.25F * 0.8F,
                                        3.5F * 0.8F,
                                        -1.5F * 0.5F,
                                        -1.25F * 0.5F,
                                        3.5F * 0.5F))
                .body()
                .either(sighted, s -> s.size(3.5).shift(-0.5, -1.5, -4), s -> s.size(0.5))
                .mesh(ResourceManager.charge_thrower)
                .skin(ResourceManager.charge_thrower_tex)
                .hinge(0, 0, -7, Axis.XN, Rig.DRAW)
                .hinge(0, -7, 4, Axis.XP, Rig.RAISE)
                .move(Rig.KICK)
                .hinge(0, 0, -2, Axis.YP, Rig.YAW)
                .hinge(0, -1, 0, Axis.ZP, Rig.ROLL)
                .part("Gun")
                .when(scoped.and(sighted.negate()), optic -> optic.part("Scope"))
                .when(
                        r ->
                                GunRenderDefinition.magazine(r).getAmount(r.stack(), null) > 0
                                        || r.carries(Rig.LOAD),
                        ammo ->
                                ammo.move(Rig.LOAD)
                                        .spin(Axis.ZP, Rig.TWIST)
                                        .when(
                                                loaded(XFactoryTool.ct_hook),
                                                hook ->
                                                        hook.skin(
                                                                        ResourceManager
                                                                                .charge_thrower_hook_tex)
                                                                .part("Hook"))
                                        .when(
                                                loaded(XFactoryTool.ct_mortar),
                                                mortar ->
                                                        mortar.skin(
                                                                        ResourceManager
                                                                                .charge_thrower_mortar_tex)
                                                                .part("Mortar"))
                                        .when(
                                                loaded(XFactoryTool.ct_mortar_charge),
                                                charge ->
                                                        charge.skin(
                                                                        ResourceManager
                                                                                .charge_thrower_mortar_tex)
                                                                .part("Mortar")
                                                                .part("Oomph")))
                .build();
    }

    private static Predicate<GunRenderDefinition.Reading> loaded(Object type) {
        return r -> GunRenderDefinition.magazine(r).getType(r.stack(), null) == type;
    }

    @Override
    protected float getTurnMagnitude(ItemStack stack) {
        return GunBaseNTItem.getIsAiming(stack) ? 0F : -0.5F;
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
        static final GunAnimationChannels.Vec3 LOAD = RIG.vec3("AMMO");
        static final GunAnimationChannels.Scalar DRAW = RIG.x("EQUIP");
        static final GunAnimationChannels.Vec3 KICK = RIG.vec3("RECOIL");
        static final GunAnimationChannels.Scalar RAISE = RIG.x("RAISE");
        static final GunAnimationChannels.Scalar TWIST = RIG.z("TWIST");
        static final GunAnimationChannels.Scalar YAW = RIG.y("TURN");
        static final GunAnimationChannels.Scalar ROLL = RIG.z("ROLL");
    }
}
