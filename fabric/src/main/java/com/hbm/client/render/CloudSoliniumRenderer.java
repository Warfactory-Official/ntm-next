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

import com.hbm.entity.effect.CloudSoliniumEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.ARGB;

public class CloudSoliniumRenderer
        extends EntityRenderer<CloudSoliniumEntity, CloudSoliniumRenderer.State>
        implements ConcurrentRenderStateExtraction {

    private static final int COLOR = 0x27FFDA;
    private static final int CORE = ARGB.colorFromFloat(1F, fr(COLOR), fg(COLOR), fb(COLOR));
    private static final int SHELL = ARGB.colorFromFloat(0.125F, fr(COLOR), fg(COLOR), fb(COLOR));

    public CloudSoliniumRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0F;
    }

    private static float fr(int color) {
        return (color >> 16 & 0xFF) / 255F;
    }

    private static float fg(int color) {
        return (color >> 8 & 0xFF) / 255F;
    }

    private static float fb(int color) {
        return (color & 0xFF) / 255F;
    }

    @Override
    protected boolean affectedByCulling(CloudSoliniumEntity entity) {
        return false;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(CloudSoliniumEntity entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.age = entity.age + partialTicks;
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.scale(state.age, state.age, state.age);

        CloudFleijaRenderer.submitSphere(
                poseStack, collector, CloudRenderTypes.OPAQUE_UNCULLED, CORE);

        double outerScale = 1.025;
        for (int i = 0; i < 3; i++) {
            poseStack.scale((float) outerScale, (float) outerScale, (float) outerScale);
            CloudFleijaRenderer.submitSphere(
                    poseStack, collector, CloudRenderTypes.ADDITIVE, SHELL);
        }
        poseStack.popPose();

        super.submit(state, poseStack, collector, camera);
    }

    public static final class State extends EntityRenderState {
        float age;
    }
}
