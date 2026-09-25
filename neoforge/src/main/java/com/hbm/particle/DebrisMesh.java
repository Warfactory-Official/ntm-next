// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.particle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.engine_room.flywheel.api.vertex.MutableVertexList;
import java.util.Arrays;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public final class DebrisMesh {

    private static final ChunkSectionLayer[] LAYERS = ChunkSectionLayer.values();

    private final float[] pos;
    private final float[] uv;
    private final float[] normal;
    private final int[] color;
    private final int[] overlay;
    private final int[] light;
    private final int vertexCount;

    private DebrisMesh(Recorder recorder) {
        this.vertexCount = recorder.count;
        this.pos = Arrays.copyOf(recorder.pos, recorder.count * 3);
        this.uv = Arrays.copyOf(recorder.uv, recorder.count * 2);
        this.normal = Arrays.copyOf(recorder.normal, recorder.count * 3);
        this.color = Arrays.copyOf(recorder.color, recorder.count);
        this.overlay = Arrays.copyOf(recorder.overlay, recorder.count);
        this.light = Arrays.copyOf(recorder.light, recorder.count);
    }

    public static DebrisMesh[] bake(DebrisChunk chunk) {
        return bake(chunk, chunk.sizeX, chunk.sizeY, chunk.sizeZ);
    }

    public static DebrisMesh[] bake(BlockAndTintGetter chunk, int sizeX, int sizeY, int sizeZ) {
        Minecraft mc = Minecraft.getInstance();
        BlockStateModelSet models = mc.getModelManager().getBlockStateModelSet();

        Recorder[] recorders = new Recorder[LAYERS.length];
        for (int i = 0; i < recorders.length; i++) recorders[i] = new Recorder();

        boolean cutoutLeaves = mc.options.cutoutLeaves().get();
        ModelBlockRenderer renderer = new ModelBlockRenderer(true, true, mc.getBlockColors());

        BlockPos.MutableBlockPos cell = new BlockPos.MutableBlockPos();
        for (int y = 0; y < sizeY; y++) {
            for (int z = 0; z < sizeZ; z++) {
                for (int x = 0; x < sizeX; x++) {
                    cell.set(x, y, z);
                    BlockState state = chunk.getBlockState(cell);
                    if (state.isAir()) continue;

                    boolean opaque = ModelBlockRenderer.forceOpaque(cutoutLeaves, state);
                    BlockQuadOutput output =
                            (qx, qy, qz, quad, instance) -> {
                                ChunkSectionLayer layer =
                                        opaque
                                                ? ChunkSectionLayer.SOLID
                                                : quad.materialInfo().layer();
                                recorders[layer.ordinal()].putBlockBakedQuad(
                                        qx, qy, qz, quad, instance);
                            };

                    renderer.tesselateBlock(
                            output,
                            x,
                            y,
                            z,
                            chunk,
                            cell,
                            state,
                            models.get(state),
                            state.getSeed(cell));
                }
            }
        }

        DebrisMesh[] out = new DebrisMesh[LAYERS.length];
        for (int i = 0; i < out.length; i++) {
            if (recorders[i].count > 0) out[i] = new DebrisMesh(recorders[i]);
        }
        return out;
    }

    public static DebrisMesh[] bakeFluids(
            BlockAndTintGetter view, int sizeX, int sizeY, int sizeZ) {
        Minecraft mc = Minecraft.getInstance();
        FluidRenderer renderer = new FluidRenderer(mc.getModelManager().getFluidStateModelSet());
        Recorder[] recorders = new Recorder[LAYERS.length];
        for (int i = 0; i < recorders.length; i++) recorders[i] = new Recorder();
        FluidRenderer.Output output = layer -> recorders[layer.ordinal()];
        BlockPos.MutableBlockPos cell = new BlockPos.MutableBlockPos();
        for (int y = 0; y < sizeY; y++) {
            for (int z = 0; z < sizeZ; z++) {
                for (int x = 0; x < sizeX; x++) {
                    cell.set(x, y, z);
                    var state = view.getBlockState(cell);
                    var fluid = state.getFluidState();
                    if (fluid.isEmpty()) continue;
                    for (Recorder recorder : recorders) recorder.offset(x & ~15, y & ~15, z & ~15);
                    renderer.tesselate(view, cell, output, state, fluid);
                }
            }
        }
        DebrisMesh[] meshes = new DebrisMesh[LAYERS.length];
        for (int i = 0; i < meshes.length; i++)
            if (recorders[i].count > 0) meshes[i] = new DebrisMesh(recorders[i]);
        return meshes;
    }

    public int vertexCount() {
        return vertexCount;
    }

    public void write(MutableVertexList dst) {
        for (int v = 0; v < vertexCount; v++) {
            dst.x(v, pos[v * 3]);
            dst.y(v, pos[v * 3 + 1]);
            dst.z(v, pos[v * 3 + 2]);
            int argb = color[v];
            dst.a(v, (argb >>> 24) / 255F);
            dst.r(v, (argb >> 16 & 0xFF) / 255F);
            dst.g(v, (argb >> 8 & 0xFF) / 255F);
            dst.b(v, (argb & 0xFF) / 255F);
            dst.u(v, uv[v * 2]);
            dst.v(v, uv[v * 2 + 1]);
            dst.overlay(v, overlay[v]);
            dst.light(v, light[v]);
            dst.normalX(v, normal[v * 3]);
            dst.normalY(v, normal[v * 3 + 1]);
            dst.normalZ(v, normal[v * 3 + 2]);
        }
    }

    public void emit(PoseStack.Pose pose, VertexConsumer tess) {
        for (int v = 0; v < vertexCount; v++) {
            tess.addVertex(pose, pos[v * 3], pos[v * 3 + 1], pos[v * 3 + 2])
                    .setColor(color[v])
                    .setUv(uv[v * 2], uv[v * 2 + 1])
                    .setOverlay(overlay[v])
                    .setLight(light[v])
                    .setNormal(pose, normal[v * 3], normal[v * 3 + 1], normal[v * 3 + 2]);
        }
    }

    private static final class Recorder implements VertexConsumer {

        private float[] pos = new float[3 * 64];
        private float[] uv = new float[2 * 64];
        private float[] normal = new float[3 * 64];
        private int[] color = new int[64];
        private int[] overlay = new int[64];
        private int[] light = new int[64];
        private int count;
        private float offsetX, offsetY, offsetZ;

        private void offset(float x, float y, float z) {
            offsetX = x;
            offsetY = y;
            offsetZ = z;
        }

        private void grow() {
            if (count < color.length) return;
            int next = color.length * 2;
            pos = Arrays.copyOf(pos, next * 3);
            uv = Arrays.copyOf(uv, next * 2);
            normal = Arrays.copyOf(normal, next * 3);
            color = Arrays.copyOf(color, next);
            overlay = Arrays.copyOf(overlay, next);
            light = Arrays.copyOf(light, next);
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            grow();
            pos[count * 3] = x + offsetX;
            pos[count * 3 + 1] = y + offsetY;
            pos[count * 3 + 2] = z + offsetZ;
            count++;
            return this;
        }

        @Override
        public VertexConsumer setColor(int r, int g, int b, int a) {
            return setColor(a << 24 | r << 16 | g << 8 | b);
        }

        @Override
        public VertexConsumer setColor(int argb) {
            color[count - 1] = argb;
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            uv[(count - 1) * 2] = u;
            uv[(count - 1) * 2 + 1] = v;
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            overlay[count - 1] = u | v << 16;
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            light[count - 1] = u | v << 16;
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            normal[(count - 1) * 3] = x;
            normal[(count - 1) * 3 + 1] = y;
            normal[(count - 1) * 3 + 2] = z;
            return this;
        }

        @Override
        public VertexConsumer setLineWidth(float width) {
            return this;
        }
    }
}
