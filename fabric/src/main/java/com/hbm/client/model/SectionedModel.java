// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.hbm.blocks.ISectionGeometry;
import com.hbm.util.Facing;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.SingleVariant;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public final class SectionedModel implements BlockStateModel {
    private static volatile IdentityHashMap<BlockState, SectionedModel> published =
            new IdentityHashMap<>();
    private static final Object2ObjectLinkedOpenHashMap<LayoutKey, LayoutEntry> LAYOUTS =
            new Object2ObjectLinkedOpenHashMap<>();
    private static final Map<LayoutKey, LayoutEntry> ACTIVE = new HashMap<>();
    private static final IdentityHashMap<SectionGeometryLayout, LayoutEntry> ENTRIES =
            new IdentityHashMap<>();
    private static final long CACHE_BYTES = 16 * 1024 * 1024;
    private static long cachedBytes;
    final BlockStateModel complete;
    final SectionGeometry.Mesh mesh;
    final boolean contextual;
    private final SectionGeometry.Prepared[] appearances = new SectionGeometry.Prepared[4];
    private final int[] appearanceHashes = new int[4];
    private int nextAppearance;
    private final Map<CardinalLighting, SectionGeometry.Prepared> uniform = new HashMap<>(2);

    final Map<Object, SectionGeometry.Cells> cells = new HashMap<>();

    public static Map<BlockState, BlockStateModel> wrap(Map<BlockState, BlockStateModel> models) {
        var result = new IdentityHashMap<BlockState, BlockStateModel>(models);
        var byState = new IdentityHashMap<BlockState, SectionedModel>();
        var shared = new IdentityHashMap<BlockStateModel, SectionedModel>();
        var meshes = new HashMap<MeshKey, SectionGeometry.Mesh>();
        synchronized (LAYOUTS) {
            LAYOUTS.clear();
            ACTIVE.clear();
            ENTRIES.clear();
            cachedBytes = 0;
        }
        for (var entry : models.entrySet()) {
            BlockState state = entry.getKey();
            BlockStateModel complete = entry.getValue();
            if (!(state.getBlock() instanceof ISectionGeometry owner)) continue;
            SectionedModel sectioned = shared.get(complete);
            if (sectioned == null) {
                var mesh = SectionGeometry.mesh(complete, state);
                if (mesh.contained() && !owner.contextualGeometry()) continue;
                mesh = meshes.computeIfAbsent(new MeshKey(mesh), key -> key.mesh);
                sectioned = new SectionedModel(complete, mesh);
                shared.put(complete, sectioned);
            }
            byState.put(state, sectioned);
            result.put(state, sectioned);
        }
        published = byState;
        return result;
    }

    public static void publish() {
        SectionGeometry.reload();
    }

    public static @Nullable SectionedModel forState(BlockState state) {

        return published.get(state);
    }

    private SectionedModel(BlockStateModel complete, SectionGeometry.Mesh mesh) {
        this.complete = complete;
        this.mesh = mesh;
        this.contextual = complete.getClass() != SingleVariant.class || !mesh.ownBlock();
    }

    SectionGeometryLayout acquire(BlockPos core, SectionGeometry.Mesh geometry) {
        int placement = core.getX() & 15 | (core.getY() & 15) << 4 | (core.getZ() & 15) << 8;
        var key = new LayoutKey(geometry.vertices(), placement);
        synchronized (LAYOUTS) {
            var entry = ACTIVE.get(key);
            if (entry != null) {
                entry.references++;
                return entry.layout;
            }
            entry = LAYOUTS.remove(key);
            if (entry == null) {
                entry =
                        new LayoutEntry(
                                key, new SectionGeometryLayout(geometry.vertices(), placement));
                ENTRIES.put(entry.layout, entry);
            } else {
                cachedBytes -= entry.bytes();
            }
            ACTIVE.put(key, entry);
            entry.references = 1;
            return entry.layout;
        }
    }

    static void release(SectionGeometryLayout layout) {
        synchronized (LAYOUTS) {
            var entry = ENTRIES.get(layout);

            if (entry == null) return;
            assert entry.references > 0;
            if (--entry.references != 0) return;
            ACTIVE.remove(entry.key);
            LAYOUTS.putAndMoveToLast(entry.key, entry);
            cachedBytes += entry.bytes();
            trimLayouts();
        }
    }

    private static void trimLayouts() {
        while (cachedBytes > CACHE_BYTES) {
            var entry = LAYOUTS.removeFirst();
            cachedBytes -= entry.bytes();
            ENTRIES.remove(entry.layout);
        }
    }

    private static final class LayoutEntry {
        final LayoutKey key;
        final SectionGeometryLayout layout;
        int references;

        LayoutEntry(LayoutKey key, SectionGeometryLayout layout) {
            this.key = key;
            this.layout = layout;
        }

        long bytes() {
            return layout.bytes() + key.bytes();
        }
    }

    SectionGeometry.Prepared prepare(ClientLevel level, BlockPos pos, BlockState state) {
        if (contextual) return share(SectionGeometry.prepare(this, level, pos, state));
        return uniform.computeIfAbsent(
                ((BlockAndTintGetter) level).cardinalLighting(),
                lighting ->
                        new SectionGeometry.Prepared(
                                mesh, SectionGeometry.colors(this, state, lighting)));
    }

    private SectionGeometry.Prepared share(SectionGeometry.Prepared value) {
        var geometry = value.mesh();
        if (Arrays.equals(geometry.vertices(), mesh.vertices())
                && Arrays.equals(geometry.normals(), mesh.normals())
                && Arrays.equals(geometry.layers(), mesh.layers())
                && Arrays.equals(geometry.surface(), mesh.surface())) {
            geometry = mesh;
            value = new SectionGeometry.Prepared(mesh, value.lit());
        }
        int hash =
                ((Arrays.hashCode(geometry.vertices()) * 31 + Arrays.hashCode(geometry.normals()))
                                                * 31
                                        + Arrays.hashCode(geometry.layers()))
                                * 31
                        + Arrays.hashCode(geometry.surface());
        hash =
                (hash * 31 + Arrays.hashCode(value.lit().colors())) * 31
                        + Arrays.hashCode(value.lit().light());
        for (int i = 0; i < appearances.length; i++) {
            var old = appearances[i];
            if (old != null
                    && appearanceHashes[i] == hash
                    && Arrays.equals(old.mesh().vertices(), geometry.vertices())
                    && Arrays.equals(old.mesh().layers(), geometry.layers())
                    && Arrays.equals(old.mesh().normals(), geometry.normals())
                    && Arrays.equals(old.mesh().surface(), geometry.surface())
                    && Arrays.equals(old.lit().colors(), value.lit().colors())
                    && Arrays.equals(old.lit().light(), value.lit().light())) return old;
        }
        int index = nextAppearance++ & 3;
        appearances[index] = value;
        appearanceHashes[index] = hash;
        return value;
    }

    private static final class MeshKey {
        final SectionGeometry.Mesh mesh;
        final int hash;

        MeshKey(SectionGeometry.Mesh mesh) {
            this.mesh = mesh;
            hash =
                    ((31 * Arrays.hashCode(mesh.vertices()) + Arrays.hashCode(mesh.normals())) * 31
                                            + Arrays.hashCode(mesh.layers()))
                                    * 31
                            + Arrays.hashCode(mesh.surface());
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof MeshKey key
                    && mesh.ownBlock() == key.mesh.ownBlock()
                    && Arrays.equals(mesh.vertices(), key.mesh.vertices())
                    && Arrays.equals(mesh.normals(), key.mesh.normals())
                    && Arrays.equals(mesh.layers(), key.mesh.layers())
                    && Arrays.equals(mesh.surface(), key.mesh.surface());
        }
    }

    private static final class LayoutKey {
        final float[] vertices;
        final int placement;
        final int hash;

        LayoutKey(float[] vertices, int placement) {
            this.vertices = vertices;
            this.placement = placement;
            int hash = placement;
            for (int i = 0; i < vertices.length; i += 5) {
                for (int axis = 0; axis < 3; axis++)
                    hash = hash * 31 + Float.floatToIntBits(vertices[i + axis]);
            }
            this.hash = hash;
        }

        long bytes() {
            return 4L * vertices.length;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof LayoutKey key)
                    || placement != key.placement
                    || vertices.length != key.vertices.length) return false;
            if (vertices == key.vertices) return true;
            for (int i = 0; i < vertices.length; i += 5) {
                for (int axis = 0; axis < 3; axis++) {
                    if (Float.floatToIntBits(vertices[i + axis])
                            != Float.floatToIntBits(key.vertices[i + axis])) return false;
                }
            }
            return true;
        }
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> output) {
        complete.collectParts(random, output);
    }

    @Override
    public void emitQuads(
            QuadEmitter emitter,
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random,
            Predicate<@Nullable Direction> cullTest) {
        if (!(level instanceof SectionGeometry.TerrainView)) {
            complete.emitQuads(emitter, level, pos, state, random, cullTest);
        }
    }

    @Override
    public Object createGeometryKey(
            BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
        return level instanceof SectionGeometry.TerrainView
                ? SectionedModel.class
                : complete.createGeometryKey(level, pos, state, random);
    }

    @Override
    public Material.Baked particleMaterial() {
        return complete.particleMaterial();
    }

    @Override
    public int materialFlags() {
        return complete.materialFlags();
    }

    public record Family(int yaw) implements BlockModel<Void> {
        @Override
        public Void prepare() {
            return null;
        }

        @Override
        public BlockStateModel.UnbakedRoot root(Void prepared, Block block, BlockState state) {
            Identifier id = BuiltInRegistries.BLOCK.getKey(block);
            return new Root(id.withPath("block/" + id.getPath()), yaw);
        }
    }

    private record Root(Identifier carrier, int yaw) implements BlockStateModel.UnbakedRoot {
        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            resolver.markDependency(carrier);
        }

        @Override
        public BlockStateModel bake(BlockState state, ModelBaker baker) {
            var model = baker.getModel(carrier);
            var slots = model.getTopTextureSlots();
            var pose = BlockModelRotation.get(Facing.ry(-yaw));
            var quads = model.bakeTopGeometry(slots, baker, pose);
            return new SingleVariant(
                    new SimpleModelWrapper(
                            quads, false, model.resolveParticleMaterial(slots, baker)));
        }

        @Override
        public Object visualEqualityGroup(BlockState state) {
            return state;
        }
    }
}
