// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.network;

import com.hbm.api.control.IControlReceiver;
import com.hbm.api.conveyor.IConveyorBelt;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.capability.NtmContracts;
import com.hbm.entity.item.EntityMovingPackage;
import com.hbm.inventory.IGUIProvider;
import com.hbm.inventory.container.MenuCraneBoxer;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public class BlockEntityCraneBoxer extends BlockEntityCraneBase
        implements IGUIProvider, IControlReceiver, SyncUnitSchema {

    public static final int SLOT_COUNT = 7 * 3;

    public static final byte MODE_4 = 0;
    public static final byte MODE_8 = 1;
    public static final byte MODE_16 = 2;
    public static final byte MODE_REDSTONE = 3;
    public static final int MODES = 4;

    private static final int[] ACCESS = accessAll();

    @SyncField(units = 1L << 0)
    public byte mode = MODE_4;

    private boolean lastRedstone = false;

    public BlockEntityCraneBoxer(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRANE_BOXER.get(), pos, state, SLOT_COUNT);
    }

    private static int[] accessAll() {
        int[] access = new int[SLOT_COUNT];
        for (int i = 0; i < SLOT_COUNT; i++) access[i] = i;
        return access;
    }

    public static int packSize(byte mode) {
        return switch (mode) {
            case MODE_4 -> 4;
            case MODE_8 -> 8;
            case MODE_16 -> 16;
            default -> 1;
        };
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.craneBoxer");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public void tickServer() {
        boolean redstone = level.hasNeighborSignal(worldPosition);

        if (mode == MODE_REDSTONE && redstone && !lastRedstone) {
            int held = 0;
            for (int i = 0; i < SLOT_COUNT; i++) if (!getItem(i).isEmpty()) held++;
            if (held > 0) ship(held, false);
        }

        this.lastRedstone = redstone;

        if (mode != MODE_REDSTONE && level.getGameTime() % 2 == 0) {
            int pack = packSize(mode);
            int fullStacks = 0;

            for (int i = 0; i < SLOT_COUNT; i++) {
                ItemStack stack = getItem(i);
                if (!stack.isEmpty() && stack.getCount() == stack.getMaxStackSize()) fullStacks++;
            }

            if (fullStacks >= pack) ship(pack, true);
        }

        networkPackNT(15);
    }

    private void ship(int pack, boolean fullStacksOnly) {
        Direction output = getOutputSide();
        BlockPos outputPos = worldPosition.relative(output);
        IConveyorBelt belt = NtmContracts.CONVEYOR_BELT.at(level, outputPos);
        if (belt == null) return;

        List<ItemStack> box = new ArrayList<>(pack);
        for (int i = 0; i < SLOT_COUNT && box.size() < pack; i++) {
            ItemStack stack = getItem(i);
            if (stack.isEmpty()) continue;
            if (fullStacksOnly && stack.getCount() != stack.getMaxStackSize()) continue;

            box.add(stack.copy());
            setItem(i, ItemStack.EMPTY);
        }
        Collections.reverse(box);

        Vec3 pos =
                new Vec3(
                        worldPosition.getX() + 0.5 + output.getStepX() * 0.55,
                        worldPosition.getY() + 0.5 + output.getStepY() * 0.55,
                        worldPosition.getZ() + 0.5 + output.getStepZ() * 0.55);
        Vec3 snap = belt.getClosestSnappingPosition(level, outputPos, pos);

        EntityMovingPackage moving = new EntityMovingPackage(level);
        moving.setItemStacks(box);
        moving.snapTo(snap.x, snap.y, snap.z, 0F, 0F);
        level.addFreshEntity(moving);
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
    public int getComparatorPower() {
        return AbstractContainerMenu.getRedstoneSignalFromContainer(this);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MenuCraneBoxer(containerId, inventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.mode = input.getByteOr("mode", MODE_4);
        this.lastRedstone = input.getBooleanOr("lastRedstone", false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putByte("mode", mode);
        output.putBoolean("lastRedstone", lastRedstone);
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.getEyePosition()
                        .distanceToSqr(
                                worldPosition.getX(), worldPosition.getY(), worldPosition.getZ())
                < 400D;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("toggle")) this.mode = (byte) ((this.mode + 1) % MODES);
    }

    @Override
    public long syncUnitMask() {
        return 1L << 0;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeByte(this.mode);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.mode = input.readByte();
            default -> throw new IllegalArgumentException();
        }
    }
}
