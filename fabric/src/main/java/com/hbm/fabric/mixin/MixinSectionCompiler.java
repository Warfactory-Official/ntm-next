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

package com.hbm.fabric.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexSorting;
import java.util.Map;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SectionCompiler.class)
public abstract class MixinSectionCompiler {
    @Shadow
    protected abstract BufferBuilder getOrBeginLayer(
            Map<ChunkSectionLayer, BufferBuilder> layers,
            SectionBufferBuilderPack buffers,
            ChunkSectionLayer layer);

    @Inject(
            method = "compile",
            at = @At(value = "INVOKE", target = "Ljava/util/Map;entrySet()Ljava/util/Set;"))
    private void hbm$append(
            SectionPos section,
            RenderSectionRegion region,
            VertexSorting sorting,
            SectionBufferBuilderPack buffers,
            CallbackInfoReturnable<SectionCompiler.Results> cir,
            @Local Map<ChunkSectionLayer, BufferBuilder> layers) {
        var geometry =
                ((com.hbm.client.model.SectionGeometry.Region) (region)).hbm$sectionGeometry();
        if (geometry != null)
            geometry.emit(region, layer -> getOrBeginLayer(layers, buffers, layer));
    }
}
