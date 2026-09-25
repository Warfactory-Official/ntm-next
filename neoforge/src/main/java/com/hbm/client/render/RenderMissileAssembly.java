// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.machine.BlockMachineHorizontal;
import com.hbm.handler.MissileStruct;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.BlockEntityMachineMissileAssembly;
import com.hbm.util.Facing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class RenderMissileAssembly
        implements BlockEntityRenderer<
                        BlockEntityMachineMissileAssembly, RenderMissileAssembly.State>,
                ConcurrentRenderStateExtraction {

    private static final RenderType STRUT =
            WorldRenderPipeline.oneSidedCutout(ResourceManager.strut_tex);

    private final MissilePronter pronter = new MissilePronter();

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
    public AABB getRenderBoundingBox(BlockEntityMachineMissileAssembly rack) {
        return AABB.INFINITE;
    }

    @Override
    public void extractRenderState(
            BlockEntityMachineMissileAssembly rack,
            State state,
            float partialTicks,
            Vec3 camera,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                rack, state, partialTicks, camera, breakProgress);
        state.facing = rack.getBlockState().getValue(BlockMachineHorizontal.FACING);
        state.parts = rack.loadedMissile;
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        MissileStruct parts = state.parts;
        int light = state.lightCoords;
        float height = parts.height();

        poseStack.pushPose();
        poseStack.translate(0.5D, 0D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(Facing.yaw(state.facing, 180)));

        int reach = (int) (height / 2 - 1);
        int step = reach >= 2 ? 2 : 1;
        for (int i = -reach; i <= reach; i += step) {
            if (i == 0) continue;
            poseStack.translate(i, 0D, 0D);
            collector.submitCustomGeometry(
                    poseStack,
                    STRUT,
                    (pose, buffer) -> ResourceManager.strut.render(pose, buffer, light, -1));
            poseStack.translate(-i, 0D, 0D);
        }

        poseStack.translate(0D, 1.5D, 0D);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180F));
        poseStack.translate(-height / 2F, 0D, 0D);
        poseStack.mulPose(Axis.XP.rotationDegrees(-90F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-90F));
        pronter.pront(parts, poseStack, collector, light);

        poseStack.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        MissileStruct parts = MissileStruct.EMPTY;
    }
}
