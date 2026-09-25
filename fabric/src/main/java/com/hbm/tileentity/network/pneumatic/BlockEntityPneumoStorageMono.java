// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.network.pneumatic;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.MenuPneumoStorageMono;
import com.hbm.items.ModDataComponents;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.platform.Services;
import com.hbm.tileentity.IControlReceiverFilter;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntityPneumoStorageMono extends BlockEntityPneumaticStorageBase
        implements IControlReceiverFilter, SyncUnitSchema {

    public static final int SLOT_COUNT = 3;
    public static final int CAPACITY = 100_000;

    @SyncField(units = 1L << 1)
    public int[] amounts;

    public BlockEntityPneumoStorageMono(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PNEUMATIC_STORAGE_MONO.get(), pos, state, SLOT_COUNT);
        this.amounts = new int[SLOT_COUNT];
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.pneumoStorageMono");
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuPneumoStorageMono(containerId, playerInventory, this);
    }

    private void writeAmounts(ByteBuf output) {
        for (int amount : amounts) output.writeInt(amount);
    }

    private void readAmounts(ByteBuf input) {
        for (int i = 0; i < amounts.length; i++) amounts[i] = input.readInt();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        int[] loaded = input.getIntArray("amounts").orElse(null);
        if (loaded != null && loaded.length == SLOT_COUNT) this.amounts = loaded;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putIntArray("amounts", amounts);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {}

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(inventory));
        List<Integer> bulk = new ArrayList<>(amounts.length);
        for (int amount : amounts) bulk.add(amount);
        components.set(ModDataComponents.PNEUMATIC_BULK.get(), bulk);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        components
                .getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                .copyInto(inventory);
        List<Integer> bulk = components.get(ModDataComponents.PNEUMATIC_BULK.get());

        if (bulk != null && bulk.size() == amounts.length) {
            for (int i = 0; i < amounts.length; i++) amounts[i] = bulk.get(i);
        }
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("inventory");
        output.discard("amounts");
    }

    @Override
    public long getAmountAt(int index) {
        return amounts[index];
    }

    @Override
    public boolean allowTypeSetting() {
        return false;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        super.receiveControl(data);

        if (data.contains("slot")) {
            setFilterContents(data);
        }
    }

    @Override
    public long useUpItem(int index, long amount) {
        if (amounts[index] <= 0) return amount;
        int toRemove = (int) Math.min(amount, amounts[index]);
        amounts[index] -= toRemove;
        return amount - toRemove;
    }

    @Override
    public long addItem(int index, long amount) {
        int capacity = CAPACITY - amounts[index];
        if (capacity <= 0) return amount;
        int toAdd = (int) Math.min(amount, capacity);
        amounts[index] += toAdd;
        return amount - toAdd;
    }

    @Override
    public long setupType(int index, ItemStack zeroStack, long amount) {
        return amount;
    }

    @Override
    public void nextMode(int i) {}

    @Override
    public int[] getFilterSlots() {
        return new int[] {0, SLOT_COUNT};
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 1L << 1;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 1 -> writeAmounts(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 1 -> readAmounts(input);
            default -> super.readSyncUnit(unit, input);
        }
    }

    @Override
    public boolean acceptsFilter(ItemStack stack) {
        return Services.PLATFORM.canFitInsideContainerItems(stack);
    }
}
