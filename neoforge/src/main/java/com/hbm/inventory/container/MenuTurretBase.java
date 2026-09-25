// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.items.ModItems;
import com.hbm.tileentity.turret.BlockEntityTurretBaseNT;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuTurretBase extends BlockEntityMenu<BlockEntityTurretBaseNT> {

    public MenuTurretBase(int containerId, Inventory playerInventory, BlockEntityTurretBaseNT be) {
        super(ModMenus.TURRET_BASE.get(), containerId, be);
        checkContainerSize(be, BlockEntityTurretBaseNT.SLOT_COUNT);
        be.startOpen(playerInventory.player);

        addSlot(new Slot(be, BlockEntityTurretBaseNT.SLOT_CHIP, 98, 27));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(
                        new Slot(
                                be,
                                BlockEntityTurretBaseNT.SLOT_AMMO_FIRST + row * 3 + col,
                                80 + col * 18,
                                63 + row * 18));
            }
        }

        addSlot(new Slot(be, BlockEntityTurretBaseNT.SLOT_BATTERY, 152, 99));

        addStandardInventorySlots(playerInventory, 8, 140);
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity().stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        blockEntity().stopOpen(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int turretEnd = BlockEntityTurretBaseNT.SLOT_COUNT;

        if (index < turretEnd) {
            if (!moveItemStackTo(stack, turretEnd, slots.size(), true)) return ItemStack.EMPTY;
        } else if (stack.is(ModItems.TURRET_CHIP.get())) {
            if (!moveItemStackTo(
                    stack,
                    BlockEntityTurretBaseNT.SLOT_CHIP,
                    BlockEntityTurretBaseNT.SLOT_AMMO_FIRST,
                    false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(
                stack, BlockEntityTurretBaseNT.SLOT_AMMO_FIRST, turretEnd, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onQuickCraft(stack, original);
        slot.onTake(player, stack);
        return original;
    }
}
