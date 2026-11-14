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

package com.hbm.particle;

import com.hbm.client.render.WeaponRenderTypes;
import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class ContrailParticleGroup extends ParticleGroup<ContrailParticle> {

    private static final int LIGHT = LightCoordsUtil.pack(15, 0);

    public ContrailParticleGroup(ParticleEngine engine) {
        super(engine);
    }

    @Override
    public ParticleGroupRenderState extractRenderState(
            Frustum frustum, Camera camera, float partialTicks) {
        int count = this.particles.size();
        ContrailParticle[] refs = new ContrailParticle[count];
        float[] pos = new float[count * 3];
        float[] par = new float[count * 6];
        Vec3 cam = camera.position();

        int i = 0;
        for (ContrailParticle fx : this.particles) {
            refs[i] = fx;
            pos[i * 3] = (float) (fx.interpX(partialTicks) - cam.x);
            pos[i * 3 + 1] = (float) (fx.interpY(partialTicks) - cam.y);
            pos[i * 3 + 2] = (float) (fx.interpZ(partialTicks) - cam.z);
            par[i * 6] = fx.red();
            par[i * 6 + 1] = fx.green();
            par[i * 6 + 2] = fx.blue();
            par[i * 6 + 3] = fx.alpha();
            par[i * 6 + 4] = fx.quadScale();
            par[i * 6 + 5] = fx.spread();
            i++;
        }

        return new State(refs, pos, par);
    }

    private record State(ContrailParticle[] refs, float[] pos, float[] par)
            implements ParticleGroupRenderState {

        private static int argb(float r, float g, float b, float a) {
            return Mth.clamp((int) (a * 255F), 0, 255) << 24
                    | Mth.clamp((int) (r * 255F), 0, 255) << 16
                    | Mth.clamp((int) (g * 255F), 0, 255) << 8
                    | Mth.clamp((int) (b * 255F), 0, 255);
        }

        @Override
        public void submit(SubmitNodeCollector collector, CameraRenderState camera) {
            if (refs.length == 0) return;

            Vector3f left = camera.orientation.transform(new Vector3f(-1F, 0F, 0F));
            Vector3f up = camera.orientation.transform(new Vector3f(0F, 1F, 0F));

            collector.submitCustomGeometry(
                    new PoseStack(),
                    WeaponRenderTypes.cloudSeparate(ResourceManager.contrail_tex),
                    (p, tess) -> {
                        for (int i = 0; i < refs.length; i++) {
                            ContrailParticle fx = refs[i];
                            float px = pos[i * 3], py = pos[i * 3 + 1], pz = pos[i * 3 + 2];
                            float r = par[i * 6], g = par[i * 6 + 1], b = par[i * 6 + 2];
                            float a = par[i * 6 + 3], s = par[i * 6 + 4], spread = par[i * 6 + 5];

                            float lx = left.x * s, ly = left.y * s, lz = left.z * s;
                            float ux = up.x * s, uy = up.y * s, uz = up.z * s;

                            for (int q = 0; q < ContrailParticle.SUBQUADS; q++) {
                                float m = fx.mod[q];
                                int color = argb(r + m, g + m, b + m, a);

                                float qx = px + (float) (fx.gaussX[q] * 0.5D) * spread;
                                float qy = py + (float) (fx.gaussY[q] * 0.5D) * spread;
                                float qz = pz + (float) (fx.gaussZ[q] * 0.5D) * spread;

                                tess.addVertex(
                                        qx - lx - ux,
                                        qy - ly - uy,
                                        qz - lz - uz,
                                        color,
                                        1F,
                                        1F,
                                        OverlayTexture.NO_OVERLAY,
                                        LIGHT,
                                        0F,
                                        1F,
                                        0F);
                                tess.addVertex(
                                        qx - lx + ux,
                                        qy - ly + uy,
                                        qz - lz + uz,
                                        color,
                                        1F,
                                        0F,
                                        OverlayTexture.NO_OVERLAY,
                                        LIGHT,
                                        0F,
                                        1F,
                                        0F);
                                tess.addVertex(
                                        qx + lx + ux,
                                        qy + ly + uy,
                                        qz + lz + uz,
                                        color,
                                        0F,
                                        0F,
                                        OverlayTexture.NO_OVERLAY,
                                        LIGHT,
                                        0F,
                                        1F,
                                        0F);
                                tess.addVertex(
                                        qx + lx - ux,
                                        qy + ly - uy,
                                        qz + lz - uz,
                                        color,
                                        0F,
                                        1F,
                                        OverlayTexture.NO_OVERLAY,
                                        LIGHT,
                                        0F,
                                        1F,
                                        0F);
                            }
                        }
                    });
        }
    }
}
