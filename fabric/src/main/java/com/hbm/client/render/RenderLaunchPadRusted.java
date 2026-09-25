// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.bomb.BlockEntityLaunchPadRusted;
import com.hbm.util.Facing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class RenderLaunchPadRusted
        implements BlockEntityRenderer<BlockEntityLaunchPadRusted, RenderLaunchPadRusted.State>,
                ConcurrentRenderStateExtraction {
    private static final RenderType MISSILE =
            RenderTypes.entityCutoutCull(ResourceManager.missileDoomsdayRusted_tex);

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
            BlockEntityLaunchPadRusted pad,
            State state,
            float partialTicks,
            Vec3 camera,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                pad, state, partialTicks, camera, breakProgress);
        state.facing = BlockMultiblockCore.coreFacing(pad.getBlockState());
        state.missileLoaded = pad.missileLoaded;
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        if (!state.missileLoaded) return;
        poseStack.pushPose();
        poseStack.translate(.5D, 1D, .5D);

        poseStack.mulPose(Axis.YP.rotationDegrees(Facing.yaw(state.facing, 90)));
        collector.submitCustomGeometry(
                poseStack,
                MISSILE,
                (pose, buffer) ->
                        ResourceManager.missileNuclear.render(pose, buffer, state.lightCoords, -1));
        poseStack.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        boolean missileLoaded;
    }
}
