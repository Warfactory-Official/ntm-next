// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.tileentity.machine.BlockEntityMachineRadiolysis;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuMachineRadiolysis extends BlockEntityMenu<BlockEntityMachineRadiolysis> {

    private final SyncedData data;

    public MenuMachineRadiolysis(
            int containerId, Inventory playerInv, BlockEntityMachineRadiolysis be) {
        super(ModMenus.MACHINE_RADIOLYSIS.get(), containerId, be);
        checkContainerSize(be, BlockEntityMachineRadiolysis.SLOT_COUNT);
        this.data =
                playerInv.player.level().isClientSide()
                        ? SyncedData.client(BlockEntityMachineRadiolysis.class)
                        : SyncedData.of(be);

        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 5; j++) {
                addSlot(new Slot(be, j + i * 5, 188 + i * 18, 8 + j * 18));
            }
        }

        addSlot(new Slot(be, BlockEntityMachineRadiolysis.SLOT_FLUID_ID, 34, 17));
        addSlot(
                new SlotFiltered(
                        be,
                        BlockEntityMachineRadiolysis.SLOT_FLUID_ID_OUT,
                        34,
                        53,
                        SlotFiltered.NONE));
        addSlot(new Slot(be, BlockEntityMachineRadiolysis.SLOT_STERILIZE_IN, 148, 17));
        addSlot(
                new SlotFiltered(
                        be,
                        BlockEntityMachineRadiolysis.SLOT_STERILIZE_OUT,
                        148,
                        53,
                        SlotFiltered.NONE));
        addSlot(new Slot(be, BlockEntityMachineRadiolysis.SLOT_BATTERY, 8, 53));

        addStandardInventorySlots(playerInv, 8, 84);
        addDataSlots(data);
    }

    public int getHeat() {
        return data.getInt("heat");
    }

    public long getPower() {
        return blockEntity().power;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMoveUnsorted(player, index, BlockEntityMachineRadiolysis.SLOT_COUNT);
    }
}
