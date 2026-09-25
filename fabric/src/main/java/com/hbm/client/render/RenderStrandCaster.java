// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.render.util.Vertices;
import com.hbm.tileentity.machine.BlockEntityMachineStrandCaster;
import com.hbm.util.Facing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderStrandCaster
        implements BlockEntityRenderer<BlockEntityMachineStrandCaster, RenderStrandCaster.State>,
                ConcurrentRenderStateExtraction {

    private static final int PLATE = ResourceManager.strand_caster.partId("plate");

    private static final double STRAND_REST = 3.4D;
    private static final double STRAND_PER_CAST = 0.375D;
    private static final double MELT_DEPTH = 0.675D;
    private static final double MELT_FLOOR = 2.3D;

    private final RenderType plateType;
    private final RenderType meltType;

    public RenderStrandCaster() {
        this.plateType = FlatCutout.of(ResourceManager.strand_caster_tex);
        this.meltType = FlatCutout.of(ResourceManager.foundry_stream_tex);
    }

    private static void vertex(
            PoseStack.Pose pose,
            VertexConsumer buf,
            int argb,
            float x,
            float y,
            float z,
            float u,
            float v) {
        Vertices.emit(buf, pose, x, y, z, argb, u, v, LightCoordsUtil.FULL_BRIGHT, 0F, 1F, 0F);
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
            BlockEntityMachineStrandCaster be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);

        state.yaw = Facing.yaw(BlockMultiblockCore.coreFacing(be.getBlockState()), 0);

        var mold = be.getInstalledMold();
        state.molten = be.amount != 0 && be.type != null && mold != null;
        if (!state.molten) return;

        state.color = ARGB.opaque(be.type.moltenColor);
        state.melt = ((double) be.amount / (double) be.getCapacity()) * MELT_DEPTH;
        state.strand =
                Math.max(
                        STRAND_REST
                                - ((double) be.amount / (double) mold.getCost()) * STRAND_PER_CAST,
                        0D);
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        if (!s.molten) return;

        int light = s.lightCoords;
        int color = s.color;

        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(s.yaw));
        ps.translate(0.5, 0.0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(180F));

        ps.pushPose();
        ps.translate(0.0, 0.0, s.strand);

        final float d = (float) (s.strand - 0.5D);
        col.submitCustomGeometry(
                ps,
                plateType,
                (pose, buffer) ->
                        ResourceManager.strand_caster.renderPartClipped(
                                pose, buffer, light, color, PLATE, 0F, 0F, -1F, d));
        ps.popPose();

        final float y = (float) (MELT_FLOOR + s.melt);
        col.submitCustomGeometry(
                ps,
                meltType,
                (pose, buf) -> {
                    vertex(pose, buf, color, -0.9F, y, -0.999F, 0F, 0F);
                    vertex(pose, buf, color, -0.9F, y, 0.999F, 0F, 1F);
                    vertex(pose, buf, color, 0.9F, y, 0.999F, 1F, 1F);
                    vertex(pose, buf, color, 0.9F, y, -0.999F, 1F, 0F);
                });

        ps.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public float yaw;
        public boolean molten;
        public int color;
        public double melt;
        public double strand;
    }
}
