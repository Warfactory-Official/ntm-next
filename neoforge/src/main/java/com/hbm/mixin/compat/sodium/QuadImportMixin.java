// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.compat.sodium;

import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(value = MutableQuadViewImpl.class, remap = false)
public class QuadImportMixin {

    @Inject(method = "fromBakedQuad", at = @At("RETURN"))
    private void hbm$copyOrigin(BakedQuad quad, CallbackInfoReturnable<MutableQuadViewImpl> cir) {
        cir.getReturnValue().setTag(quad.materialInfo().hbm$lightOrigin());
    }
}
