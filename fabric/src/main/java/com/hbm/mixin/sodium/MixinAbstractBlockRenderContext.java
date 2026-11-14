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

package com.hbm.mixin.sodium;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.util.TriState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = AbstractBlockRenderContext.class, remap = false)
public class MixinAbstractBlockRenderContext {
    @ModifyExpressionValue(
            method = "bufferDefaultModel",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/caffeinemc/mods/sodium/client/render/model/AmbientOcclusionMode;toTriState()Lnet/minecraft/util/TriState;"))
    private TriState hbm$preserveQuadAo(TriState partAo, @Local BakedQuad quad) {
        return ((net.caffeinemc.mods.sodium.client.model.quad.BakedQuadView)
                                (java.lang.Object) (quad))
                        .hasAO()
                ? partAo
                : TriState.FALSE;
    }
}
