// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.fabric.mixin.storage;

import com.hbm.fabric.integration.StoredItemTransactions;
import com.hbm.interfaces.StoredItemSlots;
import com.hbm.interfaces.StoredItems;
import java.util.Optional;
import net.minecraft.world.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import reborncore.common.util.RebornInventory;

@Pseudo
@Mixin(targets = "reborncore.common.blockentity.MachineBaseBlockEntity", remap = false)
public abstract class MixinRebornMachine implements StoredItems {
    @Shadow
    public abstract Container getUpgradeInventory();

    @Shadow
    public abstract Optional<RebornInventory<?>> getOptionalInventory();

    @Override
    public void visitStoredItems(Visitor visitor) {
        if (!StoredItemTransactions.isIdle()) return;
        if (this instanceof StoredItemSlots slots && slots.storedSlotCount() == 0) return;
        getOptionalInventory().ifPresent(visitor::visitOwned);
        visitor.visitOwned(getUpgradeInventory());
        if (this instanceof StoredItemSlots slots) visitor.visit(slots);
    }
}
