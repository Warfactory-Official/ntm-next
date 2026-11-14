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

package com.hbm.inventory;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class BlockMenus {
    public static boolean withinTwelveBlocks(BlockEntity blockEntity, Player player) {
        var position = blockEntity.getBlockPos();
        if (blockEntity.getLevel().getBlockEntity(position) != blockEntity) return false;

        double deltaX = player.getX() - position.getX() - 0.5;
        double deltaY = player.getY() - position.getY() - 0.5;
        double deltaZ = player.getZ() - position.getZ() - 0.5;
        return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ <= 144;
    }

    private static final Set<Class<?>> OWN_MENU = new HashSet<>();

    private BlockMenus() {}

    public static void declareOwnMenu(Class<? extends BlockEntity> type) {
        OWN_MENU.add(type);
    }

    public static InteractionResult openOnUse(Level level, BlockPos pos, Player player) {
        if (player.isSecondaryUseActive()) {
            return InteractionResult.SUCCESS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        var blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof MenuProvider provider) {
            open(player, provider);
        }
        return InteractionResult.SUCCESS;
    }

    public static void open(Player player, MenuProvider provider) {
        if (!(player instanceof ServerPlayer server)) return;
        if (!ownMenu(provider.getClass())) {
            server.openMenu(provider);
            return;
        }
        BlockEntity blockEntity = (BlockEntity) provider;
        if (!unpackLoot(server, blockEntity)) return;
        final BlockMenuContext<Unit> data =
                new BlockMenuContext<>(blockEntity.getBlockPos(), Unit.INSTANCE);
        final StreamCodec<? super RegistryFriendlyByteBuf, BlockMenuContext<Unit>> codec =
                BlockMenuContext.UNIT;
        server.openMenu(provider, buf -> codec.encode(buf, data));
    }

    private static boolean ownMenu(Class<?> type) {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            if (OWN_MENU.contains(c)) return true;
        }
        return false;
    }

    static boolean unpackLoot(ServerPlayer player, BlockEntity blockEntity) {
        if (blockEntity instanceof RandomizableContainer container) {
            if (container.getLootTable() != null && player.isSpectator()) return false;
            container.unpackLootTable(player);
        }
        return true;
    }
}
