// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.compat.sodium;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.caffeinemc.mods.sodium.client.model.quad.BakedQuadView;
import net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.util.TriState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(value = AbstractBlockRenderContext.class, remap = false)
public class QuadAoMixin {

    @ModifyExpressionValue(
            method = "bufferDefaultModel",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/caffeinemc/mods/sodium/client/render/model/AmbientOcclusionMode;toTriState()Lnet/minecraft/util/TriState;"))
    private TriState hbm$preserveQuadAo(TriState partAo, @Local BakedQuad quad) {
        return ((BakedQuadView) (Object) quad).hasAO() ? partAo : TriState.FALSE;
    }
}
