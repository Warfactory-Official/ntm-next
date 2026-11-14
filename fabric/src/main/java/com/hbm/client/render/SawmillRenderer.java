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

import com.hbm.blockentity.machine.thermal.TimberSawBlockEntity;
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
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

public class SawmillRenderer
        implements BlockEntityRenderer<TimberSawBlockEntity, SawmillRenderer.State>,
                ConcurrentRenderStateExtraction {
    private static final int BLADE = ResourceManager.sawmill.partId("Blade");
    private static final int GEAR_LEFT = ResourceManager.sawmill.partId("GearLeft");
    private static final int GEAR_RIGHT = ResourceManager.sawmill.partId("GearRight");

    private final HFRWavefrontObject model;
    private final RenderType bodyType;

    public SawmillRenderer() {
        this.model = ResourceManager.sawmill;
        this.bodyType = RenderTypes.entityCutoutCull(ResourceManager.sawmill_tex);
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
    public int getViewDistance() {
        return 256;
    }

    @Override
    public void extractRenderState(
            TimberSawBlockEntity be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.yaw = facingYaw(MultiblockCoreBlock.coreFacing(be.getBlockState()));
        state.rot = Mth.lerp(partialTicks, be.lastSpin, be.spin);
        state.hasBlade = be.hasBlade;
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.mulPose((Quaternionfc) Axis.YP.rotationDegrees(s.yaw));

        if (s.hasBlade) {
            ps.pushPose();
            ps.translate(0.0, 1.375, 0.0);
            ps.mulPose((Quaternionfc) Axis.ZP.rotationDegrees(-s.rot * 2F));
            ps.translate(0.0, -1.375, 0.0);
            part(col, ps, light, BLADE);
            ps.popPose();
        }

        ps.pushPose();
        ps.translate(0.5625, 1.375, 0.0);
        ps.mulPose((Quaternionfc) Axis.ZP.rotationDegrees(s.rot));
        ps.translate(-0.5625, -1.375, 0.0);
        part(col, ps, light, GEAR_LEFT);
        ps.popPose();

        ps.pushPose();
        ps.translate(-0.5625, 1.375, 0.0);
        ps.mulPose((Quaternionfc) Axis.ZP.rotationDegrees(-s.rot));
        ps.translate(0.5625, -1.375, 0.0);
        part(col, ps, light, GEAR_RIGHT);
        ps.popPose();

        ps.popPose();
    }

    private void part(SubmitNodeCollector col, PoseStack ps, int light, int name) {
        col.submitCustomGeometry(
                ps, bodyType, (pose, buffer) -> model.renderPart(pose, buffer, light, -1, name));
    }

    public static final class State extends BlockEntityRenderState {
        public float yaw;
        public float rot;
        public boolean hasBlade;
    }
}
