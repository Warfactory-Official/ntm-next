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
import net.neoforged.neoforge.client.model.quad.BakedNormals;
import org.joml.Vector3f;
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

        for (var part : parts) {
            for (int face = -1; face < Direction.values().length; face++) {
                for (var quad : part.getQuads(face == -1 ? null : Direction.values()[face])) {
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
                new ModelBlockRenderer(
                        Minecraft.getInstance().options.ambientOcclusion().get(),
                        true,
                        Minecraft.getInstance().getBlockColors());
        BlockQuadOutput output =
                (x, y, z, quad, instance) -> {
                    byte sampled = surface(quad);
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
                };
        var parts = new ArrayList<BlockStateModelPart>(2);
        model.complete.collectParts(view, pos, state, RandomSource.create(seed), parts);
        boolean offset = false;
        for (var part : parts) {
            Cells cells = model.cells.get(part);
            if (cells == null) model.cells.put(part, cells = Cells.of(part, model.complete));
            offset |= cells != Cells.NONE;
        }
        if (!offset) {
            renderer.tesselateBlock(output, 0, 0, 0, view, pos, state, model.complete, seed);
        } else {
            for (var part : parts) {
                Cells cells = model.cells.get(part);
                if (cells == Cells.NONE) {
                    renderer.tesselateBlock(
                            output,
                            0,
                            0,
                            0,
                            view,
                            pos,
                            state,
                            new CellModel(List.of(part), model.complete),
                            seed);
                    continue;
                }
                for (int i = 0; i < cells.offsets().length; i++) {
                    int cell = cells.offsets()[i];
                    renderer.tesselateBlock(
                            output,
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

        static Cells of(BlockStateModelPart part, BlockStateModel complete) {
            var builders = new Int2ObjectLinkedOpenHashMap<QuadCollection.Builder>();
            for (int face = -1; face < Direction.values().length; face++) {
                Direction cull = face == -1 ? null : Direction.values()[face];
                for (var quad : part.getQuads(cull)) {
                    int origin = quad.materialInfo().hbm$lightOrigin();
                    int cell = QuadLighting.isOffset(origin) ? origin : 0;
                    var builder =
                            builders.computeIfAbsent(cell, ignored -> new QuadCollection.Builder());
                    var moved = cell == 0 ? quad : translated(quad, cell);
                    if (cull == null) builder.addUnculledFace(moved);
                    else builder.addCulledFace(cull, moved);
                }
            }
            if (!builders.keySet().intStream().anyMatch(cell -> cell != 0)) return NONE;
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
