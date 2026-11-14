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

import com.hbm.blocks.bomb.DigammaBounds;
import java.util.List;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public final class DigammaMatterModel implements BlockStateModel {

    private final ModelBaker baker;
    private final Material.Baked texture;
    private final Material.Baked particle;
    private final boolean ambientOcclusion;
    private final int materialFlags;

    private DigammaMatterModel(ModelBaker baker, ResolvedModel carrier) {
        this.baker = RuntimeBaker.capture(baker, carrier);
        var slots = carrier.getTopTextureSlots();
        texture = BlockModel.slot(baker, carrier, slots, "texture");
        particle = carrier.resolveParticleMaterial(slots, baker);
        ambientOcclusion = carrier.getTopAmbientOcclusion();
        materialFlags =
                Boxes.face(this.baker, Boxes.UNIT_FROM, Boxes.UNIT_TO, Direction.DOWN, texture)
                        .materialInfo()
                        .flags();
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> output) {
        output.add(buildPart(DigammaBounds.sample(), 0));
    }

    public void collectParts(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random,
            List<BlockStateModelPart> output) {
        output.add(samplePart(level, pos));
    }

    @Override
    public void emitQuads(
            QuadEmitter emitter,
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random,
            Predicate<@Nullable Direction> cullTest) {
        samplePart(level, pos).emitQuads(emitter, cullTest);
    }

    @Override
    public Material.Baked particleMaterial() {
        return particle;
    }

    @Override
    public int materialFlags() {
        return materialFlags;
    }

    private BlockStateModelPart samplePart(BlockAndTintGetter level, BlockPos pos) {
        var sample = DigammaBounds.sample();
        int hiddenFaces = 0;
        for (var direction : Direction.VALUES) {
            if (onBoundary(sample, direction)
                    && level.getBlockState(pos.relative(direction)).isSolidRender()) {
                hiddenFaces |= 1 << direction.ordinal();
            }
        }
        return buildPart(sample, hiddenFaces);
    }

    private static boolean onBoundary(int sample, Direction direction) {
        return switch (direction) {
            case DOWN -> DigammaBounds.lower(sample, 1) == 0;
            case UP -> DigammaBounds.upper(sample, 1) == 1;
            case NORTH -> DigammaBounds.lower(sample, 2) == 0;
            case SOUTH -> DigammaBounds.upper(sample, 2) == 1;
            case WEST -> DigammaBounds.lower(sample, 0) == 0;
            case EAST -> DigammaBounds.upper(sample, 0) == 1;
        };
    }

    private BlockStateModelPart buildPart(int sample, int hiddenFaces) {
        var from =
                new Vector3f(
                        (float) (DigammaBounds.lower(sample, 0) * 16),
                        (float) (DigammaBounds.lower(sample, 1) * 16),
                        (float) (DigammaBounds.lower(sample, 2) * 16));
        var to =
                new Vector3f(
                        (float) (DigammaBounds.upper(sample, 0) * 16),
                        (float) (DigammaBounds.upper(sample, 1) * 16),
                        (float) (DigammaBounds.upper(sample, 2) * 16));
        var quads = new QuadCollection.Builder();
        for (var direction : Direction.VALUES) {
            if ((hiddenFaces & (1 << direction.ordinal())) == 0) {
                quads.addUnculledFace(Boxes.face(baker, from, to, direction, texture));
            }
        }
        return new SimpleModelWrapper(quads.build(), ambientOcclusion, particle);
    }

    public record Family() implements SimpleBlockModel {

        @Override
        public BlockStateModel.UnbakedRoot root(Block block, BlockState state) {
            return new Root(BlockModel.base(block));
        }
    }

    private record Root(Identifier carrier) implements BlockStateModel.UnbakedRoot {

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            resolver.markDependency(carrier);
        }

        @Override
        public BlockStateModel bake(BlockState blockState, ModelBaker baker) {
            return baker.compute(new BakedKey(carrier));
        }

        @Override
        public Object visualEqualityGroup(BlockState blockState) {
            return this;
        }
    }

    private record BakedKey(Identifier carrier)
            implements ModelBaker.SharedOperationKey<DigammaMatterModel> {

        @Override
        public DigammaMatterModel compute(ModelBaker baker) {
            return new DigammaMatterModel(baker, baker.getModel(carrier));
        }
    }
}
