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
import com.hbm.particle.casing.Casing;
import com.hbm.particle.casing.CasingShape;
import com.hbm.particle.casing.CasingSmokeTrail;
import com.hbm.render.util.Vertices;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Arrays;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;

public class CasingParticleGroup extends ParticleGroup<CasingParticle> {

    private final Frame frame = new Frame();

    public CasingParticleGroup(ParticleEngine engine) {
        super(engine);
    }

    @Override
    public ParticleGroupRenderState extractRenderState(
            Frustum frustum, Camera camera, float partialTicks) {
        Vec3 cam = camera.position();
        Player player = Minecraft.getInstance().player;
        float viewYaw = player != null ? player.getViewYRot(partialTicks) * Mth.DEG_TO_RAD : 0F;
        float viewCos = Mth.cos(viewYaw);
        float viewSin = Mth.sin(viewYaw);
        frame.begin(cam.x, cam.y, cam.z, particles.size());

        for (CasingParticle casing : particles) {
            double x = casing.getRenderX(partialTicks);
            double y = casing.getRenderY(partialTicks);
            double z = casing.getRenderZ(partialTicks);
            int light = casing.getLight(partialTicks);
            frame.addMesh(
                    casing.getCasing(),
                    x,
                    y,
                    z,
                    casing.getRenderYaw(partialTicks),
                    casing.getRenderPitch(partialTicks),
                    light);
            if (casing.prepareSmoke(x, y, z, viewCos, viewSin, light))
                frame.addSmoke(casing.getSmokeTrail());
        }
        return frame;
    }

    private static final class Frame implements ParticleGroupRenderState {
        private static final int POSE_STRIDE = 5;
        private static final int EDGE = ARGB.colorFromFloat(0F, 1F, 1F, 1F);

        private final RenderType meshType =
                RenderTypes.entityCutoutCull(ResourceManager.casings_tex);
        private final int[][] parts = new int[CasingShape.values().length][];
        private final PoseStack origin = new PoseStack();
        private final PoseStack work = new PoseStack();
        private final Quaternionf turn = new Quaternionf();
        private final SubmitNodeCollector.CustomGeometryRenderer meshes = this::drawMeshes;
        private final SubmitNodeCollector.CustomGeometryRenderer smokes = this::drawSmoke;
        private double camX;
        private double camY;
        private double camZ;
        private Casing[] casings = new Casing[16];
        private float[] poses = new float[16 * POSE_STRIDE];
        private int[] lights = new int[16];
        private int meshCount;
        private CasingSmokeTrail[] trails = new CasingSmokeTrail[16];
        private int smokeCount;

        Frame() {
            for (CasingShape shape : CasingShape.values()) {
                parts[shape.ordinal()] = ResourceManager.casings.partIds(shape.parts());
            }
        }

        void begin(double x, double y, double z, int capacity) {
            camX = x;
            camY = y;
            camZ = z;
            if (casings.length < capacity) {
                int size = Math.max(capacity, casings.length * 2);
                casings = Arrays.copyOf(casings, size);
                poses = Arrays.copyOf(poses, size * POSE_STRIDE);
                lights = Arrays.copyOf(lights, size);
                trails = Arrays.copyOf(trails, size);
            }
        }

        void addMesh(
                Casing casing, double x, double y, double z, float yaw, float pitch, int light) {
            int at = meshCount * POSE_STRIDE;
            poses[at] = (float) (x - camX);
            poses[at + 1] = (float) (y - camY);
            poses[at + 2] = (float) (z - camZ);
            poses[at + 3] = yaw;
            poses[at + 4] = pitch;
            casings[meshCount] = casing;
            lights[meshCount] = light;
            meshCount++;
        }

        void addSmoke(CasingSmokeTrail trail) {
            trails[smokeCount++] = trail;
        }

        @Override
        public void submit(SubmitNodeCollector collector, CameraRenderState camera) {
            if (meshCount > 0) collector.submitCustomGeometry(origin, meshType, meshes);
            if (smokeCount > 0)
                collector.submitCustomGeometry(origin, WeaponRenderTypes.SMOKE_DEPTH, smokes);
        }

        @Override
        public void clear() {
            Arrays.fill(casings, 0, meshCount, null);
            Arrays.fill(trails, 0, smokeCount, null);
            meshCount = 0;
            smokeCount = 0;
        }

        private void drawMeshes(PoseStack.Pose base, VertexConsumer buffer) {
            for (int i = 0; i < meshCount; i++) {
                Casing casing = casings[i];
                int at = i * POSE_STRIDE;
                work.last().set(base);
                work.translate(poses[at], poses[at + 1] + casing.meshLift(), poses[at + 2]);
                work.scale(Casing.MESH_SCALE, Casing.MESH_SCALE, Casing.MESH_SCALE);
                work.mulPose(
                        (Quaternionfc) turn.rotationY((180F - poses[at + 3]) * Mth.DEG_TO_RAD));
                work.mulPose((Quaternionfc) turn.rotationX(-poses[at + 4] * Mth.DEG_TO_RAD));
                work.scale(casing.scaleX(), casing.scaleY(), casing.scaleZ());
                int[] ids = parts[casing.shape().ordinal()];
                for (int part = 0; part < ids.length; part++) {
                    ResourceManager.casings.renderPart(
                            work.last(),
                            buffer,
                            lights[i],
                            0xFF000000 | casing.tint(part),
                            ids[part]);
                }
            }
        }

        private void drawSmoke(PoseStack.Pose pose, VertexConsumer buffer) {
            for (int t = 0; t < smokeCount; t++) {
                CasingSmokeTrail trail = trails[t];
                float ox = (float) (trail.getFrameX() - camX);
                float oy = (float) (trail.getFrameY() - camY);
                float oz = (float) (trail.getFrameZ() - camZ);
                float vx = trail.getFrameHalfWidth();
                float vz = trail.getFrameHalfDepth();
                int light = trail.getFrameLight();
                float[] nodes = trail.getFrame();
                for (int i = 0, end = trail.getSegmentCount() * 4; i < end; i += 4) {
                    float ax = ox + nodes[i], ay = oy + nodes[i + 1], az = oz + nodes[i + 2];
                    float bx = ox + nodes[i + 4], by = oy + nodes[i + 5], bz = oz + nodes[i + 6];
                    int older = ARGB.colorFromFloat(nodes[i + 3], 1F, 1F, 1F);
                    int newer = ARGB.colorFromFloat(nodes[i + 7], 1F, 1F, 1F);

                    Vertices.emit(buffer, pose, ax, ay, az, older, 0F, 0F, light, 0F, 1F, 0F);
                    Vertices.emit(
                            buffer, pose, ax + vx, ay, az + vz, EDGE, 0F, 0F, light, 0F, 1F, 0F);
                    Vertices.emit(
                            buffer, pose, bx + vx, by, bz + vz, EDGE, 0F, 0F, light, 0F, 1F, 0F);
                    Vertices.emit(buffer, pose, bx, by, bz, newer, 0F, 0F, light, 0F, 1F, 0F);

                    Vertices.emit(buffer, pose, ax, ay, az, older, 0F, 0F, light, 0F, 1F, 0F);
                    Vertices.emit(
                            buffer, pose, ax - vx, ay, az - vz, EDGE, 0F, 0F, light, 0F, 1F, 0F);
                    Vertices.emit(
                            buffer, pose, bx - vx, by, bz - vz, EDGE, 0F, 0F, light, 0F, 1F, 0F);
                    Vertices.emit(buffer, pose, bx, by, bz, newer, 0F, 0F, light, 0F, 1F, 0F);
                }
            }
        }
    }
}
