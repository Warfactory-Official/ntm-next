// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.network;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.capability.NtmContracts;
import com.hbm.inventory.container.MenuRadioTorchCounter;
import com.hbm.module.ModulePatternMatcher;
import com.hbm.packet.SyncBindings;
import com.hbm.packet.SyncField;
import com.hbm.tileentity.IControlReceiverFilter;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public final class BlockEntityRadioTorchCounter extends BlockEntityRadioTorch
        implements WorldlyContainer, MenuProvider, IControlReceiverFilter {

    public static final int FILTER_COUNT = 3;

    @SyncField(units = 0x700L)
    public final String[] channels = new String[FILTER_COUNT];

    @SyncField(units = 0x70L)
    public final int[] lastCount = new int[FILTER_COUNT];

    private final boolean[] forceUpdate = new boolean[FILTER_COUNT];
    public final NonNullList<ItemStack> filters =
            NonNullList.withSize(FILTER_COUNT, ItemStack.EMPTY);

    @SyncField(units = 1L << 7)
    public final ModulePatternMatcher matcher = new ModulePatternMatcher(FILTER_COUNT);

    public BlockEntityRadioTorchCounter(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RTTY_COUNTER.get(), pos, state, NtmContracts.INVENTORY);
        Arrays.fill(channels, "");
    }

    @Override
    public void tickServer() {
        Container inventory = attached(Container.class);
        if (inventory != null) {
            for (int i = 0; i < FILTER_COUNT; i++) {
                if (channels[i].isEmpty() || filters.get(i).isEmpty()) continue;
                int count = 0;
                for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                    ItemStack stack = inventory.getItem(slot);
                    if (!stack.isEmpty() && matcher.isValidForFilter(filters.get(i), i, stack)) {
                        count += stack.getCount();
                    }
                }
                if (polling || forceUpdate[i] || lastCount[i] != count) {
                    RTTYSystem.broadcast(level, channels[i], count);
                    forceUpdate[i] = false;
                }
                lastCount[i] = count;
            }
        }
        networkPackNT(15);
    }

    @Override
    protected void loadRadio(ValueInput input) {
        polling = input.getBooleanOr("p", false);
        input.child("inventory")
                .ifPresent(inventory -> ContainerHelper.loadAllItems(inventory, filters));
        for (int i = 0; i < FILTER_COUNT; i++) {
            channels[i] = input.getStringOr("c" + i, "");
            lastCount[i] = input.getIntOr("l" + i, 0);
        }
        matcher.load(input);
    }

    @Override
    protected void saveRadio(ValueOutput output) {
        output.putBoolean("p", polling);
        ContainerHelper.saveAllItems(output.child("inventory"), filters);
        for (int i = 0; i < FILTER_COUNT; i++) {
            output.putString("c" + i, channels[i]);
            output.putInt("l" + i, lastCount[i]);
        }
        matcher.save(output);
    }

    @Override
    protected void receiveRadioControl(CompoundTag data) {
        if (data.contains("polling")) {
            polling = !polling;
        } else {
            for (int i = 0; i < FILTER_COUNT; i++) {
                String next = data.getStringOr("c" + i, channels[i]);
                if (!next.equals(channels[i])) {
                    channels[i] = next;
                    forceUpdate[i] = true;
                }
            }
        }
        if (data.contains("slot")) setFilterContents(data);
    }

    @Override
    public void nextMode(int i) {
        matcher.nextMode(level, getItem(i), i);
        forceUpdate[i] = true;
    }

    @Override
    public int[] getFilterSlots() {
        return new int[] {0, FILTER_COUNT};
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.getEyePosition()
                        .distanceToSqr(
                                worldPosition.getX() + 0.5D,
                                worldPosition.getY() + 0.5D,
                                worldPosition.getZ() + 0.5D)
                <= 128D;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {}

    @Override
    public int getContainerSize() {
        return FILTER_COUNT;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : filters) if (!stack.isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return filters.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(filters, slot, amount);
        if (!removed.isEmpty()) setChanged();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(filters, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        filters.set(slot, stack);
        stack.limitSize(1);
        forceUpdate[slot] = true;
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return new int[0];
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    @Override
    public void clearContent() {
        filters.clear();
        Arrays.fill(forceUpdate, true);
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.rttyCounter");
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuRadioTorchCounter(containerId, playerInventory, this);
    }

    private void writeCounts(int group, ByteBuf output) {
        output.writeInt(lastCount[group]);
    }

    private void readCounts(int group, ByteBuf input) {
        lastCount[group] = input.readInt();
    }

    private void writeChannels(int group, ByteBuf output) {
        writeString(output, channels[group]);
    }

    private void readChannels(int group, ByteBuf input) {
        channels[group] = readString(input);
    }

    @Override
    public long syncUnitMask() {
        return (super.syncUnitMask() | 0x7f0L) & ~0xeL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        if (unit >= 4 && unit < 7) writeCounts(unit - 4, output);
        else if (unit == 7) matcher.serialize(output);
        else if (unit >= 8 && unit < 11) writeChannels(unit - 8, output);
        else super.writeSyncUnit(unit, output);
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        if (unit >= 4 && unit < 7) readCounts(unit - 4, input);
        else if (unit == 7) matcher.deserialize(input);
        else if (unit >= 8 && unit < 11) readChannels(unit - 8, input);
        else super.readSyncUnit(unit, input);
    }

    @Override
    public void bindSyncValue(Object value, int flags, long units) {
        if (value == this.channels) {
            SyncBindings.bindIndexed(this, value, flags, 8, 3, 0L);
            return;
        }
        if (value == this.lastCount) {
            SyncBindings.bindIndexed(this, value, flags, 4, 3, 0L);
            return;
        }
        super.bindSyncValue(value, flags, units);
    }

    @Override
    public void syncArrayChanged(Object value, int index, int flags, long units) {
        if (value == channels) {
            long selected = index < 0 ? 0x700L : 1L << (8 + index);
            if (index >= 0 && syncBound())
                SyncBindings.bindUnits(this, channels[index], flags, selected);
            syncUnitsChanged(flags, selected);
            return;
        }
        if (value == lastCount) {
            long selected = index < 0 ? 0x70L : 1L << (4 + index);
            syncUnitsChanged(flags, selected);
            return;
        }
        super.syncArrayChanged(value, index, flags, units);
    }
}
