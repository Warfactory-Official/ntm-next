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

import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.redstoneoverradio.RadioValueProvider;
import com.hbm.api.tile.HeatHandler;
import com.hbm.blockentity.FluidSettingsHandler;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.HeatexMenu;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.tank.TankConversion;
import com.hbm.inventory.fluid.trait.CoolableTrait;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class CoolingHeatBlockEntity extends MachineBlockEntity
        implements ThermalFluidHandler,
                HeatHandler,
                FluidSettingsHandler,
                RadioValueProvider,
                MenuProvider,
                SyncUnitSchema {
    public static final String[] ROR = {"VAL:hotfluid", "VAL:coldfluid", "VAL:heat"};
    public final FluidTankNTM[] tanks = {
        new FluidTankNTM(NTMFluids.COOLANT_HOT, 24000), new FluidTankNTM(NTMFluids.COOLANT, 24000)
    };
    public int heatEnergy;
    public int amountToCool;

    {
        hbm$syncSet$CoolingHeatBlockEntity$amountToCool(24000);
    }

    public int tickDelay;

    {
        hbm$syncSet$CoolingHeatBlockEntity$tickDelay(1);
    }

    private int syncInputFill;
    private final FluidTankNTM[] sending = {tanks[1]};
    private final FluidFlushOutputs flush = new FluidFlushOutputs();
    private @Nullable Fluid coolingFluid;
    private @Nullable CoolableTrait cooling;

    public CoolingHeatBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HEATER_HEATEX.get(), pos, state, 1);
    }

    @Override
    public void tickServer() {
        boolean changed = tanks[0].setType(0, 0, inventory);
        var fluid = tanks[0].getTankType();
        if (fluid != coolingFluid) {
            coolingFluid = fluid;
            cooling = CoolableTrait.forMode(fluid, CoolableTrait.Mode.HEATEXCHANGER);
        }
        if (cooling == null) {
            changed |= tanks[0].getTankType() != null || tanks[1].getTankType() != null;
            tanks[0].setTankType(null);
            tanks[1].setTankType(null);
        } else if (tanks[1].getTankType() != cooling.product()) {
            tanks[1].setTankType(cooling.product());
            changed = true;
        }
        int previousHeat = heatEnergy;
        hbm$syncSet$CoolingHeatBlockEntity$heatEnergy((int) (heatEnergy * 0.999D));
        hbm$syncSet$CoolingHeatBlockEntity$syncInputFill(tanks[0].getFill());
        if (tickDelay < 1) {
            hbm$syncSet$CoolingHeatBlockEntity$tickDelay(1);
            changed = true;
        }
        if (cooling != null && TickPhase.every(this, tickDelay)) {
            int operations =
                    TankConversion.run(
                            tanks[0],
                            cooling.consumed(),
                            tanks[1],
                            cooling.produced(),
                            amountToCool);
            if (operations > 0) {
                hbm$syncSet$CoolingHeatBlockEntity$heatEnergy(
                        (int)
                                (heatEnergy
                                        + ((int)
                                                ((double) cooling.heat()
                                                        * operations
                                                        * cooling.efficiency(
                                                                CoolableTrait.Mode
                                                                        .HEATEXCHANGER)))));
                changed = true;
            }
        }
        if (changed || heatEnergy != previousHeat) markChanged();
        networkPackNT(25);
        flush.provide((ServerLevel) level, this);
    }

    public void setCoolAmount(int amount) {
        int next = Math.clamp(amount, 1, tanks[0].getMaxFill());
        if (amountToCool != next) {
            hbm$syncSet$CoolingHeatBlockEntity$amountToCool(next);
            markChanged();
        }
    }

    public void setControlDelay(int delay) {
        int next = Math.max(delay, 1);
        if (tickDelay != next) {
            hbm$syncSet$CoolingHeatBlockEntity$tickDelay(next);
            markChanged();
        }
    }

    @Override
    public int getHeatStored() {
        return heatEnergy;
    }

    @Override
    public void setHeatStored(int remaining) {
        hbm$syncSet$CoolingHeatBlockEntity$heatEnergy(remaining);
        markChanged();
    }

    @Override
    public FluidTankNTM receivingTank() {
        return tanks[0];
    }

    @Override
    public FluidTankNTM deliveryTank() {
        return tanks[1];
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return sending;
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
    public FluidTankNTM[] settingsTanks() {
        return tanks;
    }

    @Override
    public FluidTankNTM pasteTank() {
        return tanks[0];
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.getItem() instanceof FluidIdentifierItem;
    }

    @Override
    public String[] getFunctionInfo() {
        return ROR;
    }

    @Override
    public @Nullable String getRadioValue(String key) {
        return switch (key) {
            case "VAL:hotfluid" -> Integer.toString(tanks[0].getFill());
            case "VAL:coldfluid" -> Integer.toString(tanks[1].getFill());
            case "VAL:heat" -> Integer.toString(heatEnergy);
            default -> null;
        };
    }

    @Override
    public CompoundTag copySettings(Level level, BlockPos pos) {
        var tag = FluidSettingsHandler.super.copySettings(level, pos);
        tag.putInt("toCool", amountToCool);
        return tag;
    }

    @Override
    public void pasteSettings(
            CompoundTag tag, int option, Level level, Player player, BlockPos pos) {
        tag.getInt("toCool").ifPresent(this::setCoolAmount);
        FluidSettingsHandler.super.pasteSettings(tag, option, level, player, pos);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.heaterHeatex");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new HeatexMenu(id, inventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("t0").ifPresent(tanks[0]::deserialize);
        input.child("t1").ifPresent(tanks[1]::deserialize);
        hbm$syncSet$CoolingHeatBlockEntity$heatEnergy(input.getIntOr("heatEnergy", heatEnergy));
        hbm$syncSet$CoolingHeatBlockEntity$amountToCool(input.getIntOr("toCool", amountToCool));
        hbm$syncSet$CoolingHeatBlockEntity$tickDelay(input.getIntOr("delay", tickDelay));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tanks[0].serialize(output.child("t0"));
        tanks[1].serialize(output.child("t1"));
        output.putInt("heatEnergy", heatEnergy);
        output.putInt("toCool", amountToCool);
        output.putInt("delay", tickDelay);
    }

    @Override
    public boolean initialMatchesSyncUnits() {
        return false;
    }

    @Override
    public long syncUnitMask() {
        return 31L;
    }

    @Override
    public void writeInitialSyncUnit(int unit, ByteBuf output) {
        if (unit == 0) tanks[0].packetSerialize(output);
        else writeSyncUnit(unit, output);
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> tanks[0].packetSerialize(output, syncInputFill);
            case 1 -> tanks[1].packetSerialize(output);
            case 2 -> output.writeInt(heatEnergy);
            case 3 -> output.writeInt(amountToCool);
            case 4 -> output.writeInt(tickDelay);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> tanks[0].packetDeserialize(input);
            case 1 -> tanks[1].packetDeserialize(input);
            case 2 -> hbm$syncSet$CoolingHeatBlockEntity$heatEnergy(input.readInt());
            case 3 -> hbm$syncSet$CoolingHeatBlockEntity$amountToCool(input.readInt());
            case 4 -> hbm$syncSet$CoolingHeatBlockEntity$tickDelay(input.readInt());
            default -> super.readSyncUnit(unit, input);
        }
    }

    public final FluidTankNTM hbm$syncArraySet$CoolingHeatBlockEntity$tanks(
            FluidTankNTM[] array, int index, FluidTankNTM next) {
        if (array[index] != next) {
            if (array == this.tanks && syncBound())
                com.hbm.packet.SyncBindings.unbind(this, array[index]);
            array[index] = next;
            if (array == this.tanks) {
                syncArrayChanged(array, index, 3, 3L);
            }
        }
        return next;
    }

    public final void hbm$syncSet$CoolingHeatBlockEntity$heatEnergy(int next) {
        if (this.heatEnergy != next) {
            this.heatEnergy = next;
            if ((4L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 4L);
        }
    }

    public final void hbm$syncSet$CoolingHeatBlockEntity$amountToCool(int next) {
        if (this.amountToCool != next) {
            this.amountToCool = next;
            if ((8L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 8L);
        }
    }

    public final void hbm$syncSet$CoolingHeatBlockEntity$tickDelay(int next) {
        if (this.tickDelay != next) {
            this.tickDelay = next;
            if ((16L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 16L);
        }
    }

    public final void hbm$syncSet$CoolingHeatBlockEntity$syncInputFill(int next) {
        if (this.syncInputFill != next) {
            this.syncInputFill = next;
            if ((1L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(CoolingHeatBlockEntity.class, "tanks", 3, 3L);
    }
}
