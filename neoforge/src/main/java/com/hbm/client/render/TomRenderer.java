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

import com.hbm.entity.projectile.TomEntity;
import com.hbm.main.ResourceManager;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import java.util.Random;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.LightCoordsUtil;
import org.joml.Quaternionfc;

public class TomRenderer extends EntityRenderer<TomEntity, TomRenderer.State>
        implements ConcurrentRenderStateExtraction {
    private static final RenderType TYPE = FlatCutout.of(ResourceManager.tom_main_tex);
    private static final RenderType FLAME = TomRenderTypes.flame(ResourceManager.tom_flame_tex);

    private static final int[] SHELL_OFFSETS = shellOffsets();

    private static final double SCROLL_PERIOD = 50000D;
    private static final double SCROLL_DIVISOR = 2500D;

    public TomRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    private static int[] shellOffsets() {
        Random random = new Random(0);
        int[] offsets = new int[20];
        for (int i = 0; i < offsets.length; i++) offsets[i] = random.nextInt(90);
        return offsets;
    }

    @Override
    protected boolean affectedByCulling(TomEntity entity) {
        return false;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(TomEntity entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        long now = GameTime.now();
        state.spin = -(now / 10L) % 360L;
        state.scroll = (float) ((now % SCROLL_PERIOD) / SCROLL_DIVISOR);
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0D, -50D, 0D);
        poseStack.scale(100F, 100F, 100F);
        collector.submitCustomGeometry(
                poseStack,
                TYPE,
                (pose, buffer) ->
                        ResourceManager.tom_main.render(
                                pose, buffer, LightCoordsUtil.FULL_BRIGHT, -1));

        float spin = state.spin;
        float scroll = state.scroll;
        poseStack.scale(0.8F, 5F, 0.8F);
        for (int shell = 0; shell < SHELL_OFFSETS.length; shell++) {
            poseStack.mulPose((Quaternionfc) Axis.YP.rotationDegrees(spin + SHELL_OFFSETS[shell]));
            collector.submitCustomGeometry(
                    poseStack,
                    FLAME,
                    (pose, buffer) ->
                            ResourceManager.tom_flame.render(
                                    pose,
                                    buffer,
                                    LightCoordsUtil.FULL_BRIGHT,
                                    -1,
                                    1F,
                                    1F,
                                    0F,
                                    scroll));
            poseStack.mulPose((Quaternionfc) Axis.YN.rotationDegrees(spin));
            poseStack.scale(-1.015F, 0.9F, 1.015F);
        }
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    public static final class State extends EntityRenderState {
        public float spin;
        public float scroll;
    }
}
