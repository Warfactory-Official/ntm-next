/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.blockentity.network.pneumatic;

import com.hbm.blockentity.GhostFilters;
import com.hbm.blockentity.network.pneumatic.PneumaticStorageBankBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.PneumoStorageMonoMenu;
import com.hbm.items.ModDataComponents;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.platform.Platform;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class PneumoStorageMonoBlockEntity extends PneumaticStorageBankBlockEntity
        implements GhostFilters, SyncUnitSchema {

    public static final int SLOT_COUNT = 3;
    private static final GhostFilters.Window FILTERS = new GhostFilters.Window(0, SLOT_COUNT);
    public static final int CAPACITY = 100_000;

    public int[] amounts;

    public PneumoStorageMonoBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PNEUMATIC_STORAGE_MONO.get(), pos, state, SLOT_COUNT);
        this.hbm$syncSet$PneumoStorageMonoBlockEntity$amounts(new int[SLOT_COUNT]);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.pneumoStorageMono");
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new PneumoStorageMonoMenu(containerId, playerInventory, this);
    }

    private void writeAmounts(ByteBuf output) {
        for (int amount : amounts) output.writeInt(amount);
    }

    private void readAmounts(ByteBuf input) {
        for (int i = 0; i < amounts.length; i++)
            hbm$syncArraySet$PneumoStorageMonoBlockEntity$amounts(amounts, i, input.readInt());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        int[] loaded = input.getIntArray("amounts").orElse(null);
        if (loaded != null && loaded.length == SLOT_COUNT)
            this.hbm$syncSet$PneumoStorageMonoBlockEntity$amounts(loaded);
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
            for (int i = 0; i < amounts.length; i++)
                hbm$syncArraySet$PneumoStorageMonoBlockEntity$amounts(amounts, i, bulk.get(i));
        }
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("inventory");
        output.discard("amounts");
    }

    @Override
    public long quantityAt(int index) {
        return amounts[index];
    }

    @Override
    public boolean canAssignTypes() {
        return false;
    }

    @Override
    public long removeFromSlot(int index, long amount) {
        if (amounts[index] <= 0) return amount;
        int toRemove = (int) Math.min(amount, amounts[index]);
        hbm$syncArraySet$PneumoStorageMonoBlockEntity$amounts(
                amounts, index, (int) (amounts[index] - (toRemove)));
        this.markChanged();
        return amount - toRemove;
    }

    @Override
    public long fillExistingSlot(int index, long amount) {
        int capacity = CAPACITY - amounts[index];
        if (capacity <= 0) return amount;
        int toAdd = (int) Math.min(amount, capacity);
        hbm$syncArraySet$PneumoStorageMonoBlockEntity$amounts(
                amounts, index, (int) (amounts[index] + (toAdd)));
        this.markChanged();
        return amount - toAdd;
    }

    @Override
    public long assignSlotType(int index, ItemStack zeroStack, long amount) {
        return amount;
    }

    @Override
    public void advanceFilterMode(int i) {}

    @Override
    public GhostFilters.Window filterWindow() {
        return FILTERS;
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
    public boolean admitsFilter(ItemStack stack) {
        return Platform.canFitInsideContainerItems(stack);
    }

    @Override
    public void clearBulkInventory() {
        super.clearBulkInventory();
        for (int i = 0; i < amounts.length; i++)
            hbm$syncArraySet$PneumoStorageMonoBlockEntity$amounts(amounts, i, 0);
    }

    public final void hbm$syncSet$PneumoStorageMonoBlockEntity$amounts(int[] next) {
        if (this.amounts != next) {
            boolean bound = syncBound();
            if (bound) com.hbm.packet.SyncBindings.unbind(this, this.amounts);
            this.amounts = next;
            if (bound) bindSyncValue(next, 3, 1L << 1);
            syncArrayChanged(next, -1, 3, 1L << 1);
        }
    }

    public final int hbm$syncArraySet$PneumoStorageMonoBlockEntity$amounts(
            int[] array, int index, int next) {
        if (array[index] != next) {
            array[index] = next;
            if (array == this.amounts) {
                syncArrayChanged(array, index, 3, 1L << 1);
            }
        }
        return next;
    }

    static {
        com.hbm.packet.SyncBindings.field(
                PneumoStorageMonoBlockEntity.class, "amounts", 3, 1L << 1);
    }
}
