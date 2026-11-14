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

import com.hbm.blockentity.machine.AnnihilatorBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.util.Facing;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

public class AnnihilatorRenderer
        implements BlockEntityRenderer<AnnihilatorBlockEntity, AnnihilatorRenderer.State>,
                ConcurrentRenderStateExtraction {
    private static final int ROLLER = ResourceManager.annihilator.partId("Roller");
    private static final int BELT = ResourceManager.annihilator.partId("Belt");

    private final HFRWavefrontObject model;
    private final RenderType bodyType;
    private final RenderType beltType;

    public AnnihilatorRenderer(BlockEntityRendererProvider.Context context) {
        this.model = ResourceManager.annihilator;
        this.bodyType = RenderTypes.entitySolid(ResourceManager.annihilator_tex);
        this.beltType = RenderTypes.entitySolid(ResourceManager.annihilator_belt_tex);
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
    public void extractRenderState(
            AnnihilatorBlockEntity be,
            State state,
            float pt,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, pt, cameraPosition, breakProgress);
        state.facing = MultiblockCoreBlock.coreFacing(be.getBlockState());
        long now = GameTime.now();
        state.rollerAngle = (float) (now * 0.15D % 360D);
        state.beltOffset = (float) -(now / 3000D % 1D);
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.rotate((Quaternionfc) Axis.YP.rotationDegrees(facingYaw(s.facing)));

        ps.pushPose();
        ps.translate(0.0, 1.75, 0.0);
        ps.rotate((Quaternionfc) Axis.ZN.rotationDegrees(s.rollerAngle));
        ps.translate(0.0, -1.75, 0.0);
        col.submitCustomGeometry(
                ps, bodyType, (pose, buffer) -> model.renderPart(pose, buffer, light, -1, ROLLER));
        ps.popPose();

        col.submitCustomGeometry(
                ps,
                beltType,
                (pose, buffer) ->
                        model.renderPart(pose, buffer, light, -1, BELT, 1F, 1F, s.beltOffset, 0F));

        ps.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public Direction facing = Direction.NORTH;
        public float rollerAngle;
        public float beltOffset;
    }
}
