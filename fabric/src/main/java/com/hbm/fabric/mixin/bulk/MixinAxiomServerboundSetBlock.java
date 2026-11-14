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

package com.hbm.fabric.mixin.bulk;

import com.hbm.world.bulk.BulkAction;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.moulberry.axiom.packets.AxiomServerboundSetBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = AxiomServerboundSetBlock.class, remap = false)
public abstract class MixinAxiomServerboundSetBlock {
    @WrapMethod(method = "setWithoutUpdates")
    private static void hbm$record(
            ServerLevel level, BlockPos pos, BlockState state, Operation<Void> original) {
        BulkAction.record(level, pos, level.getBlockState(pos), state);
        original.call(level, pos, state);
    }

    @WrapMethod(method = "handle")
    private void hbm$bulk(MinecraftServer server, ServerPlayer player, Operation<Void> original) {
        try (var bulk = BulkAction.begin()) {
            original.call(server, player);
        }
    }
}
