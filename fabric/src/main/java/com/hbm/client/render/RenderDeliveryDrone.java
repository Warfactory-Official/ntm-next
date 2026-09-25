// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.entity.item.EntityDeliveryDrone;
import com.hbm.entity.item.EntityDroneBase;
import com.hbm.entity.item.EntityRequestDrone;
import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;

public class RenderDeliveryDrone<T extends EntityDroneBase>
        extends EntityRenderer<T, RenderDeliveryDrone.State>
        implements ConcurrentRenderStateExtraction {

    private static final RenderType DELIVERY =
            WorldRenderPipeline.oneSidedCutout(ResourceManager.delivery_drone_tex);
    private static final RenderType REQUEST =
            WorldRenderPipeline.oneSidedCutout(ResourceManager.delivery_drone_request_tex);
    private static final RenderType EXPRESS =
            WorldRenderPipeline.oneSidedCutout(ResourceManager.delivery_drone_express_tex);
    private static final int DRONE = ResourceManager.delivery_drone.partId("Drone");
    private static final int CRATE = ResourceManager.delivery_drone.partId("Crate");
    private static final int BARREL = ResourceManager.delivery_drone.partId("Barrel");

    public RenderDeliveryDrone(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    private static void part(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            RenderType type,
            int light,
            int part) {
        collector.submitCustomGeometry(
                poseStack,
                type,
                (pose, buffer) ->
                        ResourceManager.delivery_drone.renderPart(pose, buffer, light, -1, part));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(T entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        if (entity instanceof EntityRequestDrone) state.type = REQUEST;
        else if (entity instanceof EntityDeliveryDrone delivery && delivery.isExpress())
            state.type = EXPRESS;
        else state.type = DELIVERY;
        state.appearance = entity.getAppearance();
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        int light = state.lightCoords;
        part(poseStack, collector, state.type, light, DRONE);
        if (state.appearance == 1) part(poseStack, collector, state.type, light, CRATE);
        if (state.appearance == 2) part(poseStack, collector, state.type, light, BARREL);
        super.submit(state, poseStack, collector, camera);
    }

    public static final class State extends EntityRenderState {
        public RenderType type = DELIVERY;
        public int appearance;
    }
}
