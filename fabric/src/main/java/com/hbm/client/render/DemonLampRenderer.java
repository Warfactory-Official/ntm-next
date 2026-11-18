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

import com.hbm.blockentity.machine.DemonLampBlockEntity;
import com.hbm.blocks.machine.DemonLampBlock;
import com.hbm.render.util.Vertices;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

public class DemonLampRenderer
        implements BlockEntityRenderer<DemonLampBlockEntity, DemonLampRenderer.State>,
                ConcurrentRenderStateExtraction {

    public static final int RAYS = 16;
    public static final double NEAR = 0.375D;
    public static final double FAR = 15D;
    public static final int INNER = ARGB.colorFromFloat(0.25F, 0F, 0.75F, 1F);
    public static final int OUTER = ARGB.colorFromFloat(0F, 0F, 0.75F, 1F);

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            DemonLampBlockEntity lamp,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                lamp, state, partialTicks, cameraPosition, breakProgress);
        state.facing = lamp.getBlockState().getValue(DemonLampBlock.FACING);
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);
        orient(poseStack, state.facing);
        poseStack.translate(0.0D, -0.5D, 0.0D);
        collector.submitCustomGeometry(
                poseStack, BeamRenderTypes.ADDITIVE, DemonLampRenderer::emitRays);
        poseStack.popPose();
    }

    public static void orient(PoseStack poseStack, Direction facing) {
        switch (facing) {
            case DOWN -> poseStack.rotate((Quaternionfc) Axis.XP.rotationDegrees(180));
            case UP -> {}
            case NORTH -> {
                poseStack.rotate((Quaternionfc) Axis.XP.rotationDegrees(90));
                poseStack.rotate((Quaternionfc) Axis.ZP.rotationDegrees(180));
            }
            case SOUTH -> poseStack.rotate((Quaternionfc) Axis.XP.rotationDegrees(90));
            case WEST -> {
                poseStack.rotate((Quaternionfc) Axis.XP.rotationDegrees(90));
                poseStack.rotate((Quaternionfc) Axis.ZP.rotationDegrees(90));
            }
            case EAST -> {
                poseStack.rotate((Quaternionfc) Axis.XP.rotationDegrees(90));
                poseStack.rotate((Quaternionfc) Axis.ZP.rotationDegrees(270));
            }
        }
    }

    public static void emitRays(PoseStack.Pose pose, VertexConsumer buf) {
        float turn = (float) (Math.PI * 2D / RAYS);
        float cos = Mth.cos(turn);
        float sin = Mth.sin(turn);
        double x = 1D, z = 0D;
        for (int j = 0; j < 2; j++) {
            float y0 = (float) (0.5D + j * 0.125D);
            float y1 = (float) (0.5D + j * 0.125D + (j == 0 ? -0.5D : 0.5D));
            for (int i = 0; i < RAYS; i++) {
                Vertices.emit(buf, pose, (float) (x * NEAR), y0, (float) (z * NEAR), INNER);
                Vertices.emit(buf, pose, (float) (x * FAR), y1, (float) (z * FAR), OUTER);
                double nx = x * cos + z * sin;
                z = z * cos - x * sin;
                x = nx;
                Vertices.emit(buf, pose, (float) (x * FAR), y1, (float) (z * FAR), OUTER);
                Vertices.emit(buf, pose, (float) (x * NEAR), y0, (float) (z * NEAR), INNER);
            }
        }
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    public static final class State extends BlockEntityRenderState {
        public Direction facing = Direction.DOWN;
    }
}
