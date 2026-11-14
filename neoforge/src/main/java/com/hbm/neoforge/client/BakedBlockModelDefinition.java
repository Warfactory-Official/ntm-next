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

package com.hbm.neoforge.client;

import com.hbm.client.model.BlockModel;
import com.hbm.registration.Reg;
import com.mojang.serialization.MapCodec;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.neoforged.neoforge.client.model.block.CustomBlockModelDefinition;

public final class BakedBlockModelDefinition implements CustomBlockModelDefinition {

    private final MapCodec<BakedBlockModelDefinition> codec;

    public BakedBlockModelDefinition() {
        this.codec = MapCodec.unit(() -> this);
    }

    @Override
    public Map<BlockState, BlockStateModel.UnbakedRoot> instantiate(
            StateDefinition<Block, BlockState> states, Supplier<String> sourceSupplier) {
        Block owner = states.getOwner();
        Identifier id = BuiltInRegistries.BLOCK.getKey(owner);
        Supplier<BlockModel<?>> declared = Reg.bakedBy(id);
        if (declared == null) {
            throw new IllegalStateException(
                    id
                            + " dispatches to hbm:baked but its registration states no "
                            + ".bakedBy, so nothing builds its models; the blockstate file and the registration are "
                            + "written apart and this is where they meet");
        }
        BlockModel.Prepared prepared = declared.get().prepared();
        Map<BlockState, BlockStateModel.UnbakedRoot> roots = new IdentityHashMap<>();
        for (BlockState state : states.getPossibleStates())
            roots.put(state, prepared.root(owner, state));
        return roots;
    }

    @Override
    public MapCodec<? extends CustomBlockModelDefinition> codec() {
        return codec;
    }
}
