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

import com.hbm.client.render.ParticleRenderTypes;
import com.hbm.render.util.Vertices;
import com.hbm.util.RegistryUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class MukeFlashParticleGroup extends ParticleGroup<MukeFlashParticle> {

    private static final Identifier FLARE_TEX = RegistryUtil.id("textures/particle/flare.png");
    private static final int LIGHT = LightCoordsUtil.pack(15, 0);

    public MukeFlashParticleGroup(ParticleEngine engine) {
        super(engine);
    }

    @Override
    public ParticleGroupRenderState extractRenderState(
            Frustum frustum, Camera camera, float partialTicks) {
        List<Instance> out = new ArrayList<>(this.particles.size());
        Vec3 cam = camera.position();

        for (MukeFlashParticle flash : this.particles) {
            if (flash.ownsVisual()) continue;
            out.add(
                    new Instance(
                            flash.interpX(partialTicks) - cam.x,
                            flash.interpY(partialTicks) - cam.y,
                            flash.interpZ(partialTicks) - cam.z,
                            flash.alpha(partialTicks),
                            flash.renderScale(partialTicks)));
        }

        return new State(out);
    }

    private record Instance(double relX, double relY, double relZ, float alpha, float scale) {}

    private record State(List<Instance> instances) implements ParticleGroupRenderState {

        @Override
        public void submit(SubmitNodeCollector collector, CameraRenderState camera) {
            PoseStack pose = new PoseStack();

            for (Instance in : instances) {
                pose.pushPose();
                pose.translate(in.relX, in.relY, in.relZ);

                float s = in.scale;
                int color = ARGB.colorFromFloat(in.alpha * 0.5F, 1F, 0.9F, 0.75F);
                Random rand = new Random();
                float[][] offsets = new float[24][3];
                for (int i = 0; i < 24; i++) {
                    rand.setSeed(i * 31 + 1);
                    offsets[i][0] = (float) (rand.nextDouble() * 15 - 7.5);
                    offsets[i][1] = (float) (rand.nextDouble() * 7.5 - 3.75);
                    offsets[i][2] = (float) (rand.nextDouble() * 15 - 7.5);
                }

                collector.submitCustomGeometry(
                        pose,
                        ParticleRenderTypes.flashNoFogCulled(FLARE_TEX),
                        (p, tess) -> {
                            for (float[] off : offsets) {
                                Vector3f c1 = camera.orientation.transform(new Vector3f(-s, -s, 0));
                                Vector3f c2 = camera.orientation.transform(new Vector3f(-s, s, 0));
                                Vector3f c3 = camera.orientation.transform(new Vector3f(s, s, 0));
                                Vector3f c4 = camera.orientation.transform(new Vector3f(s, -s, 0));
                                Vertices.emit(
                                        tess,
                                        p,
                                        off[0] + c4.x,
                                        off[1] + c4.y,
                                        off[2] + c4.z,
                                        color,
                                        1,
                                        1,
                                        LIGHT,
                                        0F,
                                        1F,
                                        0F);
                                Vertices.emit(
                                        tess,
                                        p,
                                        off[0] + c3.x,
                                        off[1] + c3.y,
                                        off[2] + c3.z,
                                        color,
                                        1,
                                        0,
                                        LIGHT,
                                        0F,
                                        1F,
                                        0F);
                                Vertices.emit(
                                        tess,
                                        p,
                                        off[0] + c2.x,
                                        off[1] + c2.y,
                                        off[2] + c2.z,
                                        color,
                                        0,
                                        0,
                                        LIGHT,
                                        0F,
                                        1F,
                                        0F);
                                Vertices.emit(
                                        tess,
                                        p,
                                        off[0] + c1.x,
                                        off[1] + c1.y,
                                        off[2] + c1.z,
                                        color,
                                        0,
                                        1,
                                        LIGHT,
                                        0F,
                                        1F,
                                        0F);
                            }
                        });
                pose.popPose();
            }
        }
    }
}
