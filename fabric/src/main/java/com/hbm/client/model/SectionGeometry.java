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
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.renderer.v1.Renderer;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableMesh;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadView;
import net.fabricmc.fabric.api.client.renderer.v1.render.AltModelBlockRenderer;
import net.fabricmc.fabric.api.client.renderer.v1.sprite.SpriteFinder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockModelLighter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.LightCoordsUtil;
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
import org.jspecify.annotations.Nullable;

public final class SectionGeometry {
    static final ChunkSectionLayer[] LAYERS = ChunkSectionLayer.values();
    private static final ThreadLocal<Emission> EMISSION = ThreadLocal.withInitial(Emission::new);
    private static final ThreadLocal<Preparation> PREPARATION =
            ThreadLocal.withInitial(Preparation::new);

    private SectionGeometry() {}

    private static volatile @Nullable SpriteFinder blockSprites;

    public static void blockSprites(SpriteFinder finder) {
        blockSprites = finder;
    }

    public static SpriteFinder blockSprites() {
        var finder = blockSprites;
        assert finder != null;
        return finder;
    }

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
        var finder = blockSprites;
        var emitter =
                Renderer.get()
                        .quadEmitter(
                                quad -> {
                                    ownBlock[0] &=
                                            QuadLighting.isOwnBlock(quad.tag())
                                                    && quad.tintIndex() == -1
                                                    && quad.cullFace() == null;
                                    surface.add(
                                            QuadLighting.isOwnBlock(quad.tag())
                                                    ? importedEmission(quad)
                                                    : -1);
                                    appendNormals(normals, quad);
                                    for (int v = 0; v < 4; v++) {
                                        vertices.add(quad.x(v));
                                        vertices.add(quad.y(v));
                                        vertices.add(quad.z(v));
                                        vertices.add(quad.u(v));
                                        vertices.add(quad.v(v));
                                    }
                                    layers.add((byte) quad.chunkLayer().ordinal());
                                    if (sprites != null) sprites.add(finder.find(quad));
                                });
        for (var part : parts) part.emitQuads(emitter, face -> false);
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

    private static byte importedEmission(QuadView quad) {
        if (quad.emissive()) return 15;
        int emission = 15;
        for (int v = 0; v < 4; v++) {
            int lightmap = quad.lightmap(v);
            emission =
                    Math.min(
                            emission,
                            Math.min(
                                    LightCoordsUtil.block(lightmap),
                                    LightCoordsUtil.sky(lightmap)));
        }
        return (byte) Math.max(emission, QuadLighting.emission(quad.tag()));
    }

    private static void appendNormals(FloatArrayList normals, QuadView quad) {
        for (int v = 0; v < 4; v++) {
            if (quad.hasNormal(v)) {
                normals.add(quad.normalX(v));
                normals.add(quad.normalY(v));
                normals.add(quad.normalZ(v));
            } else {
                var normal = quad.faceNormal();
                normals.add(normal.x());
                normals.add(normal.y());
                normals.add(normal.z());
            }
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
            scratch.output = null;
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
        private @Nullable Renderer owner;
        private AltModelBlockRenderer renderer;
        private QuadEmitter output;

        private void renderer() {
            var minecraft = Minecraft.getInstance();
            var current = Renderer.get();
            boolean ao = minecraft.options.ambientOcclusion().get();
            if (owner == current
                    && blockColors == minecraft.getBlockColors()
                    && ambientOcclusion == ao) return;
            owner = current;
            blockColors = minecraft.getBlockColors();
            ambientOcclusion = ao;
            renderer = current.altModelBlockRenderer(ao, true, minecraft.getBlockColors());
            output = current.quadEmitter(this::capture);
        }

        private void capture(QuadView quad) {
            boolean sampled = QuadLighting.isOwnBlock(quad.tag());
            preparedLight |= !sampled;
            surface.add(
                    sampled
                            ? (byte) (quad.emissive() ? 15 : QuadLighting.emission(quad.tag()))
                            : -1);
            appendNormals(normals, quad);
            for (int v = 0; v < 4; v++) {
                vertices.add(quad.x(v));
                vertices.add(quad.y(v));
                vertices.add(quad.z(v));
                vertices.add(quad.u(v));
                vertices.add(quad.v(v));
                colors.add(quad.color(v));
                light.add(sampled ? 0 : quad.lightmap(v));
            }
            layers.add((byte) quad.chunkLayer().ordinal());
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
            var all = Renderer.get().mutableMesh();
            var emitter =
                    Renderer.get()
                            .quadEmitter(
                                    quad -> {
                                        var emitted = all.emitter().copyFrom(quad);
                                        if (QuadLighting.isOwnBlock(quad.tag()))
                                            emitted.tag(
                                                    QuadLighting.ownBlockTag(
                                                            importedEmission(quad)));
                                        emitted.emit();
                                    });
            complete.emitQuads(emitter, view, pos, state, RandomSource.create(seed), face -> false);
            boolean[] offset = {false};
            all.forEach(quad -> offset[0] |= QuadLighting.isOffset(quad.tag()));
            if (!offset[0])
                return new Cells(
                        new int[] {0},
                        new BlockStateModel[] {new CellModel(all.immutableCopy(), complete)});
            var meshes = new Int2ObjectLinkedOpenHashMap<MutableMesh>();
            all.forEach(
                    quad -> {
                        int cell = QuadLighting.isOffset(quad.tag()) ? quad.tag() : 0;
                        var emitted =
                                meshes.computeIfAbsent(
                                                cell, ignored -> Renderer.get().mutableMesh())
                                        .emitter()
                                        .copyFrom(quad);
                        for (int v = 0; v < 4; v++) {
                            emitted.pos(
                                    v,
                                    emitted.x(v) - QuadLighting.dx(cell),
                                    emitted.y(v) - QuadLighting.dy(cell),
                                    emitted.z(v) - QuadLighting.dz(cell));
                        }
                        emitted.emit();
                    });
            int[] offsets = meshes.keySet().toIntArray();
            BlockStateModel[] models = new BlockStateModel[offsets.length];
            for (int i = 0; i < offsets.length; i++)
                models[i] = new CellModel(meshes.get(offsets[i]).immutableCopy(), complete);
            return new Cells(offsets, models);
        }
    }

    private record CellModel(
            net.fabricmc.fabric.api.client.renderer.v1.mesh.Mesh quads, BlockStateModel complete)
            implements BlockStateModel {
        @Override
        public void collectParts(RandomSource random, List<BlockStateModelPart> output) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void emitQuads(
                QuadEmitter emitter,
                BlockAndTintGetter level,
                BlockPos pos,
                BlockState state,
                RandomSource random,
                Predicate<@Nullable Direction> cullTest) {
            quads.forEach(
                    quad -> {
                        var face = quad.cullFace();
                        if (face == null || !cullTest.test(face)) emitter.copyFrom(quad).emit();
                    });
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
                Renderer.get()
                        .altModelBlockRenderer(
                                false, false, Minecraft.getInstance().getBlockColors());
        var emitter =
                Renderer.get()
                        .quadEmitter(
                                quad -> {
                                    for (int v = 0; v < 4; v++) colors[next[0]++] = quad.color(v);
                                });
        renderer.tesselateBlock(emitter, 0, 0, 0, view, BlockPos.ZERO, state, model.complete, 0);
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
