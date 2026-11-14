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

package com.hbm.blockentity.machine.thermal;

import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.api.energymk2.EnergyItem;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.blockentity.FluidSettingsHandler;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.data.MachineData;
import com.hbm.inventory.container.TurbineMenu;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.tank.TankConversion;
import com.hbm.inventory.fluid.trait.CoolableTrait;
import com.hbm.packet.SyncUnitSchema;
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
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class CompactTurbineBlockEntity extends MachineBlockEntity
        implements ThermalFluidHandler,
                FluidSettingsHandler,
                EnergyHandler,
                MenuProvider,
                SyncUnitSchema {

    public static final int SLOT_FLUID_ID_IN = 0;
    public static final int SLOT_FLUID_ID_OUT = 1;
    public static final int SLOT_CONTAINER_IN = 2;
    public static final int SLOT_CONTAINER_OUT = 3;
    public static final int SLOT_BATTERY = 4;
    public static final int SLOT_EMPTY_CONTAINER_IN = 5;
    public static final int SLOT_FILLED_CONTAINER_OUT = 6;
    public static final int SLOT_COUNT = 7;
    private static final int[] BATTERY_FACE = {SLOT_BATTERY};
    private static final int[] BOTTOM_FACE = {SLOT_FILLED_CONTAINER_OUT};

    public long power;
    public final FluidTankNTM tank0;
    public final FluidTankNTM tank1;

    private final FluidTankNTM[] tanks;
    private final FluidTankNTM[] sending;
    private final FluidFlushOutputs flush = new FluidFlushOutputs();
    private @Nullable Fluid coolingType;
    private @Nullable CoolableTrait cooling;

    public CompactTurbineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURBINE.get(), pos, state, SLOT_COUNT);
        tank0 = new FluidTankNTM(NTMFluids.STEAM, MachineData.TURBINE_INPUT_TANK_SIZE.get());
        tank1 = new FluidTankNTM(NTMFluids.SPENTSTEAM, MachineData.TURBINE_OUTPUT_TANK_SIZE.get());
        tanks = new FluidTankNTM[] {tank0, tank1};
        sending = new FluidTankNTM[] {tank1};
    }

    @Override
    public void tickServer() {
        long previousPower = power;
        if (tank0.setType(SLOT_FLUID_ID_IN, SLOT_FLUID_ID_OUT, inventory)) markChanged();
        if (tank0.loadTank(SLOT_CONTAINER_IN, SLOT_CONTAINER_OUT, inventory)) markChanged();
        hbm$syncSet$CompactTurbineBlockEntity$power(
                (long) (power - (ItemEnergyTransfer.insert(this, SLOT_BATTERY, power, false))));
        hbm$syncSet$CompactTurbineBlockEntity$power((long) (0.95D * power));

        Fluid type = tank0.getTankType();
        if (type != coolingType) {
            coolingType = type;
            cooling = CoolableTrait.forMode(type, CoolableTrait.Mode.TURBINE);
        }
        long generated = 0L;
        CoolableTrait step = cooling;
        double efficiency =
                step == null
                        ? 0D
                        : step.efficiency(CoolableTrait.Mode.TURBINE)
                                * MachineData.TURBINE_EFFICIENCY.get();
        Fluid product = efficiency > 0D ? step.product() : null;
        if (tank1.getTankType() != product) {
            tank1.setTankType(product);
            markChanged();
        }
        if (efficiency > 0D) {
            int operations =
                    TankConversion.run(
                            tank0,
                            step.consumed(),
                            tank1,
                            step.produced(),
                            MachineData.TURBINE_MAX_STEAM_PER_TICK.get() / step.consumed());
            double gain = (double) (operations * step.heat()) * efficiency;
            generated = (long) gain;
            hbm$syncSet$CompactTurbineBlockEntity$power((long) (power + gain));
            if (operations > 0) markChanged();
        }
        generationTick(generated);
        hbm$syncSet$CompactTurbineBlockEntity$power(Math.min(power, getMaxPower()));
        if (power != previousPower) markChanged();
        flush.provide((ServerLevel) level, this);
        if (tank1.unloadTank(SLOT_EMPTY_CONTAINER_IN, SLOT_FILLED_CONTAINER_OUT, inventory))
            markChanged();
        networkPackNT(25);
    }

    protected void generationTick(long generated) {}

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long stored) {
        hbm$syncSet$CompactTurbineBlockEntity$power(Math.clamp(stored, 0L, getMaxPower()));
    }

    @Override
    public long getMaxPower() {
        return MachineData.TURBINE_MAX_POWER.get();
    }

    public long getPowerScaled(long height) {
        return power * height / getMaxPower();
    }

    @Override
    public FluidTankNTM receivingTank() {
        return tank0;
    }

    @Override
    public FluidTankNTM deliveryTank() {
        return tank1;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return sending;
    }

    @Override
    public FluidTankNTM[] settingsTanks() {
        return tanks;
    }

    @Override
    public int tankCount() {
        return 2;
    }

    @Override
    public FluidTankNTM tank(int index) {
        return tanks[index];
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? BOTTOM_FACE : BATTERY_FACE;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == SLOT_BATTERY && EnergyItem.matches(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    @Override
    protected double interactionRangeSq() {
        return 64D;
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
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new TurbineMenu(id, inventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput data) {
        super.loadAdditional(data);
        hbm$syncSet$CompactTurbineBlockEntity$power(data.getLongOr("power", power));
        data.child("tank0").ifPresent(tank0::deserialize);
        data.child("tank1").ifPresent(tank1::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput data) {
        super.saveAdditional(data);
        data.putLong("power", power);
        tank0.serialize(data.child("tank0"));
        tank1.serialize(data.child("tank1"));
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 7L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf bytes) {
        switch (unit) {
            case 0 -> bytes.writeLong(power);
            case 1 -> tank0.packetSerialize(bytes);
            case 2 -> tank1.packetSerialize(bytes);
            default -> super.writeSyncUnit(unit, bytes);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf bytes) {
        switch (unit) {
            case 0 -> hbm$syncSet$CompactTurbineBlockEntity$power(bytes.readLong());
            case 1 -> tank0.packetDeserialize(bytes);
            case 2 -> tank1.packetDeserialize(bytes);
            default -> super.readSyncUnit(unit, bytes);
        }
    }

    public final void hbm$syncSet$CompactTurbineBlockEntity$power(long next) {
        if (this.power != next) {
            this.power = next;
            if ((1L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(CompactTurbineBlockEntity.class, "tank0", 3, 1L << 1);
        com.hbm.packet.SyncBindings.field(CompactTurbineBlockEntity.class, "tank1", 3, 1L << 2);
    }
}
