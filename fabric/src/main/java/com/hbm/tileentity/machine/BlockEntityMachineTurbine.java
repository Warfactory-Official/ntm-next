// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidFlushSender;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.data.MachineData;
import com.hbm.inventory.container.MenuMachineTurbine;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Coolable.CoolingType;
import com.hbm.inventory.fluid.trait.FT_Coolable;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.items.machine.IFluidContainerItem;
import com.hbm.items.machine.ItemFluidContainerInfinite;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.IFluidCopiable;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntityMachineTurbine extends BlockEntityMachineBase
        implements IEnergyHandlerMK2,
                FluidFlushSender,
                MenuProvider,
                IFluidCopiable,
                SyncUnitSchema {

    public static final int SLOT_FLUID_ID_IN = 0;
    public static final int SLOT_FLUID_ID_OUT = 1;
    public static final int SLOT_CONTAINER_IN = 2;
    public static final int SLOT_CONTAINER_OUT = 3;
    public static final int SLOT_BATTERY = 4;
    public static final int SLOT_EMPTY_CONTAINER_IN = 5;
    public static final int SLOT_FILLED_CONTAINER_OUT = 6;
    public static final int SLOT_COUNT = 7;

    private static final int[] SLOTS_DOWN = {SLOT_FILLED_CONTAINER_OUT};
    private static final int[] SLOTS_OTHER = {SLOT_BATTERY};

    @SyncField(units = 1L << 1)
    public final FluidTankNTM tank0 =
            new FluidTankNTM(NTMFluids.STEAM, MachineData.TURBINE_INPUT_TANK_SIZE.get());

    @SyncField(units = 1L << 2)
    public final FluidTankNTM tank1 =
            new FluidTankNTM(NTMFluids.SPENTSTEAM, MachineData.TURBINE_OUTPUT_TANK_SIZE.get());

    @SyncField(units = 1L << 0)
    public long power;

    private final FluidFlushOutputs flush = new FluidFlushOutputs();
    private final FluidTankNTM[] sending = {tank1};

    public BlockEntityMachineTurbine(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURBINE.get(), pos, state, SLOT_COUNT);
    }

    @Override
    protected double interactionRangeSq() {
        return 64;
    }

    private static boolean isFluidContainer(ItemStack stack) {
        return FluidTankNTM.isFluidContainer(stack);
    }

    @Override
    public void tickServer() {
        boolean changed = tank0.setType(SLOT_FLUID_ID_IN, SLOT_FLUID_ID_OUT, inventory);
        changed |= tank0.loadTank(SLOT_CONTAINER_IN, SLOT_CONTAINER_OUT, inventory);
        if (changed) setChanged();

        long maxPower = MachineData.TURBINE_MAX_POWER.get();
        power -= ItemEnergyTransfer.insert(this, SLOT_BATTERY, power, false);

        power = (long) (power * 0.95);

        Fluid in = tank0.getTankType();
        boolean valid = false;
        FT_Coolable trait = NTMFluidProperties.getTrait(in, FT_Coolable.class);
        if (trait != null) {
            double eff =
                    trait.getEfficiency(CoolingType.TURBINE) * MachineData.TURBINE_EFFICIENCY.get();
            if (eff > 0) {
                tank1.setTankType(trait.coolsTo());
                int inputOps = tank0.getFill() / trait.amountReq;
                int outputOps = (tank1.getMaxFill() - tank1.getFill()) / trait.amountProduced;
                int cap = MachineData.TURBINE_MAX_STEAM_PER_TICK.get() / trait.amountReq;
                int ops = Math.min(inputOps, Math.min(outputOps, cap));
                tank0.setFill(tank0.getFill() - ops * trait.amountReq);
                tank1.setFill(tank1.getFill() + ops * trait.amountProduced);
                power = (long) (power + ops * trait.heatEnergy * eff);
                valid = true;
            }
        }
        if (!valid) tank1.setTankType(NTMFluids.NONE);
        if (power > maxPower) power = maxPower;

        flush.provide((ServerLevel) level, this);

        if (tank1.unloadTank(SLOT_EMPTY_CONTAINER_IN, SLOT_FILLED_CONTAINER_OUT, inventory))
            setChanged();

        networkPackNT(25);
    }

    public long getPowerScaled(long i) {
        return (power * i) / getMaxPower();
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long p) {
        power = Math.max(0L, Math.min(p, getMaxPower()));
    }

    @Override
    public long getMaxPower() {
        return MachineData.TURBINE_MAX_POWER.get();
    }

    @Override
    public long getDemand(Fluid type, int pressure) {
        if (pressure != tank0.getPressure()) return 0L;
        if (!tank0.accepts(type)) return 0L;
        return (long) tank0.getMaxFill() - tank0.getFill();
    }

    @Override
    public long transferFluid(Fluid type, int pressure, long amount) {
        if (pressure != tank0.getPressure()) return amount;
        int accepted = tank0.fill(type, (int) Math.min(amount, Integer.MAX_VALUE), true);
        if (accepted > 0) setChanged();
        return amount - accepted;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return sending;
    }

    @Override
    public long getFluidAvailable(Fluid type, int pressure) {
        if (!tank1.provides(type) || tank1.getPressure() != pressure) return 0L;
        return tank1.getFill();
    }

    @Override
    public void useUpFluid(Fluid type, int pressure, long amount) {
        if (!tank1.provides(type) || tank1.getPressure() != pressure) return;
        if (tank1.drain((int) Math.min(amount, Integer.MAX_VALUE), true) > 0) setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_FLUID_ID_IN -> stack.getItem() instanceof FluidIdentifierItem;
            case SLOT_CONTAINER_IN, SLOT_EMPTY_CONTAINER_IN -> isFluidContainer(stack);
            case SLOT_BATTERY -> IBatteryItem.isBattery(stack);
            default -> false;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? SLOTS_DOWN : SLOTS_OTHER;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == SLOT_BATTERY && IBatteryItem.isBattery(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return new FluidTankNTM[] {tank0, tank1};
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineTurbine");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuMachineTurbine(containerId, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getLong("power").ifPresent(v -> power = v);
        input.child("tank0").ifPresent(tank0::deserialize);
        input.child("tank1").ifPresent(tank1::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        tank0.serialize(output.child("tank0"));
        tank1.serialize(output.child("tank1"));
    }

    @Override
    public long syncUnitMask() {
        return 0x7L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(this.power);
            case 1 -> this.tank0.packetSerialize(output);
            case 2 -> this.tank1.packetSerialize(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.power = input.readLong();
            case 1 -> this.tank0.packetDeserialize(input);
            case 2 -> this.tank1.packetDeserialize(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
