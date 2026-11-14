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

import com.hbm.entity.grenade.DisperserCanisterEntity;
import com.hbm.entity.grenade.GrenadeBouncyGenericEntity;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluidProperty;
import com.hbm.items.weapon.DisperserItem;
import com.hbm.render.util.Vertices;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import org.joml.Quaternionfc;

public class GenericGrenadeRenderer<T extends Entity>
        extends EntityRenderer<T, GenericGrenadeRenderer.State>
        implements ConcurrentRenderStateExtraction {

    private final SpriteGetter sprites;

    public GenericGrenadeRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.sprites = context.getSprites();
        this.shadowRadius = 0F;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(T entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);

        if (entity instanceof DisperserCanisterEntity canister) {
            DisperserItem type = canister.getDisperserItem();
            state.base = sprite(type.iconTexture(0));
            state.overlay = sprite(type.iconTexture(1));
            NTMFluidProperty prop = NTMFluidProperties.get(canister.getFluid());
            state.overlayColor = prop != null ? prop.color() : 0xFFFFFF;
        } else {
            state.base = sprite(((GrenadeBouncyGenericEntity) entity).getGrenade().iconTexture());
            state.overlay = null;
        }

        state.light = state.lightCoords;
    }

    private TextureAtlasSprite sprite(Identifier texture) {
        return sprites.get(Sheets.ITEMS_MAPPER.apply(texture));
    }

    @Override
    public void submit(
            State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.scale(0.5F, 0.5F, 0.5F);
        pose.mulPose((Quaternionfc) camera.orientation);

        quad(state, pose, collector, state.base, 1F, 1F, 1F);

        if (state.overlay != null) {
            int rgb = state.overlayColor;
            quad(
                    state,
                    pose,
                    collector,
                    state.overlay,
                    (((rgb >> 16) & 0xFF) / 2) / 127F,
                    (((rgb >> 8) & 0xFF) / 2) / 127F,
                    ((rgb & 0xFF) / 2) / 127F);
        }

        pose.popPose();
        super.submit(state, pose, collector, camera);
    }

    private void quad(
            State state,
            PoseStack pose,
            SubmitNodeCollector collector,
            TextureAtlasSprite sprite,
            float r,
            float g,
            float b) {
        float minU = sprite.getU0();
        float maxU = sprite.getU1();
        float minV = sprite.getV0();
        float maxV = sprite.getV1();
        float max = 1.0F;
        float offX = 0.5F;
        float offY = 0.25F;
        int light = state.light;
        int color = ARGB.colorFromFloat(1F, r, g, b);

        collector.submitCustomGeometry(
                pose,
                Sheets.cutoutItemSheet(),
                (p, tess) -> {
                    Vertices.emit(
                            tess,
                            p,
                            0.0F - offX,
                            0.0F - offY,
                            0.0F,
                            color,
                            minU,
                            maxV,
                            light,
                            0F,
                            1F,
                            0F);
                    Vertices.emit(
                            tess,
                            p,
                            max - offX,
                            0.0F - offY,
                            0.0F,
                            color,
                            maxU,
                            maxV,
                            light,
                            0F,
                            1F,
                            0F);
                    Vertices.emit(
                            tess,
                            p,
                            max - offX,
                            max - offY,
                            0.0F,
                            color,
                            maxU,
                            minV,
                            light,
                            0F,
                            1F,
                            0F);
                    Vertices.emit(
                            tess,
                            p,
                            0.0F - offX,
                            max - offY,
                            0.0F,
                            color,
                            minU,
                            minV,
                            light,
                            0F,
                            1F,
                            0F);
                });
    }

    public static final class State extends EntityRenderState {
        TextureAtlasSprite base;
        TextureAtlasSprite overlay;
        int overlayColor;
        int light;
    }
}
