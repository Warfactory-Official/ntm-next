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

import com.hbm.blockentity.machine.LPW2BlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.main.ResourceManager;
import com.hbm.util.Facing;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class LPW2Renderer
        implements BlockEntityRenderer<LPW2BlockEntity, LPW2Renderer.State>,
                ConcurrentRenderStateExtraction {
    private static final RenderType BODY = RenderTypes.entityCutoutCull(ResourceManager.lpw2_tex);
    private static final RenderType SCREEN =
            RenderTypes.entityCutoutCull(ResourceManager.lpw2_error_tex);

    public static final class State extends HybridBlockEntityRenderState {
        public LPW2Poses poses;
        private boolean drawMoving;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            LPW2BlockEntity blockEntity,
            State state,
            float partialTick,
            Vec3 cameraPosition,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                blockEntity, state, partialTick, cameraPosition, breakProgress);
        state.drawMoving =
                state.forceNative
                        || !VisualizationManager.supportsVisualization(blockEntity.getLevel());
        if (state.drawMoving) {
            if (state.poses == null) {
                state.poses = new LPW2Poses();
            }
            float worldTime = LPW2Poses.getAnimationTime(blockEntity.getLevel(), partialTick);
            float yaw =
                    Facing.yaw(
                            blockEntity.getBlockState().getValue(MultiblockCoreBlock.FACING), 90);
            state.poses.update(yaw, worldTime, worldTime);
        }
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState cameraState) {
        if (!state.drawMoving) {
            return;
        }
        int light = state.lightCoords;
        for (int slot = 0; slot < LPW2Poses.SCREEN; slot++) {
            int part = Parts.IDS[slot];
            poseStack.pushPose();
            poseStack.mulPose(state.poses.getPartPose(slot));
            collector.submitCustomGeometry(
                    poseStack,
                    BODY,
                    (pose, vertices) ->
                            ResourceManager.lpw2.renderPart(pose, vertices, light, -1, part));
            poseStack.popPose();
        }
        int screen = Parts.IDS[LPW2Poses.SCREEN];
        float scroll = state.poses.getScroll();
        poseStack.pushPose();
        poseStack.mulPose(state.poses.getPartPose(LPW2Poses.SCREEN));
        collector.submitCustomGeometry(
                poseStack,
                SCREEN,
                (pose, vertices) ->
                        ResourceManager.lpw2.renderPart(
                                pose, vertices, light, -1, screen, 1, 1, 0, scroll));
        poseStack.popPose();
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
    public AABB getRenderBoundingBox(LPW2BlockEntity blockEntity) {
        var pos = blockEntity.getBlockPos();
        return new AABB(
                pos.getX() - 10,
                pos.getY() - 0.0625,
                pos.getZ() - 10,
                pos.getX() + 11,
                pos.getY() + 7.0625,
                pos.getZ() + 11);
    }

    private static final class Parts {
        private static final int[] IDS = new int[LPW2Poses.GROUPS.length];

        static {
            for (int slot = 0; slot < IDS.length; slot++) {
                IDS[slot] = ResourceManager.lpw2.partId(LPW2Poses.GROUPS[slot]);
            }
        }
    }
}
