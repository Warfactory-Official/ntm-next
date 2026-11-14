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

package com.hbm.mixin.compat.iris;

import com.hbm.interfaces.injected.SectionGeometryVertexConsumer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.buffers.ChunkVertexConsumer;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.irisshaders.iris.vertices.sodium.terrain.ChunkVertexExtension;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ChunkVertexConsumer.class, remap = false)
public abstract class MixinChunkVertexConsumer implements SectionGeometryVertexConsumer {
    @Unique
    private static final boolean hbm$irisVertexData =
            ChunkVertexExtension.class.isAssignableFrom(ChunkVertexEncoder.Vertex.class);

    @Shadow @Final private ChunkVertexEncoder.Vertex[] vertices;
    @Shadow private int vertexIndex;
    @Unique private boolean hbm$sectionGeometry;
    @Unique private int hbm$blockId;
    @Unique private byte hbm$emission;
    @Unique private int hbm$x, hbm$y, hbm$z;

    @Override
    public void hbm$beginSectionGeometry(BlockState state, int x, int y, int z) {
        var ids = WorldRenderingSettings.INSTANCE.getBlockStateIds();
        hbm$sectionGeometry = true;
        hbm$blockId = ids == null ? -1 : ids.getOrDefault(state, -1);
        hbm$emission = (byte) state.getLightEmission();
        hbm$x = x;
        hbm$y = y;
        hbm$z = z;
    }

    @Inject(method = "setNormal", at = @At("HEAD"))
    private void hbm$normal(float x, float y, float z, CallbackInfoReturnable<VertexConsumer> cir) {
        if (hbm$sectionGeometry)
            ((com.hbm.interfaces.injected.SectionGeometryNormalAccess) (vertices[vertexIndex]))
                    .hbm$sectionNormal(x, y, z);
    }

    @Inject(method = "addVertex", at = @At("HEAD"))
    private void hbm$clearNormal(
            float x, float y, float z, CallbackInfoReturnable<VertexConsumer> cir) {
        ((com.hbm.interfaces.injected.SectionGeometryNormalAccess) (vertices[vertexIndex]))
                .hbm$clearSectionNormal();
    }

    @Inject(
            method = "potentiallyEndVertex",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/buffers/ChunkVertexConsumer;calculateNormal()I"))
    private void hbm$shaderAttributes(CallbackInfoReturnable<VertexConsumer> cir) {
        if (!hbm$sectionGeometry || !hbm$irisVertexData) return;
        for (var vertex : vertices) {
            ((net.irisshaders.iris.vertices.sodium.terrain.ChunkVertexExtension) (vertex))
                    .iris$setData(hbm$emission, (byte) 0, hbm$blockId, hbm$x, hbm$y, hbm$z);
        }
    }

    @Override
    public void hbm$endSectionGeometry() {
        if (!hbm$sectionGeometry) return;
        hbm$sectionGeometry = false;
        for (var vertex : vertices) {
            ((com.hbm.interfaces.injected.SectionGeometryNormalAccess) (vertex))
                    .hbm$clearSectionNormal();
            if (hbm$irisVertexData)
                ((net.irisshaders.iris.vertices.sodium.terrain.ChunkVertexExtension) (vertex))
                        .iris$setData((byte) 0, (byte) 0, -1, 0, 0, 0);
        }
    }
}
