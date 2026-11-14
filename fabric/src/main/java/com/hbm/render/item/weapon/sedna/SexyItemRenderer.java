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
import com.hbm.util.NtmMath;
import com.mojang.math.Axis;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionfc;

public class SexyItemRenderer extends AnimatedGunItemRenderer {

    public SexyItemRenderer(Identifier texture) {
        super(
                GunRenderDefinition.on(Rig.RIG)
                        .shift(0, 0, 0.875)
                        .aim(-1F * 0.8F, -0.75F * 0.8F, 3F * 0.8F, -0.5F, -0.5F, 2F)
                        .body()
                        .size(0.375)
                        .when(
                                r -> r.carries(Rig.BOTTLE),
                                drink ->
                                        drink.group(
                                                bottle ->
                                                        bottle.move(Rig.BOTTLE)
                                                                .about(
                                                                        0,
                                                                        2,
                                                                        0,
                                                                        a ->
                                                                                a.spin(
                                                                                                Axis
                                                                                                        .XP,
                                                                                                Rig
                                                                                                        .SIP)
                                                                                        .turn(
                                                                                                Axis
                                                                                                        .YP,
                                                                                                90)
                                                                                        .turn(
                                                                                                Axis
                                                                                                        .XP,
                                                                                                -15))
                                                                .size(1.5)
                                                                .mesh(ResourceManager.whiskey)
                                                                .skin(ResourceManager.whiskey_tex)
                                                                .whole()))
                        .mesh(ResourceManager.sexy)
                        .skin(texture)
                        .hinge(0, -1, -8, Axis.XP, Rig.DRAW)
                        .hinge(0, 0, -6, Axis.XP, Rig.LOWER)
                        .slideZ(Rig.KICK)
                        .part("Gun")
                        .group(barrel -> barrel.slideZ(Rig.BARREL).part("Barrel"))
                        .group(
                                spring ->
                                        spring.shift(0, 0, -0.375)
                                                .size(
                                                        _ -> 1,
                                                        _ -> 1,
                                                        r -> 1 + 0.457247371D * r.get(Rig.BARREL))
                                                .shift(0, 0, 0.375)
                                                .part("RecoilSpring"))
                        .group(
                                hood ->
                                        hood.hinge(0, 0.4375, -2.875, Axis.XP, Rig.HOOD)
                                                .part("Hood"))
                        .group(
                                lever ->
                                        lever.hinge(
                                                        0,
                                                        0.46875,
                                                        -6.875,
                                                        Axis.XP,
                                                        r -> r.get(Rig.LEVER) * 60)
                                                .part("Lever"))
                        .group(
                                spring ->
                                        spring.shift(0, 0, -6.75)
                                                .size(
                                                        _ -> 1,
                                                        _ -> 1,
                                                        r -> 1 - r.get(Rig.LEVER) * 0.25)
                                                .shift(0, 0, 6.75)
                                                .part("LockSpring"))
                        .group(
                                mag ->
                                        belt(
                                                mag.move(Rig.CLIP)
                                                        .hinge(0, -1, 0, Axis.ZP, Rig.CLIP_TURN)
                                                        .part("Magazine"),
                                                texture))
                        .group(
                                flash ->
                                        flash.shift(0, 0, 8)
                                                .turn(Axis.YP, 90)
                                                .roll(90)
                                                .flash(MuzzleFlash.NORMAL, 150, 7.5))
                        .build());
    }

    private static GunRenderDefinition.Builder belt(
            GunRenderDefinition.Builder b, Identifier texture) {
        var mesh = ResourceManager.sexy;
        int link = mesh.partId("Belt"), shell = mesh.partId("Shell");
        return b.custom(
                d -> {
                    double[][] shells = beltShells(d.carries(Rig.BELT) ? d.get(Rig.BELT) : 1D);
                    double cycle = d.carries(Rig.CYCLE) ? d.get(Rig.CYCLE) : 1;
                    int loaded =
                            d.carries(Rig.SHELLS)
                                    ? (int) d.get(Rig.SHELLS)
                                    : GunRenderDefinition.magazine(d).getAmount(d.stack(), null);
                    var pose = d.pose();
                    for (int i = 0; i < shells.length - 1; i++) {
                        double[] from = shells[i], to = shells[i + 1];
                        pose.pushPose();
                        pose.translate(
                                NtmMath.lerp(from[0], to[0], cycle),
                                0.375 + NtmMath.lerp(from[1], to[1], cycle),
                                0);
                        pose.rotate(
                                (Quaternionfc)
                                        Axis.ZP.rotationDegrees(
                                                (float) NtmMath.lerp(from[2], to[2], cycle)));
                        pose.translate(0, -0.375, 0);
                        submitPart(d.collector(), pose, d.skin(), mesh, link, d.light());
                        if (shells.length - i < loaded + 2)
                            submitPart(d.collector(), pose, d.skin(), mesh, shell, d.light());
                        pose.popPose();
                    }
                },
                (r, pose, target) -> {
                    double[][] shells = beltShells(1D);
                    int loaded = GunRenderDefinition.magazine(r).getAmount(r.stack(), null);
                    for (int i = 0; i < shells.length - 1; i++) {
                        double[] at = shells[i + 1];
                        var segment =
                                new Matrix4f(pose)
                                        .translate((float) at[0], (float) (0.375 + at[1]), 0F)
                                        .rotate(Axis.ZP.rotationDegrees((float) at[2]))
                                        .translate(0F, -0.375F, 0F);
                        target.part(segment, mesh, link, texture);
                        if (shells.length - i < loaded + 2)
                            target.part(segment, mesh, shell, texture);
                    }
                });
    }

    private static double[][] beltShells(double reload) {
        double p = 0.0625D;
        double x = p * 17;
        double y = p * -26;
        double angle = 0;
        Vec3 vec = new Vec3(0, 0.4375, 0);
        double[] curled = {0, 0, 20, 20, 50, 60, 70};
        double[] slack = {0, -10, -50, -60, -60, 0, 0};
        double[][] shells = new double[curled.length][3];
        for (int i = 0; i < curled.length; i++) {
            shells[i][0] = x;
            shells[i][1] = y;
            shells[i][2] = angle - 90;
            double delta = NtmMath.lerp(slack[i], curled[i], reload);
            angle += delta;
            vec = vec.zRot((float) Math.toRadians(-delta));
            x += vec.x;
            y += vec.y;
        }
        return shells;
    }

    private static final class Rig {
        static final GunAnimationChannels RIG = new GunAnimationChannels();
        static final GunAnimationChannels.Scalar CYCLE = RIG.x("CYCLE");
        static final GunAnimationChannels.Scalar BELT = RIG.x("BELT");
        static final GunAnimationChannels.Scalar SHELLS = RIG.x("SHELLS");
        static final GunAnimationChannels.Vec3 BOTTLE = RIG.vec3("BOTTLE");
        static final GunAnimationChannels.Scalar DRAW = RIG.x("EQUIP");
        static final GunAnimationChannels.Scalar LOWER = RIG.x("LOWER");
        static final GunAnimationChannels.Scalar KICK = RIG.z("RECOIL");
        static final GunAnimationChannels.Scalar BARREL = RIG.z("BARREL");
        static final GunAnimationChannels.Scalar HOOD = RIG.x("HOOD");
        static final GunAnimationChannels.Scalar LEVER = RIG.z("LEVER");
        static final GunAnimationChannels.Vec3 CLIP = RIG.vec3("MAG");
        static final GunAnimationChannels.Scalar CLIP_TURN = RIG.z("MAGROT");
        static final GunAnimationChannels.Scalar SIP = RIG.x("SIP");
    }
}
