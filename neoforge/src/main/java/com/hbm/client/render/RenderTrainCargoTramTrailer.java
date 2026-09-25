// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.ModBlocks;
import com.hbm.entity.train.TrainCargoTramTrailer;
import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class RenderTrainCargoTramTrailer extends RenderTrainCargoTram<TrainCargoTramTrailer> {

    private static final double[][][] PILES = {
        {{0.0, 0.375, 0.0}},
        {{0.1, 0.375, 0.25}, {-0.1, 0.375, -0.25}},
        {{0.1, 0.375, 0.0}, {-0.1, 0.375, 0.375}, {-0.1, 0.375, -0.375}},
        {{0.2, 0.375, 0.3}, {0.2, 0.375, -0.2}, {-0.2, 0.375, 0.2}, {-0.2, 0.375, -0.3}},
        {
            {0.2, 0.375, 0.6},
            {0.2, 0.375, 0.0},
            {0.2, 0.375, -0.5},
            {-0.2, 0.375, 0.2},
            {-0.2, 0.375, -0.3}
        },
        {
            {0.2, 0.375, 0.6}, {0.2, 0.375, 0.0}, {0.2, 0.375, -0.5},
            {-0.2, 0.375, 0.5}, {-0.2, 0.375, -0.1}, {-0.2, 0.375, -0.6}
        },
        {
            {0.2, 0.375, 0.4},
            {0.2, 0.375, 0.0},
            {0.2, 0.375, -0.4},
            {-0.2, 0.375, 0.3},
            {-0.2, 0.375, -0.1},
            {-0.2, 0.375, -0.5},
            {0.0, 0.6875, -0.25}
        },
        {
            {0.2, 0.375, 0.4}, {0.2, 0.375, 0.0}, {0.2, 0.375, -0.4},
            {-0.2, 0.375, 0.3}, {-0.2, 0.375, -0.1}, {-0.2, 0.375, -0.5},
            {0.0, 0.6875, -0.25}, {0.0, 0.6875, 0.15}
        },
        {
            {0.2, 0.375, 0.4}, {0.2, 0.375, 0.0}, {0.2, 0.375, -0.4},
            {-0.2, 0.375, 0.3}, {-0.2, 0.375, -0.1}, {-0.2, 0.375, -0.5},
            {0.0, 0.6875, -0.25}, {0.0, 0.6875, 0.15}, {-0.1, 0.375, 0.8}
        }
    };

    private final ItemModelResolver itemModelResolver;
    private final ItemStackRenderState crate = new ItemStackRenderState();
    private boolean crateResolved;

    public RenderTrainCargoTramTrailer(EntityRendererProvider.Context context) {
        super(context, ResourceManager.train_cargo_tram_trailer, ResourceManager.tram_trailer_tex);
        this.itemModelResolver = context.getItemModelResolver();
    }

    @Override
    protected void submitCargo(State state, PoseStack poseStack, SubmitNodeCollector collector) {

        if (state.occupiedSlots <= 0) return;

        if (!crateResolved) {
            itemModelResolver.updateForTopItem(
                    crate,
                    new ItemStack(ModBlocks.CRATE_STEEL.get()),
                    ItemDisplayContext.GROUND,
                    null,
                    null,
                    0);
            crateResolved = true;
        }

        double[][] pile = PILES[Math.min((state.occupiedSlots - 1) / 5, PILES.length - 1)];

        poseStack.pushPose();
        poseStack.scale(2F, 2F, 2F);
        for (double[] at : pile) {
            poseStack.pushPose();
            poseStack.translate(at[0], at[1], at[2]);
            crate.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
        poseStack.popPose();
    }
}
