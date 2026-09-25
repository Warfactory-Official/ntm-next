// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.entity.mob.EntityPigeon;
import com.hbm.main.ResourceManager;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class RenderPigeon extends MobRenderer<EntityPigeon, PigeonRenderState, ModelPigeon>
        implements ConcurrentRenderStateExtraction {

    public RenderPigeon(EntityRendererProvider.Context context) {
        super(context, new ModelPigeon(ModelPigeon.createBodyLayer().bakeRoot()), 0.3F);
    }

    @Override
    public PigeonRenderState createRenderState() {
        return new PigeonRenderState();
    }

    @Override
    public void extractRenderState(
            EntityPigeon entity, PigeonRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        float fall = Mth.lerp(partialTicks, entity.prevFallTime, entity.fallTime);
        float dest = Mth.lerp(partialTicks, entity.prevDest, entity.dest);
        state.flap = (Mth.sin(fall) + 1.0F) * dest;
        state.fat = entity.isFat();
    }

    @Override
    public Identifier getTextureLocation(PigeonRenderState state) {
        return ResourceManager.pigeon_tex;
    }
}
