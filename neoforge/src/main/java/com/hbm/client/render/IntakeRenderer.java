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

import com.hbm.blockentity.machine.IntakeBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.main.ResourceManager;
import com.hbm.util.Facing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

public class IntakeRenderer
        implements BlockEntityRenderer<IntakeBlockEntity, IntakeRenderer.State>,
                ConcurrentRenderStateExtraction {
    private static final int FAN = ResourceManager.intake.partId("Fan");

    private final RenderType bodyType;

    public IntakeRenderer() {
        this.bodyType = WorldRenderPipeline.oneSidedCutout(ResourceManager.intake_tex);
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
    public AABB getRenderBoundingBox(IntakeBlockEntity be) {
        BlockPos pos = be.getBlockPos();
        return new AABB(
                pos.getX() - 1,
                pos.getY(),
                pos.getZ() - 1,
                pos.getX() + 2,
                pos.getY() + 1,
                pos.getZ() + 2);
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public void extractRenderState(
            IntakeBlockEntity be,
            State state,
            float pt,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, pt, cameraPosition, breakProgress);
        state.facing = MultiblockCoreBlock.coreFacing(be.getBlockState());
        state.fan = Mth.lerp(pt, be.lastRotor, be.rotor);
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;

        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.mulPose((Quaternionfc) Axis.YP.rotationDegrees(facingYaw(s.facing)));
        ps.translate(-0.5, 0.0, 0.5);

        ps.pushPose();
        ps.mulPose((Quaternionfc) Axis.YP.rotationDegrees(-s.fan));
        col.submitCustomGeometry(
                ps,
                bodyType,
                (pose, buffer) -> ResourceManager.intake.renderPart(pose, buffer, light, -1, FAN));
        ps.popPose();

        ps.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public Direction facing = Direction.NORTH;
        public float fan;
    }
}
