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

import com.hbm.blockentity.machine.LargeTurbineBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

public class LargeTurbineRenderer
        implements BlockEntityRenderer<LargeTurbineBlockEntity, LargeTurbineRenderer.State>,
                ConcurrentRenderStateExtraction {
    private static final int BLADES = ResourceManager.turbine.partId("Blades");

    private final HFRWavefrontObject model;
    private final RenderType bladeType;

    public LargeTurbineRenderer() {
        this.model = ResourceManager.turbine;
        this.bladeType = RenderTypes.entityCutout(ResourceManager.turbofan_blades_tex);
    }

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
    public AABB getRenderBoundingBox(LargeTurbineBlockEntity be) {
        BlockPos p = be.getBlockPos();
        return new AABB(
                p.getX() - 8, p.getY() - 1, p.getZ() - 8, p.getX() + 9, p.getY() + 6, p.getZ() + 9);
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public void extractRenderState(
            LargeTurbineBlockEntity be,
            State s,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, s, partialTicks, cameraPosition, breakProgress);
        s.yaw = facingYaw(MultiblockCoreBlock.coreFacing(be.getBlockState()));
        s.rotor = Mth.lerp(partialTicks, be.lastRotor, be.rotor);
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.rotate((Quaternionfc) Axis.YP.rotationDegrees(s.yaw));
        ps.translate(0.0, 0.0, -1.0);
        ps.translate(0.0, 1.0, 0.0);
        ps.rotate((Quaternionfc) Axis.ZP.rotationDegrees(s.rotor));
        ps.translate(0.0, -1.0, 0.0);
        col.submitCustomGeometry(
                ps, bladeType, (pose, buf) -> model.renderPart(pose, buf, light, -1, BLADES));

        ps.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public float yaw;
        public float rotor;
    }
}
