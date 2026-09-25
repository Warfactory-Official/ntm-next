// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.machine.pile.BlockPileDevice;
import com.hbm.client.model.PileDeviceModel;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.pile.BlockEntityPileVent;
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

public class RenderPileVent
        implements BlockEntityRenderer<BlockEntityPileVent, RenderPileVent.State>,
                ConcurrentRenderStateExtraction {
    private static final int FAN = ResourceManager.pile_vent.partId(PileDeviceModel.VENT_FAN);

    private static final RenderType VENT = RenderTypes.entitySolid(ResourceManager.pile_vent_tex);

    private static float yaw(Direction facing) {
        return Facing.yaw(facing, 90);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            BlockEntityPileVent be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.facing = be.getBlockState().getValue(BlockPileDevice.FACING);
        state.spin = be.lastFan + (be.fan - be.lastFan) * partialTicks;
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.5D, 0D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw(state.facing)));
        poseStack.mulPose(Axis.YP.rotationDegrees(state.spin));
        collector.submitCustomGeometry(
                poseStack,
                VENT,
                (pose, buffer) ->
                        ResourceManager.pile_vent.renderPart(
                                pose, buffer, state.lightCoords, -1, FAN));
        poseStack.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        float spin;
    }
}
