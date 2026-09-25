// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.client.ctm;

import com.hbm.client.ctm.CtmEngine;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelDispatcher;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.neoforged.neoforge.client.model.block.CustomBlockModelDefinition;

public record CtmLibModelDefinition(
        BlockStateModelDispatcher base,
        List<Block> connections,
        Map<Identifier, Identifier> textures)
        implements CustomBlockModelDefinition {

    public static final MapCodec<CtmLibModelDefinition> CODEC =
            RecordCodecBuilder.mapCodec(
                    instance ->
                            instance.group(
                                            BlockStateModelDispatcher.VANILLA_CODEC.forGetter(
                                                    CtmLibModelDefinition::base),
                                            BuiltInRegistries.BLOCK
                                                    .byNameCodec()
                                                    .listOf()
                                                    .fieldOf("connect_to")
                                                    .forGetter(CtmLibModelDefinition::connections),
                                            Codec.unboundedMap(Identifier.CODEC, Identifier.CODEC)
                                                    .fieldOf("ctm_textures")
                                                    .forGetter(CtmLibModelDefinition::textures))
                                    .apply(instance, CtmLibModelDefinition::new));

    @Override
    public Map<BlockState, BlockStateModel.UnbakedRoot> instantiate(
            StateDefinition<Block, BlockState> states, Supplier<String> source) {
        Map<BlockState, BlockStateModel.UnbakedRoot> roots =
                base.instantiateVanilla(states, source);
        if (CtmEngine.selected() == CtmEngine.CTM_LIB) {
            roots.replaceAll((state, root) -> CtmLibBakedModel.wrap(root, connections, textures));
        }
        return roots;
    }

    @Override
    public MapCodec<? extends CustomBlockModelDefinition> codec() {
        return CODEC;
    }
}
