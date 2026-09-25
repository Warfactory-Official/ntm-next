// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.fabric.mixin;

import com.hbm.client.model.SectionGeometry;
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
public abstract class SectionCompilerGeometryMixin {
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
        var geometry = ((SectionGeometry.Region) region).hbm$sectionGeometry();
        if (geometry != null)
            geometry.emit(region, layer -> getOrBeginLayer(layers, buffers, layer));
    }
}
