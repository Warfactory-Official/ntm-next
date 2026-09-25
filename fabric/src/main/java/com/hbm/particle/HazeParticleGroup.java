// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.particle;

import com.hbm.client.render.WeaponRenderTypes;
import com.hbm.render.util.Vertices;
import com.mojang.blaze3d.vertex.PoseStack;
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
import org.joml.Vector3f;

public class HazeParticleGroup extends ParticleGroup<ParticleHaze> {

    private static final int LIGHT = LightCoordsUtil.pack(15, 0);

    public HazeParticleGroup(ParticleEngine engine) {
        super(engine);
    }

    @Override
    public ParticleGroupRenderState extractRenderState(
            Frustum frustum, Camera camera, float partialTicks) {
        List<Instance> out = new ArrayList<>(this.particles.size());
        Vec3 cam = camera.position();

        for (ParticleHaze haze : this.particles) {
            out.add(
                    new Instance(
                            haze.interpX(partialTicks) - cam.x,
                            haze.interpY(partialTicks) - cam.y,
                            haze.interpZ(partialTicks) - cam.z,
                            haze.alpha(),
                            haze.scale()));
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

                int color = ARGB.colorFromFloat(in.alpha, 1F, 1F, 1F);
                collector.submitCustomGeometry(
                        pose,
                        WeaponRenderTypes.cloudSeparate(ParticleHaze.TEXTURE),
                        (p, tess) -> {
                            for (ParticleHaze.Quad quad : ParticleHaze.QUADS) {
                                float s = quad.sizeFactor() * in.scale;
                                float ox = quad.offsetX(), oy = quad.offsetY(), oz = quad.offsetZ();

                                Vector3f c1 = camera.orientation.transform(new Vector3f(-s, -s, 0));
                                Vector3f c2 = camera.orientation.transform(new Vector3f(-s, s, 0));
                                Vector3f c3 = camera.orientation.transform(new Vector3f(s, s, 0));
                                Vector3f c4 = camera.orientation.transform(new Vector3f(s, -s, 0));

                                Vertices.emit(
                                        tess, p, ox + c4.x, oy + c4.y, oz + c4.z, color, 1, 1,
                                        LIGHT, 0F, 1F, 0F);
                                Vertices.emit(
                                        tess, p, ox + c3.x, oy + c3.y, oz + c3.z, color, 1, 0,
                                        LIGHT, 0F, 1F, 0F);
                                Vertices.emit(
                                        tess, p, ox + c2.x, oy + c2.y, oz + c2.z, color, 0, 0,
                                        LIGHT, 0F, 1F, 0F);
                                Vertices.emit(
                                        tess, p, ox + c1.x, oy + c1.y, oz + c1.z, color, 0, 1,
                                        LIGHT, 0F, 1F, 0F);
                            }
                        });
                pose.popPose();
            }
        }
    }
}
