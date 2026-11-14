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

import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.AbstractBoatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.BoatRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Unit;

public class BoatRubberRenderer extends AbstractBoatRenderer
        implements ConcurrentRenderStateExtraction {

    private final Model.Simple waterPatchModel;
    private final BoatRubberModel model;

    public BoatRubberRenderer(EntityRendererProvider.Context context) {
        super(context, ResourceManager.boat_rubber_tex);
        this.shadowRadius = 0.5F;
        this.waterPatchModel =
                new Model.Simple(
                        context.bakeLayer(ModelLayers.BOAT_WATER_PATCH),
                        ignored -> RenderTypes.waterMask());
        this.model = new BoatRubberModel(BoatRubberModel.createBodyLayer().bakeRoot());
    }

    @Override
    protected EntityModel<BoatRenderState> model() {
        return model;
    }

    @Override
    protected void submitTypeAdditions(
            BoatRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int lightCoords) {
        if (!state.isUnderWater) {
            collector.submitModel(
                    waterPatchModel,
                    Unit.INSTANCE,
                    poseStack,
                    ResourceManager.boat_rubber_tex,
                    lightCoords,
                    OverlayTexture.NO_OVERLAY,
                    state.outlineColor,
                    null);
        }
    }
}
