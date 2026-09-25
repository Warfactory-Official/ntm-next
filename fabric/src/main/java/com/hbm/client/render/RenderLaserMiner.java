// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.main.ResourceManager;
import com.hbm.render.util.BeamPronter.EnumBeamType;
import com.hbm.render.util.BeamPronter.EnumWaveType;
import com.hbm.render.util.BeamPronter;
import com.hbm.tileentity.machine.BlockEntityMachineMiningLaser;
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
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderLaserMiner
        implements BlockEntityRenderer<BlockEntityMachineMiningLaser, RenderLaserMiner.State>,
                ConcurrentRenderStateExtraction {

    private static final int PIVOT = ResourceManager.mining_laser.partId("Pivot");
    private static final int LASER = ResourceManager.mining_laser.partId("Laser");

    private static final double STANDOFF = 1.5D;
    private static final int BEAM_COLOR = 0xa00000;

    private final RenderType pivotType;
    private final RenderType laserType;

    public RenderLaserMiner() {
        this.pivotType = RenderTypes.entityCutoutCull(ResourceManager.mining_laser_pivot_tex);
        this.laserType = RenderTypes.entityCutoutCull(ResourceManager.mining_laser_laser_tex);
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
            BlockEntityMachineMiningLaser be,
            State state,
            float pt,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, pt, cameraPosition, breakProgress);

        double tx = Mth.lerp(pt, be.lastTargetX, be.targetX);
        double ty = Mth.lerp(pt, be.lastTargetY, be.targetY);
        double tz = Mth.lerp(pt, be.lastTargetZ, be.targetZ);

        double vx = tx - be.getBlockPos().getX();
        double vy = ty - be.getBlockPos().getY() + 3;
        double vz = tz - be.getBlockPos().getZ();

        Vec3 n = new Vec3(vx, vy, vz).normalize().scale(STANDOFF);
        Vec3 vec = new Vec3(vx - n.x, vy - n.y, vz - n.z);

        state.standoff = n;
        state.vec = vec;
        state.yaw = (float) Math.toDegrees(Math.atan2(vec.x, vec.z));

        double flat = Math.sqrt(vec.x * vec.x + vec.z * vec.z);
        state.pitch = (float) Math.toDegrees(Math.atan2(vec.y, flat));
        state.beam = be.beam;
        state.gameTime = be.getLevel() != null ? be.getLevel().getGameTime() : 0L;
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;

        ps.pushPose();
        ps.translate(0.5D, -1.0D, 0.5D);

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(s.yaw));
        col.submitCustomGeometry(
                ps,
                pivotType,
                (pose, buf) ->
                        ResourceManager.mining_laser.renderPart(pose, buf, light, -1, PIVOT));
        ps.popPose();

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(s.yaw));
        ps.translate(0D, -1D, 0D);
        ps.mulPose(Axis.XN.rotationDegrees(s.pitch + 90F));
        ps.translate(0D, 1D, 0D);
        col.submitCustomGeometry(
                ps,
                laserType,
                (pose, buf) ->
                        ResourceManager.mining_laser.renderPart(pose, buf, light, -1, LASER));
        ps.popPose();

        if (s.beam) {
            int range = (int) Math.ceil(s.vec.length() * 0.5D);
            ps.translate(s.standoff.x, s.standoff.y - 1D, s.standoff.z);
            int start = (int) (s.gameTime * -25L % 360L);
            for (int arm = 0; arm < 3; arm++) {
                BeamPronter.prontBeam(
                        ps,
                        col,
                        s.vec,
                        EnumWaveType.SPIRAL,
                        EnumBeamType.SOLID,
                        BEAM_COLOR,
                        BEAM_COLOR,
                        start + arm * 120,
                        range * 2,
                        0.075F,
                        3,
                        0.025F);
            }
        }

        ps.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public Vec3 vec = Vec3.ZERO;
        public Vec3 standoff = Vec3.ZERO;
        public float yaw;
        public float pitch;
        public boolean beam;
        public long gameTime;
    }
}
