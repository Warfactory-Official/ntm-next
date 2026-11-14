/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.blockentity.network;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.CraneInserterMenu;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.platform.BlockLookupCache;
import com.hbm.platform.Caps;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class CraneInserterBlockEntity extends CraneBaseBlockEntity
        implements MenuProvider, SyncUnitSchema {

    public static final int SLOT_COUNT = 21;

    private static final int[] ACCESS = accessAll();

    public boolean destroyer;

    {
        hbm$syncSet$CraneInserterBlockEntity$destroyer(true);
    }

    private @Nullable BlockLookupCache<Storage<ItemVariant>> target;
    private @Nullable Direction targetSide;

    public CraneInserterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRANE_INSERTER.get(), pos, state, SLOT_COUNT);
    }

    private static int[] accessAll() {
        int[] access = new int[SLOT_COUNT];
        for (int i = 0; i < SLOT_COUNT; i++) access[i] = i;
        return access;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.craneInserter");
    }

    @Override
    public void tickServer() {
        if (!level.hasNeighborSignal(worldPosition)) push();
        networkPackNT(15);
    }

    private void push() {
        Direction output = getOutputSide();
        if (target == null || targetSide != output) {
            target =
                    Caps.createItemStoreCache(
                            (ServerLevel) level,
                            worldPosition.relative(output),
                            output.getOpposite());
            targetSide = output;
        }
        Storage<ItemVariant> dest = target.find();
        if (dest == null) return;

        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack stack = getItem(i);
            if (stack.isEmpty()) continue;

            ItemStack left = Caps.insert(dest, stack.copy());
            if (left.getCount() != stack.getCount()) {
                setItem(i, left);
                return;
            }
        }

        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack stack = getItem(i);
            if (stack.isEmpty()) continue;

            if (Caps.insert(dest, stack.copyWithCount(1)).isEmpty()) {
                removeItem(i, 1);
                return;
            }
        }
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return true;
    }

    @Override
    public int getComparatorPower() {
        return AbstractContainerMenu.getRedstoneSignalFromContainer(this);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new CraneInserterMenu(containerId, inventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$CraneInserterBlockEntity$destroyer(input.getBooleanOr("destroyer", false));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("destroyer", destroyer);
    }

    public void toggleDestroyer() {
        hbm$syncSet$CraneInserterBlockEntity$destroyer(!destroyer);
        setChanged();
    }

    @Override
    public long syncUnitMask() {
        return 1L << 0;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeBoolean(this.destroyer);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.hbm$syncSet$CraneInserterBlockEntity$destroyer(input.readBoolean());
            default -> throw new IllegalArgumentException();
        }
    }

    public final void hbm$syncSet$CraneInserterBlockEntity$destroyer(boolean next) {
        if (this.destroyer != next) {
            this.destroyer = next;
            if ((1L << 0) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 0);
        }
    }
}
