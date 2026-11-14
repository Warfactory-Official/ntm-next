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

package com.hbm.blockentity.machine;

import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.HorizontalMachineBlock;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.inventory.container.TapeDriveMenu;
import com.hbm.items.ModItems;
import com.hbm.items.machine.DriveItem;
import com.hbm.items.machine.DriveType;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.saveddata.satellites.Satellite;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
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

public class TapeDriveBlockEntity extends MachineBlockEntity
        implements MenuProvider, SyncUnitSchema {
    public static final int SLOT_COUNT = 12;
    public static final int DATA_PERIOD = 10;
    public static final int VISUAL_EMPTY = 0;
    public static final int VISUAL_BROKEN = 1;
    public static final int VISUAL_BLANK = 2;
    public static final int VISUAL_FILLED = 3;

    private final byte[] categories = new byte[SLOT_COUNT];

    public TapeDriveBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TAPE_DRIVE.get(), pos, state, SLOT_COUNT);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineTapeDrive");
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.getItem() instanceof DriveItem;
    }

    public static int visualCategory(ItemStack stack) {
        if (stack.isEmpty()) return VISUAL_EMPTY;
        DriveType type = DriveItem.typeOf(stack);
        if (type == null) return VISUAL_BROKEN;
        return switch (type) {
            case FLASH_EMPTY, DISK_EMPTY -> VISUAL_BLANK;
            case FLASH_BROKEN, DISK_BROKEN -> VISUAL_BROKEN;
            default -> VISUAL_FILLED;
        };
    }

    public int category(int slot) {
        return categories[slot];
    }

    @Override
    public long syncUnitMask() {
        return 1L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        if (unit != 0) throw new IllegalArgumentException();
        for (int i = 0; i < SLOT_COUNT; i++) output.writeByte(visualCategory(getItem(i)));
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        if (unit != 0) throw new IllegalArgumentException();
        for (int i = 0; i < SLOT_COUNT; i++) categories[i] = input.readByte();
    }

    @Override
    public void tickServer() {
        if (TickPhase.every(this, DATA_PERIOD)) collectData((ServerLevel) level);
        networkPackNT(50);
    }

    private void collectData(ServerLevel server) {
        Direction behind = getBlockState().getValue(HorizontalMachineBlock.FACING).getOpposite();
        BlockPos adjacent = worldPosition.relative(behind);
        if (MultiblockCoreBlock.readableChunk(server, adjacent.getX(), adjacent.getZ()) == null)
            return;
        BlockPos core = MultiblockSurface.coreOfAny(server, adjacent);
        if (core == null
                || MultiblockCoreBlock.readableChunk(server, core.getX(), core.getZ()) == null)
            return;
        if (!(server.getBlockEntity(core) instanceof SatelliteLinkerBlockEntity link)
                || !link.connected()) return;

        var data = link.satelliteRegistry();
        Satellite satellite = data.getSatFromFreq(link.frequency());
        if (satellite == null || !satellite.dataReady(server)) return;
        for (int i = 0; i < SLOT_COUNT; i++) {
            DriveType input = DriveItem.typeOf(getItem(i));
            if (input == null) continue;
            var output = satellite.dataFor(input);
            if (output == null) continue;
            setItem(i, ModItems.DRIVE.stack(output));
            satellite.takeData();
            data.setDirty();
            break;
        }
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new TapeDriveMenu(containerId, playerInventory, this);
    }

    static {
        com.hbm.packet.SyncInventory.slots(
                TapeDriveBlockEntity.class, new int[] {}, 1L, true, true);
    }
}
