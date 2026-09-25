// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.handler.MissileStruct;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.bomb.BlockEntityCompactLauncher;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class RenderCompactLauncher
        implements BlockEntityRenderer<BlockEntityCompactLauncher, RenderCompactLauncher.State>,
                ConcurrentRenderStateExtraction {

    private static final double MISSILE_Y = 1.0625D;

    private static final RenderType BODY =
            RenderTypes.entityCutoutCull(ResourceManager.compact_launcher_tex);

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
    public void extractRenderState(
            BlockEntityCompactLauncher launcher,
            State state,
            float partialTicks,
            Vec3 camera,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                launcher, state, partialTicks, camera, breakProgress);
        state.parts = launcher.loadedMissile;
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        int light = state.lightCoords;
        poseStack.pushPose();
        poseStack.translate(0.5D, 0D, 0.5D);
        collector.submitCustomGeometry(
                poseStack,
                BODY,
                (pose, buffer) -> ResourceManager.compact_launcher.render(pose, buffer, light, -1));
        poseStack.translate(0D, MISSILE_Y, 0D);
        pronter.pront(state.parts, poseStack, collector, light);
        poseStack.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        MissileStruct parts = MissileStruct.EMPTY;
    }
}
