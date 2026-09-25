// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.particle;

import com.hbm.client.render.ParticleRenderTypes;
import com.hbm.render.util.Vertices;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class AmatFlashParticleGroup extends ParticleGroup<ParticleAmatFlash> {

    public AmatFlashParticleGroup(ParticleEngine engine) {
        super(engine);
    }

    @Override
    public ParticleGroupRenderState extractRenderState(
            Frustum frustum, Camera camera, float partialTicks) {
        int count = this.particles.size();

        float[] pos = new float[count * 3];

        float[] par = new float[count * 2];
        int[] light = new int[count];
        Vec3 cam = camera.position();

        int i = 0;
        for (ParticleAmatFlash fx : this.particles) {
            pos[i * 3] = (float) (fx.interpX(partialTicks) - cam.x);
            pos[i * 3 + 1] = (float) (fx.interpY(partialTicks) - cam.y);
            pos[i * 3 + 2] = (float) (fx.interpZ(partialTicks) - cam.z);
            par[i * 2] = fx.extent(partialTicks);
            par[i * 2 + 1] = fx.coreAlpha(partialTicks);
            light[i] = fx.light(partialTicks);
            i++;
        }

        return new State(count, pos, par, light);
    }

    private record State(int count, float[] pos, float[] par, int[] light)
            implements ParticleGroupRenderState {

        @Override
        public void submit(SubmitNodeCollector collector, CameraRenderState camera) {
            if (count == 0) return;

            collector.submitCustomGeometry(
                    new PoseStack(),
                    ParticleRenderTypes.AMAT_FLASH,
                    (p, tess) -> {
                        float[] rim = ParticleAmatFlash.RIM;

                        for (int i = 0; i < count; i++) {
                            float px = pos[i * 3], py = pos[i * 3 + 1], pz = pos[i * 3 + 2];
                            float k = par[i * 2];
                            int core =
                                    Mth.clamp((int) (par[i * 2 + 1] * 255F), 0, 255) << 24
                                            | 0xFFFFFF;

                            int edge = 0xFFFFFF;
                            int lit = light[i];

                            for (int s = 0; s < ParticleAmatFlash.SPIKES; s++) {
                                int b = s * 9;
                                float x0 = px + rim[b] * k,
                                        y0 = py + rim[b + 1] * k,
                                        z0 = pz + rim[b + 2] * k;
                                float x1 = px + rim[b + 3] * k,
                                        y1 = py + rim[b + 4] * k,
                                        z1 = pz + rim[b + 5] * k;
                                float x2 = px + rim[b + 6] * k,
                                        y2 = py + rim[b + 7] * k,
                                        z2 = pz + rim[b + 8] * k;

                                spike(tess, p, px, py, pz, core, lit);
                                spike(tess, p, x0, y0, z0, edge, lit);
                                spike(tess, p, x1, y1, z1, edge, lit);
                                spike(tess, p, x2, y2, z2, edge, lit);

                                spike(tess, p, px, py, pz, core, lit);
                                spike(tess, p, x2, y2, z2, edge, lit);
                                spike(tess, p, x0, y0, z0, edge, lit);
                                spike(tess, p, x0, y0, z0, edge, lit);
                            }
                        }
                    });
        }

        private static void spike(
                VertexConsumer tess,
                PoseStack.Pose pose,
                float x,
                float y,
                float z,
                int color,
                int light) {
            Vertices.emit(tess, pose, x, y, z, color, 0F, 0F, light, 0F, 0F, 1F);
        }
    }
}
