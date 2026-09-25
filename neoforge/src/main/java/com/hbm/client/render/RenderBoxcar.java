// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

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
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;

public class RenderBoxcar<T extends Entity> extends EntityRenderer<T, RenderBoxcar.State>
        implements ConcurrentRenderStateExtraction {

    private final Kind kind;
    private final RenderType body;
    private final HFRWavefrontObject model;

    public RenderBoxcar(EntityRendererProvider.Context context, Kind kind) {
        super(context);
        this.shadowRadius = 0F;
        this.kind = kind;
        this.body =
                switch (kind) {
                    case BOXCAR -> RenderTypes.entityCutoutCull(ResourceManager.boxcar_tex);
                    case DUCHESS_GAMBIT ->
                            RenderTypes.entityCutoutCull(ResourceManager.duchessgambit_tex);
                    case BUILDING ->
                            WorldRenderPipeline.oneSidedCutout(ResourceManager.building_tex);
                    case TORPEDO -> RenderTypes.entityCutoutCull(ResourceManager.torpedo_tex);
                };
        this.model =
                switch (kind) {
                    case BOXCAR -> ResourceManager.boxcar;
                    case DUCHESS_GAMBIT -> ResourceManager.duchessgambit;
                    case BUILDING -> ResourceManager.building;
                    case TORPEDO -> ResourceManager.torpedo;
                };
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
        state.age = entity.tickCount + partialTicks;
    }

    @Override
    public void submit(
            State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        int light = state.lightCoords;
        pose.pushPose();

        switch (this.kind) {
            case BOXCAR -> {
                pose.translate(0, 0, -1.5F);
                pose.mulPose(Axis.ZP.rotationDegrees(180));
                pose.mulPose(Axis.XP.rotationDegrees(90));
            }
            case DUCHESS_GAMBIT -> pose.translate(0, 0, -1.0F);
            case BUILDING -> {}
            case TORPEDO -> pose.mulPose(Axis.XP.rotationDegrees(Math.min(85, state.age * 3)));
        }

        collector.submitCustomGeometry(pose, body, (p, buf) -> model.render(p, buf, light, -1));

        pose.popPose();
        super.submit(state, pose, collector, camera);
    }

    public enum Kind {
        BOXCAR,
        DUCHESS_GAMBIT,
        BUILDING,
        TORPEDO
    }

    public static final class State extends EntityRenderState {
        public float age;
    }
}
