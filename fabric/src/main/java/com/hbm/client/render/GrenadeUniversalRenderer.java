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

import com.hbm.entity.grenade.GrenadeUniversalEntity;
import com.hbm.items.weapon.grenade.GrenadeData;
import com.hbm.items.weapon.grenade.GrenadeUniversalItem;
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
import org.joml.Quaternionfc;

public final class GrenadeUniversalRenderer
        extends EntityRenderer<GrenadeUniversalEntity, GrenadeUniversalRenderer.State>
        implements ConcurrentRenderStateExtraction {

    public GrenadeUniversalRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0F;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(GrenadeUniversalEntity entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.data = GrenadeUniversalItem.getData(entity.getGrenadeItem());
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
        pose.mulPose((Quaternionfc) Axis.YP.rotationDegrees(state.yaw));
        pose.mulPose((Quaternionfc) Axis.XP.rotationDegrees(state.spin));
        if (state.bounces > 0) pose.mulPose((Quaternionfc) Axis.ZP.rotationDegrees(-80F));
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
