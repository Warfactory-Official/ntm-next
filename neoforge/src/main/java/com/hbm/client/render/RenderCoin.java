// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.entity.projectile.EntityCoin;
import com.hbm.lib.Library;
import com.hbm.render.loader.GroupObject;
import com.hbm.render.loader.HFRWavefrontObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class RenderCoin extends EntityRenderer<EntityCoin, RenderCoin.State>
        implements ConcurrentRenderStateExtraction {

    private static final Identifier COIN_TEX = Library.id("textures/models/trinkets/chip_gold.png");

    private final GroupObject[] coin;
    private final RenderType body = RenderTypes.entityCutoutCull(COIN_TEX);

    public RenderCoin(EntityRendererProvider.Context context) {
        super(context);
        HFRWavefrontObject mesh =
                new HFRWavefrontObject(
                        Minecraft.getInstance().getResourceManager(),
                        Library.id("models/trinkets/chip.obj"));

        coin = new GroupObject[mesh.groups.length];
        for (int i = 0; i < coin.length; i++) coin[i] = mesh.groups[i].flatShaded();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(EntityCoin entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yaw = Mth.lerp(partialTicks, entity.yRotO, entity.getYRot());
        state.spin = (entity.tickCount + partialTicks) * 45;
        state.light =
                Minecraft.getInstance()
                        .getEntityRenderDispatcher()
                        .getPackedLightCoords(entity, partialTicks);
    }

    @Override
    public void submit(
            State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.mulPose(Axis.YN.rotationDegrees(state.yaw - 90.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(state.spin));
        pose.scale(0.125F, 0.125F, 0.125F);
        int light = state.light;
        collector.submitCustomGeometry(
                pose,
                body,
                (p, buf) -> {
                    for (GroupObject group : coin) group.emit(p, buf, light, -1, true);
                });
        pose.popPose();
        super.submit(state, pose, collector, camera);
    }

    public static class State extends EntityRenderState {
        public float yaw;
        public float spin;
        public int light;
    }
}
