// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.hbm.interfaces.injected.ISectionGeometryVertexConsumer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.renderer.v1.Renderer;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableMesh;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadView;
import net.fabricmc.fabric.api.client.renderer.v1.sprite.FabricTextureAtlas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.UVPair;
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
import net.minecraft.data.AtlasIds;
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

    private SectionGeometry() {}

    public interface TerrainView {}

    public interface Region {
        @Nullable Snapshot hbm$sectionGeometry();
    }

    public static void reload() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) level.hbm$sectionGeometryIndex().reload(level);
    }

    public static BlockAndTintGetter loadedView(ClientLevel level) {
        return new LoadedView(level);
    }

    public static boolean has(Level level, long section) {
        if (!(level instanceof ClientLevel client)) return false;
        client.hbm$sectionGeometryIndex().refresh(client);
        return client.hbm$sectionGeometryIndex().has(section);
    }

    public static @Nullable Snapshot capture(Level level, BlockPos sectionOrigin) {
        assert Minecraft.getInstance().isSameThread();
        return level instanceof ClientLevel client
                ? client.hbm$sectionGeometryIndex()
                        .capture(client, SectionPos.asLong(sectionOrigin))
                : null;
    }

    static Mesh mesh(BlockStateModel model, BlockState state) {
        return mesh(model, state, null);
    }

    static Mesh mesh(
            BlockStateModel model, BlockState state, @Nullable List<TextureAtlasSprite> sprites) {
        var vertices = new FloatArrayList();
        var normals = new FloatArrayList();
        var layers = new ByteArrayList();
        var surface = new ByteArrayList();
        boolean[] ownBlock = {true};
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(RandomSource.create(0), parts);

        var finder =
                sprites == null
                        ? null
                        : ((FabricTextureAtlas)
                                        Minecraft.getInstance()
                                                .getAtlasManager()
                                                .getAtlasOrThrow(AtlasIds.BLOCKS))
                                .spriteFinder();
        var emitter =
                Renderer.get()
                        .quadEmitter(
                                quad -> {
                                    ownBlock[0] &=
                                            quad.tag() == QuadLighting.OWN_BLOCK
                                                    && quad.tintIndex() == -1
                                                    && quad.cullFace() == null;
                                    surface.add(
                                            quad.tag() == QuadLighting.OWN_BLOCK
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
        return (byte) emission;
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
            VertexConsumer[] consumers = new VertexConsumer[LAYERS.length];
            SurfaceLight surface = SurfaceLight.open(region, origin);
            try {
                for (Part part : parts) emit(part, surface, consumers, output);
            } finally {
                surface.close();
            }
        }

        private static void emit(
                Part part,
                SurfaceLight surface,
                VertexConsumer[] consumers,
                Function<ChunkSectionLayer, VertexConsumer> output) {
            ChunkSectionLayer override = SectionGeometryShaders.layer(part.state);
            try {
                part.layout.emit(
                        part.section,
                        part.prepared,
                        part.state.emissiveRendering() ? 15 : 0,
                        surface,
                        part.x,
                        part.y,
                        part.z,
                        layer -> {
                            ChunkSectionLayer actual = override == null ? layer : override;
                            int index = actual.ordinal();
                            VertexConsumer consumer = consumers[index];
                            if (consumer == null) {
                                consumers[index] = consumer = output.apply(actual);
                                if (consumer instanceof ISectionGeometryVertexConsumer attributes) {
                                    attributes.hbm$beginSectionGeometry(
                                            part.state, part.x, part.y, part.z);
                                }
                            }
                            return consumer;
                        });
            } finally {
                for (int i = 0; i < consumers.length; i++) {
                    if (consumers[i] instanceof ISectionGeometryVertexConsumer attributes)
                        attributes.hbm$endSectionGeometry();
                    consumers[i] = null;
                }
            }
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
            SectionedModel model, ClientLevel level, BlockPos pos, BlockState state) {
        return prepare(model, new LoadedView(level), pos, state);
    }

    public static Prepared prepare(
            SectionedModel model, BlockAndTintGetter view, BlockPos pos, BlockState state) {
        var vertices = new FloatArrayList();
        var normals = new FloatArrayList();
        var layers = new ByteArrayList();
        var surface = new ByteArrayList();
        var colors = new IntArrayList();
        var light = new IntArrayList();
        long seed = state.getSeed(pos);
        BlockModelLighter.clearCache();

        var renderer =
                Renderer.get()
                        .altModelBlockRenderer(
                                Minecraft.getInstance().options.ambientOcclusion().get(),
                                true,
                                Minecraft.getInstance().getBlockColors());
        var emitter =
                Renderer.get()
                        .quadEmitter(
                                quad -> {
                                    boolean sampled = quad.tag() == QuadLighting.OWN_BLOCK;

                                    surface.add(sampled ? (byte) (quad.emissive() ? 15 : 0) : -1);
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
                                });
        Object key = model.complete.createGeometryKey(view, pos, state, RandomSource.create(seed));
        Cells cells = key == null ? null : model.cells.get(key);
        if (cells == null) {
            cells = Cells.of(model.complete, view, pos, state, seed);
            if (key != null) model.cells.put(key, cells);
        }
        if (cells == Cells.NONE) {
            renderer.tesselateBlock(emitter, 0, 0, 0, view, pos, state, model.complete, seed);
        } else {
            for (int i = 0; i < cells.offsets().length; i++) {
                int cell = cells.offsets()[i];
                renderer.tesselateBlock(
                        emitter,
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
        }

        return new Prepared(
                new Mesh(
                        vertices.toFloatArray(),
                        normals.toFloatArray(),
                        layers.toByteArray(),
                        surface.toByteArray(),
                        false,
                        true),
                new Lit(colors.toIntArray(), light.toIntArray()));
    }

    record Cells(int[] offsets, BlockStateModel[] models) {
        static final Cells NONE = new Cells(new int[0], new BlockStateModel[0]);

        static Cells of(
                BlockStateModel complete,
                BlockAndTintGetter view,
                BlockPos pos,
                BlockState state,
                long seed) {
            var all = Renderer.get().mutableMesh();
            complete.emitQuads(
                    all.emitter(), view, pos, state, RandomSource.create(seed), face -> false);
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
            if (!meshes.keySet().intStream().anyMatch(cell -> cell != 0)) return NONE;
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
            quads.outputTo(emitter);
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
