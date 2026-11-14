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

package com.hbm.mixin;

import com.hbm.client.render.BlockEntityShading;
import com.hbm.client.render.OitPipelineOwner;
import com.hbm.client.render.OrderedBlendCollection;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.feature.phase.FeatureRenderPhase;
import net.minecraft.client.renderer.feature.phase.SimpleFeatureRenderPhase;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(SubmitNodeCollection.class)
public class MixinSubmitNodeCollection implements OrderedBlendCollection {
    @ModifyVariable(method = "submitCustomGeometry", at = @At("HEAD"), argsOnly = true)
    private RenderType hbm$chunkShading(RenderType type) {
        return BlockEntityShading.shade(type);
    }

    @Shadow @Final public SimpleFeatureRenderPhase translucentCustomGeometry;
    @Shadow @Final public SimpleFeatureRenderPhase oitTranslucent;
    @Shadow @Final @Mutable private List<FeatureRenderPhase<?>> allPhases;
    @Unique private SimpleFeatureRenderPhase hbm$orderedBlends;

    @WrapOperation(
            method = "submitCustomGeometry",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/renderer/feature/phase/SimpleFeatureRenderPhase;submit(Lnet/minecraft/client/renderer/feature/submit/SubmitNode;)V"))
    private void hbm$orderedNativeBlend(
            SimpleFeatureRenderPhase phase,
            SubmitNode submit,
            Operation<Void> original,
            @Local(argsOnly = true) RenderType type) {
        if (phase == translucentCustomGeometry
                && phase == oitTranslucent
                && type.pipeline() instanceof OitPipelineOwner owner
                && owner.orderedBlend()) {
            if (hbm$orderedBlends == null) {
                hbm$orderedBlends = new SimpleFeatureRenderPhase();
                allPhases = new ArrayList<>(allPhases);
                allPhases.add(hbm$orderedBlends);
            }
            hbm$orderedBlends.submit(submit);
        } else original.call(phase, submit);
    }

    @Override
    public SimpleFeatureRenderPhase hbm$orderedBlends() {
        return hbm$orderedBlends;
    }
}
