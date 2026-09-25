// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.client.model.Meshes;
import com.hbm.lib.Library;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityCargoElevator;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderCargoElevator
        implements BlockEntityRenderer<BlockEntityCargoElevator, RenderCargoElevator.State>,
                ConcurrentRenderStateExtraction {

    private final HFRWavefrontObject mesh = Meshes.load(Library.id("models/machines/elevator.obj"));
    private final int platform = mesh.partId("Platform");
    private final int piston = mesh.partId("Piston");
    private final RenderType type =
            RenderTypes.entityCutoutCull(Library.id("textures/block/models/machines/elevator.png"));

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public boolean shouldRenderOffScreen() {

        return true;
    }

    @Override
    public AABB getRenderBoundingBox(BlockEntityCargoElevator elevator) {
        BlockPos pos = elevator.getBlockPos();
        return new AABB(
                pos.getX() - 1,
                pos.getY() - 0.25,
                pos.getZ() - 1,
                pos.getX() + 2,
                pos.getY() + elevator.height + 1,
                pos.getZ() + 2);
    }

    @Override
    public void extractRenderState(
            BlockEntityCargoElevator elevator,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(
                elevator, state, partialTicks, cameraPosition, breaking);
        state.extension =
                elevator.prevExtension
                        + (elevator.extension - elevator.prevExtension) * partialTicks;
        state.renderPlatform = elevator.renderPlatform;
    }

    @Override
    public void submit(
            State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.renderPlatform) return;
        int light = state.lightCoords;
        pose.pushPose();
        pose.translate(0.5, state.extension, 0.5);
        collector.submitCustomGeometry(
                pose,
                type,
                (matrix, vertices) -> mesh.renderPart(matrix, vertices, light, -1, platform));
        for (int i = 0; i < state.extension + 1; i++) {
            collector.submitCustomGeometry(
                    pose,
                    type,
                    (matrix, vertices) -> mesh.renderPart(matrix, vertices, light, -1, piston));
            pose.translate(0, -1, 0);
        }
        pose.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public double extension;
        public boolean renderPlatform;
    }
}
