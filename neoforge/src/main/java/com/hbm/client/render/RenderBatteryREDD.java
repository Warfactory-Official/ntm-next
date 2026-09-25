// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.render.util.BeamPronter;
import com.hbm.render.util.Vertices;
import com.hbm.tileentity.machine.storage.BlockEntityBatteryREDD;
import com.hbm.util.BobMathUtil;
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
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderBatteryREDD
        implements BlockEntityRenderer<BlockEntityBatteryREDD, RenderBatteryREDD.State>,
                ConcurrentRenderStateExtraction {

    private static final int WHEEL = ResourceManager.battery_redd.partId("Wheel");
    private static final int LIGHTS = ResourceManager.battery_redd.partId("Lights");
    private static final int PLASMA = ResourceManager.battery_redd.partId("Plasma");

    private static final double PIVOT = 5.5D;

    private static final double LEN = 4.25D;
    private static final double WIDTH = 0.125D;

    private static final double ARC_X = 0.8125D;

    private static final double SPARKLE_RANGE = 100D;

    private final HFRWavefrontObject model = ResourceManager.battery_redd;
    private final RenderType bodyType =
            RenderTypes.entityCutoutCull(ResourceManager.battery_redd_tex);
    private final RenderType lightsType = FlatCutout.of(ResourceManager.battery_redd_tex);
    private final RenderType plasmaType =
            FusionPlasmaRenderTypes.plasma(ResourceManager.fusion_plasma_tex);
    private final RenderType sparkleType =
            FusionPlasmaRenderTypes.plasma(ResourceManager.fusion_plasma_sparkle_tex);

    private static float facingYaw(Direction facing) {
        return Facing.yaw(facing, 270);
    }

    private static double spokeY(double degrees) {
        return Math.cos(degrees * Mth.DEG_TO_RAD);
    }

    private static double spokeZ(double degrees) {
        return -Math.sin(degrees * Mth.DEG_TO_RAD);
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
    public AABB getRenderBoundingBox(BlockEntityBatteryREDD be) {
        return AABB.INFINITE;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public void extractRenderState(
            BlockEntityBatteryREDD be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.yaw = facingYaw(BlockMultiblockCore.coreFacing(be.getBlockState()));
        state.rot = Mth.lerp(partialTicks, be.prevRotation, be.rotation);
        state.speed = be.getSpeed();
        state.timeMs = GameTime.now();

        state.nearSparkle =
                cameraPosition.distanceToSqr(
                                be.getBlockPos().getX() + 0.5,
                                be.getBlockPos().getY() + 2.5,
                                be.getBlockPos().getZ() + 0.5)
                        < SPARKLE_RANGE * SPARKLE_RANGE;

        state.zapSeed = be.getLevel() == null ? 0L : be.getLevel().getGameTime() / 5L;
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(s.yaw));

        ps.pushPose();
        ps.translate(0, PIVOT, 0);
        ps.mulPose(Axis.XP.rotationDegrees(s.rot));
        ps.translate(0, -PIVOT, 0);

        col.submitCustomGeometry(
                ps,
                bodyType,
                (pose, buffer) -> model.renderPart(pose, buffer, s.lightCoords, -1, WHEEL));
        col.submitCustomGeometry(
                ps,
                lightsType,
                (pose, buffer) ->
                        model.renderPart(pose, buffer, LightCoordsUtil.FULL_BRIGHT, -1, LIGHTS));

        ps.pushPose();
        ps.translate(0, PIVOT, 0);
        submitArc(s, ps, col);
        ps.popPose();

        submitSparkle(s, ps, col);
        ps.popPose();

        if (s.speed > 0F) submitZaps(s, ps, col);
        ps.popPose();
    }

    private void submitArc(State s, PoseStack ps, SubmitNodeCollector col) {
        double span = s.speed * 0.75D;
        if (span <= 0) return;

        col.submitCustomGeometry(
                ps,
                FusionPlasmaRenderTypes.UNTEXTURED_ADDITIVE,
                (pose, buffer) -> {
                    for (int j = -1; j <= 1; j += 2) {
                        float x = (float) (ARC_X * j);
                        for (int i = 0; i < 8; i++) {
                            double base = i * 45D;
                            arcQuad(buffer, pose, x, base, base + span, 0.75F, 0.5F);
                            arcQuad(buffer, pose, x, base + span, base + span * 2, 0.5F, 0.25F);
                            arcQuad(buffer, pose, x, base + span * 2, base + span * 3, 0.25F, 0F);
                        }
                    }
                });
    }

    private static void arcQuad(
            VertexConsumer buffer,
            PoseStack.Pose pose,
            float x,
            double from,
            double to,
            float alphaFrom,
            float alphaTo) {
        int colorFrom = ARGB.colorFromFloat(alphaFrom, 1F, 1F, 0F);
        int colorTo = ARGB.colorFromFloat(alphaTo, 1F, 1F, 0F);
        double yFrom = spokeY(from), zFrom = spokeZ(from);
        double yTo = spokeY(to), zTo = spokeZ(to);

        vertex(buffer, pose, x, yFrom, zFrom, LEN - WIDTH, colorFrom);
        vertex(buffer, pose, x, yFrom, zFrom, LEN + WIDTH, colorFrom);
        vertex(buffer, pose, x, yTo, zTo, LEN + WIDTH, colorTo);
        vertex(buffer, pose, x, yTo, zTo, LEN - WIDTH, colorTo);
    }

    private static void vertex(
            VertexConsumer buffer,
            PoseStack.Pose pose,
            float x,
            double y,
            double z,
            double reach,
            int color) {
        Vertices.emit(
                buffer,
                pose,
                x,
                (float) (y * reach),
                (float) (z * reach),
                color,
                0F,
                0F,
                LightCoordsUtil.FULL_BRIGHT,
                0F,
                1F,
                0F);
    }

    private void submitSparkle(State s, PoseStack ps, SubmitNodeCollector col) {
        double time = s.timeMs;
        float alpha = 0.45F + (float) (Math.sin(time / 1000D) * 0.15F);
        float alphaMult = s.speed / 15F;

        double mainOsc = BobMathUtil.sps(time / 1000D) % 1D;
        col.submitCustomGeometry(
                ps,
                plasmaType,
                (pose, buffer) ->
                        model.renderPart(
                                pose,
                                buffer,
                                LightCoordsUtil.FULL_BRIGHT,
                                ARGB.colorFromFloat(alpha * alphaMult, 1F, 0.25F, 0.75F),
                                PLASMA,
                                1F,
                                1F,
                                0F,
                                (float) mainOsc));

        if (!s.nearSparkle) return;

        double sparkleSpin = time / 250D * -1D % 1D;
        double sparkleOsc = Math.sin(time / 1000D) * 0.5D % 1D;
        col.submitCustomGeometry(
                ps,
                sparkleType,
                (pose, buffer) ->
                        model.renderPart(
                                pose,
                                buffer,
                                LightCoordsUtil.FULL_BRIGHT,
                                ARGB.colorFromFloat(0.75F * alphaMult, 1F, 0.5F, 1F),
                                PLASMA,
                                1F,
                                1F,
                                (float) sparkleSpin,
                                (float) sparkleOsc));
    }

    private void submitZaps(State s, PoseStack ps, SubmitNodeCollector col) {
        RandomSource rand = RandomSource.create(s.zapSeed);
        rand.nextBoolean();
        int start = (int) (s.timeMs % 1000L) / 50;

        for (int corner = 0; corner < 4; corner++) {
            boolean draw = rand.nextBoolean();
            if (!draw) continue;
            double x = (corner & 1) == 0 ? 3.125D : -3.125D;
            double z = corner < 2 ? 3.75D : -3.75D;
            ps.pushPose();
            ps.translate(x, PIVOT, 0);
            Vec3 skeleton = new Vec3(-x / 3.125D * 1.375D, -2.625D, z);
            BeamPronter.prontBeam(
                    ps,
                    col,
                    skeleton,
                    BeamPronter.EnumWaveType.RANDOM,
                    BeamPronter.EnumBeamType.SOLID,
                    0x404040,
                    0x002040,
                    start,
                    15,
                    0.25F,
                    3,
                    0.0625F);
            BeamPronter.prontBeam(
                    ps,
                    col,
                    skeleton,
                    BeamPronter.EnumWaveType.RANDOM,
                    BeamPronter.EnumBeamType.SOLID,
                    0x404040,
                    0x002040,
                    start,
                    1,
                    0F,
                    3,
                    0.0625F);
            ps.popPose();
        }
    }

    public static final class State extends BlockEntityRenderState {
        public float yaw;
        public float rot;
        public float speed;
        public long timeMs;
        public boolean nearSparkle;
        public long zapSeed;
    }
}
