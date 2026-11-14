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

import com.hbm.entity.mob.DuckEntity;
import com.hbm.entity.mob.QuackosEntity;
import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.model.animal.chicken.AdultChickenModel;
import net.minecraft.client.model.animal.chicken.ChickenModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.ChickenRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class DuckRenderer<T extends DuckEntity>
        extends MobRenderer<T, ChickenRenderState, ChickenModel>
        implements ConcurrentRenderStateExtraction {

    private final float scale;

    private DuckRenderer(EntityRendererProvider.Context context, float shadow, float scale) {
        super(context, new AdultChickenModel(context.bakeLayer(ModelLayers.CHICKEN)), shadow);
        this.scale = scale;
    }

    public static DuckRenderer<DuckEntity> duck(EntityRendererProvider.Context context) {
        return new DuckRenderer<>(context, 0.3F, 1F);
    }

    public static DuckRenderer<QuackosEntity> quackos(EntityRendererProvider.Context context) {
        return new DuckRenderer<>(context, 7.5F, 25F);
    }

    @Override
    public ChickenRenderState createRenderState() {
        return new ChickenRenderState();
    }

    @Override
    public void extractRenderState(T entity, ChickenRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.flap = Mth.lerp(partialTicks, entity.oFlap, entity.flap);
        state.flapSpeed = Mth.lerp(partialTicks, entity.oFlapSpeed, entity.flapSpeed);
    }

    @Override
    protected void scale(ChickenRenderState state, PoseStack poseStack) {
        if (scale != 1F) poseStack.scale(scale, scale, scale);
    }

    @Override
    public Identifier getTextureLocation(ChickenRenderState state) {
        return ResourceManager.duck_tex;
    }
}
