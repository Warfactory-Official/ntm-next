// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.generic.BlockLanternBehemoth;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.BlockEntityLanternBehemoth;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class RenderLanternBehemoth
        implements BlockEntityRenderer<BlockEntityLanternBehemoth, RenderLanternBehemoth.State>,
                ConcurrentRenderStateExtraction {

    private static final int LANTERN = ResourceManager.lantern.partId("Lantern");
    private static final int LIGHT = ResourceManager.lantern.partId("Light");
    private static final RenderType BODY =
            WorldRenderPipeline.oneSidedCutout(ResourceManager.lantern_rusty_tex);
    private static final RenderType LIGHT_TYPE = FlatCutout.of(ResourceManager.white_tex);

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
            BlockEntityLanternBehemoth be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breaking);
        state.broken = be.getBlockState().getValue(BlockLanternBehemoth.BROKEN);
        float wave = (float) (Math.sin(GameTime.now() / 200.0D) * 0.5D + 0.5D);
        state.color =
                state.broken
                        ? ARGB.colorFromFloat(1.0F, wave, 0.0F, 0.0F)
                        : ARGB.colorFromFloat(1.0F, 0.0F, wave * 0.5F + 0.5F, 0.0F);
    }

    @Override
    public void submit(
            State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.translate(0.5D, 0.0D, 0.5D);
        if (state.broken) {
            pose.mulPose(Axis.XP.rotationDegrees(5.0F));
            pose.mulPose(Axis.ZP.rotationDegrees(10.0F));
            collector.submitCustomGeometry(
                    pose,
                    BODY,
                    (p, buffer) ->
                            ResourceManager.lantern.renderPart(
                                    p, buffer, state.lightCoords, -1, LANTERN));
        }
        collector.submitCustomGeometry(
                pose,
                LIGHT_TYPE,
                (p, buffer) ->
                        ResourceManager.lantern.renderPart(
                                p, buffer, LightCoordsUtil.FULL_BRIGHT, state.color, LIGHT));
        pose.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        private boolean broken;
        private int color;
    }
}
