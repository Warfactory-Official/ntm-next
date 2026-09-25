// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.machine.rbmk.RBMKMiniPanelBase;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKLever.LeverUnit;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKLever;
import com.hbm.util.Facing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderRBMKLever
        implements BlockEntityRenderer<BlockEntityRBMKLever, RenderRBMKLever.State>,
                ConcurrentRenderStateExtraction {
    private static final int BASE = ResourceManager.rbmk_lever.partId("Base");
    private static final int LEVER = ResourceManager.rbmk_lever.partId("Lever");
    private final Font font;
    private final HFRWavefrontObject model = ResourceManager.rbmk_lever;
    private final RenderType type = RenderTypes.entityCutoutCull(ResourceManager.rbmk_lever_tex);

    public RenderRBMKLever(BlockEntityRendererProvider.Context context) {
        font = context.font();
    }

    @Override
    public AABB getRenderBoundingBox(BlockEntityRBMKLever be) {
        return new AABB(be.getBlockPos()).inflate(1D);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public void extractRenderState(
            BlockEntityRBMKLever be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.yaw = Facing.yaw(be.getBlockState().getValue(RBMKMiniPanelBase.FACING), 90);
        for (int i = 0; i < state.throwsAt.length; i++) {
            LeverUnit unit = be.levers[i];
            Throw out = state.throwsAt[i];
            out.active = unit.active;
            if (!unit.active) continue;
            out.progress = Mth.lerp(partialTicks, unit.prevFlipProgress, unit.flipProgress);
            out.label =
                    unit.label == null || unit.label.isEmpty()
                            ? null
                            : Component.literal(unit.label).getVisualOrderText();
        }
    }

    @Override
    public void submit(
            State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
        poses.pushPose();
        poses.translate(.5, 0, .5);
        poses.mulPose(Axis.YP.rotationDegrees(state.yaw));
        int light = state.lightCoords;
        for (int i = 0; i < state.throwsAt.length; i++) {
            Throw unit = state.throwsAt[i];
            if (!unit.active) continue;
            poses.pushPose();
            poses.translate(.25, 0, i * -.5 + .25);
            collector.submitCustomGeometry(
                    poses, type, (pose, buffer) -> model.renderPart(pose, buffer, light, -1, BASE));
            poses.pushPose();
            poses.translate(.125, .5625, 0);
            poses.mulPose(Axis.ZP.rotationDegrees(-180F * unit.progress));
            poses.translate(-.125, -.5625, 0);
            collector.submitCustomGeometry(
                    poses,
                    type,
                    (pose, buffer) -> model.renderPart(pose, buffer, light, -1, LEVER));
            poses.popPose();
            if (unit.label != null) {
                poses.translate(.01, .0625, 0);
                int width = font.width(unit.label);
                float scale = Math.min(.0125F, .4F / Math.max(width, 1));
                poses.scale(scale, -scale, scale);
                poses.mulPose(Axis.YP.rotationDegrees(90F));
                collector.submitText(
                        poses,
                        -width / 2,
                        -font.lineHeight / 2,
                        unit.label,
                        false,
                        Font.DisplayMode.NORMAL,
                        LightCoordsUtil.FULL_BRIGHT,
                        ARGB.opaque(0x00FF00),
                        0,
                        0);
            }
            poses.popPose();
        }
        poses.popPose();
    }

    public static final class Throw {
        public boolean active;
        public float progress;
        public @Nullable FormattedCharSequence label;
    }

    public static final class State extends BlockEntityRenderState {
        public final Throw[] throwsAt = new Throw[BlockEntityRBMKLever.LEVERS];
        public float yaw;

        public State() {
            for (int i = 0; i < throwsAt.length; i++) throwsAt[i] = new Throw();
        }
    }
}
