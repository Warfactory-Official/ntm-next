// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntitySoyuzLauncher;
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
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class RenderSoyuzLauncher
        implements BlockEntityRenderer<BlockEntitySoyuzLauncher, RenderSoyuzLauncher.State>,
                ConcurrentRenderStateExtraction {

    private static final float OPEN = 45F;
    private static final int SWING_TICKS = 20;

    private static final double MESH_DROP = 4D;

    private static final double ROCKET_LIFT = 5D;
    private static final double TOWER_PIVOT_Y = 5.5D;
    private static final double TOWER_PIVOT_Z = 5.5D;
    private static final double SUPPORT_PIVOT_Z = -6.5D;

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
    public AABB getRenderBoundingBox(BlockEntitySoyuzLauncher launcher) {
        return AABB.INFINITE;
    }

    @Override
    public void extractRenderState(
            BlockEntitySoyuzLauncher launcher,
            State state,
            float partialTicks,
            Vec3 camera,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                launcher, state, partialTicks, camera, breakProgress);
        state.rocket = launcher.rocketType;

        float swing = state.rocket >= 0 ? 0F : OPEN;
        if (launcher.starting && launcher.countdown < SWING_TICKS) {
            swing = (SWING_TICKS - launcher.countdown + partialTicks) * OPEN / SWING_TICKS;
        }
        state.swing = swing;
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        int light = state.lightCoords;
        poseStack.pushPose();
        poseStack.translate(0.5D, -MESH_DROP, 0.5D);

        part(
                poseStack,
                collector,
                light,
                ResourceManager.soyuz_launcher_legs,
                ResourceManager.soyuz_launcher_legs_tex);
        part(
                poseStack,
                collector,
                light,
                ResourceManager.soyuz_launcher_table,
                ResourceManager.soyuz_launcher_table_tex);
        part(
                poseStack,
                collector,
                light,
                ResourceManager.soyuz_launcher_tower_base,
                ResourceManager.soyuz_launcher_tower_base_tex);

        poseStack.pushPose();
        poseStack.translate(0D, TOWER_PIVOT_Y, TOWER_PIVOT_Z);
        poseStack.mulPose(Axis.XP.rotationDegrees(state.swing));
        poseStack.translate(0D, -TOWER_PIVOT_Y, -TOWER_PIVOT_Z);
        part(
                poseStack,
                collector,
                light,
                ResourceManager.soyuz_launcher_tower,
                ResourceManager.soyuz_launcher_tower_tex);
        poseStack.popPose();

        part(
                poseStack,
                collector,
                light,
                ResourceManager.soyuz_launcher_support_base,
                ResourceManager.soyuz_launcher_support_base_tex);

        poseStack.pushPose();
        poseStack.translate(0D, TOWER_PIVOT_Y, SUPPORT_PIVOT_Z);
        poseStack.mulPose(Axis.XN.rotationDegrees(state.swing));
        poseStack.translate(0D, -TOWER_PIVOT_Y, -SUPPORT_PIVOT_Z);
        part(
                poseStack,
                collector,
                light,
                ResourceManager.soyuz_launcher_support,
                ResourceManager.soyuz_launcher_support_tex);
        poseStack.popPose();

        if (SoyuzMesh.hasSkin(state.rocket)) {
            poseStack.translate(0D, ROCKET_LIFT, 0D);
            SoyuzMesh.submit(poseStack, collector, light, state.rocket);
        }

        poseStack.popPose();
    }

    private static void part(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int light,
            HFRWavefrontObject mesh,
            Identifier texture) {
        RenderType type = RenderTypes.entityCutoutCull(texture);
        collector.submitCustomGeometry(
                poseStack, type, (pose, buffer) -> mesh.render(pose, buffer, light, -1));
    }

    public static final class State extends BlockEntityRenderState {
        int rocket = -1;
        float swing;
    }
}
