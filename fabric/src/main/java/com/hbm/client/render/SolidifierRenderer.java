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

import com.hbm.blockentity.machine.oil.SolidifierBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluidProperty;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
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
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

public class SolidifierRenderer
        implements BlockEntityRenderer<SolidifierBlockEntity, SolidifierRenderer.State>,
                ConcurrentRenderStateExtraction {
    private static final int FLUID = ResourceManager.solidifier.partId("Fluid");
    private static final int GLASS = ResourceManager.solidifier.partId("Glass");

    private final HFRWavefrontObject model;
    private final RenderType fluidType = FlatCutout.of(ResourceManager.white_tex);
    private final RenderType glassType = FlatTranslucent.of(ResourceManager.white_tex);

    public SolidifierRenderer() {
        this.model = ResourceManager.solidifier;
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
    public int getViewDistance() {
        return 256;
    }

    @Override
    public void extractRenderState(
            SolidifierBlockEntity be,
            State state,
            float pt,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, pt, cameraPosition, breakProgress);
        state.facing = MultiblockCoreBlock.coreFacing(be.getBlockState());
        FluidTankNTM tank = be.tank;
        state.fluidFrac = tank.getMaxFill() > 0 ? (double) tank.getFill() / tank.getMaxFill() : 0D;
        NTMFluidProperty prop = tank.getFill() > 0 ? NTMFluidProperties.get(tank.getFluid()) : null;
        state.fluidColor = prop != null ? prop.colorARGB() : 0;
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.rotate((Quaternionfc) Axis.YP.rotationDegrees(facingYaw(s.facing)));

        if (s.fluidColor != 0 && s.fluidFrac > 0) {
            ps.pushPose();
            ps.translate(0, 1.25, 0);
            ps.scale(1F, (float) s.fluidFrac, 1F);
            ps.translate(0, -1.25, 0);
            int color = s.fluidColor;
            col.submitCustomGeometry(
                    ps,
                    fluidType,
                    (pose, buffer) -> model.renderPart(pose, buffer, light, color, FLUID));
            ps.popPose();
        }

        col.submitCustomGeometry(
                ps,
                glassType,
                (pose, buffer) -> model.renderPart(pose, buffer, light, 0x26BFFFFF, GLASS));

        ps.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public Direction facing = Direction.NORTH;
        public double fluidFrac;
        public int fluidColor;
    }
}
