// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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

public class RenderBoatRubber extends AbstractBoatRenderer
        implements ConcurrentRenderStateExtraction {

    private final Model.Simple waterPatchModel;
    private final ModelBoatRubber model;

    public RenderBoatRubber(EntityRendererProvider.Context context) {
        super(context, ResourceManager.boat_rubber_tex);
        this.shadowRadius = 0.5F;
        this.waterPatchModel =
                new Model.Simple(
                        context.bakeLayer(ModelLayers.BOAT_WATER_PATCH),
                        ignored -> RenderTypes.waterMask());
        this.model = new ModelBoatRubber(ModelBoatRubber.createBodyLayer().bakeRoot());
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
