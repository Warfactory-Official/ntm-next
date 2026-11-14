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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.SingleVariant;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TriState;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

final class BoundaryFaces {
    private BoundaryFaces() {}

    static BlockStateModel cull(BlockStateModel model) {
        if (model instanceof DiodeModel.Baked diode) {
            diode.prepareBoundaryFaces();
            return diode;
        }
        if (model.getClass() != SingleVariant.class) return new Contextual(model);
        var parts = new ArrayList<BlockStateModelPart>(1);
        model.collectParts(RandomSource.create(0), parts);
        return new SingleVariant(cull(parts.getFirst()));
    }

    static BlockStateModelPart cull(BlockStateModelPart part) {
        var unculled = part.getQuads(null);
        int first = 0;
        while (first < unculled.size() && boundary(unculled.get(first)) == null) first++;
        if (first == unculled.size()) return part;
        var builder = new QuadCollection.Builder();
        for (int i = 0; i < unculled.size(); i++) {
            var quad = unculled.get(i);
            var face = i < first ? null : boundary(quad);
            if (face == null) builder.addUnculledFace(quad);
            else builder.addCulledFace(face, quad);
        }
        for (var direction : Direction.VALUES) {
            for (var quad : part.getQuads(direction)) {
                builder.addCulledFace(direction, quad);
            }
        }
        QuadCollection quads = builder.build();
        if (!(part instanceof SimpleModelWrapper)) return new ClassifiedPart(quads, part);
        return new SimpleModelWrapper(quads, part.useAmbientOcclusion(), part.particleMaterial());
    }

    private static @Nullable Direction boundary(BakedQuad quad) {
        for (int v = 0; v < 4; v++) {
            var point = quad.position(v);
            if (!(point.x() >= 0
                    && point.x() <= 1
                    && point.y() >= 0
                    && point.y() <= 1
                    && point.z() >= 0
                    && point.z() <= 1)) return null;
        }
        int axis = quad.direction().getAxis().ordinal();
        return boundary(
                quad.direction(),
                quad.position0().get(axis),
                quad.position1().get(axis),
                quad.position2().get(axis),
                quad.position3().get(axis));
    }

    private record ClassifiedPart(QuadCollection quads, BlockStateModelPart source)
            implements BlockStateModelPart {
        @Override
        public List<BakedQuad> getQuads(@Nullable Direction face) {
            return quads.getQuads(face);
        }

        @Override
        public boolean useAmbientOcclusion() {
            return source.useAmbientOcclusion();
        }

        @Override
        public TriState ambientOcclusion() {
            return source.ambientOcclusion();
        }

        @Override
        public Material.Baked particleMaterial() {
            return source.particleMaterial();
        }

        @Override
        public int materialFlags() {
            return source.materialFlags();
        }
    }

    private static final class Contextual implements BlockStateModel {
        private final BlockStateModel complete;
        private volatile BlockStateModelPart[] classified = new BlockStateModelPart[0];
        private int next;

        private BlockStateModelPart classify(BlockStateModelPart part) {
            if (!(part instanceof SimpleModelWrapper)) return cull(part);
            var cache = classified;
            for (int i = 0; i < cache.length; i += 2) if (cache[i] == part) return cache[i + 1];
            var result = cull(part);
            synchronized (this) {
                cache = classified;
                for (int i = 0; i < cache.length; i += 2) if (cache[i] == part) return cache[i + 1];
                int slot;
                if (cache.length < 32) {
                    slot = cache.length;
                    cache = Arrays.copyOf(cache, cache.length + 2);
                } else {
                    slot = (next++ & 15) * 2;
                    cache = cache.clone();
                }
                cache[slot] = part;
                cache[slot + 1] = result;
                classified = cache;
            }
            return result;
        }

        private Contextual(BlockStateModel complete) {
            this.complete = complete;
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
            var parts = new ArrayList<BlockStateModelPart>();
            complete.collectParts(level, pos, state, random, parts);
            for (var part : parts) output.add(classify(part));
        }

        @Override
        public Object createGeometryKey(
                BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
            return complete.createGeometryKey(level, pos, state, random);
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

    private static @Nullable Direction boundary(
            Direction direction, float a, float b, float c, float d) {
        float edge = direction.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : 0;
        return a == edge && b == edge && c == edge && d == edge ? direction : null;
    }
}
