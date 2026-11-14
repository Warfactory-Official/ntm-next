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

package com.hbm.mixin.compat.bulk;

import com.hbm.world.bulk.BulkAction;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import nl.requios.effortlessbuilding.network.PacketHandler;
import nl.requios.effortlessbuilding.utilities.BlockEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

@Mixin(value = PacketHandler.class, remap = false)
public abstract class MixinPacketHandler {
    @WrapMethod(method = {"handlePlaceBuildMode", "handleBreakBuildMode"})
    private static void hbm$bulk(
            @Coerce Object packet, ServerPlayer player, Operation<Void> original) {
        try (var bulk = BulkAction.begin(player.isCreative())) {
            original.call(packet, player);
        }
    }

    @WrapOperation(
            method = "handleBreakBuildMode",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnl/requios/effortlessbuilding/utilities/InventoryHelper;giveOrDropItems(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/Item;I)V"))
    private static void hbm$returnStack(
            Player player,
            Item item,
            int count,
            Operation<Void> original,
            @Local(name = "drop") ItemStack drop,
            @Local(name = "oldState") BlockState state) {
        if (BulkAction.owns(state)) player.getInventory().placeItemBackInInventory(drop.copy());
        else original.call(player, item, count);
    }

    @WrapOperation(
            method = "handlePlaceBuildMode",
            at =
                    @At(
                            value = "INVOKE",
                            ordinal = 0,
                            target =
                                    "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private static boolean hbm$randomizer(
            ServerLevel level,
            BlockPos pos,
            BlockState state,
            int flags,
            Operation<Boolean> original,
            @Local(argsOnly = true) ServerPlayer player,
            @Local(name = "entry") BlockEntry entry) {
        boolean placed = original.call(level, pos, state, flags);
        if (placed && BulkAction.owns(state))
            state.getBlock().setPlacedBy(level, pos, state, player, new ItemStack(entry.item));
        return placed;
    }
}
