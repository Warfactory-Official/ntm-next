// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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
