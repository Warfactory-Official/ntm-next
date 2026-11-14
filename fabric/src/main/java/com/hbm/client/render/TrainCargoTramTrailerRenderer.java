/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

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

public class TrainCargoTramTrailerRenderer extends TrainCargoTramRenderer<TrainCargoTramTrailer> {
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

    public TrainCargoTramTrailerRenderer(EntityRendererProvider.Context context) {
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
