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

import com.hbm.blockentity.machine.albion.PABeamlineBlockEntity;
import com.hbm.blocks.machine.albion.PABeamlineBlock;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.render.util.Colors;
import com.hbm.util.Facing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

public class PABeamlineRenderer
        implements BlockEntityRenderer<PABeamlineBlockEntity, PABeamlineRenderer.State>,
                ConcurrentRenderStateExtraction {
    private static final int BEAMLINE_GLASS = ResourceManager.pa_beamline.partId("BeamlineGlass");

    private static final Identifier WHITE = RenderTextures.WHITE;

    private final HFRWavefrontObject model;
    private final RenderType glassType;

    public PABeamlineRenderer(BlockEntityRendererProvider.Context context) {
        this.model = ResourceManager.pa_beamline;
        this.glassType = FlatCutout.of(WHITE);
    }

    private static float facingYaw(Direction facing) {
        return Facing.yaw(facing, 90);
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
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(PABeamlineBlockEntity be) {
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
    public void extractRenderState(
            PABeamlineBlockEntity be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.window = be.getBlockState().getValue(PABeamlineBlock.WINDOW);
        state.yaw = facingYaw(MultiblockCoreBlock.coreFacing(be.getBlockState()));
        state.flash = be.flash(partialTicks);
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        if (!s.window || s.flash <= 0F) return;

        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.rotate((Quaternionfc) Axis.YP.rotationDegrees(s.yaw));

        int argb = Colors.argb(1F, 0.9F * s.flash, 0.9F * s.flash, s.flash);
        col.submitCustomGeometry(
                ps,
                glassType,
                (pose, buffer) ->
                        model.renderPart(
                                pose, buffer, LightCoordsUtil.FULL_BRIGHT, argb, BEAMLINE_GLASS));

        ps.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public boolean window;
        public float yaw;
        public float flash;
    }
}
