// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory;

import com.hbm.handler.UnstableFuses;
import com.hbm.hazard.HazardSystem;
import com.hbm.platform.FabricInventoryPlatform;
import java.util.ArrayList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class InventoryChanges {
    public static final int LAYOUT = Integer.MIN_VALUE;

    private InventoryChanges() {}

    public static Object owner(Object source) {
        return InventorySlotAccess.of(source).owner(source);
    }

    public static void slotChanged(Object source, int slot) {
        InventorySlotAccess.of(source).changed(source, slot);
    }

    public static void publishedStack(Object source, int slot, ItemStack stack) {
        InventorySlotBinding.bind(stack, source, slot);
        slotChanged(source, slot);
    }

    public static void layoutChanged(Object source) {
        slotChanged(source, LAYOUT);
    }

    public static void unbindSource(Object source) {
        InventorySlotAccess access = InventorySlotAccess.of(source);
        Object owner = access.owner(source);
        if (owner == null) return;
        UnstableFuses.removeSource(owner, source);
        for (int slot = 0, count = access.size(source); slot < count; slot++)
            InventorySlotBinding.unbind(access.read(source, slot), source, slot);
        access.bind(source, null);
    }

    public static void arraySlotChanged(Object source, Object array, int slot) {
        if (((ArraySlotSource) source).inventorySlotArray() == array) slotChanged(source, slot);
    }

    public static void fixedOwner(Object expected, Object owner) {
        assert owner == null || owner == expected;
    }

    public static void listReplaced(Object source) {
        if (!(source instanceof SlotSource slots)) return;
        Object owner = slots.inventoryOwner();
        if (owner == null || owner instanceof BlockEntity block && sources(block) == null) return;
        slots.refreshInventorySlots();
    }

    public static void equipmentChanged(LivingEntity owner, EquipmentSlot slot, ItemStack stack) {
        if (owner instanceof ServerPlayer player) {
            int inventorySlot =
                    switch (slot) {
                        case MAINHAND -> player.getInventory().getSelectedSlot();
                        case OFFHAND -> Inventory.SLOT_OFFHAND;
                        case FEET -> 36;
                        case LEGS -> 37;
                        case CHEST -> 38;
                        case HEAD -> 39;
                        case BODY -> Inventory.SLOT_BODY_ARMOR;
                        case SADDLE -> Inventory.SLOT_SADDLE;
                    };
            publishedStack(player.getInventory(), inventorySlot, stack);
        } else publishedStack(owner, slot.ordinal(), stack);
    }

    public static void blockChanged(BlockEntity owner) {
        if (sources(owner) != null) ((TrackedStorage) owner).refreshInventorySources();
    }

    public static void storageLayoutChanged(BlockEntity owner) {
        if (sources(owner) != null) bindBlock(owner);
    }

    public static void bindBlock(BlockEntity owner) {
        if (!BlockStorage.supported(owner)) return;
        unbindBlock(owner);
        var collected = new ArrayList<Object>();
        ((TrackedStorage) owner)
                .inventorySources(
                        source -> {
                            for (Object previous : collected)
                                if (previous == source)
                                    throw new IllegalStateException(
                                            "Duplicate inventory source: " + owner.getBlockPos());
                            InventorySlotAccess.of(source).bind(source, owner);
                            collected.add(source);
                        });
        Object sources = collected.size() == 1 ? collected.getFirst() : collected.toArray();
        sources(owner, sources);
        UnstableFuses.rebuildBlock(owner);
    }

    public static void unbindBlock(BlockEntity owner) {
        Object sources = sources(owner);
        sources(owner, null);
        if (sources == null) return;
        if (sources instanceof Object[] multiple) {
            for (Object source : multiple) release(source, owner);
        } else release(sources, owner);
    }

    private static void release(Object source, BlockEntity owner) {
        if (InventorySlotAccess.of(source).owner(source) == owner)
            InventorySlotAccess.of(source).bind(source, null);
    }

    public static Object sources(BlockEntity owner) {
        return __asm__(Object) {
            aload owner;
            getfield "net/minecraft/world/level/block/entity/BlockEntity" "hbm$inventorySources" "Ljava/lang/Object;";
        };
    }

    private static void sources(BlockEntity owner, Object sources) {
        __asm__ {
            aload owner;
            aload sources;
            putfield "net/minecraft/world/level/block/entity/BlockEntity" "hbm$inventorySources" "Ljava/lang/Object;";
        }
    }

    public static void bindPlayer(ServerPlayer owner) {
        Inventory inventory = owner.getInventory();
        if (!(inventory.getNonEquipmentItems() instanceof StoredInventory))
            inventory.items = StoredInventory.track(inventory, inventory.getNonEquipmentItems());
    }

    public static void bindMenu(AbstractContainerMenu menu, ServerPlayer owner) {
        SlotSource declared = FabricInventoryPlatform.menuInventory(menu);
        if (declared != null) {
            InventorySlotAccess.of(declared).bind(declared, owner);
            layoutChanged(declared);
        }
        Object previous = null;
        for (Slot slot : menu.slots) {
            Object source = slot.container;
            if (source == previous) continue;
            previous = source;
            InventorySlotAccess access = InventorySlotAccess.of(source);
            if (access == InventorySlotAccess.SIMPLE && access.size(source) == 0) continue;
            Object physicalOwner = access.owner(source);
            if (access == InventorySlotAccess.SIMPLE && physicalOwner == null
                    || physicalOwner == owner
                            && (access == InventorySlotAccess.DECLARED
                                    || access == InventorySlotAccess.ITEM_VIEW))
                access.bind(source, owner);
        }
    }

    public static void unbindMenu(AbstractContainerMenu menu) {
        SlotSource declared = FabricInventoryPlatform.menuInventory(menu);
        if (declared != null) unbindSource(declared);
    }

    public record MenuWatcher(ServerPlayer player) implements ContainerListener {
        @Override
        public void slotChanged(AbstractContainerMenu menu, int slotIndex, ItemStack stack) {
            Slot slot = menu.getSlot(slotIndex);
            HazardSystem.onSlotChanged(player, slot, stack);
            UnstableFuses.menuSlotChanged(player, slot);
        }

        @Override
        public void dataChanged(AbstractContainerMenu menu, int id, int value) {}
    }
}
