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

import com.hbm.blockentity.machine.TapeDriveBlockEntity;
import com.hbm.blocks.machine.HorizontalMachineBlock;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.util.Facing;
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

public class TapeDriveRenderer
        implements BlockEntityRenderer<TapeDriveBlockEntity, TapeDriveRenderer.State>,
                ConcurrentRenderStateExtraction {
    private static final int DRIVE = ResourceManager.tape_drive.partId("Drive");
    private static final int LIGHT = ResourceManager.tape_drive.partId("Light");
    private static final int RED = 0xFFFF0000;
    private static final int ORANGE = 0xFFFFBF00;
    private static final int GREEN = 0xFF00FF00;

    private final HFRWavefrontObject model = ResourceManager.tape_drive;
    private final RenderType driveType =
            RenderTypes.entityCutoutCull(ResourceManager.tape_drive_tex);
    private final RenderType lightType = FlatCutout.culled(ResourceManager.white_tex);

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            TapeDriveBlockEntity be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.yaw = Facing.yaw(be.getBlockState().getValue(HorizontalMachineBlock.FACING), 90);
        for (int i = 0; i < TapeDriveBlockEntity.SLOT_COUNT; i++) {
            state.categories[i] = be.category(i);
        }
    }

    @Override
    public void submit(
            State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose((Quaternionfc) Axis.YP.rotationDegrees(state.yaw));
        for (int i = 0; i < TapeDriveBlockEntity.SLOT_COUNT; i++) {
            int category = state.categories[i];
            if (category == TapeDriveBlockEntity.VISUAL_EMPTY) continue;
            pose.pushPose();
            pose.translate(0, 0.25 - 0.5 * (i / 6), 0.3125 - (i % 6) * 0.125);
            collector.submitCustomGeometry(
                    pose,
                    driveType,
                    (p, buffer) -> model.renderPart(p, buffer, state.lightCoords, -1, DRIVE));
            int color =
                    switch (category) {
                        case TapeDriveBlockEntity.VISUAL_BLANK -> ORANGE;
                        case TapeDriveBlockEntity.VISUAL_FILLED -> GREEN;
                        default -> RED;
                    };
            collector.submitCustomGeometry(
                    pose,
                    lightType,
                    (p, buffer) ->
                            model.renderPart(p, buffer, LightCoordsUtil.FULL_BRIGHT, color, LIGHT));
            pose.popPose();
        }
        pose.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public float yaw;
        public final int[] categories = new int[TapeDriveBlockEntity.SLOT_COUNT];
    }
}
