// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.animloader.AnimatedModel;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.BlockEntityDoorGeneric;
import com.hbm.tileentity.DoorDecl;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.BlendFactor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class RenderDoorGeneric
        implements BlockEntityRenderer<BlockEntityDoorGeneric, RenderDoorGeneric.State>,
                ConcurrentRenderStateExtraction {

    static final RenderPipeline TRANSLUCENT_CULL_PIPELINE =
            WorldRenderPipeline.of(
                    RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
                            .withLocation("pipeline/ntm_door_translucent_cull")
                            .withShaderDefine("ALPHA_CUTOUT", 0.1F)
                            .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
                            .withColorTargetState(
                                    new ColorTargetState(
                                            new BlendFunction(
                                                    BlendFactor.SRC_ALPHA,
                                                    BlendFactor.ONE_MINUS_SRC_ALPHA))));

    private static final Function<Identifier, RenderType> TRANSLUCENT_CULL =
            Util.memoize(
                    texture ->
                            RenderType.create(
                                    "ntm_door_translucent_cull",
                                    RenderSetup.builder(TRANSLUCENT_CULL_PIPELINE)
                                            .withTexture("Sampler0", texture)
                                            .useLightmap()
                                            .useOverlay()
                                            .affectsCrumbling()
                                            .sortOnUpload()
                                            .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                                            .createRenderSetup()));

    private static RenderType typeFor(Identifier texture, boolean cull, boolean blended) {
        if (blended)
            return cull
                    ? TRANSLUCENT_CULL.apply(texture)
                    : WorldRenderPipeline.oneSidedTranslucent(texture);
        return cull
                ? RenderTypes.entityCutoutCull(texture)
                : WorldRenderPipeline.oneSidedCutout(texture);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    private static final Map<DoorDecl, float[]> EXTENTS = new HashMap<>();

    @Override
    public AABB getRenderBoundingBox(BlockEntityDoorGeneric be) {
        DoorDecl door = be.getDoorType();
        BlockPos pos = be.getBlockPos();
        if (door == null) return new AABB(pos).inflate(1.0D);

        float[] extent = EXTENTS.computeIfAbsent(door, DoorState::extent);
        double radius = extent[2];
        return new AABB(
                        pos.getX() + 0.5D - radius,
                        pos.getY() + extent[0],
                        pos.getZ() + 0.5D - radius,
                        pos.getX() + 0.5D + radius,
                        pos.getY() + extent[1],
                        pos.getZ() + 0.5D + radius)
                .inflate(0.5D);
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public void extractRenderState(
            BlockEntityDoorGeneric door,
            State state,
            float partialTicks,
            Vec3 camera,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                door, state, partialTicks, camera, breakProgress);
        state.door.sample(door, partialTicks);
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        DoorState door = state.door;
        DoorFrame frame = door.emit();
        int light = state.lightCoords;
        boolean cull = frame.cull;
        boolean blended = frame.blended;

        poseStack.pushPose();
        poseStack.translate(.5D, 0D, .5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(door.facingYaw()));

        for (int i = 0; i < frame.size(); i++) {
            DoorFrame.Part part = frame.get(i);
            if (part.hidden) continue;

            poseStack.pushPose();
            poseStack.mulPose(part.drawPose());
            RenderType type = typeFor(part.texture, cull, blended);

            AnimatedModel.Mesh mesh = part.mesh;
            if (mesh != null) {
                collector.submitCustomGeometry(
                        poseStack,
                        type,
                        (pose, buffer) ->
                                mesh.render(pose.pose(), buffer, light, OverlayTexture.NO_OVERLAY));
            } else {
                HFRWavefrontObject model = part.model;
                int group = part.group;
                float[][] planes = part.cpuPlanes();
                if (planes == null) {
                    collector.submitCustomGeometry(
                            poseStack,
                            type,
                            (pose, buffer) -> model.renderPart(pose, buffer, light, -1, group));
                } else {
                    collector.submitCustomGeometry(
                            poseStack,
                            type,
                            (pose, buffer) ->
                                    model.renderPartClipped(
                                            pose, buffer, light, -1, group, planes));
                }
            }
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public final DoorState door = new DoorState();
    }
}
