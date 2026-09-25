// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKAutoloader;
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
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderRBMKAutoloader
        implements BlockEntityRenderer<BlockEntityRBMKAutoloader, RenderRBMKAutoloader.State>,
                ConcurrentRenderStateExtraction {

    private static final int PART_BASE = ResourceManager.rbmk_autoloader.partId("Base");
    private static final int PART_PISTON = ResourceManager.rbmk_autoloader.partId("Piston");

    private static final RenderType LOADER =
            RenderTypes.entitySolid(ResourceManager.rbmk_autoloader_tex);

    private static final double TRAVEL = 4D;

    @Override
    public AABB getRenderBoundingBox(BlockEntityRBMKAutoloader be) {
        BlockPos p = be.getBlockPos();
        return new AABB(p.getX(), p.getY(), p.getZ(), p.getX() + 1, p.getY() + 9, p.getZ() + 1);
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
    public int getViewDistance() {
        return 256;
    }

    @Override
    public void extractRenderState(
            BlockEntityRBMKAutoloader be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.piston = Mth.lerp(partialTicks, be.lastPiston, be.renderPiston);
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);

        int light = state.lightCoords;
        HFRWavefrontObject model = ResourceManager.rbmk_autoloader;

        collector.submitCustomGeometry(
                poseStack,
                LOADER,
                (pose, buffer) -> model.renderPart(pose, buffer, light, -1, PART_BASE));

        poseStack.translate(0.0, state.piston * -TRAVEL + TRAVEL, 0.0);
        collector.submitCustomGeometry(
                poseStack,
                LOADER,
                (pose, buffer) -> model.renderPart(pose, buffer, light, -1, PART_PISTON));

        poseStack.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public double piston;
    }
}
