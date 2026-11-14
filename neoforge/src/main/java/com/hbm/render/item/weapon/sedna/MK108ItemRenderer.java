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

import com.hbm.main.ResourceManager;
import com.hbm.render.item.weapon.sedna.AnimatedGunItemRenderer;
import com.hbm.render.item.weapon.sedna.GunAnimationChannels;
import com.hbm.render.item.weapon.sedna.GunRenderDefinition;
import com.hbm.render.item.weapon.sedna.MuzzleFlash;
import com.hbm.util.NtmMath;
import com.mojang.math.Axis;
import org.joml.Matrix4f;
import org.joml.Quaternionfc;

public class MK108ItemRenderer extends AnimatedGunItemRenderer {
    private static final double[] CURLED = {0, 0, -5, 0, -5, 60, 45, -10, 0};
    private static final double[] SLACK = {0, -30, -60, -45, -45, 0, 0, 0, 0};

    public MK108ItemRenderer() {
        super(body());
    }

    private static GunRenderDefinition body() {
        var b =
                GunRenderDefinition.on(Rig.RIG)
                        .shift(0, 0, 0.875)
                        .aim(-1F * 0.8F, -1.5F * 0.8F, 2.5F * 0.8F, -0.75F, -0.75F, 1.5F)
                        .body()
                        .size(0.375)
                        .mesh(ResourceManager.mk108)
                        .skin(ResourceManager.mk108_tex)
                        .when(
                                r -> r.carries(Rig.THROWN[0]),
                                toss -> {
                                    for (int i = 0; i < 3; i++) {
                                        var across = Rig.THROWN[i];
                                        var up = Rig.RISE[i];
                                        var spin = Rig.TUMBLE[i];
                                        toss.when(
                                                r -> !(r.get(across) <= -4),
                                                shown ->
                                                        shown.group(
                                                                grenade ->
                                                                        grenade.shift(
                                                                                        across,
                                                                                        up,
                                                                                        _ ->
                                                                                                0)
                                                                                .about(
                                                                                        0,
                                                                                        0,
                                                                                        -2.3125,
                                                                                        a ->
                                                                                                a.turn(
                                                                                                                Axis
                                                                                                                        .XN,
                                                                                                                90)
                                                                                                        .spin(
                                                                                                                Axis
                                                                                                                        .YN,
                                                                                                                spin))
                                                                                .part("Grenade")));
                                    }
                                })
                        .hinge(0, -1, -8, Axis.XP, Rig.DRAW)
                        .hinge(0, 1, -4, Axis.XP, Rig.TILT)
                        .slideZ(Rig.KICK)
                        .part("Gun")
                        .group(barrel -> barrel.slideZ(r -> r.get(Rig.BARREL) * 2).part("Barrel"))
                        .group(lid -> lid.hinge(0, 0.6875, -1, Axis.XP, Rig.LID).part("Lid"))
                        .group(drum -> belt(drum.move(Rig.DRUM).part("Drum")))
                        .group(
                                flash ->
                                        flash.shift(0, 0, 8.125)
                                                .turn(Axis.YP, 90)
                                                .roll(90)
                                                .flash(MuzzleFlash.NORMAL, 50, 5));
        return b.build();
    }

    private static GunRenderDefinition.Builder belt(GunRenderDefinition.Builder b) {
        var mesh = ResourceManager.mk108;
        int link = mesh.partId("Belt"), grenade = mesh.partId("Grenade");
        return b.custom(
                d -> {
                    double[][] links =
                            beltLinks(
                                    d.carries(Rig.BELT) ? d.get(Rig.BELT) : 1D,
                                    d.carries(Rig.CYCLE) ? d.get(Rig.CYCLE) : 1);
                    int count =
                            d.carries(Rig.SHELLS)
                                    ? (int) d.get(Rig.SHELLS)
                                    : GunRenderDefinition.magazine(d).getAmount(d.stack(), null);
                    var pose = d.pose();
                    for (int i = 0; i < links.length; i++) {
                        pose.pushPose();
                        pose.translate(links[i][0], links[i][1], 0);
                        pose.mulPose((Quaternionfc) Axis.ZP.rotationDegrees((float) links[i][2]));
                        submitPart(d.collector(), pose, d.skin(), mesh, link, d.light());
                        if (loaded(i, count))
                            submitPart(d.collector(), pose, d.skin(), mesh, grenade, d.light());
                        pose.popPose();
                    }
                },
                (r, pose, target) -> {
                    int count = GunRenderDefinition.magazine(r).getAmount(r.stack(), null);
                    double[][] links = beltLinks(1D, 1D);
                    for (int i = 0; i < links.length; i++) {
                        var at =
                                new Matrix4f(pose)
                                        .translate((float) links[i][0], (float) links[i][1], 0F)
                                        .rotate(Axis.ZP.rotationDegrees((float) links[i][2]));
                        target.part(at, mesh, link, ResourceManager.mk108_tex);
                        if (loaded(i, count))
                            target.part(at, mesh, grenade, ResourceManager.mk108_tex);
                    }
                });
    }

    private static double[][] beltLinks(double reload, double cycle) {
        double p = 0.0625D;
        double x = p * 22;
        double y = p * -46;
        double angle = 0;
        double vecX = 0, vecY = 0.53125;

        double[][] shells = new double[CURLED.length][3];
        for (int i = 0; i < CURLED.length; i++) {
            shells[i][0] = x;
            shells[i][1] = y;
            shells[i][2] = angle - 90;
            double delta = NtmMath.lerp(SLACK[i], CURLED[i], reload);
            angle += delta;
            double rad = Math.toRadians(-delta);
            double nvx = vecX * Math.cos(rad) + vecY * Math.sin(rad);
            double nvy = vecY * Math.cos(rad) - vecX * Math.sin(rad);
            vecX = nvx;
            vecY = nvy;
            x += vecX;
            y += vecY;
        }

        double[][] links = new double[shells.length - 1][];
        for (int i = 0; i < links.length; i++) {
            double[] from = shells[i], to = shells[i + 1];
            links[i] =
                    new double[] {
                        NtmMath.lerp(from[0], to[0], cycle),
                        NtmMath.lerp(from[1], to[1], cycle),
                        NtmMath.lerp(from[2], to[2], cycle)
                    };
        }
        return links;
    }

    private static boolean loaded(int link, int count) {
        return CURLED.length - link < count + 2;
    }

    private static final class Rig {
        static final GunAnimationChannels RIG = new GunAnimationChannels();
        static final GunAnimationChannels.Scalar[] THROWN = {
            RIG.x("GRENH1"), RIG.x("GRENH2"), RIG.x("GRENH3")
        };
        static final GunAnimationChannels.Scalar[] RISE = {
            RIG.y("GRENV1"), RIG.y("GRENV2"), RIG.y("GRENV3")
        };
        static final GunAnimationChannels.Scalar[] TUMBLE = {
            RIG.x("GRENS1"), RIG.x("GRENS2"), RIG.x("GRENS3")
        };
        static final GunAnimationChannels.Scalar CYCLE = RIG.x("CYCLE");
        static final GunAnimationChannels.Scalar BELT = RIG.x("BELT");
        static final GunAnimationChannels.Scalar SHELLS = RIG.x("SHELLS");
        static final GunAnimationChannels.Scalar DRAW = RIG.x("EQUIP");
        static final GunAnimationChannels.Scalar BARREL = RIG.z("BARREL");
        static final GunAnimationChannels.Scalar KICK = RIG.z("RECOIL");
        static final GunAnimationChannels.Scalar LID = RIG.x("LID");
        static final GunAnimationChannels.Vec3 DRUM = RIG.vec3("DRUM");
        static final GunAnimationChannels.Scalar TILT = RIG.x("LIFT");

        static {
            RIG.carries("HAMMER");
        }
    }
}
