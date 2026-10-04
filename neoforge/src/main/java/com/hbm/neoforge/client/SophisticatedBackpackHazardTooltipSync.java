// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.client;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackStorage;
import net.p3pp3rf1y.sophisticatedbackpacks.network.RequestBackpackInventoryContentsPayload;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;

final class SophisticatedBackpackHazardTooltipSync {
    private SophisticatedBackpackHazardTooltipSync() {}

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
        return BackpackStorage.get().getOrCreateBackpackContents(id).inventory().stacks();
    }

    static UUID contentsId(ItemStack stack) {
        if (!(stack.getItem() instanceof BackpackItem)
                || stack.has(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT.get())) return null;
        return stack.get(ModCoreDataComponents.STORAGE_UUID.get());
    }

    private static void request(UUID id) {
        if (!HazardTooltipRequests.reserve(id, true)) return;
        ClientPacketDistributor.sendToServer(new RequestBackpackInventoryContentsPayload(id));
    }
}
