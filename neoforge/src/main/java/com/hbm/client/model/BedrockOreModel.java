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

import com.hbm.blockentity.BedrockOreBlockEntity;
import java.util.List;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class BedrockOreModel implements BlockStateModel {

    public static final int SHAPES = 10;
    public static final int OVERLAY_TINT = 0;
    public static Factory FACTORY = BedrockOreModel::new;

    private final ModelBaker baker;
    private final ResolvedModel base;
    private final TextureSlots slots;
    private final Material.Baked particle;
    private final ModelPartCache cache = ModelPartCache.dense(SHAPES, this::buildPart);
    private final BlockStateModelPart bare;

    public BedrockOreModel(ModelBaker baker, ResolvedModel base) {
        this.baker = RuntimeBaker.capture(baker, base);
        this.base = base;
        this.slots = base.getTopTextureSlots();
        this.particle = base.resolveParticleMaterial(slots, baker);
        this.bare = buildPart(-1);
    }

    private static BedrockOreModel create(ModelBaker baker, ResolvedModel base) {
        return FACTORY.create(baker, base);
    }

    private static int shapeAt(BlockAndTintGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof BedrockOreBlockEntity ore
                ? Mth.positiveModulo(ore.shape, SHAPES)
                : -1;
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> output) {
        output.add(bare);
    }

    @Override
    public Material.Baked particleMaterial() {
        return particle;
    }

    @Override
    public int materialFlags() {
        return bare.materialFlags();
    }

    public void collectParts(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random,
            List<BlockStateModelPart> output) {
        int shape = shapeAt(level, pos);
        output.add(shape < 0 ? bare : partFor(shape));
    }

    public Object geometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        return new Key(shapeAt(level, pos));
    }

    private BlockStateModelPart partFor(int shape) {
        return cache.get(shape);
    }

    private BlockStateModelPart buildPart(int shape) {
        QuadCollection.Builder quads = new QuadCollection.Builder();
        Boxes.cube(quads, baker, BlockModel.slot(baker, base, slots, "all"), CuboidFace.NO_TINT);
        if (shape >= 0) {
            Boxes.cube(
                    quads,
                    baker,
                    BlockModel.slot(baker, base, slots, "overlay" + shape),
                    OVERLAY_TINT);
        }
        return new SimpleModelWrapper(quads.build(), base.getTopAmbientOcclusion(), particle);
    }

    public interface Factory {
        BedrockOreModel create(ModelBaker baker, ResolvedModel base);
    }

    public record Key(int shape) {}

    public record Family() implements SimpleBlockModel {

        @Override
        public BlockStateModel.UnbakedRoot root(Block block, BlockState state) {
            return new Root(BlockModel.base(block));
        }
    }

    public record Root(Identifier baseModel) implements BlockStateModel.UnbakedRoot {

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            resolver.markDependency(baseModel);
        }

        @Override
        public BlockStateModel bake(BlockState blockState, ModelBaker baker) {
            return baker.compute(new SharedKey(this));
        }

        @Override
        public Object visualEqualityGroup(BlockState blockState) {
            return this;
        }
    }

    private record SharedKey(Root root) implements ModelBaker.SharedOperationKey<BlockStateModel> {

        @Override
        public BlockStateModel compute(ModelBaker baker) {
            return create(baker, baker.getModel(root.baseModel()));
        }
    }
}
