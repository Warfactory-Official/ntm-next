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

import com.hbm.blockentity.turret.TurretHowardDamagedBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
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
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

public class TurretHowardDamagedRenderer
        implements BlockEntityRenderer<
                        TurretHowardDamagedBlockEntity, TurretHowardDamagedRenderer.State>,
                ConcurrentRenderStateExtraction {
    private static final int PART_CARRIAGE =
            ResourceManager.turret_howard_damaged.partId("Carriage");
    private static final int PART_BODY = ResourceManager.turret_howard_damaged.partId("Body");
    private static final int PART_BARRELS_TOP =
            ResourceManager.turret_howard_damaged.partId("BarrelsTop");
    private static final int PART_BARRELS_BOTTOM =
            ResourceManager.turret_howard_damaged.partId("BarrelsBottom");

    private static final RenderType CARRIAGE =
            RenderTypes.entityCutoutCull(ResourceManager.turret_carriage_ciws_rusted_tex);
    private static final RenderType BODY =
            RenderTypes.entityCutoutCull(ResourceManager.turret_howard_rusted_tex);
    private static final RenderType BARRELS =
            RenderTypes.entityCutoutCull(ResourceManager.turret_howard_barrels_rusted_tex);

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
            TurretHowardDamagedBlockEntity be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.facing = MultiblockCoreBlock.coreFacing(be.getBlockState());
        state.yaw =
                (float)
                        (-Math.toDegrees(Mth.lerp(partialTicks, be.lastRotationYaw, be.rotationYaw))
                                - 90D);
        state.pitch =
                (float)
                        Math.toDegrees(
                                Mth.lerp(partialTicks, be.lastRotationPitch, be.rotationPitch));
        state.spin = Mth.lerp(partialTicks, be.lastSpin, be.spin);
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        double offsetX =
                state.facing == Direction.NORTH || state.facing == Direction.WEST ? 1D : 0D;
        double offsetZ =
                state.facing == Direction.NORTH || state.facing == Direction.EAST ? 1D : 0D;
        poseStack.pushPose();
        poseStack.translate(offsetX, 0D, offsetZ);
        poseStack.rotate((Quaternionfc) Axis.YP.rotationDegrees(state.yaw));
        collector.submitCustomGeometry(
                poseStack,
                CARRIAGE,
                (pose, buffer) ->
                        ResourceManager.turret_howard_damaged.renderPart(
                                pose, buffer, state.lightCoords, -1, PART_CARRIAGE));
        poseStack.translate(0D, 2.25D, 0D);
        poseStack.rotate((Quaternionfc) Axis.ZP.rotationDegrees(state.pitch));
        poseStack.translate(0D, -2.25D, 0D);
        collector.submitCustomGeometry(
                poseStack,
                BODY,
                (pose, buffer) ->
                        ResourceManager.turret_howard_damaged.renderPart(
                                pose, buffer, state.lightCoords, -1, PART_BODY));
        poseStack.pushPose();
        poseStack.translate(0D, 2.5D, 0D);
        poseStack.rotate((Quaternionfc) Axis.XN.rotationDegrees(state.spin));
        poseStack.translate(0D, -2.5D, 0D);
        collector.submitCustomGeometry(
                poseStack,
                BARRELS,
                (pose, buffer) ->
                        ResourceManager.turret_howard_damaged.renderPart(
                                pose, buffer, state.lightCoords, -1, PART_BARRELS_TOP));
        poseStack.popPose();
        collector.submitCustomGeometry(
                poseStack,
                BARRELS,
                (pose, buffer) ->
                        ResourceManager.turret_howard_damaged.renderPart(
                                pose, buffer, state.lightCoords, -1, PART_BARRELS_BOTTOM));
        poseStack.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        float yaw;
        float pitch;
        float spin;
    }
}
