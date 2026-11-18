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

import com.hbm.client.render.ParticleRenderTypes;
import com.hbm.main.ResourceManager;
import com.hbm.render.util.Vertices;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;

public class PlasmaBlastParticleGroup extends ParticleGroup<PlasmaBlastParticle> {

    public PlasmaBlastParticleGroup(ParticleEngine engine) {
        super(engine);
    }

    @Override
    public ParticleGroupRenderState extractRenderState(
            Frustum frustum, Camera camera, float partialTicks) {
        List<Instance> out = new ArrayList<>(this.particles.size());
        Vec3 cam = camera.position();

        for (PlasmaBlastParticle blast : this.particles) {
            if (blast.ownsVisual()) continue;
            out.add(
                    new Instance(
                            blast.interpX(partialTicks) - cam.x,
                            blast.interpY(partialTicks) - cam.y,
                            blast.interpZ(partialTicks) - cam.z,
                            blast.yaw,
                            blast.pitch,
                            blast.red,
                            blast.green,
                            blast.blue,
                            blast.alpha(partialTicks),
                            blast.renderScale(partialTicks)));
        }

        return new State(out);
    }

    private record Instance(
            double relX,
            double relY,
            double relZ,
            float yaw,
            float pitch,
            float r,
            float g,
            float b,
            float alpha,
            float scale) {}

    private record State(List<Instance> instances) implements ParticleGroupRenderState {

        @Override
        public void submit(SubmitNodeCollector collector, CameraRenderState camera) {
            PoseStack pose = new PoseStack();

            for (Instance in : instances) {
                pose.pushPose();
                pose.translate(in.relX, in.relY, in.relZ);
                pose.rotate((Quaternionfc) Axis.YP.rotationDegrees(in.yaw));
                pose.rotate((Quaternionfc) Axis.XP.rotationDegrees(in.pitch));

                float s = in.scale;
                int color = ARGB.colorFromFloat(in.alpha, in.r, in.g, in.b);
                collector.submitCustomGeometry(
                        pose,
                        ParticleRenderTypes.flashNoFog(ResourceManager.shockwave_tex),
                        (p, tess) -> {
                            Vertices.emit(
                                    tess,
                                    p,
                                    -s,
                                    0,
                                    -s,
                                    color,
                                    1,
                                    1,
                                    LightCoordsUtil.pack(15, 0),
                                    0F,
                                    1F,
                                    0F);
                            Vertices.emit(
                                    tess,
                                    p,
                                    -s,
                                    0,
                                    s,
                                    color,
                                    1,
                                    0,
                                    LightCoordsUtil.pack(15, 0),
                                    0F,
                                    1F,
                                    0F);
                            Vertices.emit(
                                    tess,
                                    p,
                                    s,
                                    0,
                                    s,
                                    color,
                                    0,
                                    0,
                                    LightCoordsUtil.pack(15, 0),
                                    0F,
                                    1F,
                                    0F);
                            Vertices.emit(
                                    tess,
                                    p,
                                    s,
                                    0,
                                    -s,
                                    color,
                                    0,
                                    1,
                                    LightCoordsUtil.pack(15, 0),
                                    0F,
                                    1F,
                                    0F);
                        });
                pose.popPose();
            }
        }
    }
}
