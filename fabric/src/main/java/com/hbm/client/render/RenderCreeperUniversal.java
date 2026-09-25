// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.entity.CreeperRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.CreeperRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class RenderCreeperUniversal extends CreeperRenderer
        implements ConcurrentRenderStateExtraction {

    private final Identifier texture;
    private final float swellMod;

    public RenderCreeperUniversal(
            EntityRendererProvider.Context context, Identifier texture, float swellMod) {
        super(context);
        this.texture = texture;
        this.swellMod = swellMod;
    }

    @Override
    protected void scale(CreeperRenderState state, PoseStack poseStack) {
        float swell = state.swelling;
        float wobble = 1.0F + Mth.sin(swell * 100.0F) * swell * 0.01F;
        swell = Mth.clamp(swell, 0.0F, 1.0F);
        swell *= swell;
        swell *= swell;
        swell *= swellMod;
        poseStack.scale(
                (1.0F + swell * 0.4F) * wobble,
                (1.0F + swell * 0.1F) / wobble,
                (1.0F + swell * 0.4F) * wobble);
    }

    @Override
    public Identifier getTextureLocation(CreeperRenderState state) {
        return texture;
    }
}
