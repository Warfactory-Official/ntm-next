// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.client;

import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.network.RequestStorageContentsPayload;

final class SophisticatedStorageHazardTooltipSync {
    private static final int REQUEST_PERIOD = 20;
    private static ResourceKey<Level> lastDimension;
    private static UUID lastId;
    private static long lastTick;

    private SophisticatedStorageHazardTooltipSync() {}

    static void reset() {
        lastDimension = null;
        lastId = null;
        lastTick = 0L;
    }

    static void request(ItemStack stack) {
        if (!(stack.getItem() instanceof StorageBlockItem)) return;
        UUID id = stack.get(ModCoreDataComponents.STORAGE_UUID.get());
        if (id == null) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;
        long tick = minecraft.level.getGameTime();
        ResourceKey<Level> dimension = minecraft.level.dimension();
        if (dimension.equals(lastDimension)
                && id.equals(lastId)
                && tick >= lastTick
                && tick - lastTick < REQUEST_PERIOD) return;
        lastDimension = dimension;
        lastId = id;
        lastTick = tick;
        ClientPacketDistributor.sendToServer(new RequestStorageContentsPayload(id));
    }
}
