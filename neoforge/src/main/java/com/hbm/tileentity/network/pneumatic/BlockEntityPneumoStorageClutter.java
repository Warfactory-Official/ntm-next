// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.network.pneumatic;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.data.ItemData;
import com.hbm.inventory.container.MenuPneumoStorageClutter;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntityPneumoStorageClutter extends BlockEntityPneumaticStorageBase
        implements SyncUnitSchema {

    public static final int SLOT_COUNT = 6 * 9;

    public BlockEntityPneumoStorageClutter(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PNEUMATIC_STORAGE_CLUTTER.get(), pos, state, SLOT_COUNT);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.pneumoStorageClutter");
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuPneumoStorageClutter(containerId, playerInventory, this);
    }

    @Override
    public long getAmountAt(int index) {
        return getSlotAt(index).getCount();
    }

    @Override
    public boolean allowTypeSetting() {
        return true;
    }

    @Override
    public long useUpItem(int index, long amount) {
        ItemStack stack = inventory.get(index);
        if (!stack.isEmpty()) {
            int toRemove = (int) Math.min(stack.getCount(), amount);
            this.removeItem(index, toRemove);
            return amount - toRemove;
        }
        return amount;
    }

    @Override
    public long addItem(int index, long amount) {
        ItemStack stack = inventory.get(index);
        if (!stack.isEmpty()) {
            int capacity = this.getMaxStackSize(stack);
            int toAdd = (int) Math.min(amount, capacity - stack.getCount());
            stack.grow(toAdd);
            return amount - toAdd;
        }
        return amount;
    }

    @Override
    public long setupType(int index, ItemStack zeroStack, long amount) {
        int capacity = this.getMaxStackSize(zeroStack);
        int finalSize = (int) Math.min(amount, capacity);
        inventory.set(index, zeroStack.copyWithCount(finalSize));
        return amount - finalSize;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (!ItemData.CRATE_KEEP_CONTENTS.get()) super.preRemoveSideEffects(pos, state);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (ItemData.CRATE_KEEP_CONTENTS.get()) {
            components.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(inventory));
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        components
                .getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                .copyInto(inventory);
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("inventory");
    }
}
