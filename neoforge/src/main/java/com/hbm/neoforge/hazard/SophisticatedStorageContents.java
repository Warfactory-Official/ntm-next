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

package com.hbm.neoforge.hazard;

import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedstorage.block.ItemContentsStorage;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.network.StorageContentsPayload;
import org.jspecify.annotations.Nullable;

final class SophisticatedStorageContents {
    private SophisticatedStorageContents() {}

    static @Nullable UUID id(ItemStack stack) {
        return stack.getItem() instanceof StorageBlockItem
                ? stack.get(ModCoreDataComponents.STORAGE_UUID.get())
                : null;
    }

    static List<ItemStack> stacks(UUID id) {
        return ItemContentsStorage.get()
                .findContents(id)
                .<List<ItemStack>>map(contents -> contents.inventory().stacks())
                .orElse(List.of());
    }

    static void send(ServerPlayer player, UUID id) {
        ItemContentsStorage.get()
                .findContents(id)
                .ifPresent(
                        contents ->
                                PacketDistributor.sendToPlayer(
                                        player, new StorageContentsPayload(id, contents)));
    }
}
