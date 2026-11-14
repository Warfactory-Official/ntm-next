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

package com.hbm.fabric.mixin.sodium;

import com.hbm.client.model.SectionGeometry;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.caffeinemc.mods.sodium.fabric.level.FabricLevelRenderHooks", remap = false)
public class MixinFabricLevelRenderHooks {
    @Inject(method = "retrieveChunkMeshAppenders", at = @At("RETURN"), cancellable = true)
    private void hbm$capture(Level level, BlockPos origin, CallbackInfoReturnable<List<?>> cir) {
        var geometry = SectionGeometry.capture(level, origin);
        if (geometry == null) return;
        List<Object> renderers = new ArrayList<>(cir.getReturnValue());
        renderers.add(geometry);
        cir.setReturnValue(renderers);
    }

    @Inject(method = "runChunkMeshAppenders", at = @At("RETURN"))
    private void hbm$append(
            List<?> renderers,
            Function<ChunkSectionLayer, VertexConsumer> output,
            LevelSlice slice,
            BlockPos origin,
            CallbackInfo ci) {
        for (Object renderer : renderers) {
            if (renderer instanceof SectionGeometry.Snapshot geometry) geometry.emit(slice, output);
        }
    }
}
