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

import com.hbm.blockentity.machine.storage.FluidTankBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.inventory.fluid.HazardSymbol;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluidProperty;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.util.Facing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

public class FluidTankRenderer
        implements BlockEntityRenderer<FluidTankBlockEntity, FluidTankRenderer.State>,
                ConcurrentRenderStateExtraction {
    private final Map<Identifier, RenderType> bodyTypes = new HashMap<>();
    private final Map<Fluid, Identifier> bodyTexCache = new ConcurrentHashMap<>();

    private static float facingYaw(Direction facing) {
        return Facing.yaw(facing, 180);
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
    public AABB getRenderBoundingBox(FluidTankBlockEntity be) {
        BlockPos pos = be.getBlockPos();
        return new AABB(
                pos.getX() - 2,
                pos.getY(),
                pos.getZ() - 2,
                pos.getX() + 3,
                pos.getY() + 3,
                pos.getZ() + 3);
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public void extractRenderState(
            FluidTankBlockEntity be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.yaw = facingYaw(be.getBlockState().getValue(MultiblockCoreBlock.FACING));
        state.damaged = be.isDamaged();
        Fluid type = be.tank.getTankType();
        state.bodyTexture = bodyTextureFor(type);

        NTMFluidProperty prop = type == null ? null : NTMFluidProperties.get(type);
        state.hasFluid = prop != null;
        if (prop != null) {
            state.health = prop.nfpaHealth();
            state.flame = prop.nfpaFlame();
            state.react = prop.nfpaReact();
            state.symbol = prop.symbol();
        }
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        RenderType bodyType =
                bodyTypes.computeIfAbsent(state.bodyTexture, RenderTypes::entityCutout);
        int light = state.lightCoords;
        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.rotate((Quaternionfc) Axis.YP.rotationDegrees(state.yaw));
        HFRWavefrontObject mesh = FluidTankMeshes.mesh(state.damaged);
        int part = FluidTankMeshes.tank(state.damaged);
        collector.submitCustomGeometry(
                poseStack,
                bodyType,
                (pose, buffer) -> mesh.renderPart(pose, buffer, light, -1, part));

        if (state.hasFluid) {
            int health = state.health, flame = state.flame, react = state.react;
            HazardSymbol symbol = state.symbol;
            for (int side = -1; side <= 1; side += 2) {
                poseStack.pushPose();
                poseStack.translate(0.25 * side, 0.5, 1.501 * side);
                poseStack.rotate((Quaternionfc) Axis.YP.rotationDegrees(-90F * side));
                poseStack.scale(1F, 0.375F, 0.375F);
                collector.submitCustomGeometry(
                        poseStack,
                        BarrelRenderer.RENDER_TYPE,
                        (pose, buffer) ->
                                BarrelRenderer.pront(
                                        pose, buffer, light, health, flame, react, symbol));
                poseStack.popPose();
            }
        }

        poseStack.popPose();
    }

    private Identifier bodyTextureFor(@Nullable Fluid fluid) {
        return fluid == null
                ? FluidTankMeshes.NONE
                : bodyTexCache.computeIfAbsent(fluid, FluidTankMeshes::texture);
    }

    public static final class State extends BlockEntityRenderState {
        public float yaw;
        public Identifier bodyTexture = FluidTankMeshes.NONE;
        public boolean damaged;
        public boolean hasFluid;
        public int health;
        public int flame;
        public int react;
        public HazardSymbol symbol = HazardSymbol.NONE;
    }
}
