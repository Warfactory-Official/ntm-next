// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.entity.effect.EntityFalloutRain;
import com.hbm.lib.Library;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class RenderFallout extends EntityRenderer<EntityFalloutRain, RenderFallout.State>
        implements ConcurrentRenderStateExtraction {

    private static final Identifier TEXTURE = Library.id("textures/entity/fallout.png");
    private static final RenderType RENDER_TYPE =
            WorldRenderPipeline.oneSidedTranslucent(TEXTURE, false);
    private static final double RENDER_EDGE = (4.0 + 20.0 + 4.0) / 3.0;

    public RenderFallout(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    protected boolean affectedByCulling(EntityFalloutRain entity) {
        return false;
    }

    @Override
    public boolean shouldRender(
            EntityFalloutRain entity, Frustum culler, double camX, double camY, double camZ) {
        return withinRenderDistance(entity, camX, camY, camZ);
    }

    public static boolean withinRenderDistance(
            EntityFalloutRain entity, double camX, double camY, double camZ) {
        double range = RENDER_EDGE * 64.0 * Entity.getViewScale();
        return entity.distanceToSqr(camX, camY, camZ) < range * range;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(EntityFalloutRain entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.scale = entity.getScale();
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            CameraRenderState camera) {
        Vec3 cameraPos = camera.pos;
        double dx = cameraPos.x - state.x;
        double dy = cameraPos.y - state.y;
        double dz = cameraPos.z - state.z;
        if (dx * dx + dy * dy + dz * dz <= (double) state.scale * state.scale) {
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null) {
                submitNodeCollector.submitCustomGeometry(
                        poseStack,
                        RENDER_TYPE,
                        new Geometry(
                                level, state.x, state.y, state.z, cameraPos, state.ageInTicks));
            }
        }
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    public static class State extends EntityRenderState {
        int scale;
    }

    private record Geometry(
            ClientLevel level,
            double entityX,
            double entityY,
            double entityZ,
            Vec3 cameraPos,
            float ageInTicks)
            implements SubmitNodeCollector.CustomGeometryRenderer {
        @Override
        public void render(PoseStack.Pose pose, VertexConsumer buffer) {
            FalloutRainColumns.capture(level, entityX, entityY, entityZ, cameraPos, ageInTicks)
                    .emit(pose, buffer);
        }
    }
}
