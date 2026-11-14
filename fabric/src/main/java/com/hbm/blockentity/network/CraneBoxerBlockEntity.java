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

import com.hbm.api.conveyor.ConveyorBelt;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.capability.NtmContracts;
import com.hbm.entity.item.MovingPackageEntity;
import com.hbm.inventory.container.CraneBoxerMenu;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.platform.BlockLookupCache;
import com.hbm.platform.Platform;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
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
import net.minecraft.world.phys.Vec3;

public final class CraneBoxerBlockEntity extends CraneBaseBlockEntity
        implements SyncUnitSchema, MenuProvider {

    public static final int SLOT_COUNT = 21;
    public static final byte MODE_4 = 0;
    public static final byte MODE_8 = 1;
    public static final byte MODE_16 = 2;
    public static final byte MODE_REDSTONE = 3;

    private static final int[] SLOTS = {
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20
    };

    public byte mode;
    private boolean lastRedstone;

    private boolean powered;
    private BlockPos outputPos;
    private BlockLookupCache<ConveyorBelt> outputBelt;

    public CraneBoxerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRANE_BOXER.get(), pos, state, SLOT_COUNT);
    }

    public void refreshNeighbors() {
        powered = level.hasNeighborSignal(worldPosition);
        outputPos = worldPosition.relative(getOutputSide());
        outputBelt = NtmContracts.CONVEYOR_BELT.cacheAt((ServerLevel) level, outputPos);
    }

    @Override
    public void onGraphLoad(ServerLevel server) {
        super.onGraphLoad(server);
        refreshNeighbors();
    }

    @Override
    protected void directionStateChanged() {
        refreshNeighbors();
    }

    public static int packSize(byte mode) {
        return switch (mode) {
            case MODE_4 -> 4;
            case MODE_8 -> 8;
            case MODE_16 -> 16;
            case MODE_REDSTONE -> 0;
            default -> throw new IllegalArgumentException();
        };
    }

    private static byte decodeMode(byte value) {
        if (value < MODE_4 || value > MODE_REDSTONE) throw new IllegalArgumentException();
        return value;
    }

    public void cycleMode() {
        hbm$syncSet$CraneBoxerBlockEntity$mode((byte) ((mode + 1) & 3));
        setChanged();
    }

    @Override
    public void tickServer() {
        boolean rising = powered && !lastRedstone;
        if (powered != lastRedstone) {
            lastRedstone = powered;
            setChanged();
        }
        if (mode == MODE_REDSTONE ? rising : TickPhase.every(this, 2)) ship();
        networkPackNT(15);
    }

    private void ship() {
        var belt = outputBelt.find();
        if (belt == null) return;

        boolean all = mode == MODE_REDSTONE;
        int target = all ? SLOT_COUNT : packSize(mode);
        int selected = 0;
        int count = 0;
        for (int slot = 0; slot < SLOT_COUNT && count < target; slot++) {
            var stack = inventory.get(slot);
            if (stack.isEmpty() || !all && stack.getCount() != stack.getMaxStackSize()) continue;
            selected |= 1 << slot;
            count++;
        }
        if (count == 0 || !all && count != target) return;

        var contents = new ArrayList<ItemStack>(count);
        while (selected != 0) {
            int slot = Integer.SIZE - 1 - Integer.numberOfLeadingZeros(selected);
            selected &= ~(1 << slot);
            contents.add(inventory.set(slot, ItemStack.EMPTY));
        }
        var parcel = new MovingPackageEntity(level);
        parcel.setItemStacks(contents);
        var direction = getOutputSide();
        var mouth =
                new Vec3(
                        worldPosition.getX() + 0.5 + direction.getStepX() * 0.55,
                        worldPosition.getY() + 0.5 + direction.getStepY() * 0.55,
                        worldPosition.getZ() + 0.5 + direction.getStepZ() * 0.55);
        var snapped = belt.getSnappingPosition(level, outputPos, mouth);
        parcel.snapTo(snapped.x, snapped.y, snapped.z, 0, 0);
        level.addFreshEntity(parcel);
        setChanged();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return Platform.canFitInsideContainerItems(stack);
    }

    @Override
    public int getComparatorPower() {
        return AbstractContainerMenu.getRedstoneSignalFromContainer(this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.craneBoxer");
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new CraneBoxerMenu(containerId, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$CraneBoxerBlockEntity$mode(decodeMode(input.getByteOr("mode", MODE_4)));
        lastRedstone = input.getBooleanOr("lastRedstone", false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putByte("mode", mode);
        output.putBoolean("lastRedstone", lastRedstone);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 1;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        if (unit == 0) output.writeByte(mode);
        else super.writeSyncUnit(unit, output);
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        if (unit == 0) hbm$syncSet$CraneBoxerBlockEntity$mode(decodeMode(input.readByte()));
        else super.readSyncUnit(unit, input);
    }

    public final void hbm$syncSet$CraneBoxerBlockEntity$mode(byte next) {
        if (this.mode != next) {
            this.mode = next;
            if ((1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1);
        }
    }
}
