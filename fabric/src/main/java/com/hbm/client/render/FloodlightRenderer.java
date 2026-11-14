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

import com.hbm.blockentity.machine.FloodlightBlockEntity;
import com.hbm.blocks.machine.FloodlightBlock;
import com.hbm.main.ResourceManager;
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
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

public class FloodlightRenderer
        implements BlockEntityRenderer<FloodlightBlockEntity, FloodlightRenderer.State>,
                ConcurrentRenderStateExtraction {
    private static final int LIGHTS = ResourceManager.floodlight.partId("Lights");
    private static final int LAMPS = ResourceManager.floodlight.partId("Lamps");

    private static final RenderType BODY_TYPE =
            RenderTypes.entityCutoutCull(ResourceManager.floodlight_tex);
    private static final RenderType LAMPS_LIT_TYPE = FlatCutout.of(ResourceManager.floodlight_tex);

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
            FloodlightBlockEntity be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.facing = be.getBlockState().getValue(FloodlightBlock.FACING);
        state.rotation = be.rotation;
        state.isOn = be.isOn;
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        int meta = state.facing;

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        switch (meta) {
            case 0, 6 -> poseStack.rotate((Quaternionfc) Axis.XP.rotationDegrees(180F));
            case 1, 7 -> {}
            case 2 -> {
                poseStack.rotate((Quaternionfc) Axis.XP.rotationDegrees(90F));
                poseStack.rotate((Quaternionfc) Axis.ZP.rotationDegrees(180F));
            }
            case 3 -> poseStack.rotate((Quaternionfc) Axis.XP.rotationDegrees(90F));
            case 4 -> {
                poseStack.rotate((Quaternionfc) Axis.XP.rotationDegrees(90F));
                poseStack.rotate((Quaternionfc) Axis.ZP.rotationDegrees(90F));
            }
            case 5 -> {
                poseStack.rotate((Quaternionfc) Axis.XP.rotationDegrees(90F));
                poseStack.rotate((Quaternionfc) Axis.ZP.rotationDegrees(270F));
            }
            default -> throw new IllegalStateException("Unexpected floodlight facing: " + meta);
        }
        poseStack.translate(0.0, -0.5, 0.0);
        if (meta != 0 && meta != 1) poseStack.rotate((Quaternionfc) Axis.YP.rotationDegrees(90F));

        float rotation = state.rotation;
        if (meta == 0 || meta == 6) rotation -= 90F;
        if (meta == 1 || meta == 7) rotation += 90F;
        poseStack.translate(0.0, 0.5, 0.0);
        poseStack.rotate((Quaternionfc) Axis.ZP.rotationDegrees(rotation));
        poseStack.translate(0.0, -0.5, 0.0);

        int lightsLight = state.lightCoords;
        int lampsLight = state.isOn ? LightCoordsUtil.FULL_BRIGHT : state.lightCoords;
        int lampsColor = state.isOn ? -1 : 0xFF404040;
        collector.submitCustomGeometry(
                poseStack,
                BODY_TYPE,
                (pose, buffer) ->
                        ResourceManager.floodlight.renderPart(
                                pose, buffer, lightsLight, -1, LIGHTS));
        collector.submitCustomGeometry(
                poseStack,
                state.isOn ? LAMPS_LIT_TYPE : BODY_TYPE,
                (pose, buffer) ->
                        ResourceManager.floodlight.renderPart(
                                pose, buffer, lampsLight, lampsColor, LAMPS));
        poseStack.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public int facing = 1;
        public float rotation;
        public boolean isOn;
    }
}
