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

import com.hbm.blockentity.processing.ArcWelderBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.util.Facing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

public class ArcWelderRenderer
        implements BlockEntityRenderer<ArcWelderBlockEntity, ArcWelderRenderer.State>,
                ConcurrentRenderStateExtraction {

    private final ItemModelResolver itemModelResolver;

    public ArcWelderRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    private static float facingYaw(Direction facing) {
        return Facing.yaw(facing, 90);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public AABB getRenderBoundingBox(ArcWelderBlockEntity be) {
        BlockPos pos = be.getBlockPos();
        return new AABB(
                pos.getX() - 1,
                pos.getY(),
                pos.getZ() - 1,
                pos.getX() + 2,
                pos.getY() + 3,
                pos.getZ() + 2);
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
            ArcWelderBlockEntity be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.facing = MultiblockCoreBlock.coreFacing(be.getBlockState());
        state.arm =
                FramedItem.resolve(
                        itemModelResolver, state.item, be.display, be.getLevel(), null, 0);
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        if (state.item.isEmpty()) return;
        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.mulPose((Quaternionfc) Axis.YP.rotationDegrees(facingYaw(state.facing)));
        poseStack.translate(-0.34375, 1.125, 0.0);
        poseStack.mulPose((Quaternionfc) Axis.YP.rotationDegrees(90));
        poseStack.mulPose((Quaternionfc) Axis.XP.rotationDegrees(-90));
        poseStack.scale(1.5F, 1.5F, 1.5F);
        FramedItem.decoArm(poseStack, state.arm);
        state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public final ItemStackRenderState item = new ItemStackRenderState();
        public Direction facing = Direction.NORTH;
        public FramedItem.Arm arm = FramedItem.Arm.SPRITE;
    }
}
