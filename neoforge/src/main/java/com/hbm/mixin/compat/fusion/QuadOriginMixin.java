// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.compat.fusion;

import com.hbm.interfaces.injected.IQuadLightOrigin;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.supermartijn642.fusion.model.custom.quad.MutableQuadImpl", remap = false)
public class QuadOriginMixin implements IQuadLightOrigin {
    @Unique private int hbm$lightOrigin;

    @Override
    public int hbm$lightOrigin() {
        return hbm$lightOrigin;
    }

    @Override
    public void hbm$setLightOrigin(int value) {
        hbm$lightOrigin = value;
    }

    @Inject(method = "copyBakedQuad", at = @At("RETURN"))
    private void hbm$importOrigin(BakedQuad quad, CallbackInfoReturnable<Object> cir) {
        hbm$lightOrigin = quad.materialInfo().hbm$lightOrigin();
    }

    @Inject(method = "copyMaterialInfo", at = @At("RETURN"))
    private void hbm$materialOrigin(
            BakedQuad.MaterialInfo info, CallbackInfoReturnable<Object> cir) {
        hbm$lightOrigin = info.hbm$lightOrigin();
    }

    @Inject(method = "copyFrom", at = @At("RETURN"))
    private void hbm$copyOrigin(@Coerce Object quad, CallbackInfoReturnable<Object> cir) {
        hbm$lightOrigin = ((IQuadLightOrigin) quad).hbm$lightOrigin();
    }

    @Inject(method = "toBakedQuad", at = @At("RETURN"))
    private void hbm$exportOrigin(CallbackInfoReturnable<BakedQuad> cir) {
        cir.getReturnValue().materialInfo().hbm$setLightOrigin(hbm$lightOrigin);
    }
}
