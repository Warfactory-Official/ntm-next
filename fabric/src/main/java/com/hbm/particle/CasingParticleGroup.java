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
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;

public class CasingParticleGroup extends ParticleGroup<ParticleSpentCasing> {

    public CasingParticleGroup(ParticleEngine engine) {
        super(engine);
    }

    @Override
    public ParticleGroupRenderState extractRenderState(
            Frustum frustum, Camera camera, float partialTicks) {
        List<Instance> out = new ArrayList<>(this.particles.size());
        Vec3 cam = camera.position();
        float playerYaw =
                Minecraft.getInstance().player != null
                        ? Minecraft.getInstance().player.getViewYRot(partialTicks)
                        : 0F;

        for (ParticleSpentCasing casing : this.particles) {
            Instance in = new Instance();
            double pX = casing.interpX(partialTicks);
            double pY = casing.interpY(partialTicks);
            double pZ = casing.interpZ(partialTicks);
            in.relX = pX - cam.x;
            in.relY = pY - cam.y;
            in.relZ = pZ - cam.z;
            in.yaw = casing.interpYaw(partialTicks);
            in.pitch = casing.interpPitch(partialTicks);
            in.config = casing.config;
            in.light = casing.lightCoords(partialTicks);
            in.heightQuarter = casing.height() / 4F;
            in.scaleLift = casing.config.getScaleY() * 0.01F;

            if (casing.hasSmoke()) {

                casing.dragSmokeNodes(pX, pY, pZ);

                double timeAlpha = 1D - (double) casing.age() / (double) casing.maxSmokeGen();
                List<ParticleSpentCasing.SmokeNode> nodes = casing.smokeNodes;
                float[] smoke = new float[nodes.size() * 4];
                for (int i = 0; i < nodes.size(); i++) {
                    ParticleSpentCasing.SmokeNode node = nodes.get(i);
                    smoke[i * 4] = (float) node.x;
                    smoke[i * 4 + 1] = (float) node.y;
                    smoke[i * 4 + 2] = (float) node.z;
                    smoke[i * 4 + 3] = (float) (node.alpha * timeAlpha);
                }
                in.smoke = smoke;

                float scale = in.config.getScaleX() * 0.5F * ParticleSpentCasing.dScale;
                Vec3 vec = new Vec3(scale, 0, 0).yRot((float) Math.toRadians(-playerYaw));
                in.smokeVecX = (float) vec.x;
                in.smokeVecZ = (float) vec.z;
            }

            out.add(in);
        }

        return new State(out);
    }

    private static final class Instance {
        double relX, relY, relZ;
        float yaw, pitch;
        SpentCasing config;
        int light;

        float[] smoke;
        float smokeVecX, smokeVecZ;
        float heightQuarter;
        float scaleLift;
    }

    private record State(List<Instance> instances) implements ParticleGroupRenderState {

        @Override
        public void submit(SubmitNodeCollector collector, CameraRenderState camera) {
            PoseStack pose = new PoseStack();

            for (Instance in : instances) {
                pose.pushPose();
                pose.translate(in.relX, in.relY - in.heightQuarter + in.scaleLift, in.relZ);
                pose.scale(
                        ParticleSpentCasing.dScale,
                        ParticleSpentCasing.dScale,
                        ParticleSpentCasing.dScale);
                pose.mulPose(Axis.YP.rotationDegrees(180 - in.yaw));
                pose.mulPose(Axis.XP.rotationDegrees(-in.pitch));
                pose.scale(in.config.getScaleX(), in.config.getScaleY(), in.config.getScaleZ());

                Instance fin = in;

                collector.submitCustomGeometry(
                        pose,
                        RenderTypes.entityCutoutCull(ResourceManager.casings_tex),
                        (p, buffer) -> {
                            int[] parts = fin.config.getType().partIds();
                            for (int index = 0; index < parts.length; index++) {
                                int col = fin.config.getColors()[index];
                                ResourceManager.casings.renderPart(
                                        p, buffer, fin.light, 0xFF000000 | col, parts[index]);
                            }
                        });
                pose.popPose();

                if (in.smoke != null && in.smoke.length >= 8) {
                    pose.pushPose();
                    pose.translate(in.relX, in.relY - in.heightQuarter, in.relZ);
                    float[] nodes = in.smoke;
                    float vx = in.smokeVecX;
                    float vz = in.smokeVecZ;
                    int edge = ARGB.colorFromFloat(0F, 1F, 1F, 1F);
                    int light = in.light;
                    collector.submitCustomGeometry(
                            pose,
                            WeaponRenderTypes.SMOKE_DEPTH,
                            (p, tess) -> {
                                for (int i = 0; i + 7 < nodes.length; i += 4) {
                                    float nx = nodes[i],
                                            ny = nodes[i + 1],
                                            nz = nodes[i + 2],
                                            na = nodes[i + 3];
                                    float px = nodes[i + 4],
                                            py = nodes[i + 5],
                                            pz = nodes[i + 6],
                                            pa = nodes[i + 7];

                                    int near = ARGB.colorFromFloat(na, 1F, 1F, 1F);
                                    int far = ARGB.colorFromFloat(pa, 1F, 1F, 1F);

                                    Vertices.emit(
                                            tess, p, nx, ny, nz, near, 0F, 0F, light, 0F, 1F, 0F);
                                    Vertices.emit(
                                            tess, p, nx + vx, ny, nz + vz, edge, 0F, 0F, light, 0F,
                                            1F, 0F);
                                    Vertices.emit(
                                            tess, p, px + vx, py, pz + vz, edge, 0F, 0F, light, 0F,
                                            1F, 0F);
                                    Vertices.emit(
                                            tess, p, px, py, pz, far, 0F, 0F, light, 0F, 1F, 0F);

                                    Vertices.emit(
                                            tess, p, nx, ny, nz, near, 0F, 0F, light, 0F, 1F, 0F);
                                    Vertices.emit(
                                            tess, p, nx - vx, ny, nz - vz, edge, 0F, 0F, light, 0F,
                                            1F, 0F);
                                    Vertices.emit(
                                            tess, p, px - vx, py, pz - vz, edge, 0F, 0F, light, 0F,
                                            1F, 0F);
                                    Vertices.emit(
                                            tess, p, px, py, pz, far, 0F, 0F, light, 0F, 1F, 0F);
                                }
                            });
                    pose.popPose();
                }
            }
        }
    }
}
