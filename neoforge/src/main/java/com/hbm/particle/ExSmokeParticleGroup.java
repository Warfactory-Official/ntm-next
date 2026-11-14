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
import com.hbm.render.util.Vertices;
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
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;

public class ExSmokeParticleGroup extends ParticleGroup<ExSmokeParticle> {

    public ExSmokeParticleGroup(ParticleEngine engine) {
        super(engine);
    }

    @Override
    public ParticleGroupRenderState extractRenderState(
            Frustum frustum, Camera camera, float partialTicks) {
        List<Instance> out = new ArrayList<>(this.particles.size());
        Vec3 cam = camera.position();

        for (ExSmokeParticle smoke : this.particles) {
            if (smoke.ownsVisual()) continue;
            out.add(
                    new Instance(
                            smoke.interpX(partialTicks) - cam.x,
                            smoke.interpY(partialTicks) - cam.y,
                            smoke.interpZ(partialTicks) - cam.z,
                            smoke.seed,
                            smoke.alpha(partialTicks),
                            smoke.light(partialTicks)));
        }

        return new State(out);
    }

    private record Instance(
            double relX, double relY, double relZ, int seed, float alpha, int light) {}

    private record State(List<Instance> instances) implements ParticleGroupRenderState {

        @Override
        public void submit(SubmitNodeCollector collector, CameraRenderState camera) {
            PoseStack pose = new PoseStack();

            for (Instance in : instances) {
                Random urandom = new Random(in.seed);

                for (int i = 0; i < 6; i++) {
                    float grey = urandom.nextFloat() * 0.25F + 0.25F;
                    float scale = urandom.nextFloat() + 0.5F;
                    double oX = (urandom.nextGaussian() - 1D) * 0.75D;
                    double oY = (urandom.nextGaussian() - 1D) * 0.75D;
                    double oZ = (urandom.nextGaussian() - 1D) * 0.75D;
                    int color = ARGB.colorFromFloat(in.alpha, grey, grey, grey);

                    pose.pushPose();
                    pose.translate(in.relX + oX, in.relY + oY, in.relZ + oZ);
                    pose.rotate((Quaternionfc) camera.orientation);

                    collector.submitCustomGeometry(
                            pose,
                            WeaponRenderTypes.cloud(ResourceManager.particle_base_tex),
                            (p, tess) -> {
                                Vertices.emit(
                                        tess, p, scale, -scale, 0, color, 1, 1, in.light, 0F, 1F,
                                        0F);
                                Vertices.emit(
                                        tess, p, scale, scale, 0, color, 1, 0, in.light, 0F, 1F,
                                        0F);
                                Vertices.emit(
                                        tess, p, -scale, scale, 0, color, 0, 0, in.light, 0F, 1F,
                                        0F);
                                Vertices.emit(
                                        tess, p, -scale, -scale, 0, color, 0, 1, in.light, 0F, 1F,
                                        0F);
                            });
                    pose.popPose();
                }
            }
        }
    }
}
