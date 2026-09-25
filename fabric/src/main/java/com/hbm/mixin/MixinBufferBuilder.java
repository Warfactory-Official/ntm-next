// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.client.render.TracerRibbon;
import com.hbm.interfaces.injected.IBufferBuilderExtension;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BufferBuilder.class)
@SuppressWarnings("UnreachableCode")
public abstract class MixinBufferBuilder implements IBufferBuilderExtension {

    @Unique private static final int hbm$COLOR_ONLY = 1 << 1;

    @Shadow @Final private @Nullable VertexFormatElement[] elements;

    @Shadow @Final private int initialElementsToFill;

    @Shadow
    private long beginVertex() {
        throw new AssertionError();
    }

    @Shadow
    private static void putVec3f(long pointer, float x, float y, float z) {
        throw new AssertionError();
    }

    @Shadow
    private static void putRgba(long pointer, int argb) {
        throw new AssertionError();
    }

    @Shadow @Final private ByteBufferBuilder buffer;

    @Shadow @Final private boolean blockFormat;

    @Shadow @Final private boolean entityFormat;

    @Shadow @Final private int vertexSize;

    @Shadow @Final private VertexFormat format;

    @Shadow @Final private PrimitiveTopology primitiveTopology;

    @Shadow private int vertices;

    @Shadow private long vertexPointer;

    @Shadow
    private void ensureBuilding() {
        throw new AssertionError();
    }

    @Shadow
    private void endLastVertex() {
        throw new AssertionError();
    }

    @Shadow
    private static void putPackedUv(long pointer, int packedUv) {
        throw new AssertionError();
    }

    @Shadow
    private static void putNormals(long pointer, float nx, float ny, float nz) {
        throw new AssertionError();
    }

    @Unique private long hbm$blockPtr = -1L;

    @Unique private int hbm$blockLeft;

    @Unique private boolean hbm$blockEntity;

    @Unique private boolean hbm$blockColorOnly;

    @Override
    public void hbm$ribbon(
            float hx,
            float hy,
            float hz,
            float tx,
            float ty,
            float tz,
            int headColor,
            int tailColor,
            float headWidth,
            float tailWidth,
            int light) {
        assert this.format == TracerRibbon.FORMAT
                && this.primitiveTopology == PrimitiveTopology.QUADS;
        this.ensureBuilding();
        this.endLastVertex();
        if (this.vertices > 16777215 - 4) {
            throw new IllegalStateException(
                    "Trying to write too many vertices (>16777215) into BufferBuilder");
        }
        long base = this.buffer.reserve(4 * TracerRibbon.STRIDE);
        for (int i = 0; i < 4; i++) {
            boolean tail = i == 1 || i == 2;
            long ptr = base + (long) i * TracerRibbon.STRIDE;
            putVec3f(ptr, tail ? tx : hx, tail ? ty : hy, tail ? tz : hz);
            putVec3f(ptr + 12L, tail ? hx : tx, tail ? hy : ty, tail ? hz : tz);
            putRgba(ptr + 24L, tail ? tailColor : headColor);
            putRgba(ptr + 28L, tail ? headColor : tailColor);
            MemoryUtil.memPutFloat(ptr + 32L, tail ? tailWidth : headWidth);
            MemoryUtil.memPutFloat(ptr + 36L, tail ? headWidth : tailWidth);
            MemoryUtil.memPutFloat(ptr + 40L, tail ? 1F : 0F);
            MemoryUtil.memPutFloat(ptr + 44L, i < 2 ? -1F : 1F);
            putPackedUv(ptr + 48L, light);
        }
        this.vertices += 4;
        this.vertexPointer = base + 3L * TracerRibbon.STRIDE;
    }

    @Override
    public boolean hbm$beginBlock(int count) {
        boolean entity = this.entityFormat;
        if (!entity && !this.blockFormat) return false;
        if (count <= 0) return false;
        int size = this.vertexSize;
        if ((long) count * size > Integer.MAX_VALUE) return false;

        this.ensureBuilding();

        this.endLastVertex();
        if (this.vertices + count > 16777215) {
            throw new IllegalStateException(
                    "Trying to write too many vertices (>16777215) into BufferBuilder");
        }

        this.hbm$blockPtr = this.buffer.reserve(count * size);
        this.hbm$blockLeft = count;
        this.hbm$blockEntity = entity;
        this.hbm$blockColorOnly = false;
        this.vertices += count;
        return true;
    }

    @Override
    public void hbm$blockVertex(
            float x,
            float y,
            float z,
            int color,
            float u,
            float v,
            int overlay,
            int light,
            float nx,
            float ny,
            float nz) {
        assert !this.hbm$blockColorOnly : "blockVertex into a Position+Color block";
        int left = this.hbm$blockLeft;
        if (left <= 0) throw new IllegalStateException("Block overrun: no vertices left to write");
        long ptr = this.hbm$blockPtr;

        putVec3f(ptr, x, y, z);
        putRgba(ptr + 12L, color);
        MemoryUtil.memPutFloat(ptr + 16L, u);
        MemoryUtil.memPutFloat(ptr + 20L, v);
        if (this.hbm$blockEntity) {
            putPackedUv(ptr + 24L, overlay);
            putPackedUv(ptr + 28L, light);
            putNormals(ptr + 32L, nx, ny, nz);
        } else {
            putPackedUv(ptr + 24L, light);
        }

        this.hbm$blockPtr = ptr + this.vertexSize;
        this.hbm$blockLeft = left - 1;
    }

    @Override
    public boolean hbm$beginColorBlock(int count) {
        if (this.initialElementsToFill != hbm$COLOR_ONLY) return false;
        if (count <= 0) return false;
        int size = this.vertexSize;
        if ((long) count * size > Integer.MAX_VALUE) return false;

        this.ensureBuilding();
        this.endLastVertex();
        if (this.vertices + count > 16777215) {
            throw new IllegalStateException(
                    "Trying to write too many vertices (>16777215) into BufferBuilder");
        }

        this.hbm$blockPtr = this.buffer.reserve(count * size);
        this.hbm$blockLeft = count;
        this.hbm$blockColorOnly = true;
        this.vertices += count;
        return true;
    }

    @Override
    public void hbm$colorVertex(float x, float y, float z, int color) {
        assert this.hbm$blockColorOnly : "colorVertex into a textured block";
        int left = this.hbm$blockLeft;
        if (left <= 0) throw new IllegalStateException("Block overrun: no vertices left to write");
        long ptr = this.hbm$blockPtr;
        putVec3f(ptr + this.elements[0].offset(), x, y, z);
        putRgba(ptr + this.elements[1].offset(), color);
        this.hbm$blockPtr = ptr + this.vertexSize;
        this.hbm$blockLeft = left - 1;
    }

    @Override
    public void hbm$endBlock() {

        assert this.hbm$blockLeft == 0
                : "Block underrun: " + this.hbm$blockLeft + " vertices reserved but unwritten";
        this.vertexPointer = this.hbm$blockPtr - this.vertexSize;
        this.hbm$blockPtr = -1L;
        this.hbm$blockLeft = 0;
        this.hbm$blockColorOnly = false;
    }

    @Unique private final Vector3f hbm$position = new Vector3f();

    @Unique private final Vector3f hbm$normal = new Vector3f();

    @Override
    public boolean hbm$putPart(
            float[] src,
            int stride,
            int count,
            PoseStack.Pose pose,
            int color,
            int overlay,
            int light,
            float uScale,
            float vScale,
            float uOff,
            float vOff) {
        boolean entity = this.entityFormat;
        if (!entity && !this.blockFormat) return false;
        if (count <= 0) return true;

        int size = this.vertexSize;
        if ((long) count * size > Integer.MAX_VALUE) return false;

        this.ensureBuilding();

        this.endLastVertex();
        if (this.vertices + count > 16777215) {
            throw new IllegalStateException(
                    "Trying to write too many vertices (>16777215) into BufferBuilder");
        }

        long base = this.buffer.reserve(count * size);
        Matrix4fc matrix = pose.pose();
        Vector3f p = this.hbm$position;
        Vector3f n = this.hbm$normal;

        for (int i = 0, at = 0; i < count; i++, at += stride) {
            long ptr = base + (long) i * size;
            matrix.transformPosition(src[at], src[at + 1], src[at + 2], p);
            putVec3f(ptr, p.x, p.y, p.z);
            putRgba(ptr + 12L, color);
            MemoryUtil.memPutFloat(ptr + 16L, src[at + 3] * uScale + uOff);
            MemoryUtil.memPutFloat(ptr + 20L, src[at + 4] * vScale + vOff);
            if (entity) {
                putPackedUv(ptr + 24L, overlay);
                putPackedUv(ptr + 28L, light);
                pose.transformNormal(src[at + 5], src[at + 6], src[at + 7], n);
                putNormals(ptr + 32L, n.x, n.y, n.z);
            } else {
                putPackedUv(ptr + 24L, light);
            }
        }

        this.vertices += count;
        this.vertexPointer = base + (long) (count - 1) * size;
        return true;
    }

    @Override
    public boolean hbm$positionColor(float x, float y, float z, int color) {
        if (this.initialElementsToFill != hbm$COLOR_ONLY) return false;
        long pointer = this.beginVertex();
        putVec3f(pointer + this.elements[0].offset(), x, y, z);
        putRgba(pointer + this.elements[1].offset(), color);
        return true;
    }
}
