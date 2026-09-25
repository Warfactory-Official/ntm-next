// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.entity.mob.EntityCyberCrab;
import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class RenderCyberCrab
        extends MobRenderer<EntityCyberCrab, LivingEntityRenderState, ModelCrab>
        implements ConcurrentRenderStateExtraction {

    public RenderCyberCrab(EntityRendererProvider.Context context) {
        super(context, new ModelCrab(ModelCrab.createBodyLayer().bakeRoot()), 1.0F);
        this.shadowStrength = 0F;
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }

    @Override
    protected void scale(LivingEntityRenderState state, PoseStack poseStack) {
        poseStack.translate(0F, 1.5F, 0F);
        poseStack.mulPose(Axis.YN.rotationDegrees(90F));
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return ResourceManager.crab_tex;
    }
}
