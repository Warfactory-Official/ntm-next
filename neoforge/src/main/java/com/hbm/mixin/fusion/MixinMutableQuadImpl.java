/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.mixin.fusion;

import com.hbm.interfaces.injected.QuadLightOriginAccess;
import com.supermartijn642.fusion.model.custom.quad.MutableQuadImpl;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MutableQuadImpl.class, remap = false)
public class MixinMutableQuadImpl implements QuadLightOriginAccess {
    @Shadow private int flags;
    @Unique private int hbm$lightOrigin;

    @Shadow
    private void invalidateMaterialInfoCache() {
        throw new AssertionError();
    }

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
        hbm$lightOrigin =
                ((com.hbm.interfaces.injected.QuadLightOriginAccess)
                                (java.lang.Object) (quad.materialInfo()))
                        .hbm$lightOrigin();
    }

    @Inject(method = "copyMaterialInfo", at = @At("RETURN"))
    private void hbm$materialOrigin(
            BakedQuad.MaterialInfo info, CallbackInfoReturnable<Object> cir) {
        hbm$lightOrigin =
                ((com.hbm.interfaces.injected.QuadLightOriginAccess) (java.lang.Object) (info))
                        .hbm$lightOrigin();
    }

    @Inject(method = "copyFrom", at = @At("RETURN"))
    private void hbm$copyOrigin(@Coerce Object quad, CallbackInfoReturnable<Object> cir) {
        hbm$lightOrigin = ((QuadLightOriginAccess) quad).hbm$lightOrigin();
    }

    @Inject(
            method =
                    "lightEmission(I)Lcom/supermartijn642/fusion/api/model/custom/quad/MutableQuad;",
            at = @At("HEAD"),
            cancellable = true)
    private void hbm$lightEmission(int value, CallbackInfoReturnable<Object> cir) {
        flags = flags & ~0x1E | Math.clamp(value, 0, 15) << 1;
        invalidateMaterialInfoCache();
        cir.setReturnValue(this);
    }

    @Inject(method = "toBakedQuad", at = @At("RETURN"))
    private void hbm$exportOrigin(CallbackInfoReturnable<BakedQuad> cir) {
        ((com.hbm.interfaces.injected.QuadLightOriginAccess)
                        (java.lang.Object) (cir.getReturnValue().materialInfo()))
                .hbm$setLightOrigin(hbm$lightOrigin);
    }
}
