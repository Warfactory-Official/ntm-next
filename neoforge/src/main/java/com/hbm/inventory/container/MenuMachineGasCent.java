// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.inventory.slot.SlotUpgrade;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.items.machine.upgrade.ItemMachineUpgrade;
import com.hbm.tileentity.machine.BlockEntityMachineGasCent;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MenuMachineGasCent extends BlockEntityMenu<BlockEntityMachineGasCent> {

    private final SyncedData data;

    public MenuMachineGasCent(int containerId, Inventory playerInv, BlockEntityMachineGasCent be) {
        this(
                containerId,
                playerInv,
                be,
                playerInv.player.level().isClientSide()
                        ? SyncedData.client(BlockEntityMachineGasCent.class)
                        : SyncedData.of(be),
                be);
    }

    private MenuMachineGasCent(
            int containerId,
            Inventory playerInv,
            Container container,
            SyncedData data,
            BlockEntityMachineGasCent be) {
        super(ModMenus.MACHINE_GASCENT.get(), containerId, be, container);
        checkContainerSize(container, BlockEntityMachineGasCent.SLOT_COUNT);
        this.data = data;

        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                addSlot(
                        new SlotRecipeOutput(
                                playerInv.player, container, j + i * 2, 71 + j * 18, 53 + i * 18));
            }
        }

        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityMachineGasCent.SLOT_BATTERY,
                        182,
                        71,
                        IBatteryItem::isBattery));
        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityMachineGasCent.SLOT_FLUID_ID,
                        91,
                        15,
                        stack -> stack.getItem() instanceof FluidIdentifierItem));
        addSlot(new SlotUpgrade(container, BlockEntityMachineGasCent.SLOT_UPGRADE, 69, 15));

        addStandardInventorySlots(playerInv, 8, 122);
        addDataSlots(data);
    }

    public long getPower() {
        return data.get("power");
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    int machineEnd = BlockEntityMachineGasCent.SLOT_COUNT;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (IBatteryItem.isBattery(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineGasCent.SLOT_BATTERY,
                                BlockEntityMachineGasCent.SLOT_BATTERY + 1,
                                false);
                    if (stack.getItem() instanceof FluidIdentifierItem)
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineGasCent.SLOT_FLUID_ID,
                                BlockEntityMachineGasCent.SLOT_FLUID_ID + 1,
                                false);
                    if (ItemMachineUpgrade.isUpgrade(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineGasCent.SLOT_UPGRADE,
                                BlockEntityMachineGasCent.SLOT_UPGRADE + 1,
                                false);
                    return false;
                });
    }
}
