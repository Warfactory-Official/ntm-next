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

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

public final class OtherHandCollector implements SubmitNodeCollector {

    private final OrderedSubmitNodeCollector delegate;
    private final @Nullable SubmitNodeCollector orderable;

    public OtherHandCollector(SubmitNodeCollector delegate) {
        this.delegate = delegate;
        this.orderable = delegate;
    }

    private OtherHandCollector(OrderedSubmitNodeCollector delegate) {
        this.delegate = delegate;
        this.orderable = null;
    }

    private PoseStack mirror(PoseStack poseStack) {
        poseStack.pushPose();
        poseStack.scale(-1F, 1F, 1F);
        return poseStack;
    }

    @Override
    public OrderedSubmitNodeCollector order(int order) {
        if (orderable == null)
            throw new IllegalStateException("an ordered collector cannot be re-ordered");
        return new OtherHandCollector(orderable.order(order));
    }

    @Override
    public void submitShadow(
            PoseStack poseStack, float radius, List<EntityRenderState.ShadowPiece> pieces) {
        delegate.submitShadow(mirror(poseStack), radius, pieces);
        poseStack.popPose();
    }

    @Override
    public void submitNameTag(
            PoseStack poseStack,
            @Nullable Vec3 nameTagAttachment,
            int offset,
            Component name,
            boolean seeThrough,
            int lightCoords,
            CameraRenderState camera) {
        delegate.submitNameTag(
                mirror(poseStack),
                nameTagAttachment,
                offset,
                name,
                seeThrough,
                lightCoords,
                camera);
        poseStack.popPose();
    }

    @Override
    public void submitText(
            PoseStack poseStack,
            float x,
            float y,
            FormattedCharSequence string,
            boolean dropShadow,
            Font.DisplayMode displayMode,
            int lightCoords,
            int color,
            int backgroundColor,
            int outlineColor) {
        delegate.submitText(
                mirror(poseStack),
                x,
                y,
                string,
                dropShadow,
                displayMode,
                lightCoords,
                color,
                backgroundColor,
                outlineColor);
        poseStack.popPose();
    }

    @Override
    public void submitTextBackground(
            PoseStack poseStack,
            float x0,
            float y0,
            float x1,
            float y1,
            int color,
            Font.DisplayMode displayMode,
            int lightCoords) {
        delegate.submitTextBackground(
                mirror(poseStack), x0, y0, x1, y1, color, displayMode, lightCoords);
        poseStack.popPose();
    }

    @Override
    public void submitFlame(
            PoseStack poseStack, EntityRenderState renderState, Quaternionf rotation) {
        delegate.submitFlame(mirror(poseStack), renderState, rotation);
        poseStack.popPose();
    }

    @Override
    public void submitLeash(PoseStack poseStack, EntityRenderState.LeashState leashState) {
        delegate.submitLeash(mirror(poseStack), leashState);
        poseStack.popPose();
    }

    @Override
    public <S> void submitModel(
            Model<? super S> model,
            S state,
            PoseStack poseStack,
            RenderType renderType,
            int lightCoords,
            int overlayCoords,
            int tintedColor,
            @Nullable UvMapping uvMapping,
            int outlineColor) {
        delegate.submitModel(
                model,
                state,
                mirror(poseStack),
                renderType,
                lightCoords,
                overlayCoords,
                tintedColor,
                uvMapping,
                outlineColor);
        poseStack.popPose();
    }

    @Override
    public <S> void submitCrumblingOverlay(
            Model<? super S> model,
            S state,
            PoseStack poseStack,
            RenderType renderType,
            int lightCoords,
            int overlayCoords,
            int tintedColor,
            ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        delegate.submitCrumblingOverlay(
                model,
                state,
                mirror(poseStack),
                renderType,
                lightCoords,
                overlayCoords,
                tintedColor,
                crumblingOverlay);
        poseStack.popPose();
    }

    @Override
    public void submitMovingBlock(
            PoseStack poseStack, MovingBlockRenderState movingBlockRenderState, int outlineColor) {
        delegate.submitMovingBlock(mirror(poseStack), movingBlockRenderState, outlineColor);
        poseStack.popPose();
    }

    @Override
    public void submitBlockModel(
            PoseStack poseStack,
            RenderType renderType,
            List<BlockStateModelPart> parts,
            int[] tintLayers,
            int lightCoords,
            int overlayCoords,
            int outlineColor) {
        delegate.submitBlockModel(
                mirror(poseStack),
                renderType,
                parts,
                tintLayers,
                lightCoords,
                overlayCoords,
                outlineColor);
        poseStack.popPose();
    }

    @Override
    public void submitBreakingBlockModel(
            PoseStack poseStack,
            List<BlockStateModelPart> parts,
            int progress,
            boolean isBlockTranslucent) {
        delegate.submitBreakingBlockModel(mirror(poseStack), parts, progress, isBlockTranslucent);
        poseStack.popPose();
    }

    @Override
    public void submitShapeOutline(
            PoseStack poseStack,
            VoxelShape shape,
            RenderType renderType,
            int color,
            float width,
            boolean afterTerrain) {
        delegate.submitShapeOutline(
                mirror(poseStack), shape, renderType, color, width, afterTerrain);
        poseStack.popPose();
    }

    @Override
    public void submitItem(
            PoseStack poseStack,
            ItemDisplayContext displayContext,
            int lightCoords,
            int overlayCoords,
            int outlineColor,
            int[] tintLayers,
            ItemQuads quads,
            ItemStackRenderState.FoilType foilType) {
        delegate.submitItem(
                mirror(poseStack),
                displayContext,
                lightCoords,
                overlayCoords,
                outlineColor,
                tintLayers,
                quads,
                foilType);
        poseStack.popPose();
    }

    @Override
    public void submitCustomGeometry(
            PoseStack poseStack,
            RenderType renderType,
            SubmitNodeCollector.CustomGeometryRenderer customGeometryRenderer) {
        delegate.submitCustomGeometry(mirror(poseStack), renderType, customGeometryRenderer);
        poseStack.popPose();
    }

    @Override
    public void submitQuadParticleGroup(QuadParticleRenderState particles) {
        delegate.submitQuadParticleGroup(particles);
    }

    @Override
    public void submitGizmoPrimitives(
            DrawableGizmoPrimitives.Group group, CameraRenderState camera, boolean onTop) {
        delegate.submitGizmoPrimitives(group, camera, onTop);
    }
}
