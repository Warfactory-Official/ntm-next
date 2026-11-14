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

import com.hbm.interfaces.injected.SectionGeometryNormalAccess;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkVertexEncoder.Vertex.class)
public abstract class MixinVertex implements SectionGeometryNormalAccess {
    @Unique private boolean hbm$hasSectionNormal;
    @Unique private float hbm$normalX, hbm$normalY, hbm$normalZ;

    @Inject(method = "copyVertexTo", at = @At("RETURN"))
    private static void hbm$copyNormal(
            ChunkVertexEncoder.Vertex from, ChunkVertexEncoder.Vertex to, CallbackInfo ci) {
        var source = (SectionGeometryNormalAccess) ((java.lang.Object) (from));
        var target = (SectionGeometryNormalAccess) ((java.lang.Object) (to));
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
        ((com.hbm.interfaces.injected.SectionGeometryNormalAccess) (vertex))
                .hbm$clearSectionNormal();
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
