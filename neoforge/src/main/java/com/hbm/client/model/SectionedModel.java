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

import com.hbm.blocks.SectionGeometryProvider;
import com.hbm.registration.Reg;
import com.hbm.util.Facing;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public final class SectionedModel implements BlockStateModel {
    private static volatile IdentityHashMap<BlockState, SectionedModel> published =
            new IdentityHashMap<>();
    private static volatile @Nullable Pending pending;
    private static final Object2ObjectLinkedOpenHashMap<LayoutKey, LayoutEntry> LAYOUTS =
            new Object2ObjectLinkedOpenHashMap<>();
    private static final Map<LayoutKey, LayoutEntry> ACTIVE = new HashMap<>();
    private static final IdentityHashMap<SectionGeometryLayout, LayoutEntry> ENTRIES =
            new IdentityHashMap<>();
    private static final long CACHE_BYTES = 16 * 1024 * 1024;
    private static final IdentityHashMap<SectionGeometryLayout.Culls, Integer> CACHED_CULLS =
            new IdentityHashMap<>();
    private static long cachedBytes;
    final BlockStateModel complete;
    final SectionGeometry.Mesh mesh;
    final boolean contextual;
    private final SectionGeometry.Prepared[] appearances = new SectionGeometry.Prepared[4];
    private int nextAppearance;
    private final Map<CardinalLighting, SectionGeometry.Prepared> uniform = new HashMap<>(2);
    private final Object[] cellKeys = new Object[4];
    private final SectionGeometry.Cells[] cells = new SectionGeometry.Cells[4];
    private int nextCells;

    private record Pending(
            IdentityHashMap<BlockState, SectionedModel> byState,
            IdentityHashMap<BlockState, OccluderMasks.Mask> masks) {}

    record Wrapped(
            IdentityHashMap<BlockState, BlockStateModel> models,
            IdentityHashMap<BlockState, SectionedModel> byState) {}

    public static Map<BlockState, BlockStateModel> prepare(
            Map<BlockState, BlockStateModel> models) {
        var derived = OccluderMasks.derive(models);
        var wrapped = wrap(models, derived.meshes());
        pending = new Pending(wrapped.byState(), derived.masks());
        return wrapped.models();
    }

    public static void publish() {
        var next = pending;
        pending = null;
        synchronized (LAYOUTS) {
            LAYOUTS.clear();
            ACTIVE.clear();
            ENTRIES.clear();
            CACHED_CULLS.clear();
            SectionGeometryLayout.clearOriginals();
            cachedBytes = 0;
        }
        published = next.byState();
        OccluderMasks.publish(next.masks());
    }

    static Wrapped wrap(
            Map<BlockState, BlockStateModel> models,
            Map<BlockStateModel, SectionGeometry.Mesh> captured) {
        var result = new IdentityHashMap<BlockState, BlockStateModel>(models);
        var byState = new IdentityHashMap<BlockState, SectionedModel>();
        var shared = new IdentityHashMap<BlockStateModel, SectionedModel>();
        var contained = new IdentityHashMap<BlockStateModel, BlockStateModel>();
        var meshes = new HashMap<MeshKey, SectionGeometry.Mesh>();
        for (var entry : models.entrySet()) {
            BlockState state = entry.getKey();
            BlockStateModel complete = entry.getValue();
            if (!Reg.isNtmNamespace(BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace())
                    || state.getRenderShape() != RenderShape.MODEL) continue;
            boolean contextual =
                    state.getBlock() instanceof SectionGeometryProvider owner
                            && owner.contextualGeometry();
            SectionedModel sectioned = shared.get(complete);
            if (sectioned == null) {
                var mesh =
                        captured.computeIfAbsent(
                                complete, model -> SectionGeometry.mesh(model, state));
                if (mesh.contained() && !contextual) {
                    if (mesh.vertices().length == 0 && complete.getClass() == SingleVariant.class)
                        continue;
                    if (state.getBlock() instanceof SectionGeometryProvider owner
                            && owner.ownsBoundaryFaceAdmission()) {
                        result.put(state, complete);
                    } else {
                        result.put(state, contained.computeIfAbsent(complete, BoundaryFaces::cull));
                    }
                    continue;
                }
                mesh = meshes.computeIfAbsent(new MeshKey(mesh), key -> key.mesh);
                sectioned = new SectionedModel(complete, mesh);
                shared.put(complete, sectioned);
            }
            byState.put(state, sectioned);
            result.put(state, sectioned);
        }
        return new Wrapped(result, byState);
    }

    public static @Nullable SectionedModel forState(BlockState state) {
        return published.get(state);
    }

    private SectionedModel(BlockStateModel complete, SectionGeometry.Mesh mesh) {
        this.complete = complete;
        this.mesh = mesh;
        this.contextual = complete.getClass() != SingleVariant.class || !mesh.ownBlock();
    }

    SectionGeometry.@Nullable Cells cells(@Nullable Object key) {
        if (key == null) return null;
        for (int i = 0; i < cellKeys.length; i++) {
            if (key.equals(cellKeys[i])) return cells[i];
        }
        return null;
    }

    void cells(Object key, SectionGeometry.Cells value) {
        int at = nextCells++ & 3;
        cellKeys[at] = key;
        cells[at] = value;
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
                uncache(entry);
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
            var culls = entry.layout.originalCulls;
            int count = CACHED_CULLS.getOrDefault(culls, 0);
            if (count == 0) cachedBytes += culls.bytes();
            CACHED_CULLS.put(culls, count + 1);
            trimLayouts();
        }
    }

    private static void uncache(LayoutEntry entry) {
        cachedBytes -= entry.bytes();
        var culls = entry.layout.originalCulls;
        int count = CACHED_CULLS.get(culls);
        if (count == 1) {
            CACHED_CULLS.remove(culls);
            cachedBytes -= culls.bytes();
        } else CACHED_CULLS.put(culls, count - 1);
    }

    private static void trimLayouts() {
        while (cachedBytes > CACHE_BYTES) {
            var entry = LAYOUTS.removeFirst();
            uncache(entry);
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
        return prepare(level, pos, state, null);
    }

    SectionGeometry.Prepared prepare(
            ClientLevel level,
            BlockPos pos,
            BlockState state,
            SectionGeometry.@Nullable Prepared previous) {
        if (contextual) return SectionGeometry.prepare(this, level, pos, state, previous);
        return uniform.computeIfAbsent(
                ((BlockAndTintGetter) level).cardinalLighting(),
                lighting ->
                        new SectionGeometry.Prepared(
                                mesh, SectionGeometry.colors(this, state, lighting)));
    }

    SectionGeometry.Prepared prepared(
            SectionGeometry.Preparation scratch, SectionGeometry.@Nullable Prepared previous) {
        if (previous != null && scratch.matches(previous)) return previous;
        for (var old : appearances) if (old != null && scratch.matches(old)) return old;
        var geometry = mesh;
        if (!scratch.matches(geometry)) {
            geometry =
                    previous != null && scratch.matches(previous.mesh()) ? previous.mesh() : null;
            if (geometry == null) {
                for (var old : appearances) {
                    if (old != null && scratch.matches(old.mesh())) {
                        geometry = old.mesh();
                        break;
                    }
                }
            }
            if (geometry == null)
                geometry =
                        new SectionGeometry.Mesh(
                                scratch.vertices.toFloatArray(),
                                scratch.normals.toFloatArray(),
                                scratch.layers.toByteArray(),
                                scratch.surface.toByteArray(),
                                false,
                                true);
        }
        var value =
                new SectionGeometry.Prepared(
                        geometry,
                        new SectionGeometry.Lit(
                                scratch.colors.toIntArray(),
                                scratch.preparedLight ? scratch.light.toIntArray() : null));
        appearances[nextAppearance++ & 3] = value;
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
    public void collectParts(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random,
            List<BlockStateModelPart> output) {
        if (!(level instanceof SectionGeometry.TerrainView))
            complete.collectParts(level, pos, state, random, output);
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
