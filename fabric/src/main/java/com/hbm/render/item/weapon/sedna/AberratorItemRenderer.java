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

import com.hbm.client.render.FlatCutout;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.UnitQuad;
import com.hbm.render.util.Vertices;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Quaternionfc;

public class AberratorItemRenderer extends AnimatedGunItemRenderer {
    private static final Identifier HALO_TEXTURE =
            Identifier.withDefaultNamespace("textures/item/golden_sword.png");
    private static final RenderType HALO = FlatCutout.of(HALO_TEXTURE);
    public static final int FIREBALL_FLASH = 150;
    private static final UnitQuad PLUME =
            UnitQuad.of(new float[] {0F, 1F, 1F, 1F, 1F, 0F, 0F, 0F}, 0F, 1F, 0F);
    private static final UnitQuad HALO_QUAD =
            UnitQuad.of(new float[] {1F, 1F, 0F, 1F, 0F, 0F, 1F, 0F}, 0F, 1F, 0F);
    private static final int HALO_BLADES = 16;

    public AberratorItemRenderer() {
        super(body());
    }

    private static GunRenderDefinition body() {
        var b =
                GunRenderDefinition.on(AberratorGunModel.RIG)
                        .shift(0, 0, 1)
                        .aim(-1.0F * 0.8F, -1.25F * 0.8F, 1.25F * 0.8F, 0, -5.25 / 8D, 0.125)
                        .body()
                        .size(0.25);
        AberratorGunModel.gun(b, ResourceManager.aberrator_tex, 1);
        return b.shift(0, 2, 4.5)
                .spin(Axis.ZN, AberratorGunModel.ROLL)
                .spin(Axis.XN, AberratorGunModel.KICK)
                .spin(Axis.XN, AberratorGunModel.DRAW)
                .custom(
                        AberratorItemRenderer::halo,
                        (_, pose, target) ->
                                target.spinZ(
                                        new Matrix4f(pose),
                                        50D,
                                        turned -> {
                                            var corners = new Matrix4f();
                                            UnitQuad.matrix(
                                                    -0.5F, -0.5F, -0.5F, 0.5F, -0.5F, -0.5F, 0.5F,
                                                    0.5F, -0.5F, -0.5F, 0.5F, -0.5F, corners);
                                            var turn = new Matrix4f();
                                            for (int i = 0; i < HALO_BLADES; i++) {
                                                turned.quad(
                                                        new Matrix4f(turn)
                                                                .translate(0F, -1.5F, 0F)
                                                                .rotate(
                                                                        Axis.ZP.rotationDegrees(
                                                                                -45)),
                                                        corners,
                                                        HALO_QUAD,
                                                        HALO_TEXTURE);
                                                turn.rotate(
                                                        Axis.ZP.rotationDegrees(
                                                                360F / HALO_BLADES));
                                            }
                                        }))
                .build();
    }

    private static void halo(GunRenderDefinition.Drawing d) {
        var pose = d.pose();
        pose.mulPose((Quaternionfc) Axis.ZP.rotationDegrees((float) (GameTime.now() / 50D % 360D)));
        float closing = Math.min(1F, d.aim() * 2);
        int light = d.light();
        for (int i = 0; i < HALO_BLADES; i++) {
            pose.pushPose();
            pose.translate(0, -1.5 - closing, 0);
            pose.mulPose((Quaternionfc) Axis.XP.rotationDegrees(90 * closing));
            pose.mulPose((Quaternionfc) Axis.ZP.rotationDegrees(-45));
            d.collector()
                    .submitCustomGeometry(
                            pose,
                            HALO,
                            (p, tess) -> {
                                Vertices.emit(
                                        tess, p, -0.5F, -0.5F, -0.5F, -1, 1, 1, light, 0F, 1F, 0F);
                                Vertices.emit(
                                        tess, p, 0.5F, -0.5F, -0.5F, -1, 0, 1, light, 0F, 1F, 0F);
                                Vertices.emit(
                                        tess, p, 0.5F, 0.5F, -0.5F, -1, 0, 0, light, 0F, 1F, 0F);
                                Vertices.emit(
                                        tess, p, -0.5F, 0.5F, -0.5F, -1, 1, 0, light, 0F, 1F, 0F);
                            });
            pose.popPose();
            pose.mulPose((Quaternionfc) Axis.ZP.rotationDegrees(360F / HALO_BLADES));
        }
    }

    public static void renderFireball(
            SubmitNodeCollector collector, PoseStack pose, long lastShot) {
        MuzzleFlash.FIREBALL.submit(collector, pose, lastShot, 0, 0, 0);
    }

    public static void fireballQuads(double fire, Matrix4f scratch, UnitQuad.Visitor visitor) {
        float height = (float) (5 * fire);
        float length = (float) (10 * fire);
        float offset = (float) (1 * fire);
        float lengthOffset = -1.125F;
        PLUME.visit(
                visitor,
                scratch,
                height,
                -offset,
                0,
                -height,
                -offset,
                0,
                -height,
                -offset + length,
                -lengthOffset,
                height,
                -offset + length,
                -lengthOffset);
        PLUME.visit(
                visitor,
                scratch,
                height,
                -offset,
                0,
                -height,
                -offset,
                0,
                -height,
                -offset + length,
                lengthOffset,
                height,
                -offset + length,
                lengthOffset);
    }
}
