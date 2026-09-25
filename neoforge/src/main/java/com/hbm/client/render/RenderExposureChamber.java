// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.render.util.BeamPronter.EnumBeamType;
import com.hbm.render.util.BeamPronter.EnumWaveType;
import com.hbm.render.util.BeamPronter;
import com.hbm.tileentity.machine.BlockEntityMachineExposureChamber;
import com.hbm.util.Facing;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import java.util.Random;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderExposureChamber
        implements BlockEntityRenderer<
                        BlockEntityMachineExposureChamber, RenderExposureChamber.State>,
                ConcurrentRenderStateExtraction {

    private static final int MAGNETS = ResourceManager.exposure_chamber.partId("Magnets");
    private static final int CORE = ResourceManager.exposure_chamber.partId("Core");

    private static final int FLICKER_PERIOD = 8;
    private static final int FLICKER_CHANCE = 2;
    private static final int FLICKER_TINT = 0x80d0ff;
    private static final int FLICKER_PLAIN = 0xFFFFFF;

    private static final Vec3 INJECTOR = new Vec3(0, 0, 5);
    private static final Vec3 COLUMN = new Vec3(0, 1.5, 0);
    private static final Vec3 RING = new Vec3(0, 0, -1);

    private final HFRWavefrontObject model;
    private final RenderType baseType;
    private final RenderType coreType;

    public RenderExposureChamber() {
        this.model = ResourceManager.exposure_chamber;
        this.baseType = WorldRenderPipeline.oneSidedCutout(ResourceManager.exposure_chamber_tex);

        this.coreType = FlatCutout.of(ResourceManager.exposure_chamber_tex);
    }

    @Override
    public AABB getRenderBoundingBox(BlockEntityMachineExposureChamber be) {
        BlockPos pos = be.getBlockPos();
        return new AABB(
                pos.getX() - 8,
                pos.getY(),
                pos.getZ() - 8,
                pos.getX() + 9,
                pos.getY() + 5,
                pos.getZ() + 9);
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
            BlockEntityMachineExposureChamber be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breaking);
        state.yaw = Facing.yaw(be.getBlockState().getValue(BlockMultiblockCore.FACING), 90);
        state.rotation = Mth.lerp(partialTicks, be.prevRotation, be.rotation);
        state.isOn = be.isOn;

        long time = be.getLevel().getGameTime();

        state.bob = Math.sin((time % (Math.PI * 16D) + partialTicks) * 0.125D) * 0.0625D;
        state.flickerSeed = time / FLICKER_PERIOD;
        state.tinted = time % FLICKER_PERIOD >= FLICKER_PERIOD / 2;
        state.beamStart = GameTime.now();
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;

        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(s.yaw));

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(s.rotation));
        col.submitCustomGeometry(
                ps, baseType, (pose, buf) -> model.renderPart(pose, buf, light, -1, MAGNETS));
        ps.popPose();

        if (s.isOn) {
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees(s.rotation / 2F));
            ps.translate(0.0, s.bob, 0.0);
            col.submitCustomGeometry(
                    ps,
                    coreType,
                    (pose, buf) ->
                            model.renderPart(pose, buf, LightCoordsUtil.FULL_BRIGHT, -1, CORE));
            ps.popPose();

            submitBeams(s, ps, col);
        }

        ps.popPose();
    }

    private void submitBeams(State s, PoseStack ps, SubmitNodeCollector col) {
        Random rand = new Random(s.flickerSeed);
        int color = s.tinted ? FLICKER_TINT : FLICKER_PLAIN;
        int start = (int) (s.beamStart % 1000) / 50;
        rand.nextInt(FLICKER_CHANCE);

        if (rand.nextInt(FLICKER_CHANCE) == 0) injector(ps, col, 0, 3.675, color, start);
        if (rand.nextInt(FLICKER_CHANCE) == 0) injector(ps, col, 1.1875, 2.5, color, start);
        if (rand.nextInt(FLICKER_CHANCE) == 0) injector(ps, col, -1.1875, 2.5, color, start);

        ps.pushPose();
        ps.translate(0.0, 1.75, 0.0);
        BeamPronter.prontBeam(
                ps,
                col,
                COLUMN,
                EnumWaveType.RANDOM,
                EnumBeamType.LINE,
                FLICKER_TINT,
                FLICKER_PLAIN,
                start,
                10,
                0.125F,
                1,
                0);

        BeamPronter.prontBeam(
                ps,
                col,
                COLUMN,
                EnumWaveType.RANDOM,
                EnumBeamType.LINE,
                0x8080FF,
                FLICKER_PLAIN,
                (int) (s.beamStart + 5) / 50,
                10,
                0.125F,
                1,
                0);
        ps.popPose();

        ps.pushPose();
        ps.translate(0.0, 2.5, 0.0);
        int spin = (int) (s.beamStart % 360);
        BeamPronter.prontBeam(
                ps,
                col,
                RING,
                EnumWaveType.SPIRAL,
                EnumBeamType.LINE,
                0xFFFF80,
                FLICKER_PLAIN,
                spin,
                15,
                0.125F,
                1,
                0);
        BeamPronter.prontBeam(
                ps,
                col,
                RING,
                EnumWaveType.SPIRAL,
                EnumBeamType.LINE,
                0xFF8080,
                FLICKER_PLAIN,
                spin + 180,
                15,
                0.125F,
                1,
                0);
        ps.popPose();
    }

    private void injector(
            PoseStack ps, SubmitNodeCollector col, double x, double y, int color, int start) {
        ps.pushPose();
        ps.translate(x, y, -7.5);
        BeamPronter.prontBeam(
                ps,
                col,
                INJECTOR,
                EnumWaveType.RANDOM,
                EnumBeamType.LINE,
                color,
                FLICKER_PLAIN,
                start,
                15,
                0.125F,
                1,
                0);
        ps.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public float yaw;
        public float rotation;
        public boolean isOn;
        public double bob;
        public long flickerSeed;
        public boolean tinted;
        public long beamStart;
    }
}
