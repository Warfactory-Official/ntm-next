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

package com.hbm.client.render;

import com.hbm.blockentity.machine.oil.PumpjackBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.main.ResourceManager;
import com.hbm.render.util.Vertices;
import com.hbm.util.Facing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

public class PumpjackRenderer
        implements BlockEntityRenderer<PumpjackBlockEntity, PumpjackRenderer.State>,
                ConcurrentRenderStateExtraction {
    private static final int ROTOR = ResourceManager.pumpjack.partId("Rotor");
    private static final int HEAD = ResourceManager.pumpjack.partId("Head");
    private static final int CARRIAGE = ResourceManager.pumpjack.partId("Carriage");

    private static final RenderType LINKAGE_TYPE = WorldRenderPipeline.UNTEXTURED;

    private final RenderType bodyType;

    public PumpjackRenderer() {
        this.bodyType = RenderTypes.entitySolid(ResourceManager.pumpjack_tex);
    }

    private static void renderLinkage(
            PoseStack.Pose pose, VertexConsumer buf, float rotation, int light) {
        float backAngle = -(float) Math.sin(Math.toRadians(rotation)) * 0.25F;
        double backY = -2D * Mth.sin(backAngle), backZ = -2D * Mth.cos(backAngle);
        float rotorAngle = -(float) Math.toRadians(rotation - 90);
        double rotorY = .5D * Mth.cos(rotorAngle), rotorZ = -.5D * Mth.sin(rotorAngle);

        int gray = ARGB.colorFromFloat(1F, 0.5F, 0.5F, 0.5F);
        for (int i = -1; i <= 1; i += 2) {
            float xi = 0.53125F * i;
            double y1 = 1.5F + rotorY, z1 = -5.5F + rotorZ;
            double y2 = 3.5F + backY, z2 = -3.5F + backZ;
            corner(buf, pose, xi, y1, z1 - 0.0625F, gray, light);
            corner(buf, pose, xi, y1, z1 + 0.0625F, gray, light);
            corner(buf, pose, xi, y2, z2 + 0.0625F, gray, light);
            corner(buf, pose, xi, y2, z2 - 0.0625F, gray, light);
        }

        int dark = ARGB.colorFromFloat(1F, 0.2F, 0.2F, 0.2F);
        float pd = 0.03125F;
        float width = 0.25F;
        float height = -(float) Math.sin(Math.toRadians(rotation));
        float cutlet = 360F / 32F;

        for (int i = -1; i <= 1; i += 2) {
            float pRot = -(float) (Math.sin(Math.toRadians(rotation)) * 0.25);

            float frontCos = Mth.cos(pRot), frontSin = Mth.sin(pRot);
            double frontY = frontSin, frontZ = frontCos;

            float dist = 0.03125F;
            double radiusY = (2.5F + dist) * (double) frontSin;
            double radiusZ = (2.5F + dist) * (double) frontCos;
            float startAngle = -(float) Math.toRadians(cutlet * -3);
            float startCos = Mth.cos(startAngle), startSin = Mth.sin(startAngle);
            double turnedY = radiusY * startCos + radiusZ * startSin;
            radiusZ = radiusZ * startCos - radiusY * startSin;
            radiusY = turnedY;
            float stepAngle = -(float) Math.toRadians(cutlet);
            float stepCos = Mth.cos(stepAngle), stepSin = Mth.sin(stepAngle);

            for (int j = 0; j < 4; j++) {
                double sumY1 = frontY + radiusY;
                double sumZ1 = radiusY < 0 ? 3.5F + dist * 0.5F : frontZ + radiusZ;

                turnedY = radiusY * stepCos + radiusZ * stepSin;
                radiusZ = radiusZ * stepCos - radiusY * stepSin;
                radiusY = turnedY;

                double sumY2 = frontY + radiusY;
                double sumZ2 = radiusY < 0 ? 3.5F + dist * 0.5F : frontZ + radiusZ;

                float xL = (width - pd) * i, xR = (width + pd) * i;
                corner(buf, pose, xL, 3.5F + sumY1, -3.5F + sumZ1, dark, light);
                corner(buf, pose, xR, 3.5F + sumY1, -3.5F + sumZ1, dark, light);
                corner(buf, pose, xR, 3.5F + sumY2, -3.5F + sumZ2, dark, light);
                corner(buf, pose, xL, 3.5F + sumY2, -3.5F + sumZ2, dark, light);
            }

            double sumY = frontY + radiusY;
            double sumZ = radiusY < 0 ? 3.5F + dist * 0.5F : frontZ + radiusZ;
            float xR = (width + pd) * i, xL = (width - pd) * i;
            corner(buf, pose, xR, 3.5F + sumY, -3.5F + sumZ, dark, light);
            corner(buf, pose, xL, 3.5F + sumY, -3.5F + sumZ, dark, light);
            corner(buf, pose, xL, 2F + height, 0F, dark, light);
            corner(buf, pose, xR, 2F + height, 0F, dark, light);
        }

        float p = 0.03125F;
        corner(buf, pose, p, height + 1.5F, p, dark, light);
        corner(buf, pose, -p, height + 1.5F, -p, dark, light);
        corner(buf, pose, -p, 0.75F, -p, dark, light);
        corner(buf, pose, p, 0.75F, p, dark, light);
        corner(buf, pose, -p, height + 1.5F, p, dark, light);
        corner(buf, pose, p, height + 1.5F, -p, dark, light);
        corner(buf, pose, p, 0.75F, -p, dark, light);
        corner(buf, pose, -p, 0.75F, p, dark, light);
    }

    private static void corner(
            VertexConsumer buf,
            PoseStack.Pose pose,
            double x,
            double y,
            double z,
            int color,
            int light) {
        Vertices.emit(buf, pose, (float) x, (float) y, (float) z, color, 0F, 0F, light, 0F, 0F, 1F);
    }

    private static float facingYaw(Direction facing) {
        return Facing.yaw(facing, 90);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public void extractRenderState(
            PumpjackBlockEntity be,
            State state,
            float pt,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, pt, cameraPosition, breakProgress);
        state.facing = MultiblockCoreBlock.coreFacing(be.getBlockState());
        state.rotation = Mth.lerp(pt, be.prevAngle, be.angle);
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;
        float rotation = s.rotation;

        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.rotate((Quaternionfc) Axis.YP.rotationDegrees(facingYaw(s.facing)));

        ps.pushPose();
        ps.translate(0, 1.5, -5.5);
        ps.rotate((Quaternionfc) Axis.XP.rotationDegrees(rotation - 90F));
        ps.translate(0, -1.5, 5.5);
        part(col, ps, light, ROTOR);
        ps.popPose();

        ps.pushPose();
        ps.translate(0, 3.5, -3.5);
        ps.rotate(
                (Quaternionfc)
                        Axis.XP.rotationDegrees(
                                (float) Math.toDegrees(Math.sin(Math.toRadians(rotation)))
                                        * 0.25F));
        ps.translate(0, -3.5, 3.5);
        part(col, ps, light, HEAD);
        ps.popPose();

        ps.pushPose();
        ps.translate(0, -Math.sin(Math.toRadians(rotation)), 0);
        part(col, ps, light, CARRIAGE);
        ps.popPose();

        col.submitCustomGeometry(
                ps, LINKAGE_TYPE, (pose, buffer) -> renderLinkage(pose, buffer, rotation, light));

        ps.popPose();
    }

    private void part(SubmitNodeCollector col, PoseStack ps, int light, int name) {
        col.submitCustomGeometry(
                ps,
                bodyType,
                (pose, buffer) ->
                        ResourceManager.pumpjack.renderPart(pose, buffer, light, -1, name));
    }

    public static final class State extends BlockEntityRenderState {
        public Direction facing = Direction.NORTH;
        public float rotation;
    }
}
