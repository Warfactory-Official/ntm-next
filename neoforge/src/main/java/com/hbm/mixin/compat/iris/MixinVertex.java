// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.compat.iris;

import com.hbm.interfaces.injected.ISectionGeometryNormal;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkVertexEncoder.Vertex.class)
public abstract class MixinVertex implements ISectionGeometryNormal {
    @Unique private boolean hbm$hasSectionNormal;
    @Unique private float hbm$normalX, hbm$normalY, hbm$normalZ;

    @Inject(method = "copyVertexTo", at = @At("RETURN"))
    private static void hbm$copyNormal(
            ChunkVertexEncoder.Vertex from, ChunkVertexEncoder.Vertex to, CallbackInfo ci) {
        var source = (ISectionGeometryNormal) from;
        var target = (ISectionGeometryNormal) to;
        if (source.hbm$hasSectionNormal())
            target.hbm$sectionNormal(
                    source.hbm$sectionNormalX(),
                    source.hbm$sectionNormalY(),
                    source.hbm$sectionNormalZ());
        else target.hbm$clearSectionNormal();
    }

    @Inject(method = "writeVertex", at = @At("HEAD"))
    private static void hbm$clearNormal(
            ChunkVertexEncoder.Vertex vertex,
            float x,
            float y,
            float z,
            int color,
            float ao,
            float u,
            float v,
            int light,
            CallbackInfo ci) {
        ((ISectionGeometryNormal) vertex).hbm$clearSectionNormal();
    }

    @Override
    public boolean hbm$hasSectionNormal() {
        return hbm$hasSectionNormal;
    }

    @Override
    public float hbm$sectionNormalX() {
        return hbm$normalX;
    }

    @Override
    public float hbm$sectionNormalY() {
        return hbm$normalY;
    }

    @Override
    public float hbm$sectionNormalZ() {
        return hbm$normalZ;
    }

    @Override
    public void hbm$sectionNormal(float x, float y, float z) {
        hbm$hasSectionNormal = true;
        hbm$normalX = x;
        hbm$normalY = y;
        hbm$normalZ = z;
    }

    @Override
    public void hbm$clearSectionNormal() {
        hbm$hasSectionNormal = false;
    }
}
