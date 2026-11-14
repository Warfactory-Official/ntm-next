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

import com.hbm.blockentity.machine.SolarMirrorBlockEntity;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

public class SolarMirrorRenderer
        implements BlockEntityRenderer<SolarMirrorBlockEntity, SolarMirrorRenderer.State>,
                ConcurrentRenderStateExtraction {
    private static final int MIRROR = ResourceManager.solar_mirror.partId("Mirror");

    private final HFRWavefrontObject model;
    private final RenderType bodyType;

    public SolarMirrorRenderer() {
        this.model = ResourceManager.solar_mirror;
        this.bodyType = RenderTypes.entityCutoutCull(ResourceManager.solar_mirror_tex);
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
    public int getViewDistance() {
        return 256;
    }

    @Override
    public void extractRenderState(
            SolarMirrorBlockEntity be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);

        double dx = (double) be.targetX() - be.getBlockPos().getX();
        double dy = (double) be.targetY() - be.getBlockPos().getY();
        double dz = (double) be.targetZ() - be.getBlockPos().getZ();

        state.aimed = be.targetY() > be.getBlockPos().getY();
        if (state.aimed) {
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            state.pitch = (float) (Math.toDegrees(-Math.asin(dy / dist)) + 90);
            state.yaw = (float) (Math.toDegrees(-Math.atan2(dz, dx)) + 180);
        }
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.translate(0.0, 1.0, 0.0);
        if (s.aimed) {
            ps.mulPose((Quaternionfc) Axis.YP.rotationDegrees(s.yaw));
            ps.mulPose((Quaternionfc) Axis.ZP.rotationDegrees(s.pitch));
        }
        ps.translate(0.0, -1.0, 0.0);
        col.submitCustomGeometry(
                ps, bodyType, (pose, buffer) -> model.renderPart(pose, buffer, light, -1, MIRROR));
        ps.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public boolean aimed;
        public float pitch;
        public float yaw;
    }
}
