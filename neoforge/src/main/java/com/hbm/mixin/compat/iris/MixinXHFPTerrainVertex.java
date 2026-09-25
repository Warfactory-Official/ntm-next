// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.compat.iris;

import com.hbm.interfaces.injected.ISectionGeometryNormal;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.irisshaders.iris.vertices.NormalHelper;
import net.irisshaders.iris.vertices.sodium.terrain.XHFPTerrainVertex;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(XHFPTerrainVertex.class)
public abstract class MixinXHFPTerrainVertex {
    @Unique private final Vector3f hbm$normal = new Vector3f();
    @Unique private final Vector4f hbm$tangent = new Vector4f();
    @Unique private final Vector3f hbm$tangentDirection = new Vector3f();
    @Shadow @Final private int normalOffset;
    @Shadow @Final private Vector3f[] scratchValues;
    @Unique private boolean hbm$hasNormal;
    @Unique private int hbm$normalTangent;

    @WrapOperation(
            method = "write",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/irisshaders/iris/vertices/sodium/terrain/XHFPTerrainVertex;packLightAndData(IZI)I"))
    private int hbm$normalFrame(
            int light,
            boolean handedness,
            int section,
            Operation<Integer> original,
            @Local ChunkVertexEncoder.Vertex vertex,
            @Local(argsOnly = true) ChunkVertexEncoder.Vertex[] vertices) {
        var source = (ISectionGeometryNormal) vertex;
        hbm$hasNormal = normalOffset != 0 && source.hbm$hasSectionNormal();
        if (!hbm$hasNormal) return original.call(light, handedness, section);
        hbm$normal.set(
                source.hbm$sectionNormalX(),
                source.hbm$sectionNormalY(),
                source.hbm$sectionNormalZ());
        assert hbm$normal.lengthSquared() > 0;
        hbm$normal.normalize();

        hbm$tangent.set(0, 0, 0, 1);
        if (hbm$tangent(vertices[0], vertices[1], vertices[2]) == -1)
            hbm$tangent(vertices[2], vertices[3], vertices[0]);
        hbm$normalTangent =
                NormalHelper.encodeNormalTangent(
                        hbm$normal,
                        hbm$tangentDirection.set(hbm$tangent),
                        scratchValues[0],
                        scratchValues[1],
                        scratchValues[2]);
        return original.call(light, hbm$tangent.w >= 0, section);
    }

    @Unique
    private int hbm$tangent(
            ChunkVertexEncoder.Vertex a, ChunkVertexEncoder.Vertex b, ChunkVertexEncoder.Vertex c) {
        return NormalHelper.computeTangent(
                hbm$tangent,
                hbm$normal.x,
                hbm$normal.y,
                hbm$normal.z,
                a.x,
                a.y,
                a.z,
                a.u,
                a.v,
                b.x,
                b.y,
                b.z,
                b.u,
                b.v,
                c.x,
                c.y,
                c.z,
                c.u,
                c.v);
    }

    @WrapOperation(
            method = "write",
            slice =
                    @Slice(
                            from =
                                    @At(
                                            value = "FIELD",
                                            target =
                                                    "Lnet/irisshaders/iris/vertices/sodium/terrain/XHFPTerrainVertex;normalOffset:I",
                                            ordinal = 1)),
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/system/MemoryUtil;memPutInt(JI)V"))
    private void hbm$writeNormal(long address, int value, Operation<Void> original) {
        original.call(address, hbm$hasNormal ? hbm$normalTangent : value);
    }
}
