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

import com.hbm.entity.mob.UndeadSoldierEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.zombie.ZombieModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;

public final class UndeadSoldierRenderer
        extends MobRenderer<
                UndeadSoldierEntity,
                UndeadSoldierRenderState,
                ZombieModel<UndeadSoldierRenderState>>
        implements ConcurrentRenderStateExtraction {

    private static final Identifier ZOMBIE_TEXTURE =
            Identifier.withDefaultNamespace("textures/entity/zombie/zombie.png");
    private static final Identifier SKELETON_TEXTURE =
            Identifier.withDefaultNamespace("textures/entity/skeleton/skeleton.png");

    private final ZombieModel<UndeadSoldierRenderState> zombieModel;
    private final ZombieModel<UndeadSoldierRenderState> skeletonModel;

    public UndeadSoldierRenderer(EntityRendererProvider.Context context) {
        super(context, new ZombieModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), .5F);
        zombieModel = model;
        skeletonModel = new ZombieModel<>(context.bakeLayer(ModelLayers.SKELETON));
        ArmorModelSet<HumanoidModel<UndeadSoldierRenderState>> armor =
                ArmorModelSet.bake(
                        ModelLayers.ZOMBIE_ARMOR, context.getModelSet(), HumanoidModel::new);
        addLayer(
                new CustomHeadLayer<>(
                        this,
                        context.getModelSet(),
                        context.getPlayerSkinRenderCache(),
                        CustomHeadLayer.Transforms.DEFAULT));
        addLayer(new WingsLayer<>(this, context.getModelSet(), context.getEquipmentRenderer()));
        addLayer(new ItemInHandLayer<>(this));
        addLayer(new HumanoidArmorLayer<>(this, armor, context.getEquipmentRenderer()));
    }

    @Override
    public UndeadSoldierRenderState createRenderState() {
        return new UndeadSoldierRenderState();
    }

    @Override
    public void extractRenderState(
            UndeadSoldierEntity entity, UndeadSoldierRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        HumanoidMobRenderer.extractHumanoidRenderState(
                entity, state, partialTicks, itemModelResolver);
        state.soldierType = entity.soldierType();
    }

    @Override
    public void submit(
            UndeadSoldierRenderState state,
            PoseStack pose,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        model =
                state.soldierType == UndeadSoldierEntity.TYPE_SKELETON
                        ? skeletonModel
                        : zombieModel;
        super.submit(state, pose, collector, camera);
    }

    @Override
    public Identifier getTextureLocation(UndeadSoldierRenderState state) {
        return state.soldierType == UndeadSoldierEntity.TYPE_SKELETON
                ? SKELETON_TEXTURE
                : ZOMBIE_TEXTURE;
    }
}
