// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankEndpoint;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.ContainerSync;
import com.hbm.inventory.container.MenuMachineHydrotreater;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.HydrotreatingRecipe;
import com.hbm.inventory.recipes.HydrotreatingRecipes;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.IFluidCopiable;
import com.hbm.tileentity.PersistentDrop;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntityMachineHydrotreater extends BlockEntityMachineBase
        implements IEnergyHandlerMK2,
                FluidTankEndpoint,
                MenuProvider,
                PersistentDrop,
                IFluidCopiable,
                SyncUnitSchema {

    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_INPUT_IN = 1;
    public static final int SLOT_INPUT_OUT = 2;
    public static final int SLOT_OUTPUT_IN_START = 3;
    public static final int SLOT_OUTPUT_OUT_START = 4;
    public static final int SLOT_FLUID_ID = 7;
    public static final int SLOT_CATALYST = 8;
    public static final int SLOT_COUNT = 9;

    public static final long MAX_POWER = 1_000_000L;
    public static final int OUTPUT_TANK_COUNT = 2;
    public static final int INPUT_TANK_CAPACITY = 64_000;
    public static final int OUTPUT_TANK_CAPACITY = 24_000;

    private static final String[] PERSISTENT_KEYS = {"t0", "t1", "t2", "t3"};

    @SyncField(units = 1L << 1)
    public final FluidTankNTM[] tanks = new FluidTankNTM[4];

    @SyncField(units = 1L << 0)
    @ContainerSync
    public long power;

    private final FluidFlushOutputs flush = new FluidFlushOutputs();
    private final FluidTankNTM[] receiving;
    private final FluidTankNTM[] sending;

    public BlockEntityMachineHydrotreater(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HYDROTREATER.get(), pos, state, SLOT_COUNT);
        tanks[0] = new FluidTankNTM(NTMFluids.OIL, INPUT_TANK_CAPACITY);
        tanks[1] = new FluidTankNTM(NTMFluids.HYDROGEN, INPUT_TANK_CAPACITY).withPressure(1);
        tanks[2] = new FluidTankNTM(NTMFluids.OIL_DS, OUTPUT_TANK_CAPACITY);
        tanks[3] = new FluidTankNTM(NTMFluids.SOURGAS, OUTPUT_TANK_CAPACITY);
        receiving = new FluidTankNTM[] {tanks[0], tanks[1]};
        sending = new FluidTankNTM[] {tanks[2], tanks[3]};
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hydrotreater");
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
    public void tickServer() {
        long prevPower = power;

        power += ItemEnergyTransfer.extract(this, SLOT_BATTERY, MAX_POWER - power, false);
        boolean changed = tanks[0].setType(SLOT_FLUID_ID, SLOT_FLUID_ID, inventory);
        changed |= tanks[0].loadTank(SLOT_INPUT_IN, SLOT_INPUT_OUT, inventory);

        if (TickPhase.every(this, 2)) reform();
        for (int i = 0; i < OUTPUT_TANK_COUNT; i++) {
            changed |=
                    tanks[i + 2].unloadTank(
                            SLOT_OUTPUT_IN_START + i * 2, SLOT_OUTPUT_OUT_START + i * 2, inventory);
        }
        if (changed) setChanged();

        if (power != prevPower) setChanged();
        flush.provide((ServerLevel) level, this);

        networkPackNT(25);
    }

    private void reform() {
        HydrotreatingRecipe recipe =
                HydrotreatingRecipes.INSTANCE.getRecipeForInput(tanks[0].getTankType());
        if (recipe == null) {
            tanks[2].setTankType(null);
            tanks[3].setTankType(null);
            return;
        }

        FluidStackNTM in = recipe.inputFluid[0];
        FluidStackNTM hydrogen = recipe.inputFluid[1];
        FluidStackNTM out1 = recipe.outputFluid[0];
        FluidStackNTM out2 = recipe.outputFluid[1];

        tanks[1].withPressure(hydrogen.pressure()).setTankType(hydrogen.type());
        tanks[2].setTankType(out1.type());
        tanks[3].setTankType(out2.type());

        if (power < recipe.power) return;
        if (tanks[0].getFill() < in.amount()) return;
        if (tanks[1].getFill() < hydrogen.amount()) return;
        if (!inventory.get(SLOT_CATALYST).is(ModItems.CATALYTIC_CONVERTER.get())) return;

        if (tanks[2].getFill() + out1.amount() > tanks[2].getMaxFill()) return;
        if (tanks[3].getFill() + out2.amount() > tanks[3].getMaxFill()) return;

        tanks[0].setFill((int) (tanks[0].getFill() - in.amount()));
        tanks[1].setFill((int) (tanks[1].getFill() - hydrogen.amount()));
        tanks[2].setFill((int) (tanks[2].getFill() + out1.amount()));
        tanks[3].setFill((int) (tanks[3].getFill() + out2.amount()));

        power -= recipe.power;
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return tanks;
    }

    @Override
    public FluidTankNTM getTankToPaste() {
        return tanks[0];
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
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuMachineHydrotreater(containerId, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getLong("power").ifPresent(v -> power = v);
        input.child("t0").ifPresent(tanks[0]::deserialize);
        input.child("t1").ifPresent(tanks[1]::deserialize);
        input.child("t2").ifPresent(tanks[2]::deserialize);
        input.child("t3").ifPresent(tanks[3]::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        tanks[0].serialize(output.child("t0"));
        tanks[1].serialize(output.child("t1"));
        tanks[2].serialize(output.child("t2"));
        tanks[3].serialize(output.child("t3"));
    }

    @Override
    public void writePersistent(DataComponentMap.Builder components) {
        for (FluidTankNTM tank : tanks) {
            if (tank.getFill() > 0) {
                components.set(
                        ModDataComponents.TANK_CONTENTS.get(), FluidStackNTM.snapshot(tanks));
                return;
            }
        }
    }

    @Override
    public void readPersistent(DataComponentGetter components) {
        List<FluidStackNTM> contents = components.get(ModDataComponents.TANK_CONTENTS.get());
        if (contents != null) FluidStackNTM.restore(contents, tanks);
    }

    @Override
    public String[] persistentKeys() {
        return PERSISTENT_KEYS;
    }

    @Override
    public long syncUnitMask() {
        return 0x3L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(this.power);
            case 1 -> writeTanks(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.power = input.readLong();
            case 1 -> readTanks(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
