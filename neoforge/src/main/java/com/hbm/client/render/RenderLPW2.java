// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityMachineLPW2;
import com.hbm.util.BobMathUtil;
import com.hbm.util.Facing;
import com.hbm.util.GameTime;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderLPW2
        implements BlockEntityRenderer<BlockEntityMachineLPW2, RenderLPW2.State>,
                ConcurrentRenderStateExtraction {

    private static final int CENTER = ResourceManager.lpw2.partId("Center");
    private static final int ROTOR = ResourceManager.lpw2.partId("Rotor");
    private static final int TURBINE_FRONT = ResourceManager.lpw2.partId("TurbineFront");
    private static final int TURBINE_BACK = ResourceManager.lpw2.partId("TurbineBack");
    private static final int PISTON = ResourceManager.lpw2.partId("Piston");
    private static final int ENGINE = ResourceManager.lpw2.partId("Engine");
    private static final int SHROUD_H = ResourceManager.lpw2.partId("ShroudH");
    private static final int SHROUD_V = ResourceManager.lpw2.partId("ShroudV");
    private static final int FLAP = ResourceManager.lpw2.partId("Flap");
    private static final int SUSPENSION_LEFT = ResourceManager.lpw2.partId("SuspensionLeft");
    private static final int SUSPENSION_RIGHT = ResourceManager.lpw2.partId("SuspensionRight");
    private static final int SUSPENSION_TOP = ResourceManager.lpw2.partId("SuspensionTop");
    private static final int SUSPENSION_BOTTOM = ResourceManager.lpw2.partId("SuspensionBottom");
    private static final int WIRE_LEFT = ResourceManager.lpw2.partId("WireLeft");
    private static final int WIRE_RIGHT = ResourceManager.lpw2.partId("WireRight");
    private static final int COVER = ResourceManager.lpw2.partId("Cover");
    private static final int SUSPENSION_COVER_FRONT =
            ResourceManager.lpw2.partId("SuspensionCoverFront");
    private static final int SUSPENSION_COVER_BACK =
            ResourceManager.lpw2.partId("SuspensionCoverBack");
    private static final int SUSPENSION_BACK_OUTER =
            ResourceManager.lpw2.partId("SuspensionBackOuter");
    private static final int SUSPENSION_BACK_CENTER =
            ResourceManager.lpw2.partId("SuspensionBackCenter");
    private static final int SERVER_1 = ResourceManager.lpw2.partId("Server1");
    private static final int SERVER_2 = ResourceManager.lpw2.partId("Server2");
    private static final int SERVER_3 = ResourceManager.lpw2.partId("Server3");
    private static final int SERVER_4 = ResourceManager.lpw2.partId("Server4");
    private static final int MONITOR = ResourceManager.lpw2.partId("Monitor");
    private static final int SCREEN = ResourceManager.lpw2.partId("Screen");

    private static final double SERVER_SWAY = 0.0625D * 0.25D;

    private final HFRWavefrontObject model;
    private final RenderType bodyType;
    private final RenderType screenType;

    public RenderLPW2() {
        this.model = ResourceManager.lpw2;
        this.bodyType = RenderTypes.entityCutoutCull(ResourceManager.lpw2_tex);
        this.screenType = RenderTypes.entityCutoutCull(ResourceManager.lpw2_error_tex);
    }

    private static float facingYaw(Direction facing) {
        return Facing.yaw(facing, 90);
    }

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
    public AABB getRenderBoundingBox(BlockEntityMachineLPW2 be) {
        BlockPos pos = be.getBlockPos();
        return new AABB(
                pos.getX() - 10,
                pos.getY() - 0.0625,
                pos.getZ() - 10,
                pos.getX() + 11,
                pos.getY() + 7.0625,
                pos.getZ() + 11);
    }

    @Override
    public void extractRenderState(
            BlockEntityMachineLPW2 be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.yaw = facingYaw(be.getBlockState().getValue(BlockMultiblockCore.FACING));

        double time = (GameTime.now() % 50_000_000L) / 50D;

        double swayTimer = (time / 3D) % (Math.PI * 4);
        state.sway =
                (Math.sin(swayTimer) + Math.sin(swayTimer * 2) + Math.sin(swayTimer * 4) + 2.23255D)
                        * 0.5;

        double bellTimer = (time / 5D) % (Math.PI * 4);
        state.h = (Math.sin(bellTimer + Math.PI) + Math.sin(bellTimer * 1.5D)) / 1.90596D;
        state.v = (Math.sin(bellTimer) + Math.sin(bellTimer * 1.5D)) / 1.90596D;

        double pistonTimer = (time / 5D) % (Math.PI * 2);
        state.piston = BobMathUtil.sps(pistonTimer);
        double rotorTimer = (time / 5D) % (Math.PI * 16);
        state.rotor = (BobMathUtil.sps(rotorTimer) + rotorTimer / 2D - 1) / 25.1327412287D;
        state.turbine = (time % 100) / 100D;

        double coverTimer = (time / 5D) % (Math.PI * 4);
        state.cover =
                (Math.sin(coverTimer) + Math.sin(coverTimer * 2) + Math.sin(coverTimer * 4)) * 0.5;

        double serverTimer = (time / 2D) % (Math.PI * 4);
        state.sx = (Math.sin(serverTimer + Math.PI) + Math.sin(serverTimer * 1.5D)) / 1.90596D;
        state.sy = (Math.sin(serverTimer) + Math.sin(serverTimer * 1.5D)) / 1.90596D;

        double errorTimer = time / 3D;
        state.errorScroll = (BobMathUtil.sps(errorTimer) + errorTimer / 2D) % 1;
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(s.yaw));

        mainAssembly(s, ps, col, light);

        ps.pushPose();
        ps.translate(-2.9375, 0, 2.375);
        ps.mulPose(Axis.YP.rotationDegrees((float) (s.sway * 10)));
        ps.translate(2.9375, 0, -2.375);
        part(col, ps, light, WIRE_LEFT);
        ps.popPose();

        ps.pushPose();
        ps.translate(2.9375, 0, 2.375);
        ps.mulPose(Axis.YP.rotationDegrees((float) (s.sway * -10)));
        ps.translate(-2.9375, 0, -2.375);
        part(col, ps, light, WIRE_RIGHT);
        ps.popPose();

        ps.pushPose();
        ps.translate(0, 0, -s.cover * 0.125);
        part(col, ps, light, COVER);
        ps.popPose();

        ps.pushPose();
        ps.translate(0, 0, 3.5);
        ps.scale(1F, 1F, (float) ((3 + s.cover * 0.125) / 3));
        ps.translate(0, 0, -3.5);
        part(col, ps, light, SUSPENSION_COVER_FRONT);
        ps.popPose();

        ps.pushPose();
        ps.translate(0, 0, -5.5);
        ps.scale(1F, 1F, (float) ((1.5 - s.cover * 0.125) / 1.5));
        ps.translate(0, 0, 5.5);
        part(col, ps, light, SUSPENSION_COVER_BACK);
        ps.popPose();

        ps.pushPose();
        ps.translate(0, 0, -9);
        ps.scale(1F, 1F, (float) ((1.25 - s.sway * 0.125) / 1.25));
        ps.translate(0, 0, 9);
        part(col, ps, light, SUSPENSION_BACK_OUTER);
        ps.popPose();

        ps.pushPose();
        ps.translate(0, 0, -9.5);
        ps.scale(1F, 1F, (float) ((1.75 - s.sway * 0.125) / 1.75));
        ps.translate(0, 0, 9.5);
        part(col, ps, light, SUSPENSION_BACK_CENTER);
        ps.popPose();

        server(col, ps, light, s.sx * SERVER_SWAY, s.sy * SERVER_SWAY, SERVER_1);
        server(col, ps, light, -s.sy * SERVER_SWAY, s.sx * SERVER_SWAY, SERVER_2);
        server(col, ps, light, s.sy * SERVER_SWAY, -s.sx * SERVER_SWAY, SERVER_3);
        server(col, ps, light, -s.sx * SERVER_SWAY, -s.sy * SERVER_SWAY, SERVER_4);

        ps.pushPose();
        ps.translate(s.sy * SERVER_SWAY, 0, s.sx * SERVER_SWAY);
        part(col, ps, light, MONITOR);

        float scroll = (float) s.errorScroll;
        final HFRWavefrontObject m = this.model;
        col.submitCustomGeometry(
                ps,
                screenType,
                (pose, buffer) ->
                        m.renderPart(pose, buffer, light, -1, SCREEN, 1F, 1F, 0F, scroll));
        ps.popPose();

        ps.popPose();
    }

    private void mainAssembly(State s, PoseStack ps, SubmitNodeCollector col, int light) {
        ps.pushPose();
        ps.translate(0, 0, -s.sway * 0.125);
        part(col, ps, light, CENTER);

        ps.pushPose();
        ps.translate(0, 3.5, 0);

        ps.pushPose();
        ps.mulPose(Axis.ZN.rotationDegrees((float) (s.rotor * 360)));
        ps.translate(0, -3.5, 0);
        part(col, ps, light, ROTOR);
        ps.popPose();

        ps.pushPose();
        ps.mulPose(Axis.ZP.rotationDegrees((float) (s.turbine * 360)));
        ps.translate(0, -3.5, 0);
        part(col, ps, light, TURBINE_FRONT);
        ps.popPose();

        ps.pushPose();
        ps.mulPose(Axis.ZN.rotationDegrees((float) (s.turbine * 360)));
        ps.translate(0, -3.5, 0);
        part(col, ps, light, TURBINE_BACK);
        ps.popPose();

        ps.popPose();

        ps.pushPose();
        ps.translate(0, 0, s.piston * 0.375D + 0.375D);
        part(col, ps, light, PISTON);
        ps.popPose();

        bell(s, ps, col, light);
        ps.popPose();

        shroud(s, ps, col, light);
    }

    private void bell(State s, PoseStack ps, SubmitNodeCollector col, int light) {
        ps.pushPose();
        ps.translate(0, 3.5, 2.75);
        double magnitude = 2D;
        ps.mulPose(Axis.YP.rotationDegrees((float) (s.v * magnitude)));
        ps.mulPose(Axis.XP.rotationDegrees((float) (s.h * magnitude)));
        ps.translate(0, -3.5, -2.75);
        part(col, ps, light, ENGINE);
        ps.popPose();
    }

    private void shroud(State s, PoseStack ps, SubmitNodeCollector col, int light) {
        double magnitude = 0.125D;
        double rotation = 5D;
        double offset = 10D;

        ps.pushPose();
        ps.translate(0, -s.h * magnitude, 0);
        part(col, ps, light, SHROUD_H);

        flap(ps, col, light, 90 + 22.5D, rotation * s.v + offset);
        flap(ps, col, light, 90 - 22.5D, rotation * s.v + offset);
        flap(ps, col, light, 270 + 22.5D, rotation * -s.v + offset);
        flap(ps, col, light, 270 - 22.5D, rotation * -s.v + offset);

        ps.popPose();

        ps.pushPose();
        ps.translate(s.v * magnitude, 0, 0);
        part(col, ps, light, SHROUD_V);

        flap(ps, col, light, 22.5D, rotation * s.h + offset);
        flap(ps, col, light, -22.5D, rotation * s.h + offset);
        flap(ps, col, light, 180 + 22.5D, rotation * -s.h + offset);
        flap(ps, col, light, 180 - 22.5D, rotation * -s.h + offset);

        ps.popPose();

        double length = 0.6875D;

        ps.pushPose();
        ps.translate(-2.625D, 0, 0);
        ps.scale((float) ((length + s.v * magnitude) / length), 1F, 1F);
        ps.translate(2.625D, 0, 0);
        part(col, ps, light, SUSPENSION_LEFT);
        ps.popPose();

        ps.pushPose();
        ps.translate(2.625D, 0, 0);
        ps.scale((float) ((length - s.v * magnitude) / length), 1F, 1F);
        ps.translate(-2.625D, 0, 0);
        part(col, ps, light, SUSPENSION_RIGHT);
        ps.popPose();

        ps.pushPose();
        ps.translate(0, 6.125D, 0);
        ps.scale(1F, (float) ((length + s.h * magnitude) / length), 1F);
        ps.translate(0, -6.125D, 0);
        part(col, ps, light, SUSPENSION_TOP);
        ps.popPose();

        ps.pushPose();
        ps.translate(0, 0.875D, 0);
        ps.scale(1F, (float) ((length - s.h * magnitude) / length), 1F);
        ps.translate(0, -0.875D, 0);
        part(col, ps, light, SUSPENSION_BOTTOM);
        ps.popPose();
    }

    private void flap(
            PoseStack ps, SubmitNodeCollector col, int light, double position, double rotation) {
        ps.pushPose();

        ps.translate(0, 3.5D, 0);
        ps.mulPose(Axis.ZP.rotationDegrees((float) position));
        ps.translate(0, -3.5D, 0);

        ps.translate(0, 6.96875D, 8.5D);
        ps.mulPose(Axis.XP.rotationDegrees((float) rotation));
        ps.translate(0, -6.96875D, -8.5D);

        part(col, ps, light, FLAP);
        ps.popPose();
    }

    private void server(
            SubmitNodeCollector col, PoseStack ps, int light, double x, double z, int name) {
        ps.pushPose();
        ps.translate(x, 0, z);
        part(col, ps, light, name);
        ps.popPose();
    }

    private void part(SubmitNodeCollector col, PoseStack ps, int light, int name) {
        final HFRWavefrontObject m = this.model;
        col.submitCustomGeometry(
                ps, bodyType, (pose, buffer) -> m.renderPart(pose, buffer, light, -1, name));
    }

    public static final class State extends BlockEntityRenderState {
        public float yaw;
        public double sway;
        public double h;
        public double v;
        public double piston;
        public double rotor;
        public double turbine;
        public double cover;
        public double sx;
        public double sy;
        public double errorScroll;
    }
}
