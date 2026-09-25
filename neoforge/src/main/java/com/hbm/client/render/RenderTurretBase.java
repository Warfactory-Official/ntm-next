// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.turret.BlockEntityTurretBaseNT;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public abstract class RenderTurretBase<T extends BlockEntityTurretBaseNT>
        implements BlockEntityRenderer<T, RenderTurretBase.State>, ConcurrentRenderStateExtraction {
    private static final Map<Identifier, RenderType> TYPES = new HashMap<>();
    private static final int CHEKHOV_CONNECTORS =
            ResourceManager.turret_chekhov.partId("Connectors");

    private static final float[] PLUG_ROT = {0F, 0F, 90F, 90F, 180F, 180F, 270F, 270F};
    private static final float[] PLUG_OZ = {0F, -1F, -1F, 0F, -1F, 0F, -1F, 0F};

    protected static RenderType typeFor(Identifier texture) {
        return TYPES.computeIfAbsent(texture, RenderTypes::entityCutout);
    }

    protected static void connectors(TurretFrame frame, int mask) {
        for (int i = 0; i < 8; i++) {
            if ((mask & (1 << i)) == 0) continue;
            TurretFrame.Part plug =
                    frame.push()
                            .obj(ResourceManager.turret_chekhov, CHEKHOV_CONNECTORS)
                            .texture(ResourceManager.turret_connector_tex);
            plug.pose.rotateY(PLUG_ROT[i] * Mth.DEG_TO_RAD).translate(0F, 0F, PLUG_OZ[i]);
        }
    }

    protected static Matrix4f mount(
            TurretFrame frame,
            State state,
            HFRWavefrontObject carriage,
            int carriageId,
            Identifier carriageTex,
            float pivot,
            HFRWavefrontObject gun,
            int gunId,
            Identifier gunTex) {
        TurretFrame.Part mount = frame.push().obj(carriage, carriageId).texture(carriageTex);
        mount.pose.rotateY((state.yaw - 90F) * Mth.DEG_TO_RAD);
        TurretFrame.Part body = frame.push().obj(gun, gunId).texture(gunTex);
        body.pose
                .set(mount.pose)
                .translate(0F, pivot, 0F)
                .rotateZ(state.pitch * Mth.DEG_TO_RAD)
                .translate(0F, -pivot, 0F);
        return body.pose;
    }

    protected abstract void emit(T turret, State state, TurretFrame frame);

    protected void submitEffects(State state, PoseStack poseStack, SubmitNodeCollector collector) {}

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
    public AABB getRenderBoundingBox(T be) {
        return new AABB(
                Double.NEGATIVE_INFINITY,
                Double.NEGATIVE_INFINITY,
                Double.NEGATIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY);
    }

    @Override
    public void extractRenderState(
            T turret,
            State state,
            float partialTicks,
            Vec3 camera,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                turret, state, partialTicks, camera, breakProgress);
        Vec3 offset = turret.getHorizontalOffset();
        state.offsetX = offset.x;
        state.offsetZ = offset.z;
        state.yaw =
                (float)
                        -Math.toDegrees(
                                Mth.lerp(partialTicks, turret.lastRotationYaw, turret.rotationYaw));
        state.pitch =
                (float)
                        Math.toDegrees(
                                Mth.lerp(
                                        partialTicks,
                                        turret.lastRotationPitch,
                                        turret.rotationPitch));
        state.partialTicks = partialTicks;
        state.gameTime = turret.getLevel().getGameTime();
        state.beam = false;
        state.effect.identity();
        state.frame.rewind();
        emit(turret, state, state.frame);
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        TurretFrame frame = state.frame;
        int light = state.lightCoords;

        poseStack.pushPose();
        poseStack.translate(state.offsetX, 0D, state.offsetZ);

        for (int i = 0; i < frame.size(); i++) {
            TurretFrame.Part part = frame.get(i);
            HFRWavefrontObject model = part.model;
            int group = part.group;
            poseStack.pushPose();
            poseStack.mulPose(part.pose);
            collector.submitCustomGeometry(
                    poseStack,
                    typeFor(part.texture),
                    (pose, buffer) -> model.renderPart(pose, buffer, light, -1, group));
            poseStack.popPose();
        }

        submitEffects(state, poseStack, collector);
        poseStack.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public final TurretFrame frame = new TurretFrame();
        public final Matrix4f effect = new Matrix4f();
        public boolean beam;
        public double beamLength;
        public int beamSegments;
        public double offsetX;
        public double offsetZ;
        public float yaw;
        public float pitch;
        public float partialTicks;
        public long gameTime;
    }
}
