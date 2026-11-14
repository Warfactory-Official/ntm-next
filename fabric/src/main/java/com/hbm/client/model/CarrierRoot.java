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

import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.SingleVariant;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;

public interface CarrierRoot extends BlockStateModel.UnbakedRoot {

    Identifier carrier();

    QuadCollection quads(
            BlockState state, ModelBaker baker, ResolvedModel carrier, TextureSlots slots);

    default void resolveExtraDependencies(ResolvableModel.Resolver resolver) {}

    @Override
    default void resolveDependencies(ResolvableModel.Resolver resolver) {
        resolver.markDependency(carrier());
        resolveExtraDependencies(resolver);
    }

    @Override
    default BlockStateModel bake(BlockState state, ModelBaker baker) {
        ResolvedModel carrier = baker.getModel(carrier());
        TextureSlots slots = carrier.getTopTextureSlots();
        return new SingleVariant(
                new SimpleModelWrapper(
                        quads(state, baker, carrier, slots),
                        carrier.getTopAmbientOcclusion(),
                        carrier.resolveParticleMaterial(slots, baker)));
    }
}
