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
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackStorage;
import net.p3pp3rf1y.sophisticatedbackpacks.network.BackpackContentsPayload;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import org.jspecify.annotations.Nullable;

final class SophisticatedBackpackContents {
    private SophisticatedBackpackContents() {}

    static @Nullable UUID id(ItemStack stack) {
        if (!(stack.getItem() instanceof BackpackItem)
                || stack.has(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT.get())) {
            return null;
        }
        return stack.get(ModCoreDataComponents.STORAGE_UUID.get());
    }

    static List<ItemStack> stacks(UUID id) {
        return BackpackStorage.get()
                .getBackpackContents(id)
                .<List<ItemStack>>map(contents -> contents.inventory().stacks())
                .orElse(List.of());
    }

    static void send(ServerPlayer player, UUID id) {
        BackpackStorage.get()
                .getBackpackContents(id)
                .ifPresent(
                        contents ->
                                PacketDistributor.sendToPlayer(
                                        player, new BackpackContentsPayload(id, contents)));
    }
}
