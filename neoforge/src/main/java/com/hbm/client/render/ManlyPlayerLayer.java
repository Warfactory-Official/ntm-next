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

import com.hbm.client.model.Meshes;
import com.hbm.interfaces.injected.PlayerAppearance;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.util.RegistryUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import org.jspecify.annotations.Nullable;

public class ManlyPlayerLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    private static final Identifier TEXTURE = RegistryUtil.id("textures/entity/player_fem.png");
    private final HFRWavefrontObject mesh;
    private final int head, body, leftArm, rightArm, leftLeg, rightLeg;

    public ManlyPlayerLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);
        mesh = Meshes.load(RegistryUtil.id("models/armor/player_fem.obj")).noSmooth();
        head = mesh.partId("Head");
        body = mesh.partId("Body");
        leftArm = mesh.partId("LeftArm");
        rightArm = mesh.partId("RightArm");
        leftLeg = mesh.partId("LeftLeg");
        rightLeg = mesh.partId("RightLeg");
    }

    @Override
    public void submit(
            PoseStack pose,
            SubmitNodeCollector collector,
            int light,
            AvatarRenderState state,
            float yaw,
            float pitch) {
        if ((((com.hbm.interfaces.injected.PlayerAppearance) (state)).hbm$appearance()
                        & PlayerAppearance.MANLY)
                == 0) return;
        boolean translucent = state.isInvisible && !state.isInvisibleToPlayer;
        RenderType type =
                translucent
                        ? RenderTypes.entityTranslucentCull(TEXTURE)
                        : state.isInvisible ? null : WorldRenderPipeline.oneSidedCutout(TEXTURE);
        int color = translucent ? 654311423 : -1;
        int overlay = LivingEntityRenderer.getOverlayCoords(state, 0F);
        PlayerModel model = getParentModel();
        limb(
                pose,
                collector,
                light,
                model.head,
                head,
                0,
                0,
                type,
                color,
                overlay,
                state.outlineColor);
        limb(
                pose,
                collector,
                light,
                model.body,
                body,
                0,
                0,
                type,
                color,
                overlay,
                state.outlineColor);
        limb(
                pose,
                collector,
                light,
                model.leftArm,
                leftArm,
                5F,
                2F,
                type,
                color,
                overlay,
                state.outlineColor);
        limb(
                pose,
                collector,
                light,
                model.rightArm,
                rightArm,
                -5F,
                2F,
                type,
                color,
                overlay,
                state.outlineColor);
        limb(
                pose,
                collector,
                light,
                model.leftLeg,
                leftLeg,
                1.9F,
                12F,
                type,
                color,
                overlay,
                state.outlineColor);
        limb(
                pose,
                collector,
                light,
                model.rightLeg,
                rightLeg,
                -1.9F,
                12F,
                type,
                color,
                overlay,
                state.outlineColor);
    }

    private void limb(
            PoseStack pose,
            SubmitNodeCollector collector,
            int light,
            ModelPart bone,
            int part,
            float originX,
            float originY,
            @Nullable RenderType type,
            int color,
            int overlay,
            int outlineColor) {
        pose.pushPose();
        bone.translateAndRotate(pose);
        pose.translate(-originX / 16F, -originY / 16F, 0);
        pose.scale(1F / 16F, 1F / 16F, 1F / 16F);
        if (type != null) {
            collector.submitCustomGeometry(
                    pose,
                    type,
                    (matrix, vertices) ->
                            mesh.renderPart(matrix, vertices, light, color, overlay, part));
        }
        if (outlineColor != 0) {
            WorldRenderPipeline.oneSidedCutout(TEXTURE)
                    .outline()
                    .ifPresent(
                            outline ->
                                    collector.submitCustomGeometry(
                                            pose,
                                            outline,
                                            (matrix, vertices) ->
                                                    mesh.renderPart(
                                                            matrix,
                                                            vertices,
                                                            LightCoordsUtil.FULL_BRIGHT,
                                                            outlineColor,
                                                            part)));
        }
        pose.popPose();
    }
}
