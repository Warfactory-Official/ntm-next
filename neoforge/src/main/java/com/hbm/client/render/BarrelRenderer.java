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

import com.hbm.blockentity.machine.storage.FluidBarrelBlockEntity;
import com.hbm.inventory.fluid.HazardSymbol;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluidProperty;
import com.hbm.render.util.Vertices;
import com.hbm.util.RegistryUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

public class BarrelRenderer
        implements BlockEntityRenderer<FluidBarrelBlockEntity, BarrelRenderer.State>,
                ConcurrentRenderStateExtraction {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/models/misc/danger_diamond.png");
    static final RenderType RENDER_TYPE = FlatCutout.culled(TEXTURE);

    private static final float P = 1F / 256F;

    private static final float S = 1F / 139F;

    static void pront(
            PoseStack.Pose pose,
            VertexConsumer buffer,
            int light,
            int health,
            int flame,
            int react,
            HazardSymbol symbol) {
        quad(
                pose, buffer, light, 0.0F, 0.5F, -0.5F, P * 144, P * 45, 0.0F, 0.5F, 0.5F, P * 5,
                P * 45, 0.0F, -0.5F, 0.5F, P * 5, P * 184, 0.0F, -0.5F, -0.5F, P * 144, P * 184);

        digit(pose, buffer, light, health, 0F, 33 * S);
        digit(pose, buffer, light, flame, 33 * S, 0F);
        digit(pose, buffer, light, react, 0F, -33 * S);

        if (symbol != HazardSymbol.NONE) {
            float symSize = 59F / 2F * S;
            float oY = -33 * S;
            int x = symbol.cellX;
            int y = symbol.cellY;
            quad(
                    pose,
                    buffer,
                    light,
                    0.01F,
                    symSize + oY,
                    -symSize,
                    (x + 59) * P,
                    y * P,
                    0.01F,
                    symSize + oY,
                    symSize,
                    x * P,
                    y * P,
                    0.01F,
                    -symSize + oY,
                    symSize,
                    x * P,
                    (y + 59) * P,
                    0.01F,
                    -symSize + oY,
                    -symSize,
                    (x + 59) * P,
                    (y + 59) * P);
        }
    }

    private static void digit(
            PoseStack.Pose pose, VertexConsumer buffer, int light, int value, float oY, float oZ) {
        if (value < 0 || value >= 6) return;
        float width = 10F * S;
        float height = 14F * S;
        int x = value == 0 ? 125 : 5 + (value - 1) * 24;
        int y = 5;
        quad(
                pose,
                buffer,
                light,
                0.01F,
                height + oY,
                -width + oZ,
                (x + 20) * P,
                y * P,
                0.01F,
                height + oY,
                width + oZ,
                x * P,
                y * P,
                0.01F,
                -height + oY,
                width + oZ,
                x * P,
                (y + 28) * P,
                0.01F,
                -height + oY,
                -width + oZ,
                (x + 20) * P,
                (y + 28) * P);
    }

    private static void quad(
            PoseStack.Pose pose,
            VertexConsumer buffer,
            int light,
            float x0,
            float y0,
            float z0,
            float u0,
            float v0,
            float x1,
            float y1,
            float z1,
            float u1,
            float v1,
            float x2,
            float y2,
            float z2,
            float u2,
            float v2,
            float x3,
            float y3,
            float z3,
            float u3,
            float v3) {
        vertex(pose, buffer, light, x0, y0, z0, u0, v0);
        vertex(pose, buffer, light, x1, y1, z1, u1, v1);
        vertex(pose, buffer, light, x2, y2, z2, u2, v2);
        vertex(pose, buffer, light, x3, y3, z3, u3, v3);
    }

    private static void vertex(
            PoseStack.Pose pose,
            VertexConsumer buffer,
            int light,
            float x,
            float y,
            float z,
            float u,
            float v) {
        Vertices.emit(buffer, pose, x, y, z, -1, u, v, light, 1F, 0F, 0F);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            FluidBarrelBlockEntity be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        Fluid type = be.tank.getTankType();
        NTMFluidProperty prop = type == null ? null : NTMFluidProperties.get(type);
        state.hasFluid = prop != null;
        if (prop != null) {
            state.health = prop.nfpaHealth();
            state.flame = prop.nfpaFlame();
            state.react = prop.nfpaReact();
            state.symbol = prop.symbol();
        }
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        if (!state.hasFluid) return;
        int light = state.lightCoords;
        for (int j = 0; j < 4; j++) {
            poseStack.pushPose();
            poseStack.translate(0.5, 0.5, 0.5);
            poseStack.mulPose((Quaternionfc) Axis.YP.rotationDegrees(90F * j));
            poseStack.translate(0.4, 0.30, -0.24);
            poseStack.scale(1.0F, 0.25F, 0.25F);
            int health = state.health, flame = state.flame, react = state.react;
            HazardSymbol symbol = state.symbol;
            collector.submitCustomGeometry(
                    poseStack,
                    RENDER_TYPE,
                    (pose, buffer) -> pront(pose, buffer, light, health, flame, react, symbol));
            poseStack.popPose();
        }
    }

    public static final class State extends BlockEntityRenderState {
        public boolean hasFluid;
        public int health;
        public int flame;
        public int react;
        public HazardSymbol symbol = HazardSymbol.NONE;
    }
}
