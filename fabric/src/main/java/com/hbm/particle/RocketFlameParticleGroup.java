// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.particle;

import com.hbm.client.render.WeaponRenderTypes;
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
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class RocketFlameParticleGroup extends ParticleGroup<ParticleRocketFlame> {

    private static final int LIGHT = LightCoordsUtil.pack(15, 0);

    public RocketFlameParticleGroup(ParticleEngine engine) {
        super(engine);
    }

    private static int channel(float v) {
        return Mth.clamp((int) (v * 255F), 0, 255);
    }

    @Override
    public ParticleGroupRenderState extractRenderState(
            Frustum frustum, Camera camera, float partialTicks) {
        List<Instance> out = new ArrayList<>(this.particles.size());
        Vec3 cam = camera.position();

        for (ParticleRocketFlame flame : this.particles) {
            out.add(
                    new Instance(
                            flame.interpX(partialTicks) - cam.x,
                            flame.interpY(partialTicks) - cam.y,
                            flame.interpZ(partialTicks) - cam.z,
                            flame.seed,
                            flame.alpha(),
                            flame.dark(),
                            flame.spread(),
                            flame.growth(),
                            flame.baseScale()));
        }

        return new State(out);
    }

    private record Instance(
            double relX,
            double relY,
            double relZ,
            int seed,
            float alpha,
            float dark,
            float spread,
            float growth,
            float baseScale) {}

    private record State(List<Instance> instances) implements ParticleGroupRenderState {

        @Override
        public void submit(SubmitNodeCollector collector, CameraRenderState camera) {
            PoseStack pose = new PoseStack();

            for (Instance in : instances) {
                Random urandom = new Random(in.seed);

                for (int i = 0; i < 10; i++) {
                    float add = urandom.nextFloat() * 0.3F;
                    float scale = (urandom.nextFloat() * 0.5F + 0.1F + in.growth) * in.baseScale;
                    double oX = (urandom.nextGaussian() - 1D) * 0.2F * in.spread;
                    double oY = (urandom.nextGaussian() - 1D) * 0.5F * in.spread;
                    double oZ = (urandom.nextGaussian() - 1D) * 0.2F * in.spread;
                    int color =
                            ARGB.color(
                                    channel(in.alpha),
                                    channel(in.dark + add),
                                    channel(0.6F * in.dark + add),
                                    channel(add));

                    pose.pushPose();
                    pose.translate(in.relX + oX, in.relY + oY, in.relZ + oZ);
                    pose.mulPose(camera.orientation);

                    collector.submitCustomGeometry(
                            pose,
                            WeaponRenderTypes.ROCKET_FLAME,
                            (p, tess) -> {
                                Vertices.emit(
                                        tess, p, scale, -scale, 0, color, 1, 1, LIGHT, 0F, 1F, 0F);
                                Vertices.emit(
                                        tess, p, scale, scale, 0, color, 1, 0, LIGHT, 0F, 1F, 0F);
                                Vertices.emit(
                                        tess, p, -scale, scale, 0, color, 0, 0, LIGHT, 0F, 1F, 0F);
                                Vertices.emit(
                                        tess, p, -scale, -scale, 0, color, 0, 1, LIGHT, 0F, 1F, 0F);
                            });
                    pose.popPose();
                }
            }
        }
    }
}
