// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory;

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
    }
}
