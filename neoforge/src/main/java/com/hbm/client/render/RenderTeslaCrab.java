// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.entity.mob.EntityTaintCrab;
import com.hbm.entity.mob.EntityTeslaCrab;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.render.util.BeamPronter.EnumBeamType;
import com.hbm.render.util.BeamPronter.EnumWaveType;
import com.hbm.render.util.BeamPronter;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

public class RenderTeslaCrab<T extends Mob> extends EntityRenderer<T, RenderTeslaCrab.State>
        implements ConcurrentRenderStateExtraction {

    private final HFRWavefrontObject model;
    private final RenderType type;
    private final int body;
    private final int legsLeft;
    private final int legsRight;
    private final float baseYaw;
    private final double arcHeight;

    protected RenderTeslaCrab(
            EntityRendererProvider.Context context,
            HFRWavefrontObject model,
            Identifier texture,
            String left,
            String right,
            float baseYaw,
            double arcHeight) {
        super(context);
        this.model = model;
        this.type = WorldRenderPipeline.oneSidedCutout(texture);
        this.body = model.partId("Body");
        this.legsLeft = model.partId(left);
        this.legsRight = model.partId(right);
        this.baseYaw = baseYaw;
        this.arcHeight = arcHeight;
        this.shadowStrength = 0F;
    }

    public static RenderTeslaCrab<EntityTeslaCrab> tesla(EntityRendererProvider.Context context) {
        return new RenderTeslaCrab<>(
                context,
                ResourceManager.teslacrab,
                ResourceManager.teslacrab_tex,
                "Front",
                "Back",
                0F,
                1D);
    }

    public static RenderTeslaCrab<EntityTaintCrab> taint(EntityRendererProvider.Context context) {
        return new RenderTeslaCrab<>(
                context,
                ResourceManager.taintcrab,
                ResourceManager.taintcrab_tex,
                "Legs1",
                "Legs2",
                90F,
                1.25D);
    }

    private static List<Vec3> arcsOf(Mob entity) {
        if (entity instanceof EntityTeslaCrab crab) return crab.targets;
        if (entity instanceof EntityTaintCrab crab) return crab.targets;
        return List.of();
    }

    @Override
    protected boolean affectedByCulling(T entity) {
        return false;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(T entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.bodyYaw = Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);
        state.walkCycle = entity.walkAnimation.position(partialTicks);
        state.walkSpeed = entity.walkAnimation.speed(partialTicks);
        state.beams.clear();
        for (Vec3 target : arcsOf(entity)) {
            state.beams.add(
                    target.subtract(entity.getX(), entity.getY() + arcHeight, entity.getZ()));
        }
        state.start = (int) entity.level().getGameTime() % 1000 + 1;
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        int light = state.lightCoords;
        float swing = -(Mth.cos(state.walkCycle * 0.6662F * 2F) * 0.4F) * state.walkSpeed * 57.3F;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180F - state.bodyYaw));
        poseStack.mulPose(Axis.YP.rotationDegrees(baseYaw));
        part(poseStack, collector, light, body);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(swing));
        part(poseStack, collector, light, legsLeft);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.mulPose(Axis.YN.rotationDegrees(swing));
        part(poseStack, collector, light, legsRight);
        poseStack.popPose();
        poseStack.popPose();

        if (!state.beams.isEmpty()) {
            poseStack.pushPose();
            poseStack.translate(0D, arcHeight, 0D);
            for (Vec3 skeleton : state.beams) {
                BeamPronter.prontBeam(
                        poseStack,
                        collector,
                        skeleton,
                        EnumWaveType.RANDOM,
                        EnumBeamType.SOLID,
                        0x404040,
                        0x404040,
                        state.start,
                        (int) (skeleton.length() * 5D),
                        0.125F,
                        2,
                        0.03125F);
            }
            poseStack.popPose();
        }

        super.submit(state, poseStack, collector, camera);
    }

    private void part(PoseStack poseStack, SubmitNodeCollector collector, int light, int part) {
        collector.submitCustomGeometry(
                poseStack, type, (pose, buffer) -> model.renderPart(pose, buffer, light, -1, part));
    }

    public static final class State extends EntityRenderState {
        public final List<Vec3> beams = new ArrayList<>();
        float bodyYaw;
        float walkCycle;
        float walkSpeed;
        int start;
    }
}
