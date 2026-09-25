// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.data.MachineData;
import com.hbm.inventory.container.MenuMachineAshpit;
import com.hbm.items.ModItems;
import com.hbm.items.machine.EnumAshType;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.BlockEntityMachineBase;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntityAshpit extends BlockEntityMachineBase
        implements MenuProvider, SyncUnitSchema {

    public int ashLevelWood;
    public int ashLevelCoal;
    public int ashLevelMisc;
    public int ashLevelFly;
    public int ashLevelSoot;

    @SyncField(units = 1L << 1)
    public boolean isFull;

    public float doorAngle;
    public float prevDoorAngle;

    @SyncField(units = 1L << 0)
    private int playersUsing;

    public BlockEntityAshpit(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ASHPIT.get(), pos, state, 5);
    }

    @Override
    public void startOpen(ContainerUser user) {
        if (!level.isClientSide()) this.playersUsing++;
    }

    @Override
    public void stopOpen(ContainerUser user) {
        if (!level.isClientSide()) this.playersUsing--;
    }

    @Override
    public void tickServer() {
        int wood = MachineData.ASHPIT_THRESHOLD_WOOD.get();
        int coal = MachineData.ASHPIT_THRESHOLD_COAL.get();
        int misc = MachineData.ASHPIT_THRESHOLD_MISC.get();
        int fly = MachineData.ASHPIT_THRESHOLD_FLY.get();
        int soot = MachineData.ASHPIT_THRESHOLD_SOOT.get();
        if (processAsh(ashLevelWood, EnumAshType.WOOD, wood)) ashLevelWood -= wood;
        if (processAsh(ashLevelCoal, EnumAshType.COAL, coal)) ashLevelCoal -= coal;
        if (processAsh(ashLevelMisc, EnumAshType.MISC, misc)) ashLevelMisc -= misc;
        if (processAsh(ashLevelFly, EnumAshType.FLY, fly)) ashLevelFly -= fly;
        if (processAsh(ashLevelSoot, EnumAshType.SOOT, soot)) ashLevelSoot -= soot;

        isFull = false;
        for (int i = 0; i < 5; i++) {
            if (!inventory.get(i).isEmpty()) isFull = true;
        }

        networkPackNT(50);
    }

    @Override
    public void tickClient() {
        this.prevDoorAngle = this.doorAngle;
        float swingSpeed = (doorAngle / 10F) + 3;

        if (this.playersUsing > 0) {
            this.doorAngle += swingSpeed;
        } else {
            this.doorAngle -= swingSpeed;
        }

        this.doorAngle = Mth.clamp(this.doorAngle, 0F, 135F);
    }

    private boolean processAsh(int level, EnumAshType type, int threshold) {
        if (level < threshold) return false;

        for (int i = 0; i < 5; i++) {
            ItemStack stack = inventory.get(i);
            if (stack.isEmpty()) {
                inventory.set(i, ModItems.POWDER_ASH.stack(type));
                this.ashLevelWood -= threshold;
                return true;
            }
            if (ModItems.POWDER_ASH.is(stack, type) && stack.getCount() < stack.getMaxStackSize()) {
                stack.grow(1);
                return true;
            }
        }
        return false;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return new int[] {0, 1, 2, 3, 4};
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return true;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.ashpit");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuMachineAshpit(containerId, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ashLevelWood = input.getIntOr("ashLevelWood", 0);
        ashLevelCoal = input.getIntOr("ashLevelCoal", 0);
        ashLevelMisc = input.getIntOr("ashLevelMisc", 0);
        ashLevelFly = input.getIntOr("ashLevelFly", 0);
        ashLevelSoot = input.getIntOr("ashLevelSoot", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("ashLevelWood", ashLevelWood);
        output.putInt("ashLevelCoal", ashLevelCoal);
        output.putInt("ashLevelMisc", ashLevelMisc);
        output.putInt("ashLevelFly", ashLevelFly);
        output.putInt("ashLevelSoot", ashLevelSoot);
    }

    @Override
    public long syncUnitMask() {
        return 0x3L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeInt(this.playersUsing);
            case 1 -> output.writeBoolean(this.isFull);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.playersUsing = input.readInt();
            case 1 -> this.isFull = input.readBoolean();
            default -> throw new IllegalArgumentException();
        }
    }
}
