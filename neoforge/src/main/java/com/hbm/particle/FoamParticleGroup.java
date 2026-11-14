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

package com.hbm.particle;

import com.hbm.client.render.WeaponRenderTypes;
import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class FoamParticleGroup extends ParticleGroup<FoamParticle> {

    public FoamParticleGroup(ParticleEngine engine) {
        super(engine);
    }

    @Override
    public ParticleGroupRenderState extractRenderState(
            Frustum frustum, Camera camera, float partialTicks) {
        List<Cluster> clusters = new ArrayList<>();
        Vec3 cam = camera.position();

        for (FoamParticle fx : this.particles) {
            if (fx.ownsVisual()) continue;
            int bubbles = fx.bubbleCount();
            float spread = fx.spread();
            int light = fx.light(partialTicks);

            clusters.add(
                    new Cluster(
                            (float) (fx.headX() - cam.x),
                            (float) (fx.headY() - cam.y),
                            (float) (fx.headZ() - cam.z),
                            fx.scale(),
                            fx.alpha(),
                            spread,
                            bubbles,
                            light,
                            fx.headBubbles()));

            int i = 0;
            for (FoamParticle.Point point : fx.trail()) {
                if (i > 0) {
                    float fade = 1.0F - (float) i / fx.trailLength();
                    clusters.add(
                            new Cluster(
                                    (float) (point.x() - cam.x),
                                    (float) (point.y() - cam.y),
                                    (float) (point.z() - cam.z),
                                    fx.scale() * fade,
                                    fx.alpha() * fade * 0.7F,
                                    spread,
                                    bubbles,
                                    light,
                                    point.bubbles()));
                }
                i++;
            }
        }

        return new State(clusters);
    }

    private record Cluster(
            float px,
            float py,
            float pz,
            float scale,
            float alpha,
            float spread,
            int bubbles,
            int light,
            float[] data) {}

    private record State(List<Cluster> clusters) implements ParticleGroupRenderState {

        @Override
        public void submit(SubmitNodeCollector collector, CameraRenderState camera) {
            if (clusters.isEmpty()) return;

            Vector3f left = camera.orientation.transform(new Vector3f(-1F, 0F, 0F));
            Vector3f up = camera.orientation.transform(new Vector3f(0F, 1F, 0F));

            collector.submitCustomGeometry(
                    new PoseStack(),
                    WeaponRenderTypes.cloud(ResourceManager.particle_base_tex),
                    (p, tess) -> {
                        for (Cluster c : clusters) {
                            if (c.alpha <= 0F) continue;
                            int a = Mth.clamp((int) (c.alpha * 255F), 0, 255);

                            for (int i = 0; i < c.bubbles; i++) {
                                int b = i * FoamParticle.BUBBLE_STRIDE;
                                int white = Mth.clamp((int) (c.data[b + 4] * 255F), 0, 255);
                                int color = a << 24 | white << 16 | white << 8 | white;
                                float s = c.scale * c.data[b + 3];

                                float bx = c.px + c.data[b] * c.spread;
                                float by = c.py + c.data[b + 1] * c.spread * 0.7F;
                                float bz = c.pz + c.data[b + 2] * c.spread;

                                corner(tess, left, up, bx, by, bz, -s, -s, color, 1F, 1F, c.light);
                                corner(tess, left, up, bx, by, bz, -s, s, color, 1F, 0F, c.light);
                                corner(tess, left, up, bx, by, bz, s, s, color, 0F, 0F, c.light);
                                corner(tess, left, up, bx, by, bz, s, -s, color, 0F, 1F, c.light);
                            }
                        }
                    });
        }

        private static void corner(
                VertexConsumer tess,
                Vector3f left,
                Vector3f up,
                float px,
                float py,
                float pz,
                float a,
                float b,
                int color,
                float u,
                float v,
                int light) {
            tess.addVertex(
                    px + left.x * a + up.x * b,
                    py + left.y * a + up.y * b,
                    pz + left.z * a + up.z * b,
                    color,
                    u,
                    v,
                    OverlayTexture.NO_OVERLAY,
                    light,
                    0F,
                    1F,
                    0F);
        }
    }
}
