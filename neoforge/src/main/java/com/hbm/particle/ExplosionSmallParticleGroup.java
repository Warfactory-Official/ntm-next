// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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

public class ExplosionSmallParticleGroup extends ParticleGroup<ParticleExplosionSmall> {

    public ExplosionSmallParticleGroup(ParticleEngine engine) {
        super(engine);
    }

    @Override
    public ParticleGroupRenderState extractRenderState(
            Frustum frustum, Camera camera, float partialTicks) {
        List<Instance> out = new ArrayList<>(this.particles.size());
        Vec3 cam = camera.position();

        for (ParticleExplosionSmall cloud : this.particles) {
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
                pose.mulPose(camera.orientation);
                pose.mulPose(Axis.ZP.rotationDegrees(in.roll));

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
