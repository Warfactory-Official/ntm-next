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

import com.hbm.entity.projectile.RubbleEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionfc;
import org.joml.Vector3f;

public class RubbleRenderer extends EntityRenderer<RubbleEntity, RubbleRenderer.State>
        implements ConcurrentRenderStateExtraction {
    private static final Axis TUMBLE = Axis.of(new Vector3f(1F, 1F, 1F));

    private final ModelPart model = RubbleModel.createBodyLayer().bakeRoot();
    private final Map<BlockState, RenderType> skins = new ConcurrentHashMap<>();

    public RubbleRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(RubbleEntity entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.skin = skins.computeIfAbsent(entity.getBlockState(), RubbleRenderer::skinFor);
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.mulPose((Quaternionfc) Axis.XP.rotationDegrees(180F));
        poseStack.mulPose((Quaternionfc) TUMBLE.rotationDegrees(state.ageInTicks % 360F * 10F));
        collector.submitModelPart(
                model, poseStack, state.skin, state.lightCoords, OverlayTexture.NO_OVERLAY, null);
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    private static RenderType skinFor(BlockState state) {
        Identifier name = downSprite(state).contents().name();
        return RenderTypes.entityCutoutCull(
                Identifier.fromNamespaceAndPath(
                        name.getNamespace(), "textures/" + name.getPath() + ".png"));
    }

    private static TextureAtlasSprite downSprite(BlockState state) {
        BlockStateModelSet models =
                Minecraft.getInstance().getModelManager().getBlockStateModelSet();
        BlockStateModel model = models.get(state);
        List<BlockStateModelPart> parts = new ArrayList<>(1);
        model.collectParts(RandomSource.create(state.getSeed(BlockPos.ZERO)), parts);
        for (BlockStateModelPart part : parts) {
            List<BakedQuad> quads = part.getQuads(Direction.DOWN);
            if (!quads.isEmpty()) return quads.getFirst().materialInfo().sprite();
        }
        return model.particleMaterial().sprite();
    }

    public static final class State extends EntityRenderState {
        RenderType skin;
    }
}
