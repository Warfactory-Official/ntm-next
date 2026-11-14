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

import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.entity.CreeperRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.CreeperRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class CreeperUniversalRenderer extends CreeperRenderer
        implements ConcurrentRenderStateExtraction {

    private final Identifier texture;
    private final float swellMod;

    public CreeperUniversalRenderer(
            EntityRendererProvider.Context context, Identifier texture, float swellMod) {
        super(context);
        this.texture = texture;
        this.swellMod = swellMod;
    }

    @Override
    protected void scale(CreeperRenderState state, PoseStack poseStack) {
        float swell = state.swelling;
        float wobble = 1.0F + Mth.sin(swell * 100.0F) * swell * 0.01F;
        swell = Mth.clamp(swell, 0.0F, 1.0F);
        swell *= swell;
        swell *= swell;
        swell *= swellMod;
        poseStack.scale(
                (1.0F + swell * 0.4F) * wobble,
                (1.0F + swell * 0.1F) / wobble,
                (1.0F + swell * 0.4F) * wobble);
    }

    @Override
    public Identifier getTextureLocation(CreeperRenderState state) {
        return texture;
    }
}
