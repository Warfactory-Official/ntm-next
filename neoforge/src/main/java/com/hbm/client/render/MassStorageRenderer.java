/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
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

import com.hbm.blockentity.machine.storage.MassStorageBlockEntity;
import com.hbm.blocks.machine.storage.MassStorageBlock;
import com.hbm.render.util.Vertices;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Style;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

public class MassStorageRenderer
        implements BlockEntityRenderer<MassStorageBlockEntity, MassStorageRenderer.State>,
                ConcurrentRenderStateExtraction {
    public static final int TEXT_COLOR = 0xFF00FF00;
    public static final int VIEW_DISTANCE = 32;

    private final ItemModelResolver itemModelResolver;
    private final Font font;

    public MassStorageRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
        this.font = context.font();
    }

    public static String countText(int stockpile, boolean unicode) {
        if (stockpile >= 100_000_000 || (stockpile >= 1_000_000 && unicode)) {
            return String.format("%.0fM", stockpile / 1_000_000f);
        }
        if (stockpile >= 1_000_000) return String.format("%.1fM", stockpile / 1_000_000f);
        if (stockpile >= 100_000 || (stockpile >= 10_000 && unicode)) {
            return String.format("%.0fK", stockpile / 1000f);
        }
        if (stockpile >= 10_000) return String.format("%.1fK", stockpile / 1000f);
        return String.valueOf(stockpile);
    }

    private static void barVertex(
            VertexConsumer buf, PoseStack.Pose pose, float x, float y, int color) {
        Vertices.emit(buf, pose, x, y, 0F, color, 0F, 0F, LightCoordsUtil.FULL_BRIGHT, 0F, 0F, 1F);
    }

    public static float panelAngle(Direction facing) {
        return switch (facing) {
            case SOUTH -> 180F;
            case WEST -> -90F;
            case EAST -> 90F;
            default -> 0F;
        };
    }

    public static void panelPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose((Quaternionfc) Axis.ZP.rotationDegrees(180F));
        poseStack.mulPose((Quaternionfc) Axis.YP.rotationDegrees(panelAngle(facing)));
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        poseStack.translate(0F, 0F, -0.005F);
        poseStack.scale(1F / 16F, 1F / 16F, -0.0001F);
    }

    public static void iconPose(PoseStack poseStack) {
        poseStack.translate(4F, 2.5F, 0F);
        poseStack.scale(8F / 16F, 8F / 16F, 1F);
        poseStack.translate(8F, 8F, 0F);
        poseStack.scale(16F, -16F, 16F);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public AABB getRenderBoundingBox(MassStorageBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(0.0625D);
    }

    @Override
    public int getViewDistance() {
        return VIEW_DISTANCE;
    }

    @Override
    public void extractRenderState(
            MassStorageBlockEntity storage,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                storage, state, partialTicks, cameraPosition, breakProgress);
        state.iconOnly =
                !state.forceNative
                        && VisualizationManager.supportsVisualization(storage.getLevel());
        var type = storage.getItem(MassStorageBlockEntity.SLOT_TYPE);
        if (type.isEmpty()) {
            state.type = null;
            return;
        }
        if (state.type == null) state.type = new ItemStackRenderState();
        itemModelResolver.updateForTopItem(
                state.type, type, ItemDisplayContext.GUI, storage.getLevel(), null, 0);
        state.facing = storage.getBlockState().getValue(MassStorageBlock.FACING);
        state.fraction = (float) storage.getStockpile() / storage.getCapacity();
        state.count =
                countText(
                        storage.getStockpile(),
                        Minecraft.getInstance().options.forceUnicodeFont().get());
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        if (state.type == null) return;

        poseStack.pushPose();
        panelPose(poseStack, state.facing);

        poseStack.pushPose();
        iconPose(poseStack);
        state.type.submit(
                poseStack, collector, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
        if (state.iconOnly) {
            poseStack.popPose();
            return;
        }

        poseStack.pushPose();
        poseStack.scale(4F / 16F, 4F / 16F, 4F / 16F);
        collector.submitText(
                poseStack,
                32F - font.width(state.count) / 2F,
                44F,
                FormattedCharSequence.forward(state.count, Style.EMPTY),
                true,
                Font.DisplayMode.NORMAL,
                LightCoordsUtil.FULL_BRIGHT,
                TEXT_COLOR,
                0,
                0);
        poseStack.popPose();

        int barColor = ARGB.colorFromFloat(1F, 1F - state.fraction, state.fraction, 0F);
        collector.submitCustomGeometry(
                poseStack,
                WorldRenderPipeline.UNTEXTURED_CULL,
                (pose, buf) -> {
                    float maxX = 2F + state.fraction * 12F;
                    barVertex(buf, pose, 2F, 14F, barColor);
                    barVertex(buf, pose, maxX, 14F, barColor);
                    barVertex(buf, pose, maxX, 13.5F, barColor);
                    barVertex(buf, pose, 2F, 13.5F, barColor);
                });

        poseStack.popPose();
    }

    public static final class State extends HybridBlockEntityRenderState {
        public @Nullable ItemStackRenderState type;
        public Direction facing = Direction.NORTH;
        public float fraction;
        public String count = "0";
        public boolean iconOnly;
    }
}
