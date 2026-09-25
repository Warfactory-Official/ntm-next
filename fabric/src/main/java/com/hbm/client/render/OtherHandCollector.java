// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.SubmitRenderPhase;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
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
            @Nullable TextureAtlasSprite sprite,
            int outlineColor,
            ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay) {
        delegate.submitModel(
                model,
                state,
                mirror(poseStack),
                renderType,
                lightCoords,
                overlayCoords,
                tintedColor,
                sprite,
                outlineColor,
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
            PoseStack poseStack, List<BlockStateModelPart> parts, int progress) {
        delegate.submitBreakingBlockModel(mirror(poseStack), parts, progress);
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
            List<BakedQuad> quads,
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

    @Override
    public <T extends SubmitNode> void submitCustom(SubmitRenderPhase<T> phase, T node) {
        delegate.submitCustom(phase, node);
    }
}
