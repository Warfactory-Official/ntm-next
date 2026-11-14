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

import com.hbm.blockentity.machine.IndustrialTurbineBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.inventory.fluid.NTMFluids;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

public class IndustrialTurbineRenderer
        implements BlockEntityRenderer<
                        IndustrialTurbineBlockEntity, IndustrialTurbineRenderer.State>,
                ConcurrentRenderStateExtraction {
    private static final int GAUGE = ResourceManager.industrial_turbine.partId("Gauge");
    private static final int FLYWHEEL = ResourceManager.industrial_turbine.partId("Flywheel");

    private final HFRWavefrontObject model;
    private final RenderType type;

    public IndustrialTurbineRenderer() {
        this.model = ResourceManager.industrial_turbine;
        this.type = RenderTypes.entityCutoutCull(ResourceManager.industrial_turbine_tex);
    }

    private static int steamTier(@Nullable Fluid type) {
        if (type == NTMFluids.HOTSTEAM) return 1;
        if (type == NTMFluids.SUPERHOTSTEAM) return 2;
        if (type == NTMFluids.ULTRAHOTSTEAM) return 3;
        return 0;
    }

    private static float facingYaw(Direction facing) {
        return Facing.yaw(facing, 180);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public AABB getRenderBoundingBox(IndustrialTurbineBlockEntity be) {
        BlockPos p = be.getBlockPos();
        return new AABB(
                p.getX() - 3, p.getY(), p.getZ() - 3, p.getX() + 4, p.getY() + 3, p.getZ() + 4);
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public void extractRenderState(
            IndustrialTurbineBlockEntity be,
            State s,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, s, partialTicks, cameraPosition, breakProgress);
        s.yaw = facingYaw(MultiblockCoreBlock.coreFacing(be.getBlockState()));
        s.gauge = 135F - steamTier(be.tanks[0].getTankType()) * 90F;
        s.rotor = Mth.lerp(partialTicks, be.lastRotor, be.rotor);
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.mulPose((Quaternionfc) Axis.YP.rotationDegrees(s.yaw));

        ps.pushPose();
        ps.translate(0.0, 1.5, 0.0);
        ps.mulPose((Quaternionfc) Axis.ZP.rotationDegrees(s.gauge));
        ps.translate(0.0, -1.5, 0.0);
        col.submitCustomGeometry(
                ps, type, (pose, buf) -> model.renderPart(pose, buf, light, -1, GAUGE));
        ps.popPose();

        ps.pushPose();
        ps.translate(0.0, 1.5, 0.0);
        ps.mulPose((Quaternionfc) Axis.ZN.rotationDegrees(s.rotor));
        ps.translate(0.0, -1.5, 0.0);
        col.submitCustomGeometry(
                ps, type, (pose, buf) -> model.renderPart(pose, buf, light, -1, FLYWHEEL));
        ps.popPose();

        ps.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public float yaw;
        public float gauge;
        public float rotor;
    }
}
