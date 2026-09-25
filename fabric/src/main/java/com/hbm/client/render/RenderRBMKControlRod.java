// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKControl;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKControlAuto;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKControlManual;
import com.hbm.tileentity.machine.rbmk.RBMKConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderRBMKControlRod
        implements BlockEntityRenderer<BlockEntityRBMKControl, RenderRBMKControlRod.State>,
                ConcurrentRenderStateExtraction {
    private static final int LID = ResourceManager.rbmk_rods.partId("Lid");

    private final RenderType manualType =
            RenderTypes.entityCutoutCull(ResourceManager.rbmk_control_tex);
    private final RenderType autoType =
            RenderTypes.entityCutoutCull(ResourceManager.rbmk_control_auto_tex);

    private final RenderType[] colorTypes = buildColorTypes();

    private static RenderType[] buildColorTypes() {
        RenderType[] t = new RenderType[ResourceManager.rbmk_control_color_tex.length];
        for (int i = 0; i < t.length; i++) {
            t[i] = RenderTypes.entityCutoutCull(ResourceManager.rbmk_control_color_tex[i]);
        }
        return t;
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
            BlockEntityRBMKControl be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.level = Mth.lerp(partialTicks, be.lastLevel, be.level);
        state.offset = RBMKConfig.getColumnHeight(be.getLevel());

        state.capLight =
                LightCoordsUtil.getLightCoords(
                        be.getLevel(), be.getBlockPos().above(state.offset + 1));
        state.auto = be instanceof BlockEntityRBMKControlAuto;
        state.color =
                (be instanceof BlockEntityRBMKControlManual m && m.color != null)
                        ? m.color.ordinal()
                        : -1;
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        final int light = state.capLight;
        RenderType type =
                state.auto ? autoType : (state.color < 0 ? manualType : colorTypes[state.color]);
        poseStack.pushPose();
        poseStack.translate(0.5, state.offset + state.level, 0.5);
        collector.submitCustomGeometry(
                poseStack,
                type,
                (pose, buffer) ->
                        ResourceManager.rbmk_rods.renderPart(pose, buffer, light, -1, LID));
        poseStack.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public double level;
        public int offset;
        public int capLight;
        public boolean auto;
        public int color = -1;
    }
}
