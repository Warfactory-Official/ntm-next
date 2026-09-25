// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.items.machine.ItemBlades;
import com.hbm.tileentity.machine.BlockEntityMachineShredder;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuMachineShredder extends NtmContainerMenu {

    private final SyncedData data;

    public MenuMachineShredder(int containerId, Inventory playerInv) {
        this(
                containerId,
                playerInv,
                new SimpleContainer(BlockEntityMachineShredder.SLOT_COUNT),
                SyncedData.client(BlockEntityMachineShredder.class));
    }

    public MenuMachineShredder(
            int containerId, Inventory playerInv, BlockEntityMachineShredder be) {
        this(containerId, playerInv, be, SyncedData.of(be));
    }

    private MenuMachineShredder(
            int containerId, Inventory playerInv, Container container, SyncedData data) {
        super(ModMenus.MACHINE_SHREDDER.get(), containerId, container);
        checkContainerSize(container, BlockEntityMachineShredder.SLOT_COUNT);
        this.data = data;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int index = BlockEntityMachineShredder.SLOT_INPUT_START + col + row * 3;
                addSlot(new Slot(container, index, 44 + col * 18, 18 + row * 18));
            }
        }
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 3; col++) {
                int index = BlockEntityMachineShredder.SLOT_OUTPUT_START + col + row * 3;
                addSlot(
                        new SlotRecipeOutput(
                                playerInv.player, container, index, 116 + col * 18, 18 + row * 18));
            }
        }

        addSlot(new Slot(container, BlockEntityMachineShredder.SLOT_BLADE_LEFT, 44, 108));
        addSlot(new Slot(container, BlockEntityMachineShredder.SLOT_BLADE_RIGHT, 80, 108));
        addSlot(new Slot(container, BlockEntityMachineShredder.SLOT_BATTERY, 8, 108));

        addStandardInventorySlots(playerInv, 8, 151);
        addDataSlots(data);
    }

    public long getPower() {
        return data.get("power");
    }

    public int getProgress() {
        return data.getInt("progress");
    }

    public int getProgressScaled(int i) {
        return getProgress() * i / BlockEntityMachineShredder.PROCESSING_SPEED;
    }

    public long getPowerScaled(long i) {
        return getPower() * i / BlockEntityMachineShredder.MAX_POWER;
    }

    public int getGearLeft() {
        return BlockEntityMachineShredder.gearOf(
                getSlot(BlockEntityMachineShredder.SLOT_BLADE_LEFT).getItem());
    }

    public int getGearRight() {
        return BlockEntityMachineShredder.gearOf(
                getSlot(BlockEntityMachineShredder.SLOT_BLADE_RIGHT).getItem());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    int machineEnd = BlockEntityMachineShredder.SLOT_COUNT;
                    int invEnd = slots.size();

                    if (index < machineEnd) {
                        if (!moveItemStackTo(stack, machineEnd, invEnd, true)) return false;

                    } else if (IBatteryItem.isBattery(stack)) {
                        if (!moveItemStackTo(
                                stack,
                                BlockEntityMachineShredder.SLOT_BATTERY,
                                machineEnd,
                                false)) {
                            return false;
                        }
                    } else if (stack.getItem() instanceof ItemBlades) {
                        if (!moveItemStackTo(
                                stack,
                                BlockEntityMachineShredder.SLOT_BLADE_LEFT,
                                BlockEntityMachineShredder.SLOT_BATTERY,
                                false)) {
                            return false;
                        }
                    } else {
                        if (!moveItemStackTo(
                                stack,
                                BlockEntityMachineShredder.SLOT_INPUT_START,
                                BlockEntityMachineShredder.SLOT_INPUT_END,
                                false)) {
                            return false;
                        }
                    }
                    return true;
                });
    }
}
