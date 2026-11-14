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

import com.hbm.client.render.WeaponRenderTypes;
import com.hbm.items.weapon.sedna.factory.XFactoryCatapult;
import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.function.Predicate;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;

public class FatManItemRenderer extends AnimatedGunItemRenderer {
    public FatManItemRenderer() {
        super(body());
    }

    private static GunRenderDefinition body() {
        Predicate<GunRenderDefinition.Reading> loaded =
                r -> GunRenderDefinition.magazine(r).getAmount(r.stack(), null) > 0;
        return GunRenderDefinition.on(Rig.RIG)
                .shift(0, 0, 0.875)
                .aim(-1.5F * 0.8F, -1.25F * 0.8F, 0.5F * 0.8F, -1F * 0.8F, -1.25F * 0.8F, 0F * 0.8F)
                .body()
                .size(0.5)
                .mesh(ResourceManager.fatman)
                .skin(ResourceManager.fatman_tex)
                .hinge(0, 1, -2, Axis.XP, Rig.DRAW)
                .part("Launcher")
                .group(
                        handle ->
                                handle.slideZ(Rig.HANDLE)
                                        .part("Handle")
                                        .hinge(0.4375, -0.875, 0, Axis.ZP, Rig.GAUGE)
                                        .part("Gauge"))
                .group(lid -> lid.hinge(0.25, 0.125, 0, Axis.ZP, Rig.LID).part("Lid"))
                .group(
                        piston ->
                                piston.slideZ(Rig.PISTON)
                                        .when(
                                                loaded.negate().and(r -> r.get(Rig.PISTON) == 0),
                                                spent -> spent.shift(0, 0, 3))
                                        .part("Piston"))
                .when(
                        loaded.or(
                                r ->
                                        r.x(Rig.NUKE) != 0
                                                || r.y(Rig.NUKE) != 0
                                                || r.z(Rig.NUKE) != 0),
                        shown ->
                                shown.group(
                                        nuke ->
                                                nuke.move(Rig.NUKE)
                                                        .custom(
                                                                d ->
                                                                        renderNuke(
                                                                                d.collector(),
                                                                                d.pose(),
                                                                                d.light(),
                                                                                GunRenderDefinition
                                                                                        .magazine(d)
                                                                                        .getType(
                                                                                                d
                                                                                                        .stack(),
                                                                                                null)),
                                                                (r, pose, target) -> {
                                                                    if (GunRenderDefinition
                                                                                    .magazine(r)
                                                                                    .getType(
                                                                                            r
                                                                                                    .stack(),
                                                                                            null)
                                                                            == XFactoryCatapult
                                                                                    .nuke_balefire) {
                                                                        target.part(
                                                                                pose,
                                                                                ResourceManager
                                                                                        .fatman,
                                                                                Parts.MINI_NUKE,
                                                                                ResourceManager
                                                                                        .fatman_balefire_tex);
                                                                        target.balefireGlint(
                                                                                pose,
                                                                                ResourceManager
                                                                                        .fatman,
                                                                                Parts.MINI_NUKE);
                                                                    } else {
                                                                        target.part(
                                                                                pose,
                                                                                ResourceManager
                                                                                        .fatman,
                                                                                Parts.MINI_NUKE,
                                                                                ResourceManager
                                                                                        .fatman_mininuke_tex);
                                                                    }
                                                                })))
                .build();
    }

    public static void renderNuke(
            SubmitNodeCollector collector, PoseStack pose, int light, Object type) {
        if (type == XFactoryCatapult.nuke_balefire) {
            renderBalefire(collector, pose, light);
        } else {
            submitPart(
                    collector,
                    pose,
                    RenderTypes.entityCutout(ResourceManager.fatman_mininuke_tex),
                    ResourceManager.fatman,
                    Parts.MINI_NUKE,
                    light);
        }
    }

    public static void renderBalefire(SubmitNodeCollector collector, PoseStack pose, int light) {
        submitPart(
                collector,
                pose,
                RenderTypes.entityCutout(ResourceManager.fatman_balefire_tex),
                ResourceManager.fatman,
                Parts.MINI_NUKE,
                light);

        for (int k = 0; k < 3; k++) {
            submitPart(
                    collector,
                    pose,
                    WeaponRenderTypes.balefireGlint(ResourceManager.glint_bf_tex, k),
                    ResourceManager.fatman,
                    Parts.MINI_NUKE,
                    light,
                    WeaponRenderTypes.BALEFIRE_GLINT_TINT);
        }
    }

    @Override
    protected float idleTurnMagnitude() {
        return -0.5F;
    }

    private static final class Rig {
        static final GunAnimationChannels RIG = new GunAnimationChannels();
        static final GunAnimationChannels.Scalar DRAW = RIG.x("EQUIP");
        static final GunAnimationChannels.Scalar LID = RIG.z("LID");
        static final GunAnimationChannels.Vec3 NUKE = RIG.vec3("NUKE");
        static final GunAnimationChannels.Scalar PISTON = RIG.z("PISTON");
        static final GunAnimationChannels.Scalar HANDLE = RIG.z("HANDLE");
        static final GunAnimationChannels.Scalar GAUGE = RIG.z("GAUGE");
    }

    private static final class Parts {
        static final int MINI_NUKE = ResourceManager.fatman.partId("MiniNuke");
    }
}
