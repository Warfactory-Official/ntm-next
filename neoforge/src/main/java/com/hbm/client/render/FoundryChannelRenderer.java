/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.client.render;

import com.hbm.blockentity.machine.FoundryChannelBlockEntity;
import com.hbm.blocks.machine.FoundryChannelBlock;
import com.hbm.client.render.FoundryFaces;
import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class FoundryChannelRenderer
        implements BlockEntityRenderer<FoundryChannelBlockEntity, FoundryChannelRenderer.State>,
                ConcurrentRenderStateExtraction {

    private final RenderType fillType;

    public FoundryChannelRenderer() {
        this.fillType = FlatCutout.culled(ResourceManager.foundry_stream_tex);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            FoundryChannelBlockEntity be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);

        state.doRender = be.amount > 0 && be.type != null;
        if (!state.doRender) return;
        state.level = (float) (be.amount * 0.25D / be.getCapacity());
        state.color = FoundryFaces.brightenMolten(be.type.moltenColor());

        BlockState block = be.getBlockState();
        state.north = block.getValue(FoundryChannelBlock.NORTH);
        state.south = block.getValue(FoundryChannelBlock.SOUTH);
        state.west = block.getValue(FoundryChannelBlock.WEST);
        state.east = block.getValue(FoundryChannelBlock.EAST);
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        if (!s.doRender) return;

        final float top = 0.125F + s.level;
        final int color = s.color;
        final boolean n = s.north, so = s.south, w = s.west, e = s.east;

        col.submitCustomGeometry(
                ps,
                fillType,
                (pose, buf) -> {
                    FoundryFaces.face(
                            pose,
                            buf,
                            Direction.UP,
                            0.375F,
                            0.125F,
                            0.375F,
                            0.625F,
                            top,
                            0.625F,
                            color,
                            FoundryFaces.MOLTEN_LIGHT);

                    if (e) {
                        FoundryFaces.face(
                                pose,
                                buf,
                                Direction.UP,
                                0.625F,
                                0.125F,
                                0.3125F,
                                1F,
                                top,
                                0.6875F,
                                color,
                                FoundryFaces.MOLTEN_LIGHT);
                        FoundryFaces.face(
                                pose,
                                buf,
                                Direction.EAST,
                                0.625F,
                                0.125F,
                                0.3125F,
                                1F,
                                top,
                                0.6875F,
                                color,
                                FoundryFaces.MOLTEN_LIGHT);
                    }
                    if (w) {
                        FoundryFaces.face(
                                pose,
                                buf,
                                Direction.UP,
                                0F,
                                0.125F,
                                0.3125F,
                                0.375F,
                                top,
                                0.6875F,
                                color,
                                FoundryFaces.MOLTEN_LIGHT);
                        FoundryFaces.face(
                                pose,
                                buf,
                                Direction.WEST,
                                0F,
                                0.125F,
                                0.3125F,
                                0.375F,
                                top,
                                0.6875F,
                                color,
                                FoundryFaces.MOLTEN_LIGHT);
                    }
                    if (so) {
                        FoundryFaces.face(
                                pose,
                                buf,
                                Direction.UP,
                                0.3125F,
                                0.125F,
                                0.625F,
                                0.6875F,
                                top,
                                1F,
                                color,
                                FoundryFaces.MOLTEN_LIGHT);
                        FoundryFaces.face(
                                pose,
                                buf,
                                Direction.SOUTH,
                                0.3125F,
                                0.125F,
                                0.625F,
                                0.6875F,
                                top,
                                1F,
                                color,
                                FoundryFaces.MOLTEN_LIGHT);
                    }
                    if (n) {
                        FoundryFaces.face(
                                pose,
                                buf,
                                Direction.UP,
                                0.3125F,
                                0.125F,
                                0F,
                                0.6875F,
                                top,
                                0.375F,
                                color,
                                FoundryFaces.MOLTEN_LIGHT);
                        FoundryFaces.face(
                                pose,
                                buf,
                                Direction.NORTH,
                                0.3125F,
                                0.125F,
                                0F,
                                0.6875F,
                                top,
                                0.375F,
                                color,
                                FoundryFaces.MOLTEN_LIGHT);
                    }
                });
    }

    public static final class State extends BlockEntityRenderState {
        public boolean doRender;
        public float level;
        public int color;
        public boolean north, south, west, east;
    }
}
