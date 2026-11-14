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

package com.hbm.client.model;

import com.hbm.interfaces.injected.SectionGeometryVertexConsumer;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockModelLighter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.config.NeoForgeClientConfig;
import net.neoforged.neoforge.client.model.quad.BakedNormals;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public final class SectionGeometry {
    static final ChunkSectionLayer[] LAYERS = ChunkSectionLayer.values();
    private static final ThreadLocal<Emission> EMISSION = ThreadLocal.withInitial(Emission::new);
    private static final ThreadLocal<Preparation> PREPARATION =
            ThreadLocal.withInitial(Preparation::new);

    private SectionGeometry() {}

    public interface TerrainView {}

    public interface Region {
        @Nullable Snapshot hbm$sectionGeometry();
    }

    public static void reload() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null)
            ((com.hbm.interfaces.injected.ClientLevelExtension) (level))
                    .hbm$sectionGeometryIndex()
                    .reload(level);
    }

    public static BlockAndTintGetter loadedView(ClientLevel level) {
        return new LoadedView(level);
    }

    public static boolean has(Level level, long section) {
        if (!(level instanceof ClientLevel client)) return false;
        ((com.hbm.interfaces.injected.ClientLevelExtension) (client))
                .hbm$sectionGeometryIndex()
                .refresh(client);
        return ((com.hbm.interfaces.injected.ClientLevelExtension) (client))
                .hbm$sectionGeometryIndex()
                .has(section);
    }

    public static @Nullable Snapshot capture(Level level, BlockPos sectionOrigin) {
        assert Minecraft.getInstance().isSameThread();
        return level instanceof ClientLevel client
                ? ((com.hbm.interfaces.injected.ClientLevelExtension) (client))
                        .hbm$sectionGeometryIndex()
                        .capture(client, SectionPos.asLong(sectionOrigin))
                : null;
    }

    static Mesh mesh(BlockStateModel model, BlockState state) {
        return mesh(model, state, null);
    }

    static Mesh mesh(
            BlockStateModel model, BlockState state, @Nullable List<TextureAtlasSprite> sprites) {
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(RandomSource.create(0), parts);
        return mesh(parts, state, sprites);
    }

    static Mesh mesh(
            List<BlockStateModelPart> parts,
            BlockState state,
            @Nullable List<TextureAtlasSprite> sprites) {
        var vertices = new FloatArrayList();
        var normals = new FloatArrayList();
        var layers = new ByteArrayList();
        var surface = new ByteArrayList();
        boolean[] ownBlock = {true};
        for (var part : parts) {
            for (int face = -1; face < Direction.VALUES.length; face++) {
                for (var quad : part.getQuads(face == -1 ? null : Direction.VALUES[face])) {
                    ownBlock[0] &=
                            QuadLighting.usesOwnBlock(quad.materialInfo())
                                    && quad.materialInfo().tintIndex() == -1
                                    && face == -1;
                    surface.add(surface(quad));
                    appendNormals(normals, quad);
                    for (int v = 0; v < 4; v++) {
                        var pos = quad.position(v);
                        long uv = quad.packedUV(v);
                        vertices.add(pos.x());
                        vertices.add(pos.y());
                        vertices.add(pos.z());
                        vertices.add(UVPair.unpackU(uv));
                        vertices.add(UVPair.unpackV(uv));
                    }
                    layers.add((byte) quad.materialInfo().layer().ordinal());
                    if (sprites != null) sprites.add(quad.materialInfo().sprite());
                }
            }
        }
        float[] data = vertices.toFloatArray();
        boolean contained = true;
        for (int i = 0; i < data.length; i += 5) {
            for (int axis = 0; axis < 3; axis++) {
                float value = data[i + axis];
                if (!Float.isFinite(value))
                    throw new IllegalStateException(state + " has a non-finite baked vertex");
                contained &= value >= 0 && value <= 1;
            }
        }
        return new Mesh(
                data,
                normals.toFloatArray(),
                layers.toByteArray(),
                surface.toByteArray(),
                contained,
                ownBlock[0]);
    }

    public record Mesh(
            float[] vertices,
            float[] normals,
            byte[] layers,
            byte[] surface,
            boolean contained,
            boolean ownBlock) {}

    private static byte surface(BakedQuad quad) {
        return QuadLighting.usesOwnBlock(quad.materialInfo())
                ? (byte) quad.materialInfo().lightEmission()
                : -1;
    }

    private static void appendNormals(FloatArrayList normals, BakedQuad quad) {
        int faceNormal = 0;
        for (int v = 0; v < 4; v++) {
            int normal = quad.bakedNormals().normal(v);
            if (BakedNormals.isUnspecified(normal)) {
                if (faceNormal == 0)
                    faceNormal =
                            BakedNormals.computeQuadNormal(
                                    quad.position0(),
                                    quad.position1(),
                                    quad.position2(),
                                    quad.position3());
                normal = faceNormal;
            }
            normals.add(BakedNormals.unpackX(normal));
            normals.add(BakedNormals.unpackY(normal));
            normals.add(BakedNormals.unpackZ(normal));
        }
    }

    public record Snapshot(BlockPos origin, List<Part> parts) {
        public Snapshot {
            parts = List.copyOf(parts);
        }

        public void emit(
                BlockAndTintGetter region, Function<ChunkSectionLayer, VertexConsumer> output) {
            var surface = SurfaceLight.open(region, origin);
            var emission = EMISSION.get();
            emission.output = output;
            try {
                for (Part part : parts) emission.emit(part, surface);
            } finally {
                Arrays.fill(emission.consumers, null);
                emission.output = null;
                emission.state = null;
                surface.close();
            }
        }
    }

    private static final class Emission implements Function<ChunkSectionLayer, VertexConsumer> {
        private final VertexConsumer[] consumers = new VertexConsumer[LAYERS.length];
        private @Nullable Function<ChunkSectionLayer, VertexConsumer> output;
        private @Nullable BlockState state;
        private @Nullable ChunkSectionLayer override;
        private int x, y, z;

        void emit(Part part, SurfaceLight surface) {
            state = part.state;
            x = part.x;
            y = part.y;
            z = part.z;
            override = SectionGeometryShaders.layer(state);
            try {
                part.layout.emit(
                        part.section,
                        part.prepared,
                        part.state.emissiveRendering() ? 15 : 0,
                        surface,
                        x,
                        y,
                        z,
                        this);
            } finally {
                for (int i = 0; i < consumers.length; i++) {
                    if (consumers[i] instanceof SectionGeometryVertexConsumer attributes)
                        attributes.hbm$endSectionGeometry();
                    consumers[i] = null;
                }
            }
        }

        @Override
        public VertexConsumer apply(ChunkSectionLayer layer) {
            var actual = override == null ? layer : override;
            int index = actual.ordinal();
            var consumer = consumers[index];
            if (consumer == null) {
                consumers[index] = consumer = output.apply(actual);
                if (consumer instanceof SectionGeometryVertexConsumer attributes) {
                    attributes.hbm$beginSectionGeometry(state, x, y, z);
                }
            }
            return consumer;
        }
    }

    public record Part(
            SectionedModel model,
            BlockState state,
            long core,
            SectionGeometryLayout layout,
            int section,
            int x,
            int y,
            int z,
            Prepared prepared) {}

    public record Prepared(Mesh mesh, Lit lit) {}

    static Prepared prepare(
            SectionedModel model,
            ClientLevel level,
            BlockPos pos,
            BlockState state,
            @Nullable Prepared previous) {
        return prepare(model, new LoadedView(level), pos, state, previous);
    }

    public static Prepared prepare(
            SectionedModel model, BlockAndTintGetter view, BlockPos pos, BlockState state) {
        return prepare(model, view, pos, state, null);
    }

    private static Prepared prepare(
            SectionedModel model,
            BlockAndTintGetter view,
            BlockPos pos,
            BlockState state,
            @Nullable Prepared previous) {
        var scratch = PREPARATION.get();
        while (scratch.busy) {
            if (scratch.next == null) scratch.next = new Preparation();
            scratch = scratch.next;
        }
        scratch.busy = true;
        try {
            long seed = state.getSeed(pos);
            BlockModelLighter.clearCache();
            scratch.random.setSeed(seed);
            Object key = model.complete.createGeometryKey(view, pos, state, scratch.random);
            var cells = model.cells(key);
            if (cells == null) {
                cells = Cells.of(model.complete, view, pos, state, seed);
                if (key != null) model.cells(key, cells);
            }
            scratch.renderer();
            for (int i = 0; i < cells.offsets().length; i++) {
                int cell = cells.offsets()[i];
                scratch.renderer.tesselateBlock(
                        scratch.output,
                        QuadLighting.dx(cell),
                        QuadLighting.dy(cell),
                        QuadLighting.dz(cell),
                        view,
                        pos.offset(
                                QuadLighting.dx(cell),
                                QuadLighting.dy(cell),
                                QuadLighting.dz(cell)),
                        state,
                        cells.models()[i],
                        seed);
            }
            return model.prepared(scratch, previous);
        } catch (RuntimeException | Error failure) {
            scratch.blockColors = null;
            scratch.renderer = null;
            throw failure;
        } finally {
            scratch.clear();
        }
    }

    static final class Preparation {
        final FloatArrayList vertices = new FloatArrayList();
        final FloatArrayList normals = new FloatArrayList();
        final ByteArrayList layers = new ByteArrayList();
        final ByteArrayList surface = new ByteArrayList();
        final IntArrayList colors = new IntArrayList();
        final IntArrayList light = new IntArrayList();
        final RandomSource random = RandomSource.create();
        boolean preparedLight;
        private boolean busy;
        private @Nullable Preparation next;
        private @Nullable Object blockColors;
        private boolean ambientOcclusion;

        private ModelBlockRenderer renderer;
        private final BlockQuadOutput output = this::capture;
        private boolean enhancedLighting;

        private void renderer() {
            var minecraft = Minecraft.getInstance();
            boolean ao = minecraft.options.ambientOcclusion().get();
            boolean enhanced = NeoForgeClientConfig.INSTANCE.enhancedLighting.getAsBoolean();
            if (blockColors == minecraft.getBlockColors()
                    && ambientOcclusion == ao
                    && enhancedLighting == enhanced) return;
            blockColors = minecraft.getBlockColors();
            ambientOcclusion = ao;
            enhancedLighting = enhanced;
            renderer = new ModelBlockRenderer(ao, true, minecraft.getBlockColors());
        }

        private void capture(float x, float y, float z, BakedQuad quad, QuadInstance instance) {
            byte sampled = surface(quad);
            preparedLight |= sampled < 0;
            surface.add(sampled);
            appendNormals(normals, quad);
            for (int v = 0; v < 4; v++) {
                var point = quad.position(v);
                long uv = quad.packedUV(v);
                vertices.add(point.x() + x);
                vertices.add(point.y() + y);
                vertices.add(point.z() + z);
                vertices.add(UVPair.unpackU(uv));
                vertices.add(UVPair.unpackV(uv));
                colors.add(instance.getColor(v));
                light.add(
                        sampled >= 0
                                ? 0
                                : instance.getLightCoordsWithEmission(
                                        v, quad.materialInfo().lightEmission()));
            }
            layers.add((byte) quad.materialInfo().layer().ordinal());
        }

        boolean matches(Mesh mesh) {
            return equal(vertices, mesh.vertices())
                    && equal(normals, mesh.normals())
                    && equal(layers, mesh.layers())
                    && equal(surface, mesh.surface());
        }

        boolean matches(Prepared prepared) {
            return equal(colors, prepared.lit().colors())
                    && (prepared.lit().light() == null
                            ? !preparedLight
                            : equal(light, prepared.lit().light()))
                    && matches(prepared.mesh());
        }

        private static boolean equal(FloatArrayList values, float[] array) {
            return values.size() == array.length
                    && Arrays.equals(values.elements(), 0, values.size(), array, 0, array.length);
        }

        private static boolean equal(ByteArrayList values, byte[] array) {
            return values.size() == array.length
                    && Arrays.equals(values.elements(), 0, values.size(), array, 0, array.length);
        }

        private static boolean equal(IntArrayList values, int @Nullable [] array) {
            return array != null
                    && values.size() == array.length
                    && Arrays.equals(values.elements(), 0, values.size(), array, 0, array.length);
        }

        private void clear() {
            vertices.clear();
            normals.clear();
            layers.clear();
            surface.clear();
            colors.clear();
            light.clear();
            preparedLight = false;
            busy = false;
        }
    }

    record Cells(int[] offsets, BlockStateModel[] models) {
        static Cells of(
                BlockStateModel complete,
                BlockAndTintGetter view,
                BlockPos pos,
                BlockState state,
                long seed) {
            var parts = new ArrayList<BlockStateModelPart>(2);
            complete.collectParts(view, pos, state, RandomSource.create(seed), parts);
            boolean offset = false;
            for (var part : parts) {
                if (hasOffset(part)) {
                    offset = true;
                    break;
                }
            }
            if (!offset)
                return new Cells(
                        new int[] {0}, new BlockStateModel[] {new CellModel(parts, complete)});
            var offsets = new IntArrayList();
            var models = new ArrayList<BlockStateModel>();
            for (var part : parts) {
                Cells cells = of(part, complete);
                offsets.addElements(offsets.size(), cells.offsets());
                Collections.addAll(models, cells.models());
            }
            return new Cells(offsets.toIntArray(), models.toArray(BlockStateModel[]::new));
        }

        private static boolean hasOffset(BlockStateModelPart part) {
            for (int face = -1; face < Direction.VALUES.length; face++) {
                Direction cull = face == -1 ? null : Direction.VALUES[face];
                for (var quad : part.getQuads(cull)) {
                    if (QuadLighting.isOffset(
                            ((com.hbm.interfaces.injected.QuadLightOriginAccess)
                                            (java.lang.Object) (quad.materialInfo()))
                                    .hbm$lightOrigin())) return true;
                }
            }
            return false;
        }

        private static Cells of(BlockStateModelPart part, BlockStateModel complete) {
            if (!hasOffset(part))
                return new Cells(
                        new int[] {0},
                        new BlockStateModel[] {new CellModel(List.of(part), complete)});
            var builders = new Int2ObjectLinkedOpenHashMap<QuadCollection.Builder>();
            for (int face = -1; face < Direction.VALUES.length; face++) {
                Direction cull = face == -1 ? null : Direction.VALUES[face];
                for (var quad : part.getQuads(cull)) {
                    int origin =
                            ((com.hbm.interfaces.injected.QuadLightOriginAccess)
                                            (java.lang.Object) (quad.materialInfo()))
                                    .hbm$lightOrigin();
                    int cell = QuadLighting.isOffset(origin) ? origin : 0;
                    var builder =
                            builders.computeIfAbsent(cell, ignored -> new QuadCollection.Builder());
                    var moved = cell == 0 ? quad : translated(quad, cell);
                    if (cull == null) builder.addUnculledFace(moved);
                    else builder.addCulledFace(cull, moved);
                }
            }
            int[] offsets = builders.keySet().toIntArray();
            BlockStateModel[] models = new BlockStateModel[offsets.length];
            for (int i = 0; i < offsets.length; i++) {
                var wrapped =
                        new SimpleModelWrapper(
                                builders.get(offsets[i]).build(),
                                part.useAmbientOcclusion(),
                                part.particleMaterial());
                models[i] = new CellModel(List.of(wrapped), complete);
            }
            return new Cells(offsets, models);
        }
    }

    private record CellModel(List<BlockStateModelPart> parts, BlockStateModel complete)
            implements BlockStateModel {
        @Override
        public void collectParts(RandomSource random, List<BlockStateModelPart> output) {
            output.addAll(parts);
        }

        @Override
        public Material.Baked particleMaterial() {
            return complete.particleMaterial();
        }

        @Override
        public int materialFlags() {
            return complete.materialFlags();
        }
    }

    private static BakedQuad translated(BakedQuad quad, int cell) {
        var offset =
                new Vector3f(QuadLighting.dx(cell), QuadLighting.dy(cell), QuadLighting.dz(cell));
        return new BakedQuad(
                new Vector3f(quad.position0()).sub(offset),
                new Vector3f(quad.position1()).sub(offset),
                new Vector3f(quad.position2()).sub(offset),
                new Vector3f(quad.position3()).sub(offset),
                quad.packedUV0(),
                quad.packedUV1(),
                quad.packedUV2(),
                quad.packedUV3(),
                quad.direction(),
                quad.materialInfo(),
                quad.bakedNormals(),
                quad.bakedColors());
    }

    private record LoadedView(ClientLevel level) implements BlockAndTintGetter {
        @Override
        public CardinalLighting cardinalLighting() {
            return ((BlockAndTintGetter) level).cardinalLighting();
        }

        @Override
        public int getBrightness(LightLayer layer, BlockPos pos) {
            return level.getBrightness(layer, pos);
        }

        @Override
        public int getRawBrightness(BlockPos pos, int darkening) {
            return level.getRawBrightness(pos, darkening);
        }

        @Override
        public LevelLightEngine getLightEngine() {
            return level.getLightEngine();
        }

        @Override
        public int getBlockTint(BlockPos pos, ColorResolver resolver) {
            return level.getBlockTint(pos, resolver);
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            var chunk = level.getChunkSource().getChunk(pos.getX() >> 4, pos.getZ() >> 4, false);
            return chunk == null ? Blocks.AIR.defaultBlockState() : chunk.getBlockState(pos);
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return getBlockState(pos).getFluidState();
        }

        @Override
        public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
            var chunk = level.getChunkSource().getChunk(pos.getX() >> 4, pos.getZ() >> 4, false);
            return chunk == null ? null : chunk.getBlockEntity(pos);
        }

        @Override
        public int getHeight() {
            return level.getHeight();
        }

        @Override
        public int getMinY() {
            return level.getMinY();
        }
    }

    public record Lit(int[] colors, int @Nullable [] light) {}

    static Lit colors(SectionedModel model, BlockState state, CardinalLighting lighting) {
        int[] colors = new int[model.mesh.layers.length * 4];
        int[] next = {0};
        var view = new ColorView(state, lighting);
        BlockModelLighter.clearCache();
        var renderer =
                new ModelBlockRenderer(false, false, Minecraft.getInstance().getBlockColors());
        renderer.tesselateBlock(
                (x, y, z, quad, instance) -> {
                    for (int v = 0; v < 4; v++) colors[next[0]++] = instance.getColor(v);
                },
                0,
                0,
                0,
                view,
                BlockPos.ZERO,
                state,
                model.complete,
                0);
        if (next[0] != colors.length)
            throw new IllegalStateException(
                    state + " changed its static quad count while lighting");
        return new Lit(colors, null);
    }

    private record ColorView(BlockState state, CardinalLighting cardinalLighting)
            implements BlockAndTintGetter {
        @Override
        public int getBrightness(LightLayer layer, BlockPos pos) {
            return 0;
        }

        @Override
        public int getRawBrightness(BlockPos pos, int darkening) {
            return 0;
        }

        @Override
        public LevelLightEngine getLightEngine() {
            return LevelLightEngine.EMPTY;
        }

        @Override
        public int getBlockTint(BlockPos pos, ColorResolver resolver) {
            throw new IllegalStateException("Sectioned models must declare their vertex colors");
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            return state;
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return state.getFluidState();
        }

        @Override
        public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
            return null;
        }

        @Override
        public int getHeight() {
            return 16;
        }

        @Override
        public int getMinY() {
            return 0;
        }
    }
}
