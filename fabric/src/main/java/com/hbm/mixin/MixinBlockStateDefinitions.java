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

package com.hbm.mixin;

import com.hbm.registration.Shared;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import java.util.function.Function;
import net.minecraft.client.resources.model.BlockStateDefinitions;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockStateDefinitions.class)
public abstract class MixinBlockStateDefinitions {
    @ModifyReturnValue(method = "definitionLocationToBlockStateMapper", at = @At("RETURN"))
    private static Function<Identifier, StateDefinition<Block, BlockState>> hbm$sharedBlockStates(
            Function<Identifier, StateDefinition<Block, BlockState>> definitions) {
        return id -> {
            Identifier shared = Shared.registryId(Registries.BLOCK, id);
            return definitions.apply(shared != null ? shared : id);
        };
    }
}
