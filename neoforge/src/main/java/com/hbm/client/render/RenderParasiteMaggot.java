// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.entity.mob.EntityParasiteMaggot;
import com.hbm.lib.Library;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.silverfish.SilverfishModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public final class RenderParasiteMaggot
        extends MobRenderer<EntityParasiteMaggot, ParasiteMaggotRenderState, SilverfishModel>
        implements ConcurrentRenderStateExtraction {

    private static final Identifier TEXTURE = Library.id("textures/entity/parasite_maggot.png");

    public RenderParasiteMaggot(EntityRendererProvider.Context context) {
        super(context, new SilverfishModel(context.bakeLayer(ModelLayers.SILVERFISH)), 0.3F);
    }

    @Override
    public ParasiteMaggotRenderState createRenderState() {
        return new ParasiteMaggotRenderState();
    }

    @Override
    protected float getFlipDegrees() {
        return 180.0F;
    }

    @Override
    public Identifier getTextureLocation(ParasiteMaggotRenderState state) {
        return TEXTURE;
    }
}
