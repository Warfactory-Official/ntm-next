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
        Map<Identifier, Identifier> textures,
        Map<Identifier, List<BlockState>> connectionStates)
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
                                                    .forGetter(CtmLibModelDefinition::textures),
                                            Codec.unboundedMap(
                                                            Identifier.CODEC,
                                                            BlockState.CODEC.listOf())
                                                    .optionalFieldOf("connect_to_states", Map.of())
                                                    .forGetter(
                                                            CtmLibModelDefinition
                                                                    ::connectionStates))
                                    .apply(instance, CtmLibModelDefinition::new));

    @Override
    public Map<BlockState, BlockStateModel.UnbakedRoot> instantiate(
            StateDefinition<Block, BlockState> states, Supplier<String> source) {
        Map<BlockState, BlockStateModel.UnbakedRoot> roots =
                base.instantiateVanilla(states, source);
        if (CtmEngine.selected() == CtmEngine.CTM_LIB) {
            roots.replaceAll(
                    (state, root) ->
                            CtmLibBakedModel.wrap(root, connections, textures, connectionStates));
        }
        return roots;
    }

    @Override
    public MapCodec<? extends CustomBlockModelDefinition> codec() {
        return CODEC;
    }
}
