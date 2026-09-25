// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.entity.projectile.EntityRBMKDebris.DebrisType;
import com.hbm.entity.projectile.EntityRBMKDebris;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

public class RenderRBMKDebris extends EntityRenderer<EntityRBMKDebris, RenderRBMKDebris.State>
        implements ConcurrentRenderStateExtraction {

    private static final float DIAG = 0.57735026F;

    public RenderRBMKDebris(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(EntityRBMKDebris entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.type = entity.getDebrisType();
        state.rot = Mth.lerp(partialTicks, entity.lastRot, entity.rot);
        state.idSpin = entity.getId() % 360;
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        HFRWavefrontObject obj;
        Identifier tex;
        switch (state.type) {
            case BLANK -> {
                obj = ResourceManager.deb_blank;
                tex = ResourceManager.deb_blank_tex;
            }
            case ELEMENT -> {
                obj = ResourceManager.deb_element;
                tex = ResourceManager.deb_element_tex;
            }
            case FUEL -> {
                obj = ResourceManager.deb_fuel;
                tex = ResourceManager.deb_fuel_tex;
            }
            case GRAPHITE -> {
                obj = ResourceManager.deb_graphite;
                tex = ResourceManager.deb_graphite_tex;
            }
            case LID -> {
                obj = ResourceManager.deb_lid;
                tex = ResourceManager.deb_lid_tex;
            }
            case ROD -> {
                obj = ResourceManager.deb_rod;
                tex = ResourceManager.deb_control_tex;
            }
            default -> {
                obj = ResourceManager.deb_element;
                tex = ResourceManager.deb_element_tex;
            }
        }

        final HFRWavefrontObject fobj = obj;
        final int light = state.lightCoords;

        poseStack.pushPose();
        poseStack.translate(0.0, 0.125, 0.0);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.idSpin));
        poseStack.mulPose(
                new Quaternionf()
                        .rotationAxis((float) Math.toRadians(state.rot), DIAG, DIAG, DIAG));
        collector.submitCustomGeometry(
                poseStack,
                RenderTypes.entityCutoutCull(tex),
                (pose, buffer) -> fobj.render(pose, buffer, light, -1));
        poseStack.popPose();

        super.submit(state, poseStack, collector, camera);
    }

    public static final class State extends EntityRenderState {
        public DebrisType type = DebrisType.ELEMENT;
        public float rot;
        public int idSpin;
    }
}
