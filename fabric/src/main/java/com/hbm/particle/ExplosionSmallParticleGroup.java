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

import com.hbm.client.render.WeaponRenderTypes;
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
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;

public class ExplosionSmallParticleGroup extends ParticleGroup<ExplosionSmallParticle> {

    public ExplosionSmallParticleGroup(ParticleEngine engine) {
        super(engine);
    }

    @Override
    public ParticleGroupRenderState extractRenderState(
            Frustum frustum, Camera camera, float partialTicks) {
        List<Instance> out = new ArrayList<>(this.particles.size());
        Vec3 cam = camera.position();

        for (ExplosionSmallParticle cloud : this.particles) {
            if (cloud.ownsVisual()) continue;
            out.add(
                    new Instance(
                            cloud.interpX(partialTicks) - cam.x,
                            cloud.interpY(partialTicks) - cam.y,
                            cloud.interpZ(partialTicks) - cam.z,
                            cloud.roll(partialTicks),
                            cloud.color(partialTicks),
                            cloud.alpha(partialTicks),
                            cloud.renderScale(partialTicks),
                            cloud.light(partialTicks)));
        }

        return new State(out);
    }

    private record Instance(
            double relX,
            double relY,
            double relZ,
            float roll,
            int color,
            float alpha,
            float scale,
            int light) {}

    private record State(List<Instance> instances) implements ParticleGroupRenderState {

        @Override
        public void submit(SubmitNodeCollector collector, CameraRenderState camera) {
            PoseStack pose = new PoseStack();

            for (Instance in : instances) {
                pose.pushPose();
                pose.translate(in.relX, in.relY, in.relZ);
                pose.rotate((Quaternionfc) camera.orientation);
                pose.rotate((Quaternionfc) Axis.ZP.rotationDegrees(in.roll));

                float s = in.scale;
                int color =
                        ARGB.colorFromFloat(
                                in.alpha,
                                ARGB.red(in.color) / 255F,
                                ARGB.green(in.color) / 255F,
                                ARGB.blue(in.color) / 255F);
                collector.submitCustomGeometry(
                        pose,
                        WeaponRenderTypes.cloud(ResourceManager.particle_base_tex),
                        (p, tess) -> {
                            Vertices.emit(tess, p, s, -s, 0, color, 1, 1, in.light, 0F, 1F, 0F);
                            Vertices.emit(tess, p, s, s, 0, color, 1, 0, in.light, 0F, 1F, 0F);
                            Vertices.emit(tess, p, -s, s, 0, color, 0, 0, in.light, 0F, 1F, 0F);
                            Vertices.emit(tess, p, -s, -s, 0, color, 0, 1, in.light, 0F, 1F, 0F);
                        });
                pose.popPose();
            }
        }
    }
}
