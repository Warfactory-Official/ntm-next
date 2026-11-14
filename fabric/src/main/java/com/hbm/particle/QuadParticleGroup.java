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

package com.hbm.particle;

import com.hbm.client.particle.VisualParticle;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class QuadParticleGroup extends ParticleGroup<Particle> {

    private final RenderType renderType;

    public QuadParticleGroup(ParticleEngine engine, RenderType renderType) {
        super(engine);
        this.renderType = renderType;
    }

    @Override
    public ParticleGroupRenderState extractRenderState(
            Frustum frustum, Camera camera, float partialTicks) {
        int capacity = this.particles.size();
        float[] geo = new float[capacity * 6];
        int[] tint = new int[capacity * 2];
        float[] uv = new float[capacity * 4];
        Vec3 cam = camera.position();

        int i = 0;
        for (Particle particle : this.particles) {
            if (particle instanceof VisualParticle visual && visual.ownsVisual()) continue;
            QuadParticle fx = (QuadParticle) particle;
            float spin = fx.spin(partialTicks);
            geo[i * 6] = (float) (fx.interpX(partialTicks) - cam.x);
            geo[i * 6 + 1] = (float) (fx.interpY(partialTicks) - cam.y);
            geo[i * 6 + 2] = (float) (fx.interpZ(partialTicks) - cam.z);
            geo[i * 6 + 3] = fx.size(partialTicks);
            geo[i * 6 + 4] = RotatedQuad.cos(spin);
            geo[i * 6 + 5] = RotatedQuad.sin(spin);
            tint[i * 2] = fx.argb(partialTicks);
            tint[i * 2 + 1] = fx.light(partialTicks);
            uv[i * 4] = fx.u0();
            uv[i * 4 + 1] = fx.u1();
            uv[i * 4 + 2] = fx.v0();
            uv[i * 4 + 3] = fx.v1();
            i++;
        }

        return new State(renderType, i, geo, tint, uv);
    }

    private record State(RenderType renderType, int count, float[] geo, int[] tint, float[] uv)
            implements ParticleGroupRenderState {

        @Override
        public void submit(SubmitNodeCollector collector, CameraRenderState camera) {
            if (count == 0) return;

            Vector3f left = camera.orientation.transform(new Vector3f(-1F, 0F, 0F));
            Vector3f up = camera.orientation.transform(new Vector3f(0F, 1F, 0F));

            collector.submitCustomGeometry(
                    new PoseStack(),
                    renderType,
                    (p, tess) -> {
                        for (int i = 0; i < count; i++) {
                            float px = geo[i * 6], py = geo[i * 6 + 1], pz = geo[i * 6 + 2];
                            float s = geo[i * 6 + 3], cos = geo[i * 6 + 4], sin = geo[i * 6 + 5];
                            int color = tint[i * 2], light = tint[i * 2 + 1];
                            float u0 = uv[i * 4],
                                    u1 = uv[i * 4 + 1],
                                    v0 = uv[i * 4 + 2],
                                    v1 = uv[i * 4 + 3];

                            corner(
                                    tess, left, up, px, py, pz, -s, -s, cos, sin, color, u1, v1,
                                    light);
                            corner(
                                    tess, left, up, px, py, pz, -s, s, cos, sin, color, u1, v0,
                                    light);
                            corner(
                                    tess, left, up, px, py, pz, s, s, cos, sin, color, u0, v0,
                                    light);
                            corner(
                                    tess, left, up, px, py, pz, s, -s, cos, sin, color, u0, v1,
                                    light);
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
                float cos,
                float sin,
                int color,
                float u,
                float v,
                int light) {
            float la = RotatedQuad.leftOf(a, b, cos, sin);
            float ub = RotatedQuad.upOf(a, b, cos, sin);
            tess.addVertex(
                    px + left.x * la + up.x * ub,
                    py + left.y * la + up.y * ub,
                    pz + left.z * la + up.z * ub,
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
