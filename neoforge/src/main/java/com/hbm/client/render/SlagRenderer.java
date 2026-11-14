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

import com.hbm.blocks.generic.DynamicSlagBlock.BlockEntitySlag;
import com.hbm.client.render.FoundryFaces;
import com.hbm.inventory.material.Material;
import com.hbm.util.RegistryUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class SlagRenderer
        implements BlockEntityRenderer<BlockEntitySlag, SlagRenderer.State>,
                ConcurrentRenderStateExtraction {

    private static final Identifier SHEET = RegistryUtil.id("textures/block/slag.png");

    private static boolean remapped(@Nullable Material mat) {
        return mat != null && mat.lightColor() != mat.darkColor();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            BlockEntitySlag be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.height = Math.min(1F, (float) be.amount / BlockEntitySlag.MAX_AMOUNT);
        state.texture = remapped(be.mat) ? SlagTextures.texture(be.mat) : SHEET;
        state.color = be.mat == null || remapped(be.mat) ? 0xFFFFFF : be.mat.moltenColor();
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        if (s.height <= 0F) return;
        final float h = s.height;
        final int rgb = s.color;
        final int light = s.lightCoords;
        col.submitCustomGeometry(
                ps,
                FlatCutout.culled(s.texture),
                (pose, buf) -> {
                    for (Direction dir : Direction.VALUES) {
                        FoundryFaces.face(
                                pose,
                                buf,
                                dir,
                                0F,
                                0F,
                                0F,
                                1F,
                                h,
                                1F,
                                FoundryFaces.shaded(rgb, dir),
                                light);
                    }
                });
    }

    public static final class State extends BlockEntityRenderState {
        public float height;
        public Identifier texture = SHEET;
        public int color;
    }
}
