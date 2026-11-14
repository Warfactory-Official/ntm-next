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

import com.hbm.entity.mob.PigeonEntity;
import com.hbm.main.ResourceManager;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class PigeonRenderer extends MobRenderer<PigeonEntity, PigeonRenderState, PigeonModel>
        implements ConcurrentRenderStateExtraction {

    public PigeonRenderer(EntityRendererProvider.Context context) {
        super(context, new PigeonModel(PigeonModel.createBodyLayer().bakeRoot()), 0.3F);
    }

    @Override
    public PigeonRenderState createRenderState() {
        return new PigeonRenderState();
    }

    @Override
    public void extractRenderState(
            PigeonEntity entity, PigeonRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        float fall = Mth.lerp(partialTicks, entity.prevFallTime, entity.fallTime);
        float dest = Mth.lerp(partialTicks, entity.prevDest, entity.dest);
        state.flap = (Mth.sin(fall) + 1.0F) * dest;
        state.fat = entity.isFat();
    }

    @Override
    public Identifier getTextureLocation(PigeonRenderState state) {
        return ResourceManager.pigeon_tex;
    }
}
