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

import com.hbm.blockentity.machine.CargoElevatorBlockEntity;
import com.hbm.client.model.Meshes;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.util.RegistryUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class CargoElevatorRenderer
        implements BlockEntityRenderer<CargoElevatorBlockEntity, CargoElevatorRenderer.State>,
                ConcurrentRenderStateExtraction {

    private final HFRWavefrontObject mesh =
            Meshes.load(RegistryUtil.id("models/machines/elevator.obj"));
    private final int platform = mesh.partId("Platform");
    private final int piston = mesh.partId("Piston");
    private final RenderType type =
            RenderTypes.entityCutoutCull(
                    RegistryUtil.id("textures/block/models/machines/elevator.png"));

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(CargoElevatorBlockEntity elevator) {
        BlockPos pos = elevator.getBlockPos();
        return new AABB(
                pos.getX() - 1,
                pos.getY() - 0.25,
                pos.getZ() - 1,
                pos.getX() + 2,
                pos.getY() + elevator.height() + 1,
                pos.getZ() + 2);
    }

    @Override
    public void extractRenderState(
            CargoElevatorBlockEntity elevator,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(
                elevator, state, partialTicks, cameraPosition, breaking);
        state.extension = elevator.renderExtension(partialTicks);
        state.renderPlatform = elevator.platformVisible();
    }

    @Override
    public void submit(
            State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.renderPlatform) return;
        int light = state.lightCoords;
        pose.pushPose();
        pose.translate(0.5, state.extension, 0.5);
        collector.submitCustomGeometry(
                pose,
                type,
                (matrix, vertices) -> mesh.renderPart(matrix, vertices, light, -1, platform));
        for (int i = 0; i < state.extension + 1; i++) {
            collector.submitCustomGeometry(
                    pose,
                    type,
                    (matrix, vertices) -> mesh.renderPart(matrix, vertices, light, -1, piston));
            pose.translate(0, -1, 0);
        }
        pose.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public double extension;
        public boolean renderPlatform;
    }
}
