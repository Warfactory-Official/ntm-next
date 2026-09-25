// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.interfaces.injected.IQuadLightOrigin;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BakedQuad.MaterialInfo.class)
public class MixinQuadMaterialLighting implements IQuadLightOrigin {
    @Unique private int hbm$lightOrigin;

    @Override
    public int hbm$lightOrigin() {
        return hbm$lightOrigin;
    }

    @Override
    public void hbm$setLightOrigin(int origin) {
        hbm$lightOrigin = origin;
    }

    @Inject(method = "equals", at = @At("RETURN"), cancellable = true)
    private void hbm$compareLighting(Object other, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            cir.setReturnValue(hbm$lightOrigin == ((IQuadLightOrigin) other).hbm$lightOrigin());
        }
    }

    @Inject(method = "hashCode", at = @At("RETURN"), cancellable = true)
    private void hbm$hashLighting(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(31 * cir.getReturnValueI() + hbm$lightOrigin);
    }
}
