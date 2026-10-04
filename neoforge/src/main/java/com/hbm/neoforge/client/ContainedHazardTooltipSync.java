// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.client;

import com.hbm.config.BalanceConfig;
import com.hbm.hazard.transformer.HazardTransformerRadiationContainer;
import com.hbm.inventory.ItemContents;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.neoforged.fml.ModList;

final class ContainedHazardTooltipSync {
    private static final boolean STORAGE = ModList.get().isLoaded("sophisticatedstorage");
    private static final boolean BACKPACKS = ModList.get().isLoaded("sophisticatedbackpacks");
    private static final ObjectOpenHashSet<UUID> visitedStorage = new ObjectOpenHashSet<>();
    private static final ObjectOpenHashSet<UUID> visitedBackpacks = new ObjectOpenHashSet<>();

    private ContainedHazardTooltipSync() {}

    static void request(ItemStack stack) {
        if (!(STORAGE || BACKPACKS) || stack.isEmpty()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || !hasContents(stack)) return;
        assert minecraft.isSameThread();
        visitedStorage.clear();
        visitedBackpacks.clear();
        try {
            requestStoredItem(stack);
        } finally {
            visitedStorage.clear();
            visitedBackpacks.clear();
        }
    }

    static void reset() {
        HazardTooltipRequests.reset();
    }

    private static boolean hasContents(ItemStack stack) {
        return ItemContents.hasContents(stack, true, containerVisible(stack))
                || STORAGE && SophisticatedStorageHazardTooltipSync.contentsId(stack) != null
                || BACKPACKS && SophisticatedBackpackHazardTooltipSync.contentsId(stack) != null;
    }

    private static final ItemContents.Reader REQUEST =
            (item, quantity) -> {
                if (quantity > 0)
                    requestStoredItem(
                            item instanceof ItemStack stack
                                    ? stack
                                    : ((ItemStackTemplate) item).create());
            };

    private static void requestStoredItem(ItemStack stack) {
        if (stack.isEmpty() || ItemContents.radiationShielded(stack)) return;
        if (STORAGE) {
            for (ItemStack held :
                    SophisticatedStorageHazardTooltipSync.requestNested(stack, visitedStorage)) {
                if (!held.isEmpty()) requestStoredItem(held);
            }
        }
        if (BACKPACKS) {
            for (ItemStack held :
                    SophisticatedBackpackHazardTooltipSync.requestNested(stack, visitedBackpacks)) {
                if (!held.isEmpty()) requestStoredItem(held);
            }
        }
        ItemContents.visit(stack, null, REQUEST, true, containerVisible(stack));
    }

    private static boolean containerVisible(ItemStack stack) {
        return !BalanceConfig.enableLBSM
                || !BalanceConfig.enableLBSMSafeCrates
                || !HazardTransformerRadiationContainer.ownsContainer(stack);
    }
}
