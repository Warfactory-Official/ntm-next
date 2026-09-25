// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankEndpoint;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.IGUIProvider;
import com.hbm.inventory.container.ContainerSync;
import com.hbm.inventory.container.MenuMachineRadiolysis;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.RadiolysisRecipe;
import com.hbm.inventory.recipes.RadiolysisRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemRTGPellet;
import com.hbm.items.machine.ItemRTGPelletDepleted;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.IFluidCopiable;
import com.hbm.util.ContagionUtil;
import com.hbm.util.RTGUtil;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntityMachineRadiolysis extends BlockEntityMachineBase
        implements IEnergyHandlerMK2,
                FluidTankEndpoint,
                IGUIProvider,
                IFluidCopiable,
                SyncUnitSchema {

    public static final int SLOT_COUNT = 15;
    public static final int SLOT_FLUID_ID = 10;
    public static final int SLOT_FLUID_ID_OUT = 11;
    public static final int SLOT_STERILIZE_IN = 12;
    public static final int SLOT_STERILIZE_OUT = 13;
    public static final int SLOT_BATTERY = 14;

    public static final long MAX_POWER = 1_000_000L;
    public static final int TANK_CAPACITY = 2_000;
    public static final int POWER_PER_HEAT = 10;
    public static final int CRACK_HEAT = 100;
    public static final int STERILIZE_HEAT = 200;
    public static final int STERILIZE_PERIOD = 100;

    private static final int[] SLOT_IO = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 12, 13};
    private static final int[] SLOT_RTG = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};

    @SyncField(units = 1L << 2)
    public final FluidTankNTM[] tanks = {
        new FluidTankNTM(TANK_CAPACITY),
        new FluidTankNTM(TANK_CAPACITY),
        new FluidTankNTM(TANK_CAPACITY)
    };

    private final FluidTankNTM[] receiving = {tanks[0]};
    private final FluidTankNTM[] sending = {tanks[1], tanks[2]};

    @SyncField(units = 1L << 0)
    public long power;

    @SyncField(units = 1L << 1)
    @ContainerSync
    public int heat;

    private final FluidFlushOutputs flush = new FluidFlushOutputs();

    public BlockEntityMachineRadiolysis(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RADIOLYSIS.get(), pos, state, SLOT_COUNT);
    }

    public static int crackPeriod(int heat) {
        return (int) Math.max(-0.1 * (heat - CRACK_HEAT) + 30, 5);
    }

    @Override
    public void tickServer() {
        power -= ItemEnergyTransfer.insert(this, SLOT_BATTERY, power, false);

        heat = RTGUtil.updateRTGs(inventory, SLOT_RTG);
        power = Math.min(MAX_POWER, power + (long) heat * POWER_PER_HEAT);

        if (tanks[0].setType(SLOT_FLUID_ID, SLOT_FLUID_ID_OUT, inventory)) setChanged();
        setupTanks();

        if (heat > CRACK_HEAT) {
            if (TickPhase.every(this, crackPeriod(heat))) crack();
            if (heat >= STERILIZE_HEAT && TickPhase.every(this, STERILIZE_PERIOD)) sterilize();
        }

        flush.provide((ServerLevel) level, this);

        networkPackNT(50);
    }

    private void crack() {
        RadiolysisRecipe recipe = RadiolysisRecipes.INSTANCE.getRadiolysis(tanks[0].getTankType());
        if (recipe == null || recipe.outputFluid.length < 2) return;

        int left = (int) recipe.outputFluid[0].amount();
        int right = (int) recipe.outputFluid[1].amount();
        if (tanks[0].getFill() < RadiolysisRecipes.FILL_PER_OP || !hasSpace(left, right)) return;

        tanks[0].setFill(tanks[0].getFill() - RadiolysisRecipes.FILL_PER_OP);
        tanks[1].setFill(tanks[1].getFill() + left);
        tanks[2].setFill(tanks[2].getFill() + right);
        setChanged();
    }

    private boolean hasSpace(int left, int right) {
        return tanks[1].getFill() + left <= tanks[1].getMaxFill()
                && tanks[2].getFill() + right <= tanks[2].getMaxFill();
    }

    private void setupTanks() {
        RadiolysisRecipe recipe = RadiolysisRecipes.INSTANCE.getRadiolysis(tanks[0].getTankType());
        if (recipe != null && recipe.outputFluid.length >= 2) {
            tanks[1].setTankType(recipe.outputFluid[0].type());
            tanks[2].setTankType(recipe.outputFluid[1].type());
        } else {
            tanks[0].setTankType(null);
            tanks[1].setTankType(null);
            tanks[2].setTankType(null);
        }
    }

    private void sterilize() {
        ItemStack input = inventory.get(SLOT_STERILIZE_IN);
        if (input.isEmpty()) return;

        if (input.has(DataComponents.FOOD) && !input.is(ModItems.PANCAKE.get())) {
            removeItem(SLOT_STERILIZE_IN, 1);
        }

        input = inventory.get(SLOT_STERILIZE_IN);
        if (input.isEmpty() || !ContagionUtil.isContagious(input)) return;

        ItemStack cured = input.copyWithCount(1);
        ContagionUtil.cure(cured);

        ItemStack output = inventory.get(SLOT_STERILIZE_OUT);
        if (output.isEmpty()) {
            removeItem(SLOT_STERILIZE_IN, 1);
            inventory.set(SLOT_STERILIZE_OUT, cured);
            setChanged();
        } else if (ItemStack.isSameItemSameComponents(output, cured)
                && output.getCount() + 1 <= output.getMaxStackSize()) {
            removeItem(SLOT_STERILIZE_IN, 1);
            output.grow(1);
            setChanged();
        }
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long p) {
        power = Math.max(0L, Math.min(p, MAX_POWER));
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return receiving;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return sending;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == SLOT_STERILIZE_IN
                || (slot < SLOT_FLUID_ID && stack.getItem() instanceof ItemRTGPellet);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOT_IO;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return (slot < SLOT_FLUID_ID && stack.getItem() instanceof ItemRTGPelletDepleted)
                || slot == SLOT_STERILIZE_OUT;
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return tanks;
    }

    private void writeTanks(ByteBuf output) {
        for (FluidTankNTM tank : tanks) tank.packetSerialize(output);
    }

    private void readTanks(ByteBuf input) {
        for (FluidTankNTM tank : tanks) tank.packetDeserialize(input);
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.radiolysis");
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuMachineRadiolysis(containerId, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getLong("power").ifPresent(v -> power = v);
        input.getInt("heat").ifPresent(v -> heat = v);
        input.child("input").ifPresent(tanks[0]::deserialize);
        input.child("output1").ifPresent(tanks[1]::deserialize);
        input.child("output2").ifPresent(tanks[2]::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putInt("heat", heat);
        tanks[0].serialize(output.child("input"));
        tanks[1].serialize(output.child("output1"));
        tanks[2].serialize(output.child("output2"));
    }

    @Override
    public long syncUnitMask() {
        return 0x7L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(this.power);
            case 1 -> output.writeInt(this.heat);
            case 2 -> writeTanks(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.power = input.readLong();
            case 1 -> this.heat = input.readInt();
            case 2 -> readTanks(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
