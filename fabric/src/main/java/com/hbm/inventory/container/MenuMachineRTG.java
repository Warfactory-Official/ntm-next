// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.tileentity.machine.BlockEntityMachineRTG;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuMachineRTG extends BlockEntityMenu<BlockEntityMachineRTG> {

    private final SyncedData data;

    public MenuMachineRTG(int containerId, Inventory playerInv, BlockEntityMachineRTG be) {
        super(ModMenus.MACHINE_RTG.get(), containerId, be);
        checkContainerSize(be, BlockEntityMachineRTG.SLOT_COUNT);
        this.data =
                playerInv.player.level().isClientSide()
                        ? SyncedData.client(BlockEntityMachineRTG.class)
                        : SyncedData.of(be);

        for (int i = 0; i < BlockEntityMachineRTG.SLOT_COUNT; i++) {
            addSlot(new Slot(be, i, 16 + (i % 5) * 18, 18 + (i / 5) * 18));
        }

        addStandardInventorySlots(playerInv, 8, 106);
        addDataSlots(data);
    }

    public int getHeat() {
        return data.getInt("heat");
    }

    public int getHeatScaled(int i) {
        return getHeat() * i / BlockEntityMachineRTG.MAX_HEAT;
    }

    public long getPower() {
        return blockEntity().power;
    }

    public long getPowerScaled(long i) {
        return blockEntity().power * i / BlockEntityMachineRTG.MAX_POWER;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMoveUnsorted(player, index, BlockEntityMachineRTG.SLOT_COUNT);
    }
}
