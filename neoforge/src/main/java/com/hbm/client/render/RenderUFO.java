// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.entity.mob.EntityUFO;
import com.hbm.main.ResourceManager;
import com.hbm.render.util.BeamPronter.EnumBeamType;
import com.hbm.render.util.BeamPronter.EnumWaveType;
import com.hbm.render.util.BeamPronter;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public class RenderUFO extends EntityRenderer<EntityUFO, RenderUFO.State>
        implements ConcurrentRenderStateExtraction {

    private static final RenderType TYPE = RenderTypes.entityCutoutCull(ResourceManager.ufo_tex);
    private static final double SCALE = 2D;

    public RenderUFO(EntityRendererProvider.Context context) {
        super(context);
        this.shadowStrength = 0F;
    }

    @Override
    protected boolean affectedByCulling(EntityUFO entity) {
        return false;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(EntityUFO entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);

        state.spin = (float) ((entity.tickCount + partialTicks) * 5D % 360D);
        state.alive = entity.isAlive();
        state.deathTilt = entity.deathTime + 30 + partialTicks;
        state.beam = entity.getBeam();
        state.beamLength = state.beam ? entity.getY() - groundBelow(entity) : 0D;
        state.age = entity.tickCount;
    }

    private static int groundBelow(EntityUFO entity) {

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int x = Mth.floor(entity.getX());
        int z = Mth.floor(entity.getZ());

        for (int y = (int) Math.ceil(entity.getY()); y >= entity.level().getMinY(); y--) {
            if (!entity.level().getBlockState(pos.set(x, y, z)).isAir()) return y;
        }

        return entity.level().getMinY();
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {

        int light = state.lightCoords;

        poseStack.pushPose();
        poseStack.translate(0F, 1F, 0F);

        if (!state.alive)
            poseStack.mulPose(
                    new Quaternionf()
                            .rotateAxis(
                                    state.deathTilt * Mth.DEG_TO_RAD, 0.7071068F, 0F, 0.7071068F));

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(state.spin));
        poseStack.scale((float) SCALE, (float) SCALE, (float) SCALE);
        collector.submitCustomGeometry(
                poseStack,
                TYPE,
                (pose, buffer) -> ResourceManager.ufo.render(pose, buffer, light, -1));
        poseStack.popPose();

        if (state.beam && state.beamLength > 0D) {
            Vec3 skeleton = new Vec3(0D, -state.beamLength, 0D);
            BeamPronter.prontBeam(
                    poseStack,
                    collector,
                    skeleton,
                    EnumWaveType.SPIRAL,
                    EnumBeamType.SOLID,
                    0x101020,
                    0x101020,
                    0,
                    (int) (state.beamLength + 1),
                    0F,
                    6,
                    (float) SCALE * 0.75F);
            BeamPronter.prontBeam(
                    poseStack,
                    collector,
                    skeleton,
                    EnumWaveType.RANDOM,
                    EnumBeamType.SOLID,
                    0x202060,
                    0x202060,
                    state.age / 2,
                    (int) (state.beamLength / 2 + 1),
                    (float) SCALE * 1.5F,
                    2,
                    0.0625F);
            BeamPronter.prontBeam(
                    poseStack,
                    collector,
                    skeleton,
                    EnumWaveType.RANDOM,
                    EnumBeamType.SOLID,
                    0x202060,
                    0x202060,
                    state.age / 4,
                    (int) (state.beamLength / 2 + 1),
                    (float) SCALE * 1.5F,
                    2,
                    0.0625F);
        }

        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    public static final class State extends EntityRenderState {
        float spin;
        boolean alive;
        float deathTilt;
        boolean beam;
        double beamLength;
        int age;
    }
}
