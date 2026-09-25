// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.render.util.Vertices;
import com.hbm.tileentity.machine.BlockEntityMachineExcavator;
import com.hbm.util.Facing;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
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

public class RenderExcavator
        implements BlockEntityRenderer<BlockEntityMachineExcavator, RenderExcavator.State>,
                ConcurrentRenderStateExtraction {

    private static final int CRUSHER1 = ResourceManager.mining_drill.partId("Crusher1");
    private static final int CRUSHER2 = ResourceManager.mining_drill.partId("Crusher2");
    private static final int DRILLBIT = ResourceManager.mining_drill.partId("Drillbit");
    private static final int SHAFT = ResourceManager.mining_drill.partId("Shaft");

    private static final float BODY_LIFT = -3F;
    private static final double CRUSHER1_PIVOT_Z = 2.8125D;
    private static final double CRUSHER2_PIVOT_Z = 2.1875D;
    private static final double CRUSHER_PIVOT_Y = 2.0D;

    private static final double SHAFT_STEP = 2.0D;
    private static final double SHAFT_END = -1.5D;

    private static final double SCROLL_PERIOD = 250D;

    private final RenderType bodyType;
    private final RenderType cobbleType;
    private final RenderType gravelType;

    public RenderExcavator() {
        this.bodyType = RenderTypes.entityCutoutCull(ResourceManager.mining_drill_tex);
        this.cobbleType = RenderTypes.entityCutoutCull(ResourceManager.mining_drill_cobble_tex);
        this.gravelType = RenderTypes.entityCutoutCull(ResourceManager.mining_drill_gravel_tex);
    }

    private static void vertex(
            PoseStack.Pose pose,
            VertexConsumer buf,
            int light,
            float nx,
            float ny,
            float nz,
            double x,
            double y,
            double z,
            double u,
            double v) {
        Vertices.emit(
                buf, pose, (float) x, (float) y, (float) z, -1, (float) u, (float) v, light, nx, ny,
                nz);
    }

    private static void chuteBox(
            PoseStack.Pose pose,
            VertexConsumer buf,
            int light,
            double widthX,
            double widthZ,
            double top,
            double bottom,
            double uEdge,
            double uSide,
            double vTop,
            double vBottom) {
        vertex(pose, buf, light, 0F, 0F, 1F, widthX, top, 2.5 + widthZ, 0, vTop);
        vertex(pose, buf, light, 0F, 0F, 1F, -widthX, top, 2.5 + widthZ, uEdge, vTop);
        vertex(pose, buf, light, 0F, 0F, 1F, -widthX, bottom, 2.5 + widthZ, uEdge, vBottom);
        vertex(pose, buf, light, 0F, 0F, 1F, widthX, bottom, 2.5 + widthZ, 0, vBottom);

        vertex(pose, buf, light, 0F, 0F, -1F, -widthX, top, 2.5 - widthZ, uEdge, vTop);
        vertex(pose, buf, light, 0F, 0F, -1F, widthX, top, 2.5 - widthZ, 0, vTop);
        vertex(pose, buf, light, 0F, 0F, -1F, widthX, bottom, 2.5 - widthZ, 0, vBottom);
        vertex(pose, buf, light, 0F, 0F, -1F, -widthX, bottom, 2.5 - widthZ, uEdge, vBottom);

        vertex(pose, buf, light, -1F, 0F, 0F, -widthX, top, 2.5 + widthZ, 0, vTop);
        vertex(pose, buf, light, -1F, 0F, 0F, -widthX, top, 2.5 - widthZ, uSide, vTop);
        vertex(pose, buf, light, -1F, 0F, 0F, -widthX, bottom, 2.5 - widthZ, uSide, vBottom);
        vertex(pose, buf, light, -1F, 0F, 0F, -widthX, bottom, 2.5 + widthZ, 0, vBottom);

        vertex(pose, buf, light, 1F, 0F, 0F, widthX, top, 2.5 - widthZ, uSide, vTop);
        vertex(pose, buf, light, 1F, 0F, 0F, widthX, top, 2.5 + widthZ, 0, vTop);
        vertex(pose, buf, light, 1F, 0F, 0F, widthX, bottom, 2.5 + widthZ, 0, vBottom);
        vertex(pose, buf, light, 1F, 0F, 0F, widthX, bottom, 2.5 - widthZ, uSide, vBottom);
    }

    @Override
    public AABB getRenderBoundingBox(BlockEntityMachineExcavator be) {
        BlockPos pos = be.getBlockPos();
        int floor = be.getLevel() != null ? be.getLevel().getMinY() : pos.getY() - 64;
        return new AABB(
                pos.getX() - 3,
                floor - 0.5,
                pos.getZ() - 3,
                pos.getX() + 4,
                pos.getY() + 5,
                pos.getZ() + 4);
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
            BlockEntityMachineExcavator be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);

        state.yaw = Facing.yaw(BlockMultiblockCore.coreFacing(be.getBlockState()), 180);
        state.drillRotation = Mth.lerp(partialTicks, be.prevDrillRotation, be.drillRotation);
        state.crusherRotation = Mth.lerp(partialTicks, be.prevCrusherRotation, be.crusherRotation);
        state.extension = Mth.lerp(partialTicks, be.prevDrillExtension, be.drillExtension);
        state.chute = be.chuteTimer > 0;
        state.crushing = be.enableCrusher;
        state.scroll = -(GameTime.now() % (long) SCROLL_PERIOD) / SCROLL_PERIOD;
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;

        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(s.yaw));
        ps.translate(0.0, BODY_LIFT, 0.0);

        ps.pushPose();
        ps.translate(0.0, CRUSHER_PIVOT_Y, CRUSHER1_PIVOT_Z);
        ps.mulPose(Axis.XP.rotationDegrees(-s.crusherRotation));
        ps.translate(0.0, -CRUSHER_PIVOT_Y, -CRUSHER1_PIVOT_Z);
        col.submitCustomGeometry(
                ps,
                bodyType,
                (pose, buf) ->
                        ResourceManager.mining_drill.renderPart(pose, buf, light, -1, CRUSHER1));
        ps.popPose();

        ps.pushPose();
        ps.translate(0.0, CRUSHER_PIVOT_Y, CRUSHER2_PIVOT_Z);
        ps.mulPose(Axis.XP.rotationDegrees(s.crusherRotation));
        ps.translate(0.0, -CRUSHER_PIVOT_Y, -CRUSHER2_PIVOT_Z);
        col.submitCustomGeometry(
                ps,
                bodyType,
                (pose, buf) ->
                        ResourceManager.mining_drill.renderPart(pose, buf, light, -1, CRUSHER2));
        ps.popPose();

        ps.pushPose();
        ps.mulPose(Axis.YN.rotationDegrees(s.drillRotation));
        ps.translate(0.0, -s.extension, 0.0);
        col.submitCustomGeometry(
                ps,
                bodyType,
                (pose, buf) ->
                        ResourceManager.mining_drill.renderPart(pose, buf, light, -1, DRILLBIT));

        for (double ext = s.extension; ext >= SHAFT_END; ext -= SHAFT_STEP) {
            col.submitCustomGeometry(
                    ps,
                    bodyType,
                    (pose, buf) ->
                            ResourceManager.mining_drill.renderPart(pose, buf, light, -1, SHAFT));
            ps.translate(0.0, SHAFT_STEP, 0.0);
        }
        ps.popPose();

        if (s.chute) {
            double vTop = s.scroll;
            double vBottom = vTop + 4;

            col.submitCustomGeometry(
                    ps,
                    cobbleType,
                    (pose, buf) ->
                            chuteBox(pose, buf, light, 0.125, 0.125, 3, 2, 1, 1, vTop, vBottom));

            double widthX = s.crushing ? 0.5 : 0.25;
            double uEdge = s.crushing ? 4 : 2;
            col.submitCustomGeometry(
                    ps,
                    s.crushing ? gravelType : cobbleType,
                    (pose, buf) ->
                            chuteBox(
                                    pose, buf, light, widthX, 0.0625, 2, 1, uEdge, 0.5, vTop,
                                    vBottom));
        }

        ps.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public float yaw;
        public float drillRotation;
        public float crusherRotation;
        public float extension;
        public boolean chute;
        public boolean crushing;
        public double scroll;
    }
}
