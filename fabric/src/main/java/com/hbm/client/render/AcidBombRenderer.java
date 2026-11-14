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

import com.hbm.entity.projectile.AcidBombEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

public final class AcidBombRenderer extends EntityRenderer<AcidBombEntity, AcidBombRenderer.State>
        implements ConcurrentRenderStateExtraction {

    private final ItemModelResolver itemModelResolver;

    public AcidBombRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemModelResolver = context.getItemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(AcidBombEntity entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        if (state.item == null) state.item = new ItemStackRenderState();
        itemModelResolver.updateForTopItem(
                state.item,
                new ItemStack(Items.SLIME_BALL),
                ItemDisplayContext.GROUND,
                entity.level(),
                null,
                entity.getId());
    }

    @Override
    public void submit(
            State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.scale(0.5F, 0.5F, 0.5F);
        pose.rotate((Quaternionfc) camera.orientation);
        pose.rotate((Quaternionfc) Axis.YP.rotationDegrees(180.0F));
        state.item.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
        super.submit(state, pose, collector, camera);
    }

    public static final class State extends EntityRenderState {
        public @Nullable ItemStackRenderState item;
    }
}
