// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.BlockEntityLaunchpadSoyuz;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class RenderLaunchpadSoyuz
        implements BlockEntityRenderer<BlockEntityLaunchpadSoyuz, RenderLaunchpadSoyuz.State>,
                ConcurrentRenderStateExtraction {

    private static final String[] STRUTS = {"Strut1", "Strut2", "Strut3", "Strut4", "Strut5"};
    private static final double[] WHEEL_FORWARD = {17D, 19D, 29D, 31D};
    private static final double[] WHEEL_SIDE = {6.75D, 5.25D, -5.25D, -6.75D};
    private static final int CARRIAGE = ResourceManager.launchpad_soyuz.partId("Carriage");
    private static final int ROTOR = ResourceManager.launchpad_soyuz.partId("Rotor");
    private static final int MOUNT = ResourceManager.launchpad_soyuz.partId("Mount");
    private static final int[] STRUT_PARTS = parts(STRUTS);
    private static final int[][] WHEEL_PARTS = wheels();

    private static int[] parts(String[] names) {
        int[] result = new int[names.length];
        for (int i = 0; i < names.length; i++)
            result[i] = ResourceManager.launchpad_soyuz.partId(names[i]);
        return result;
    }

    private static int[][] wheels() {
        int[][] result = new int[4][4];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                result[i][j] =
                        ResourceManager.launchpad_soyuz.partId("Wheel_" + (i + 1) + "_" + (j + 1));
            }
        }
        return result;
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
    public AABB getRenderBoundingBox(BlockEntityLaunchpadSoyuz blockEntity) {
        return AABB.INFINITE;
    }

    @Override
    public void extractRenderState(
            BlockEntityLaunchpadSoyuz pad,
            State state,
            float partialTicks,
            Vec3 camera,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                pad, state, partialTicks, camera, breakProgress);
        state.rotation =
                switch (pad.getBlockState().getValue(BlockMultiblockCore.FACING)) {
                    case NORTH -> 90F;
                    case WEST -> 180F;
                    case SOUTH -> 270F;
                    default -> 0F;
                };
        for (int i = 0; i < state.positions.length; i++)
            state.positions[i] = pad.getInterpPos(i, partialTicks);
        state.loadedType = pad.loadedType;
        state.status = pad.soyuzStatus;
    }

    @Override
    public void submit(
            State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.translate(0.5D, 0D, 0.5D);
        pose.mulPose(Axis.YP.rotationDegrees(state.rotation));
        pose.translate(-4D, 0D, -4D);

        float rotor =
                Math.clamp(
                        state.positions[BlockEntityLaunchpadSoyuz.INDEX_ROTOR] * -180F + 180F,
                        0F,
                        180F);
        float carriage =
                Math.clamp(
                        state.positions[BlockEntityLaunchpadSoyuz.INDEX_CARRIAGE] * -19.5F + 19.5F,
                        0F,
                        19.5F);
        float wheels = (float) (carriage * 360D / Math.PI);
        float tilt = state.positions[BlockEntityLaunchpadSoyuz.INDEX_TILT];
        boolean renderRocket =
                state.loadedType >= 0
                        && state.status != BlockEntityLaunchpadSoyuz.SoyuzStatus.ABSENT;
        boolean locked = state.status == BlockEntityLaunchpadSoyuz.SoyuzStatus.LAUNCHING;

        if (renderRocket && locked) rocket(state, pose, collector);

        for (int i = 0; i < 5; i++) {
            pose.pushPose();
            float extension = i == 4 ? 3F : 4.5F;
            float shift = Math.clamp((1F - state.positions[i]) * extension, 0F, extension);
            pose.translate(0D, 0D, shift);
            part(pose, collector, state.lightCoords, STRUT_PARTS[i]);
            pose.popPose();
        }

        pose.translate(0D, 0D, -carriage);
        pose.translate(0D, 1.5D, -32D);
        pose.mulPose(Axis.XN.rotationDegrees(tilt));
        pose.translate(0D, -1.5D, 32D);
        part(pose, collector, state.lightCoords, CARRIAGE);

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                pose.pushPose();
                double side = WHEEL_SIDE[j], forward = WHEEL_FORWARD[i];
                pose.translate(side, 0D, -forward);
                pose.mulPose(Axis.YP.rotationDegrees(wheels * (j % 2 == 1 ? -1F : 1F)));
                pose.translate(-side, 0D, forward);
                part(pose, collector, state.lightCoords, WHEEL_PARTS[i][j]);
                pose.popPose();
            }
        }

        pose.translate(0D, 24.5D, -18D);
        pose.mulPose(Axis.XN.rotationDegrees(rotor));
        pose.translate(0D, -24.5D, 18D);
        part(pose, collector, state.lightCoords, ROTOR);

        pose.translate(0D, 24.5D, -6D);
        pose.mulPose(Axis.XP.rotationDegrees(rotor));
        pose.translate(0D, -24.5D, 6D);
        part(pose, collector, state.lightCoords, MOUNT);

        if (renderRocket && !locked) rocket(state, pose, collector);
        pose.popPose();
    }

    private static void rocket(State state, PoseStack pose, SubmitNodeCollector collector) {
        pose.pushPose();
        pose.translate(0D, 4D, 0D);
        pose.mulPose(Axis.YN.rotationDegrees(state.rotation));
        SoyuzMesh.submit(pose, collector, state.lightCoords, state.loadedType);
        pose.popPose();
    }

    private static void part(PoseStack pose, SubmitNodeCollector collector, int light, int part) {
        collector.submitCustomGeometry(
                pose,
                RenderTypes.entityCutoutCull(ResourceManager.launchpad_soyuz_tex),
                (p, buffer) ->
                        ResourceManager.launchpad_soyuz.renderPart(p, buffer, light, -1, part));
    }

    public static final class State extends BlockEntityRenderState {
        final float[] positions = new float[8];
        float rotation;
        int loadedType = -1;
        BlockEntityLaunchpadSoyuz.SoyuzStatus status = BlockEntityLaunchpadSoyuz.SoyuzStatus.ABSENT;
    }
}
