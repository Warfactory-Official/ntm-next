// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

public final class InventoryPublications {
    @FunctionalInterface
    public interface Notifications {
        void afterChanges(Runnable notification);
    }

    private InventoryPublications() {}

    public static void afterReplacement(
            InventorySlotAccess access,
            Object source,
            int slot,
            Object owner,
            @Nullable Object carrier,
            Notifications visitor) {
        visitor.afterChanges(
                () -> {
                    if (owner instanceof ItemInteriorBackend backend)
                        backend.publishStoredChanges((SlotSource) source, slot);
                    else {
                        if (access.owner(source) != owner
                                || !InventoryOwnerAccess.of(owner).live(owner)) return;
                        access.publish(source, slot);
                    }
                    if (carrier instanceof BlockEntity block
                            && InventoryOwnerAccess.BLOCK.live(block))
                        InventoryOwnerAccess.BLOCK.publish(block);
                });
        if (carrier instanceof ServerPlayer player) {
            AbstractContainerMenu menu = player.containerMenu;
            ItemStackContainer previous = null;
            for (Slot visible : menu.slots)
                if (visible.container instanceof ItemStackContainer mirror
                        && mirror != previous
                        && (mirror.target().isEmpty()
                                || mirror.target().has(DataComponents.CONTAINER))) {
                    previous = mirror;
                    visitor.afterChanges(
                            () -> {
                                if (player.containerMenu == menu) mirror.refreshFromTarget();
                            });
                }
        }
    }
}
