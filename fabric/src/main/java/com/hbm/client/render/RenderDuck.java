// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.entity.mob.EntityDuck;
import com.hbm.entity.mob.EntityQuackos;
import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.model.animal.chicken.AdultChickenModel;
import net.minecraft.client.model.animal.chicken.ChickenModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.ChickenRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class RenderDuck<T extends EntityDuck>
        extends MobRenderer<T, ChickenRenderState, ChickenModel>
        implements ConcurrentRenderStateExtraction {

    private final float scale;

    private RenderDuck(EntityRendererProvider.Context context, float shadow, float scale) {
        super(context, new AdultChickenModel(context.bakeLayer(ModelLayers.CHICKEN)), shadow);
        this.scale = scale;
    }

    public static RenderDuck<EntityDuck> duck(EntityRendererProvider.Context context) {
        return new RenderDuck<>(context, 0.3F, 1F);
    }

    public static RenderDuck<EntityQuackos> quackos(EntityRendererProvider.Context context) {
        return new RenderDuck<>(context, 7.5F, 25F);
    }

    @Override
    public ChickenRenderState createRenderState() {
        return new ChickenRenderState();
    }

    @Override
    public void extractRenderState(T entity, ChickenRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.flap = Mth.lerp(partialTicks, entity.oFlap, entity.flap);
        state.flapSpeed = Mth.lerp(partialTicks, entity.oFlapSpeed, entity.flapSpeed);
    }

    @Override
    protected void scale(ChickenRenderState state, PoseStack poseStack) {
        if (scale != 1F) poseStack.scale(scale, scale, scale);
    }

    @Override
    public Identifier getTextureLocation(ChickenRenderState state) {
        return ResourceManager.duck_tex;
    }
}
