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

package com.hbm.neoforge.mixin;

import com.hbm.client.model.QuadLighting;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.QuadInstance;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockModelLighter;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.ao.EnhancedBlockModelLighter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EnhancedBlockModelLighter.class, remap = false)
public abstract class MixinEnhancedBlockModelLighter extends BlockModelLighter {
    @ModifyVariable(method = "prepareQuadFlat", at = @At("HEAD"), argsOnly = true)
    private int hbm$ownFlatLight(
            int original,
            BlockAndTintGetter level,
            BlockState state,
            BlockPos pos,
            int lightCoords,
            BakedQuad quad,
            QuadInstance output) {
        return QuadLighting.usesOwnBlock(quad.materialInfo())
                ? getLightCoords(state, level, pos)
                : original;
    }

    @Inject(method = "prepareQuadAmbientOcclusion", at = @At("RETURN"))
    private void hbm$ownLight(
            BlockAndTintGetter level,
            BlockState state,
            BlockPos pos,
            BakedQuad quad,
            QuadInstance output,
            CallbackInfo ci) {
        if (QuadLighting.usesOwnBlock(quad.materialInfo()))
            output.setLightCoords(getLightCoords(state, level, pos));
    }

    @ModifyExpressionValue(
            method = "calculateIrregular",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/block/state/BlockState;isCollisionShapeFullBlock(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Z"))
    private boolean hbm$cellFaces(boolean fullBlock, @Local(argsOnly = true) BakedQuad quad) {
        return fullBlock && !QuadLighting.usesCell(quad.materialInfo());
    }
}
