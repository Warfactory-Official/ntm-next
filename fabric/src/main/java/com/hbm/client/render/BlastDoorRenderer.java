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

import com.hbm.blockentity.door.BlastDoorBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

public class BlastDoorRenderer
        implements BlockEntityRenderer<BlastDoorBlockEntity, BlastDoorRenderer.State>,
                ConcurrentRenderStateExtraction {
    private static final float EXTEND = 5.0F;
    private static final int SLIDERS = 4;

    private final Map<Identifier, RenderType> types = new HashMap<>();

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
            BlastDoorBlockEntity door,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                door, state, partialTicks, cameraPosition, breakProgress);
        state.facing = door.getBlockState().getValue(MultiblockCoreBlock.FACING);
        state.ramp = ramp(door, partialTicks);
    }

    private static float ramp(BlastDoorBlockEntity door, float partialTicks) {
        if (door.phase == BlastDoorBlockEntity.RETRACTED) return 0.0F;
        if (door.phase != BlastDoorBlockEntity.TRAVELLING) return EXTEND;
        float travelled =
                Mth.clamp(
                        (door.elapsedTicks + partialTicks) / BlastDoorBlockEntity.MOTION_TICKS,
                        0.0F,
                        1.0F);
        return EXTEND * (door.retracting ? 1.0F - travelled : travelled);
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        int light = state.lightCoords;
        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.rotate((Quaternionfc) Axis.YP.rotationDegrees(180.0F));
        if (state.facing.getAxis() == Direction.Axis.Z)
            poseStack.rotate((Quaternionfc) Axis.YP.rotationDegrees(90.0F));

        poseStack.translate(0.0, 5.0 - state.ramp, 0.0);
        part(
                poseStack,
                collector,
                light,
                ResourceManager.blast_door_tooth,
                ResourceManager.blast_door_tooth_tex);

        for (int i = 1; i <= SLIDERS; i++) {
            if (state.ramp <= i) break;
            if (i > 1) poseStack.translate(0.0, 1.0, 0.0);
            part(
                    poseStack,
                    collector,
                    light,
                    ResourceManager.blast_door_slider,
                    ResourceManager.blast_door_slider_tex);
        }

        poseStack.popPose();
    }

    private void part(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int light,
            HFRWavefrontObject model,
            Identifier texture) {
        RenderType type = types.computeIfAbsent(texture, RenderTypes::entityCutout);
        collector.submitCustomGeometry(
                poseStack, type, (pose, buffer) -> model.render(pose, buffer, light, -1));
    }

    public static final class State extends BlockEntityRenderState {
        public Direction facing = Direction.NORTH;
        public float ramp = EXTEND;
    }
}
