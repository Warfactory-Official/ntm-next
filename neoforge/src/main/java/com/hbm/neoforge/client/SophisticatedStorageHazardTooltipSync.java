// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.client;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedstorage.block.ItemContentsStorage;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.network.RequestStorageContentsPayload;

final class SophisticatedStorageHazardTooltipSync {
    private SophisticatedStorageHazardTooltipSync() {}

    static void reset() {
        HazardTooltipRequests.reset();
    }

    static void request(ItemStack stack) {
        UUID id = contentsId(stack);
        if (id != null) request(id);
    }

    static List<ItemStack> requestNested(ItemStack stack, Set<UUID> visited) {
        UUID id = contentsId(stack);
        if (id == null || !visited.add(id)) return List.of();
        request(id);
        return ItemContentsStorage.get().getOrCreateContents(id).inventory().stacks();
    }

    static UUID contentsId(ItemStack stack) {
        return stack.getItem() instanceof StorageBlockItem
                ? stack.get(ModCoreDataComponents.STORAGE_UUID.get())
                : null;
    }

    private static void request(UUID id) {
        if (!HazardTooltipRequests.reserve(id, false)) return;
        ClientPacketDistributor.sendToServer(new RequestStorageContentsPayload(id));
    }
}
