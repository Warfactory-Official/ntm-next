/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.client.render;

import com.hbm.entity.projectile.CoinEntity;
import com.hbm.render.loader.GroupObject;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.util.RegistryUtil;
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
import org.joml.Quaternionfc;

public class CoinRenderer extends EntityRenderer<CoinEntity, CoinRenderer.State>
        implements ConcurrentRenderStateExtraction {

    private static final Identifier COIN_TEX =
            RegistryUtil.id("textures/models/trinkets/chip_gold.png");

    private final GroupObject[] coin;
    private final RenderType body = RenderTypes.entityCutoutCull(COIN_TEX);

    public CoinRenderer(EntityRendererProvider.Context context) {
        super(context);
        HFRWavefrontObject mesh =
                new HFRWavefrontObject(
                        Minecraft.getInstance().getResourceManager(),
                        RegistryUtil.id("models/trinkets/chip.obj"));
        coin = new GroupObject[mesh.groups.length];
        for (int i = 0; i < coin.length; i++) coin[i] = mesh.groups[i].flatShaded();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(CoinEntity entity, State state, float partialTicks) {
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
        pose.rotate((Quaternionfc) Axis.YN.rotationDegrees(state.yaw - 90.0F));
        pose.rotate((Quaternionfc) Axis.ZP.rotationDegrees(state.spin));
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
