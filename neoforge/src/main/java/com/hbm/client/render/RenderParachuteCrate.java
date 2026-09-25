// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.entity.item.EntityParachuteCrate;
import com.hbm.main.ResourceManager;
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

public class RenderParachuteCrate
        extends EntityRenderer<EntityParachuteCrate, RenderParachuteCrate.State>
        implements ConcurrentRenderStateExtraction {
    private static final int CHUTE = ResourceManager.soyuz_lander.partId("Chute");

    private static final int PIVOT_HEIGHT = 7;
    private final RenderType crate = RenderTypes.entityCutoutCull(ResourceManager.supply_crate_tex);
    private final RenderType chute = RenderTypes.entityCutoutCull(ResourceManager.soyuz_chute_tex);

    public RenderParachuteCrate(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    @Override
    protected boolean affectedByCulling(EntityParachuteCrate entity) {
        return false;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(EntityParachuteCrate entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        double time = entity.level().getGameTime();
        state.swingZ = (float) (Math.sin(time * 0.05) * 5);
        state.swingX = (float) (Math.sin(time * 0.05 + Math.PI * 0.5) * 5);
    }

    @Override
    public void submit(
            State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        int light = state.lightCoords;
        pose.pushPose();

        pose.translate(0, PIVOT_HEIGHT, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(state.swingZ));
        pose.mulPose(Axis.XP.rotationDegrees(state.swingX));
        pose.translate(0, -PIVOT_HEIGHT, 0);

        collector.submitCustomGeometry(
                pose, crate, (p, buf) -> ResourceManager.conservecrate.render(p, buf, light, -1));

        pose.translate(0, -1, 0);
        collector.submitCustomGeometry(
                pose,
                chute,
                (p, buf) -> ResourceManager.soyuz_lander.renderPart(p, buf, light, -1, CHUTE));

        pose.popPose();
        super.submit(state, pose, collector, camera);
    }

    public static final class State extends EntityRenderState {
        public float swingZ;
        public float swingX;
    }
}
