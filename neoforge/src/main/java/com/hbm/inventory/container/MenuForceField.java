// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.data.MachineData;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.items.ModItems;
import com.hbm.tileentity.machine.BlockEntityForceField;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MenuForceField extends BlockEntityMenu<BlockEntityForceField> {

    private final SyncedData data;

    public MenuForceField(int containerId, Inventory playerInv, BlockEntityForceField be) {
        this(
                containerId,
                playerInv,
                be,
                playerInv.player.level().isClientSide()
                        ? SyncedData.client(BlockEntityForceField.class)
                        : SyncedData.of(be),
                be);
    }

    private MenuForceField(
            int containerId,
            Inventory playerInv,
            Container container,
            SyncedData data,
            BlockEntityForceField be) {
        super(ModMenus.FORCEFIELD.get(), containerId, be, container);
        checkContainerSize(container, BlockEntityForceField.SLOT_COUNT);
        this.data = data;

        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityForceField.SLOT_BATTERY,
                        26,
                        53,
                        IBatteryItem::isBattery));
        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityForceField.SLOT_RADIUS,
                        89,
                        35,
                        stack -> stack.is(ModItems.UPGRADE_RADIUS.get())));
        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityForceField.SLOT_HEALTH,
                        107,
                        35,
                        stack -> stack.is(ModItems.UPGRADE_HEALTH.get())));

        addStandardInventorySlots(playerInv, 8, 84);
        addDataSlots(data);
    }

    public long getPower() {
        return data.get("power");
    }

    public int getPowerScaled(int i) {
        return (int) (getPower() * i / MachineData.FORCE_FIELD_MAX_POWER.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int machineEnd = BlockEntityForceField.SLOT_COUNT;
                    if (index < machineEnd)
                        return moveItemStackTo(stack, machineEnd, slots.size(), true);
                    if (moveItemStackTo(
                            stack,
                            BlockEntityForceField.SLOT_RADIUS,
                            BlockEntityForceField.SLOT_HEALTH + 1,
                            false)) {
                        return true;
                    }
                    return moveItemStackTo(
                            stack,
                            BlockEntityForceField.SLOT_BATTERY,
                            BlockEntityForceField.SLOT_BATTERY + 1,
                            false);
                });
    }
}
