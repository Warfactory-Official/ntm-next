// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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

public class ExSmokeParticleGroup extends ParticleGroup<ParticleExSmoke> {

    public ExSmokeParticleGroup(ParticleEngine engine) {
        super(engine);
    }

    @Override
    public ParticleGroupRenderState extractRenderState(
            Frustum frustum, Camera camera, float partialTicks) {
        List<Instance> out = new ArrayList<>(this.particles.size());
        Vec3 cam = camera.position();

        for (ParticleExSmoke smoke : this.particles) {
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
                    pose.mulPose(camera.orientation);

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
