// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.items.machine.ItemMold.Mold;
import com.hbm.items.machine.ItemMold;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncInventory;
import com.hbm.packet.SyncSlots;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

@SyncSlots(
        value = {0, 1},
        units = 1L << 2,
        components = false)
public abstract class BlockEntityFoundryCastingBase extends BlockEntityFoundryBase
        implements WorldlyContainer {

    private static final int[] ACCESSIBLE_SLOTS = {1};

    @SyncField(units = 1L << 2)
    public final NonNullList<ItemStack> inventory;

    public int cooloff = 100;

    protected BlockEntityFoundryCastingBase(BlockEntityType<?> be, BlockPos pos, BlockState state) {
        this(be, pos, state, 2);
    }

    protected BlockEntityFoundryCastingBase(
            BlockEntityType<?> be, BlockPos pos, BlockState state, int slots) {
        super(be, pos, state);
        this.inventory = SyncInventory.create(this, slots);
    }

    @Override
    public void tickServer() {
        if (this.amount > this.getCapacity()) {
            this.amount = this.getCapacity();
        }

        if (this.amount == 0) {
            this.type = null;
        }

        Mold mold = this.getInstalledMold();

        if (mold != null && this.amount == this.getCapacity() && inventory.get(1).isEmpty()) {
            cooloff--;

            if (cooloff <= 0) {
                this.amount = 0;

                ItemStack out = mold.getOutput(type);
                if (out != null) {
                    inventory.set(1, out.copy());
                }

                cooloff = 200;
                setChanged();
            }
        } else {
            cooloff = 200;
        }

        super.tickServer();
    }

    public @Nullable Mold getInstalledMold() {
        if (inventory.get(0).getItem() instanceof ItemMold item
                && item.mold.size == this.getMoldSize()) {
            return item.mold;
        }
        return null;
    }

    @Override
    public int getCapacity() {
        Mold mold = this.getInstalledMold();
        return mold == null ? 0 : mold.getCost();
    }

    @Override
    public boolean standardCheck(Level level, BlockPos pos, Direction side, MaterialStack stack) {
        if (!super.standardCheck(level, pos, side, stack)) return false;
        if (!this.inventory.get(1).isEmpty()) return false;
        Mold mold = this.getInstalledMold();
        if (mold == null) return false;

        return mold.getOutput(stack.material) != null;
    }

    public abstract int getMoldSize();

    private void writeSlots(ByteBuf buf) {
        serializeMold(buf);
        ItemStack out = inventory.get(1);
        buf.writeInt(out.isEmpty() ? -1 : BuiltInRegistries.ITEM.getId(out.getItem()));
        buf.writeByte(out.getCount());
    }

    private void readSlots(ByteBuf buf) {
        deserializeMold(buf);
        int outId = buf.readInt();
        int outCount = buf.readByte();
        inventory.set(
                1,
                outId == -1
                        ? ItemStack.EMPTY
                        : new ItemStack(BuiltInRegistries.ITEM.byId(outId), outCount));
    }

    protected final void serializeMold(ByteBuf buf) {
        buf.writeInt(
                inventory.get(0).isEmpty()
                        ? -1
                        : BuiltInRegistries.ITEM.getId(inventory.get(0).getItem()));
    }

    protected final void deserializeMold(ByteBuf buf) {
        int moldId = buf.readInt();
        inventory.set(
                0,
                moldId == -1
                        ? ItemStack.EMPTY
                        : new ItemStack(BuiltInRegistries.ITEM.byId(moldId)));
    }

    @Override
    public int getContainerSize() {
        return inventory.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : inventory) if (!stack.isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return inventory.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack removed = ContainerHelper.removeItem(inventory, slot, count);
        if (!removed.isEmpty()) setChanged();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(inventory, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        inventory.set(slot, stack);
        stack.limitSize(getMaxStackSize(stack));
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        inventory.clear();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == 1;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("items").ifPresent(in -> ContainerHelper.loadAllItems(in, inventory));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output.child("items"), inventory);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 1L << 2;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 2 -> writeSlots(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 2 -> readSlots(input);
            default -> super.readSyncUnit(unit, input);
        }
    }
}
