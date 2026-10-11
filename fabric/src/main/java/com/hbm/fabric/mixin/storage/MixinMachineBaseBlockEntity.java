// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.fabric.mixin.storage;

import com.hbm.fabric.integration.StoredItemTransactions;
import com.hbm.interfaces.StoredItemSlots;
import com.hbm.interfaces.StoredItems;
import com.hbm.inventory.InventoryChanges;
import com.hbm.inventory.SlotSource;
import com.hbm.inventory.TrackedStorage;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import reborncore.common.blockentity.MachineBaseBlockEntity;
import reborncore.common.util.RebornInventory;

@Mixin(value = MachineBaseBlockEntity.class, remap = false)
public abstract class MixinMachineBaseBlockEntity implements StoredItems, TrackedStorage {
    @Shadow
    public abstract Container getUpgradeInventory();

    @Shadow
    public abstract Optional<RebornInventory<?>> getOptionalInventory();

    @Unique private Object hbm$main;
    @Unique private Object hbm$upgrades;

    @Override
    public void inventorySources(Consumer<Object> sources) {
        if (this instanceof SlotSource slots && slots.storedSlotCount() == 0) return;
        hbm$main = getOptionalInventory().orElse(null);
        hbm$upgrades = getUpgradeInventory();
        if (hbm$main != null) sources.accept(hbm$main);
        sources.accept(hbm$upgrades);
        if (this instanceof SlotSource) sources.accept(this);
    }

    @Override
    public void refreshInventorySources() {
        if (this instanceof SlotSource slots && slots.storedSlotCount() == 0) return;
        if (hbm$main != getOptionalInventory().orElse(null)
                || hbm$upgrades != getUpgradeInventory())
            InventoryChanges.storageLayoutChanged((BlockEntity) (Object) this);
    }

    @Override
    public void visitStoredItems(Visitor visitor) {
        if (!StoredItemTransactions.isIdle()) return;
        if (this instanceof StoredItemSlots slots && slots.storedSlotCount() == 0) return;
        getOptionalInventory().ifPresent(visitor::visitOwned);
        visitor.visitOwned(getUpgradeInventory());
        if (this instanceof StoredItemSlots slots) visitor.visit(slots);
    }
}
