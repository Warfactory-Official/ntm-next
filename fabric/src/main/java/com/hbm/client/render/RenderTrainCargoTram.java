// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.entity.train.EntityRailCarBase;
import com.hbm.entity.train.EntityRailCarCargo;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class RenderTrainCargoTram<T extends EntityRailCarBase>
        extends EntityRenderer<T, RenderTrainCargoTram.State>
        implements ConcurrentRenderStateExtraction {

    private final HFRWavefrontObject mesh;
    private final RenderType type;

    public RenderTrainCargoTram(EntityRendererProvider.Context context) {
        this(context, ResourceManager.train_cargo_tram, ResourceManager.train_tram_tex);
    }

    protected RenderTrainCargoTram(
            EntityRendererProvider.Context context, HFRWavefrontObject mesh, Identifier texture) {
        super(context);
        this.mesh = mesh;
        this.type = WorldRenderPipeline.oneSidedCutout(texture);
        this.shadowRadius = 0F;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(T entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.offsetX =
                Mth.lerp(partialTicks, entity.xo, entity.getX())
                        - Mth.lerp(partialTicks, entity.lastRenderX, entity.renderX);
        state.offsetY =
                Mth.lerp(partialTicks, entity.yo, entity.getY())
                        - Mth.lerp(partialTicks, entity.lastRenderY, entity.renderY);
        state.offsetZ =
                Mth.lerp(partialTicks, entity.zo, entity.getZ())
                        - Mth.lerp(partialTicks, entity.lastRenderZ, entity.renderZ);
        state.yaw = Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot());
        state.pitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
        state.occupiedSlots =
                entity instanceof EntityRailCarCargo cargo ? cargo.getOccupiedSlots() : 0;
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(-state.offsetX, -state.offsetY, -state.offsetZ);
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(-state.pitch));

        collector.submitCustomGeometry(
                poseStack,
                type,
                (pose, buffer) -> mesh.render(pose, buffer, state.lightCoords, -1));

        submitCargo(state, poseStack, collector);

        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    protected void submitCargo(State state, PoseStack poseStack, SubmitNodeCollector collector) {}

    public static class State extends EntityRenderState {
        public double offsetX, offsetY, offsetZ;
        public float yaw, pitch;
        public int occupiedSlots;
    }
}
