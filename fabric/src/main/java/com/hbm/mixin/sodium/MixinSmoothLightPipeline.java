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

import com.hbm.client.model.QuadLighting;
import com.hbm.interfaces.injected.ModelLightCache;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.Arrays;
import net.caffeinemc.mods.sodium.client.model.light.data.LightDataAccess;
import net.caffeinemc.mods.sodium.client.model.light.data.QuadLightData;
import net.caffeinemc.mods.sodium.client.model.light.smooth.SmoothLightPipeline;
import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadView;
import net.caffeinemc.mods.sodium.client.render.model.QuadViewImpl;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SmoothLightPipeline.class, remap = false)
public class MixinSmoothLightPipeline implements ModelLightCache {
    @Shadow @Final private LightDataAccess lightCache;

    @Shadow private long cachedPos;

    @Override
    public void hbm$resetLightCache() {
        cachedPos = Long.MIN_VALUE;
    }

    @Inject(method = "calculate", at = @At("RETURN"))
    private void hbm$ownLight(
            ModelQuadView quad,
            BlockPos pos,
            QuadLightData out,
            Direction cullFace,
            Direction lightFace,
            Direction shadeOverride,
            boolean enhanced,
            CallbackInfo ci) {
        if (quad instanceof QuadViewImpl view && QuadLighting.isOwnBlock(view.getTag())) {
            Arrays.fill(
                    out.lm,
                    QuadLighting.applyEmission(
                            LightDataAccess.getEmissiveLightmap(lightCache.get(pos)),
                            view.getTag()));
        }
    }

    @ModifyExpressionValue(
            method = "calculate",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/caffeinemc/mods/sodium/client/model/light/data/LightDataAccess;unpackFC(I)Z"))
    private boolean hbm$cellFaces(boolean fullBlock, @Local(argsOnly = true) ModelQuadView quad) {
        return fullBlock
                && !(quad instanceof QuadViewImpl view && QuadLighting.isCell(view.getTag()));
    }
}
