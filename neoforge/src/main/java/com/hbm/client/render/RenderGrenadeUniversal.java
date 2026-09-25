// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.entity.grenade.EntityGrenadeUniversal;
import com.hbm.items.weapon.grenade.GrenadeData;
import com.hbm.items.weapon.grenade.ItemGrenadeUniversal;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;

public final class RenderGrenadeUniversal
        extends EntityRenderer<EntityGrenadeUniversal, RenderGrenadeUniversal.State>
        implements ConcurrentRenderStateExtraction {

    public RenderGrenadeUniversal(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0F;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(EntityGrenadeUniversal entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.data = ItemGrenadeUniversal.getData(entity.getGrenadeItem());
        state.yaw = Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot());
        state.spin = (float) Mth.lerp(partialTicks, entity.prevSpin, entity.spin);
        state.bounces = entity.getBounces();
        state.light = state.lightCoords;
    }

    @Override
    public void submit(
            State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.scale(0.0625F, 0.0625F, 0.0625F);
        pose.mulPose(Axis.YP.rotationDegrees(state.yaw));
        pose.mulPose(Axis.XP.rotationDegrees(state.spin));
        if (state.bounces > 0) pose.mulPose(Axis.ZP.rotationDegrees(-80F));
        GrenadeItemRenderer.draw(collector, pose, state.data, ItemDisplayContext.NONE, state.light);
        pose.popPose();
        super.submit(state, pose, collector, camera);
    }

    public static final class State extends EntityRenderState {
        GrenadeData data;
        float yaw;
        float spin;
        int bounces;
        int light;
    }
}
