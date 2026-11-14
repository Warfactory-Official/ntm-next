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
import java.util.List;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.renderer.v1.Renderer;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.model.MeshQuadCollection;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.SingleVariant;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
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
        var mesh = Renderer.get().mutableMesh();
        var emitter = mesh.emitter();
        var copy =
                Renderer.get()
                        .quadEmitter(
                                quad -> {
                                    emitter.copyFrom(quad);
                                    cull(emitter);
                                    emitter.emit();
                                });
        part.emitQuads(copy, _ -> false);
        QuadCollection quads = new MeshQuadCollection(mesh.immutableCopy());
        return new SimpleModelWrapper(quads, part.useAmbientOcclusion(), part.particleMaterial());
    }

    private static void cull(MutableQuadView quad) {
        if (quad.cullFace() != null) return;
        for (int v = 0; v < 4; v++) {
            if (quad.x(v) < 0
                    || quad.x(v) > 1
                    || quad.y(v) < 0
                    || quad.y(v) > 1
                    || quad.z(v) < 0
                    || quad.z(v) > 1) return;
        }
        var direction = quad.lightFace();
        int axis = direction.getAxis().ordinal();
        var nominal = quad.nominalFace();
        quad.cullFace(
                        boundary(
                                direction,
                                quad.posByIndex(0, axis),
                                quad.posByIndex(1, axis),
                                quad.posByIndex(2, axis),
                                quad.posByIndex(3, axis)))
                .nominalFace(nominal);
    }

    private static final class Contextual implements BlockStateModel {
        private final BlockStateModel complete;

        private Contextual(BlockStateModel complete) {
            this.complete = complete;
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
            emitter.pushTransform(
                    quad -> {
                        cull(quad);
                        var face = quad.cullFace();
                        return face == null || !cullTest.test(face);
                    });
            try {
                complete.emitQuads(emitter, level, pos, state, random, cullTest);
            } finally {
                emitter.popTransform();
            }
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
