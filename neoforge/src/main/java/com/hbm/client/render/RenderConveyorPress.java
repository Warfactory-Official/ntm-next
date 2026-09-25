// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.BlockEntityConveyorPress;
import com.hbm.util.Facing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderConveyorPress
        implements BlockEntityRenderer<BlockEntityConveyorPress, RenderConveyorPress.State>,
                ConcurrentRenderStateExtraction {

    private static final int PISTON = ResourceManager.conveyor_press.partId("Piston");
    private static final int BELT = ResourceManager.conveyor_press.partId("Belt");

    private static final float STROKE = 0.75F;
    private static final int BELT_FRAMES = 16;
    private static final int BELT_PHASE = 2;

    private final RenderType pressType =
            RenderTypes.entityCutoutCull(ResourceManager.conveyor_press_tex);
    private final RenderType beltType =
            RenderTypes.entityCutoutCull(ResourceManager.conveyor_press_belt_tex);

    public RenderConveyorPress(BlockEntityRendererProvider.Context context) {}

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(BlockEntityConveyorPress be) {
        BlockPos pos = be.getBlockPos();
        return new AABB(
                pos.getX() - 1,
                pos.getY(),
                pos.getZ() - 1,
                pos.getX() + 2,
                pos.getY() + 3,
                pos.getZ() + 2);
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public void extractRenderState(
            BlockEntityConveyorPress be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);

        state.yaw = Facing.yaw(BlockMultiblockCore.coreFacing(be.getBlockState()), 90);
        state.press = (float) (be.lastPress + (be.renderPress - be.lastPress) * partialTicks);
        state.stamped = !be.syncStack.isEmpty();

        long ticks = be.getLevel() == null ? 0L : be.getLevel().getGameTime();
        state.beltOffset = (ticks % BELT_FRAMES - BELT_PHASE) / (float) BELT_FRAMES;
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        int light = state.lightCoords;

        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));

        if (state.stamped) {
            poseStack.pushPose();
            poseStack.translate(0.0, -state.press * STROKE, 0.0);
            collector.submitCustomGeometry(
                    poseStack,
                    pressType,
                    (pose, buffer) ->
                            ResourceManager.conveyor_press.renderPart(
                                    pose, buffer, light, -1, PISTON));
            poseStack.popPose();
        }

        float offset = state.beltOffset;
        collector.submitCustomGeometry(
                poseStack,
                beltType,
                (pose, buffer) ->
                        ResourceManager.conveyor_press.renderPart(
                                pose, buffer, light, -1, BELT, 1F, 1F, 0F, offset));

        poseStack.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public float yaw;
        public float press;
        public boolean stamped;
        public float beltOffset;
    }
}
