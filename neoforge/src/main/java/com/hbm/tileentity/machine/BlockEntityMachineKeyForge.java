// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.MenuMachineKeyForge;
import com.hbm.items.tool.ItemKeyPin;
import com.hbm.tileentity.BlockEntityMachineBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class BlockEntityMachineKeyForge extends BlockEntityMachineBase implements MenuProvider {

    public static final int SLOT_SOURCE = 0;
    public static final int SLOT_COPY = 1;
    public static final int SLOT_RANDOM = 2;
    public static final int SLOT_COUNT = 3;

    private static final int[] SLOTS_TOP = {SLOT_SOURCE};
    private static final int[] SLOTS_BOTTOM = {SLOT_COPY};
    private static final int[] SLOTS_SIDE = {SLOT_RANDOM};

    private @Nullable ItemStack rolled;

    public BlockEntityMachineKeyForge(BlockPos pos, BlockState state) {
        super(ModBlockEntities.KEY_FORGE.get(), pos, state, SLOT_COUNT);
    }

    @Override
    protected double interactionRangeSq() {
        return 64;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.keyForge");
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide()) forge();
    }

    private void forge() {
        boolean written = false;
        ItemStack source = inventory.get(SLOT_SOURCE);
        ItemStack copy = inventory.get(SLOT_COPY);
        if (source.getItem() instanceof ItemKeyPin sourceKey
                && copy.getItem() instanceof ItemKeyPin copyKey
                && sourceKey.canTransfer()
                && copyKey.canTransfer()) {
            int pins = ItemKeyPin.getPins(source);
            if (ItemKeyPin.getPins(copy) != pins) {
                ItemKeyPin.setPins(copy, pins);
                written = true;
            }
        }

        ItemStack random = inventory.get(SLOT_RANDOM);
        if (random.isEmpty()) {
            rolled = null;
        } else if (random != rolled
                && random.getItem() instanceof ItemKeyPin key
                && key.canTransfer()) {
            rolled = random;
            ItemKeyPin.setPins(random, level.getRandom().nextInt(900) + 100);
            written = true;
        }

        if (written) super.setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.UP
                ? SLOTS_TOP
                : side == Direction.DOWN ? SLOTS_BOTTOM : SLOTS_SIDE;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return true;
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuMachineKeyForge(containerId, playerInventory, this);
    }
}
