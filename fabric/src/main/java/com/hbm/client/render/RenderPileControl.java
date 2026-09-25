// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.machine.pile.BlockPileDevice;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.pile.BlockEntityPileControl;
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

public class RenderPileControl
        implements BlockEntityRenderer<BlockEntityPileControl, RenderPileControl.State>,
                ConcurrentRenderStateExtraction {
    private static final int ROD = ResourceManager.pile_control.partId("Rod");
    private static final RenderType MATERIAL =
            RenderTypes.entitySolid(ResourceManager.pile_control_tex);

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            BlockEntityPileControl be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.facing = be.getBlockState().getValue(BlockPileDevice.FACING);
        state.extension = be.lastExtension + (be.extension - be.lastExtension) * partialTicks;
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.5D, state.extension * 0.75D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(Facing.yaw(state.facing, 90)));
        collector.submitCustomGeometry(
                poseStack,
                MATERIAL,
                (pose, buffer) ->
                        ResourceManager.pile_control.renderPart(
                                pose, buffer, state.lightCoords, -1, ROD));
        poseStack.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        double extension;
    }
}
