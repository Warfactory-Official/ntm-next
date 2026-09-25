// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.train;

import net.minecraft.core.NonNullList;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public abstract class EntityRailCarCargo extends EntityRailCarBase
        implements Container, MenuProvider {

    private static final EntityDataAccessor<Integer> OCCUPIED =
            SynchedEntityData.defineId(EntityRailCarCargo.class, EntityDataSerializers.INT);

    private NonNullList<ItemStack> slots;

    public EntityRailCarCargo(EntityType<? extends EntityRailCarCargo> type, Level level) {
        super(type, level);
        this.slots = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(OCCUPIED, 0);
    }

    public int countOccupiedSlots() {
        int occupied = 0;
        for (ItemStack stack : this.slots) if (!stack.isEmpty()) occupied++;
        return occupied;
    }

    public int getOccupiedSlots() {
        return this.entityData.get(OCCUPIED);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) this.entityData.set(OCCUPIED, this.countOccupiedSlots());
    }

    @Override
    public int getContainerSize() {
        return 0;
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.slots) if (!stack.isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.slots.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return ContainerHelper.removeItem(this.slots, slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.slots, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.slots.set(slot, stack);
        stack.limitSize(this.getMaxStackSize(stack));
    }

    @Override
    public void setChanged() {}

    @Override
    public boolean stillValid(Player player) {
        return !this.isRemoved() && player.distanceToSqr(this) <= 64.0D;
    }

    @Override
    public void clearContent() {
        this.slots.clear();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        ContainerHelper.saveAllItems(output, this.slots);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.slots = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.slots);
        this.entityData.set(OCCUPIED, this.countOccupiedSlots());
    }
}
