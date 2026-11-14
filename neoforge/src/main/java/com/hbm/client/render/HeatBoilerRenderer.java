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

import com.hbm.blockentity.machine.HeatBoilerBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.util.Facing;
import com.hbm.util.GameTime;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

public class HeatBoilerRenderer
        implements BlockEntityRenderer<HeatBoilerBlockEntity, HeatBoilerRenderer.State>,
                ConcurrentRenderStateExtraction {

    private final HFRWavefrontObject model;
    private final HFRWavefrontObject burstModel;
    private final RenderType bodyType;
    private final RenderType burstType;

    public HeatBoilerRenderer() {
        this.model = ResourceManager.boiler;
        this.burstModel = ResourceManager.boiler_burst;
        this.bodyType = RenderTypes.entityCutoutCull(ResourceManager.boiler_tex);
        this.burstType = WorldRenderPipeline.oneSidedCutout(ResourceManager.boiler_tex);
    }

    private static float facingYaw(Direction facing) {
        return Facing.yaw(facing, 180);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public AABB getRenderBoundingBox(HeatBoilerBlockEntity be) {
        BlockPos pos = be.getBlockPos();
        return new AABB(
                pos.getX() - 1.0625,
                pos.getY(),
                pos.getZ() - 1.0625,
                pos.getX() + 2.0625,
                pos.getY() + 4.0625,
                pos.getZ() + 2.0625);
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
            HeatBoilerBlockEntity be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.yaw = facingYaw(be.getBlockState().getValue(MultiblockCoreBlock.FACING));
        state.exploded = be.hasExploded;
        FluidTankNTM steam = be.tanks[1];
        state.breathing = !be.hasExploded && steam.getFill() > steam.getMaxFill() * 0.9;
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.mulPose((Quaternionfc) Axis.YP.rotationDegrees(s.yaw));

        final HFRWavefrontObject boiler = this.model;
        final HFRWavefrontObject burst = this.burstModel;
        if (!s.exploded) {
            if (s.breathing) {
                double sine = Math.sin(GameTime.now() / 50D % (Math.PI * 2)) * 0.01D;
                ps.scale((float) (1 - sine), (float) (1 + sine), (float) (1 - sine));
            }
            col.submitCustomGeometry(
                    ps, bodyType, (pose, buffer) -> boiler.render(pose, buffer, light, -1));
        } else {
            col.submitCustomGeometry(
                    ps, burstType, (pose, buffer) -> burst.render(pose, buffer, light, -1));
        }

        ps.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public float yaw;
        public boolean exploded;
        public boolean breathing;
    }
}
