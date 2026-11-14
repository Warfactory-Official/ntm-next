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

import com.hbm.client.render.GlyphidRenderer;
import com.hbm.entity.mob.glyphid.GlyphidNuclearEntity;
import com.hbm.main.ResourceManager;
import com.hbm.util.RegistryUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.joml.Quaternionfc;

public final class GlyphidNuclearRenderer extends GlyphidRenderer<GlyphidNuclearEntity> {

    private static final Identifier MINI_NUKE_TEX =
            RegistryUtil.id("textures/models/projectiles/mini_nuke.png");
    private static final RenderType MINI_NUKE_TYPE =
            WorldRenderPipeline.oneSidedCutout(MINI_NUKE_TEX);
    private static final RenderType WHITE_FLASH_TYPE =
            WorldRenderPipeline.oneSidedTranslucent(ResourceManager.white_tex, false);

    private final int miniNuke;

    public GlyphidNuclearRenderer(EntityRendererProvider.Context context) {
        super(context);
        miniNuke = ResourceManager.projectiles.partId("MiniNuke");
    }

    @Override
    public void extractRenderState(
            GlyphidNuclearEntity entity, GlyphidRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.deathAge = entity.deathTicks + partialTicks;
    }

    @Override
    public void submit(
            GlyphidRenderState state,
            PoseStack pose,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        int light = state.lightCoords;
        RenderType type = WorldRenderPipeline.oneSidedCutout(state.texture);

        pose.pushPose();
        applySwellScale(pose, state);
        applyBaseTransform(pose, state);
        submitLimbs(state, pose, collector, light, type, -1);
        submitMiniNuke(pose, collector, light, MINI_NUKE_TYPE, -1);

        int packedAlpha = flashPackedAlpha(state.deathAge / 20F);
        if (packedAlpha > 0) {
            int flashColor = ARGB.color(packedAlpha, 255, 255, 255);
            submitLimbs(state, pose, collector, light, WHITE_FLASH_TYPE, flashColor);
            submitMiniNuke(pose, collector, light, WHITE_FLASH_TYPE, flashColor);
        }
        pose.popPose();

        submitVanillaTail(state, pose, collector, camera);
    }

    private void applySwellScale(PoseStack pose, GlyphidRenderState state) {
        float swell = state.deathAge / 95F;
        float flash = 1.0F + Mth.sin(swell * 100.0F) * swell * 0.01F;

        if (swell < 0.0F) swell = 0.0F;
        if (swell > 1.0F) swell = 1.0F;

        swell *= swell;
        swell *= swell;

        float scaleHorizontal = (1.0F + swell * 0.4F) * flash;
        float scaleVertical = (1.0F + swell * 0.1F) / flash;
        pose.scale(scaleHorizontal, scaleVertical, scaleHorizontal);
    }

    private static int flashPackedAlpha(float swell) {
        int a = (int) (swell * 0.2F * 255.0F);

        if ((int) (swell * 10.0F) % 4 < 2) return (int) (a * 0.75F);

        if (a < 0) a = 0;
        if (a > 255) a = 255;
        return a;
    }

    private void submitMiniNuke(
            PoseStack pose, SubmitNodeCollector collector, int light, RenderType type, int color) {
        pose.pushPose();
        pose.translate(0F, 1F, 0F);
        pose.mulPose((Quaternionfc) Axis.XP.rotationDegrees(90F));
        collector.submitCustomGeometry(
                pose,
                type,
                (p, buf) -> ResourceManager.projectiles.renderPart(p, buf, light, color, miniNuke));
        pose.popPose();
    }
}
