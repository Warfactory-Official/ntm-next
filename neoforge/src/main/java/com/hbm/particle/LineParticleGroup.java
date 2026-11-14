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
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class LineParticleGroup extends ParticleGroup<Particle> {
    private static final float SPARK_WIDTH = 3F;
    private static final float DEBUG_WIDTH = 1F;

    public LineParticleGroup(ParticleEngine engine) {
        super(engine);
    }

    @Override
    public ParticleGroupRenderState extractRenderState(
            Frustum frustum, Camera camera, float partialTicks) {
        List<Segment> sparks = new ArrayList<>();
        List<Segment> debug = new ArrayList<>();
        Vec3 cam = camera.position();

        for (Particle particle : this.particles) {
            if (particle instanceof SparkParticle spark) {
                int steps = spark.stepCount();
                if (steps < 2) continue;
                double px = spark.interpX(partialTicks) - cam.x;
                double py = spark.interpY(partialTicks) - cam.y;
                double pz = spark.interpZ(partialTicks) - cam.z;

                for (int i = 1; i < steps - 1; i++) {
                    double nx = px + spark.step(i, 0),
                            ny = py + spark.step(i, 1),
                            nz = pz + spark.step(i, 2);
                    sparks.add(
                            new Segment(
                                    (float) px,
                                    (float) py,
                                    (float) pz,
                                    (float) nx,
                                    (float) ny,
                                    (float) nz,
                                    0xFFFFFFFF,
                                    SPARK_WIDTH));
                    px = nx;
                    py = ny;
                    pz = nz;
                }
            } else if (particle instanceof DebugLineParticle line) {
                float px = (float) (line.interpX(partialTicks) - cam.x);
                float py = (float) (line.interpY(partialTicks) - cam.y);
                float pz = (float) (line.interpZ(partialTicks) - cam.z);
                int fade = Mth.clamp((int) (line.brightness(partialTicks) * 255F), 0, 255);
                debug.add(
                        new Segment(
                                px,
                                py,
                                pz,
                                (float) (px + line.dx()),
                                (float) (py + line.dy()),
                                (float) (pz + line.dz()),
                                fade << 24 | line.rgb(),
                                DEBUG_WIDTH));
            }
        }

        return new State(sparks, debug);
    }

    private record Segment(
            float x0, float y0, float z0, float x1, float y1, float z1, int color, float width) {}

    private record State(List<Segment> sparks, List<Segment> debug)
            implements ParticleGroupRenderState {

        @Override
        public void submit(SubmitNodeCollector collector, CameraRenderState camera) {
            emit(collector, RenderTypes.lines(), sparks);
            emit(collector, ParticleRenderTypes.LINES_NO_DEPTH, debug);
        }

        private static void emit(
                SubmitNodeCollector collector, RenderType renderType, List<Segment> segments) {
            if (segments.isEmpty()) return;

            collector.submitCustomGeometry(
                    new PoseStack(),
                    renderType,
                    (p, tess) -> {
                        for (Segment s : segments) {
                            float dx = s.x1 - s.x0, dy = s.y1 - s.y0, dz = s.z1 - s.z0;
                            float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
                            if (len < 1.0e-6F) continue;
                            dx /= len;
                            dy /= len;
                            dz /= len;

                            vertex(tess, s.x0, s.y0, s.z0, s.color, dx, dy, dz, s.width);
                            vertex(tess, s.x1, s.y1, s.z1, s.color, dx, dy, dz, s.width);
                        }
                    });
        }

        private static void vertex(
                VertexConsumer tess,
                float x,
                float y,
                float z,
                int color,
                float nx,
                float ny,
                float nz,
                float width) {
            tess.addVertex(x, y, z).setColor(color).setNormal(nx, ny, nz).setLineWidth(width);
        }
    }
}
