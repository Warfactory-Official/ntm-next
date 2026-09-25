// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.turret.BlockEntityTurretHowardDamaged;
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
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderTurretHowardDamaged
        implements BlockEntityRenderer<
                        BlockEntityTurretHowardDamaged, RenderTurretHowardDamaged.State>,
                ConcurrentRenderStateExtraction {
    private static final int PART_CARRIAGE =
            ResourceManager.turret_howard_damaged.partId("Carriage");
    private static final int PART_BODY = ResourceManager.turret_howard_damaged.partId("Body");
    private static final int PART_BARRELS_TOP =
            ResourceManager.turret_howard_damaged.partId("BarrelsTop");
    private static final int PART_BARRELS_BOTTOM =
            ResourceManager.turret_howard_damaged.partId("BarrelsBottom");

    private static final RenderType CARRIAGE =
            RenderTypes.entityCutoutCull(ResourceManager.turret_carriage_ciws_rusted_tex);
    private static final RenderType BODY =
            RenderTypes.entityCutoutCull(ResourceManager.turret_howard_rusted_tex);
    private static final RenderType BARRELS =
            RenderTypes.entityCutoutCull(ResourceManager.turret_howard_barrels_rusted_tex);

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
    public AABB getRenderBoundingBox(BlockEntityTurretHowardDamaged be) {
        return new AABB(be.getBlockPos()).inflate(6.0D);
    }

    @Override
    public void extractRenderState(
            BlockEntityTurretHowardDamaged be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.facing = BlockMultiblockCore.coreFacing(be.getBlockState());
        state.yaw =
                (float)
                        (-Math.toDegrees(Mth.lerp(partialTicks, be.lastRotationYaw, be.rotationYaw))
                                - 90D);
        state.pitch =
                (float)
                        Math.toDegrees(
                                Mth.lerp(partialTicks, be.lastRotationPitch, be.rotationPitch));
        state.spin = Mth.lerp(partialTicks, be.lastSpin, be.spin);
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {

        double offsetX =
                state.facing == Direction.NORTH || state.facing == Direction.WEST ? 1D : 0D;
        double offsetZ =
                state.facing == Direction.NORTH || state.facing == Direction.EAST ? 1D : 0D;
        poseStack.pushPose();
        poseStack.translate(offsetX, 0D, offsetZ);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
        collector.submitCustomGeometry(
                poseStack,
                CARRIAGE,
                (pose, buffer) ->
                        ResourceManager.turret_howard_damaged.renderPart(
                                pose, buffer, state.lightCoords, -1, PART_CARRIAGE));
        poseStack.translate(0D, 2.25D, 0D);
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.pitch));
        poseStack.translate(0D, -2.25D, 0D);
        collector.submitCustomGeometry(
                poseStack,
                BODY,
                (pose, buffer) ->
                        ResourceManager.turret_howard_damaged.renderPart(
                                pose, buffer, state.lightCoords, -1, PART_BODY));
        poseStack.pushPose();
        poseStack.translate(0D, 2.5D, 0D);
        poseStack.mulPose(Axis.XN.rotationDegrees(state.spin));
        poseStack.translate(0D, -2.5D, 0D);
        collector.submitCustomGeometry(
                poseStack,
                BARRELS,
                (pose, buffer) ->
                        ResourceManager.turret_howard_damaged.renderPart(
                                pose, buffer, state.lightCoords, -1, PART_BARRELS_TOP));
        poseStack.popPose();
        collector.submitCustomGeometry(
                poseStack,
                BARRELS,
                (pose, buffer) ->
                        ResourceManager.turret_howard_damaged.renderPart(
                                pose, buffer, state.lightCoords, -1, PART_BARRELS_BOTTOM));
        poseStack.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        float yaw;
        float pitch;
        float spin;
    }
}
