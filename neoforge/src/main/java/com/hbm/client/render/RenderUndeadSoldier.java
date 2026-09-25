// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.entity.mob.EntityUndeadSoldier;
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

public final class RenderUndeadSoldier
        extends MobRenderer<
                EntityUndeadSoldier,
                UndeadSoldierRenderState,
                ZombieModel<UndeadSoldierRenderState>>
        implements ConcurrentRenderStateExtraction {

    private static final Identifier ZOMBIE_TEXTURE =
            Identifier.withDefaultNamespace("textures/entity/zombie/zombie.png");
    private static final Identifier SKELETON_TEXTURE =
            Identifier.withDefaultNamespace("textures/entity/skeleton/skeleton.png");

    private final ZombieModel<UndeadSoldierRenderState> zombieModel;
    private final ZombieModel<UndeadSoldierRenderState> skeletonModel;

    public RenderUndeadSoldier(EntityRendererProvider.Context context) {
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
            EntityUndeadSoldier entity, UndeadSoldierRenderState state, float partialTicks) {
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
                state.soldierType == EntityUndeadSoldier.TYPE_SKELETON
                        ? skeletonModel
                        : zombieModel;
        super.submit(state, pose, collector, camera);
    }

    @Override
    public Identifier getTextureLocation(UndeadSoldierRenderState state) {
        return state.soldierType == EntityUndeadSoldier.TYPE_SKELETON
                ? SKELETON_TEXTURE
                : ZOMBIE_TEXTURE;
    }
}
