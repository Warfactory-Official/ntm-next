// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.inventory.slot.SlotUpgrade;
import com.hbm.items.machine.upgrade.ItemMachineUpgrade;
import com.hbm.tileentity.machine.BlockEntityMachineMiningLaser;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuMachineMiningLaser extends BlockEntityMenu<BlockEntityMachineMiningLaser> {

    private final SyncedData data;

    public MenuMachineMiningLaser(
            int containerId, Inventory playerInv, BlockEntityMachineMiningLaser be) {
        this(
                containerId,
                playerInv,
                be,
                playerInv.player.level().isClientSide()
                        ? SyncedData.client(BlockEntityMachineMiningLaser.class)
                        : SyncedData.of(be),
                be);
    }

    private MenuMachineMiningLaser(
            int containerId,
            Inventory playerInv,
            Container container,
            SyncedData data,
            BlockEntityMachineMiningLaser be) {
        super(ModMenus.MACHINE_MINING_LASER.get(), containerId, be, container);
        checkContainerSize(container, BlockEntityMachineMiningLaser.SLOT_COUNT);
        this.data = data;

        addSlot(new Slot(container, BlockEntityMachineMiningLaser.SLOT_BATTERY, 8, 108));

        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 4; col++) {
                addSlot(
                        new SlotUpgrade(
                                container,
                                BlockEntityMachineMiningLaser.SLOT_UPGRADE_START + row * 4 + col,
                                98 + col * 18,
                                18 + row * 18));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 7; col++) {
                addSlot(
                        new Slot(
                                container,
                                BlockEntityMachineMiningLaser.SLOT_OUTPUT_START + row * 7 + col,
                                44 + col * 18,
                                72 + row * 18));
            }
        }

        addStandardInventorySlots(playerInv, 8, 140);
        addDataSlots(data);
    }

    public long getPower() {
        return data.get("power");
    }

    public int getPowerScaled(int i) {
        return (int) (getPower() * i / BlockEntityMachineMiningLaser.MAX_POWER);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int machineEnd = BlockEntityMachineMiningLaser.SLOT_COUNT;
                    if (index < machineEnd)
                        return moveItemStackTo(stack, machineEnd, slots.size(), true);
                    if (IBatteryItem.isBattery(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineMiningLaser.SLOT_BATTERY,
                                BlockEntityMachineMiningLaser.SLOT_BATTERY + 1,
                                false);
                    if (ItemMachineUpgrade.isUpgrade(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineMiningLaser.SLOT_UPGRADE_START,
                                BlockEntityMachineMiningLaser.SLOT_UPGRADE_END + 1,
                                false);

                    return false;
                });
    }
}
