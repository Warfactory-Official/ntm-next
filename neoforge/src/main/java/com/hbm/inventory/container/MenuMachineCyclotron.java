// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.inventory.slot.SlotUpgrade;
import com.hbm.items.machine.upgrade.ItemMachineUpgrade;
import com.hbm.tileentity.machine.BlockEntityMachineCyclotron;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuMachineCyclotron extends BlockEntityMenu<BlockEntityMachineCyclotron> {

    private final SyncedData data;

    public MenuMachineCyclotron(
            int containerId, Inventory playerInv, BlockEntityMachineCyclotron be) {
        this(
                containerId,
                playerInv,
                be,
                playerInv.player.level().isClientSide()
                        ? SyncedData.client(BlockEntityMachineCyclotron.class)
                        : SyncedData.of(be),
                be);
    }

    private MenuMachineCyclotron(
            int containerId,
            Inventory playerInv,
            Container container,
            SyncedData data,
            BlockEntityMachineCyclotron be) {
        super(ModMenus.MACHINE_CYCLOTRON.get(), containerId, be, container);
        checkContainerSize(container, BlockEntityMachineCyclotron.SLOT_COUNT);
        this.data = data;

        for (int i = 0; i < BlockEntityMachineCyclotron.LINES; i++) {
            addSlot(
                    new Slot(
                            container,
                            BlockEntityMachineCyclotron.SLOT_PARTICLE_START + i,
                            11,
                            18 + i * 18));
            addSlot(
                    new Slot(
                            container,
                            BlockEntityMachineCyclotron.SLOT_TARGET_START + i,
                            101,
                            18 + i * 18));
            addSlot(
                    new SlotRecipeOutput(
                            playerInv.player,
                            container,
                            BlockEntityMachineCyclotron.SLOT_OUTPUT_START + i,
                            131,
                            18 + i * 18));
        }

        addSlot(new Slot(container, BlockEntityMachineCyclotron.SLOT_BATTERY, 168, 83));
        addSlot(new SlotUpgrade(container, BlockEntityMachineCyclotron.SLOT_UPGRADE_START, 60, 81));
        addSlot(new SlotUpgrade(container, BlockEntityMachineCyclotron.SLOT_UPGRADE_END, 78, 81));

        addStandardInventorySlots(playerInv, 15, 133);
        addDataSlots(data);
    }

    public long getPower() {
        return data.get("power");
    }

    public int getProgress() {
        return (int) data.get("progress");
    }

    public int getPowerScaled(int i) {
        return (int) (getPower() * i / BlockEntityMachineCyclotron.MAX_POWER);
    }

    public int getProgressScaled(int i) {
        return getProgress() * i / BlockEntityMachineCyclotron.DURATION;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int machineEnd = BlockEntityMachineCyclotron.SLOT_COUNT;
                    if (index < machineEnd)
                        return moveItemStackTo(stack, machineEnd, slots.size(), true);
                    if (IBatteryItem.isBattery(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineCyclotron.SLOT_BATTERY,
                                BlockEntityMachineCyclotron.SLOT_BATTERY + 1,
                                false);
                    if (ItemMachineUpgrade.isUpgrade(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineCyclotron.SLOT_UPGRADE_START,
                                BlockEntityMachineCyclotron.SLOT_UPGRADE_END + 1,
                                false);
                    return moveItemStackTo(
                            stack,
                            BlockEntityMachineCyclotron.SLOT_PARTICLE_START,
                            BlockEntityMachineCyclotron.SLOT_OUTPUT_START,
                            false);
                });
    }
}
