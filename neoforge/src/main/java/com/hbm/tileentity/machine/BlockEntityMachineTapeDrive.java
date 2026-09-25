// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.BlockMachineHorizontal;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.inventory.IGUIProvider;
import com.hbm.inventory.container.MenuTapeDrive;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemDrive;
import com.hbm.packet.SyncSlots;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.saveddata.satellites.Satellite;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

@SyncSlots(all = true, units = 1L)
public class BlockEntityMachineTapeDrive extends BlockEntityMachineBase
        implements IGUIProvider, SyncUnitSchema {
    public static final int SLOT_COUNT = 12;
    public static final int DATA_PERIOD = 10;
    public static final int VISUAL_EMPTY = 0;
    public static final int VISUAL_BROKEN = 1;
    public static final int VISUAL_BLANK = 2;
    public static final int VISUAL_FILLED = 3;

    private final byte[] categories = new byte[SLOT_COUNT];

    public BlockEntityMachineTapeDrive(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TAPE_DRIVE.get(), pos, state, SLOT_COUNT);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineTapeDrive");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.getItem() instanceof ItemDrive;
    }

    public static int visualCategory(ItemStack stack) {
        if (stack.isEmpty()) return VISUAL_EMPTY;
        Satellite.DriveType type = ItemDrive.typeOf(stack);
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
        Direction behind = getBlockState().getValue(BlockMachineHorizontal.FACING).getOpposite();
        BlockPos adjacent = worldPosition.relative(behind);
        if (BlockMultiblockCore.readableChunk(server, adjacent.getX(), adjacent.getZ()) == null)
            return;
        BlockPos core = MultiblockSurface.coreOfAny(server, adjacent);
        if (core == null
                || BlockMultiblockCore.readableChunk(server, core.getX(), core.getZ()) == null)
            return;
        if (!(server.getBlockEntity(core) instanceof BlockEntityMachineSatLink link)
                || !link.connected) return;

        SatelliteSavedData data = SatelliteSavedData.get(server);
        Satellite satellite = data.getSatFromFreq(link.freq);
        if (satellite == null || !satellite.hasData(server)) return;
        for (int i = 0; i < SLOT_COUNT; i++) {
            Satellite.DriveType input = ItemDrive.typeOf(getItem(i));
            if (input == null) continue;
            var output = satellite.getOutputData(input);
            if (output.isEmpty()) continue;
            setItem(i, ModItems.DRIVE.stack(output.get()));
            satellite.consumeData();
            data.setDirty();
            break;
        }
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuTapeDrive(containerId, playerInventory, this);
    }
}
