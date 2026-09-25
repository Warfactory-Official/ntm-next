// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.compat.iris;

import com.hbm.interfaces.injected.ISectionGeometryNormal;
import com.hbm.interfaces.injected.ISectionGeometryVertexConsumer;
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
public abstract class MixinChunkVertexConsumer implements ISectionGeometryVertexConsumer {

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
            ((ISectionGeometryNormal) vertices[vertexIndex]).hbm$sectionNormal(x, y, z);
    }

    @Inject(method = "addVertex", at = @At("HEAD"))
    private void hbm$clearNormal(
            float x, float y, float z, CallbackInfoReturnable<VertexConsumer> cir) {
        ((ISectionGeometryNormal) vertices[vertexIndex]).hbm$clearSectionNormal();
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
            ((ChunkVertexExtension) vertex)
                    .iris$setData(hbm$emission, (byte) 0, hbm$blockId, hbm$x, hbm$y, hbm$z);
        }
    }

    @Override
    public void hbm$endSectionGeometry() {
        if (!hbm$sectionGeometry) return;
        hbm$sectionGeometry = false;
        for (var vertex : vertices) {
            ((ISectionGeometryNormal) vertex).hbm$clearSectionNormal();
            if (hbm$irisVertexData)
                ((ChunkVertexExtension) vertex).iris$setData((byte) 0, (byte) 0, -1, 0, 0, 0);
        }
    }
}
