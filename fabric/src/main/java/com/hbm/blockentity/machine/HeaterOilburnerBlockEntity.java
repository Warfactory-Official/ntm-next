/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.blockentity.machine;

import com.hbm.api.environment.Environment;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankHandler;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.api.redstoneoverradio.RadioCommandParser;
import com.hbm.api.redstoneoverradio.RadioControlHandler;
import com.hbm.api.redstoneoverradio.RadioValueProvider;
import com.hbm.api.tile.HeatHandler;
import com.hbm.blockentity.FluidSettingsHandler;
import com.hbm.blockentity.PollutingMachineBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.OilburnerMenu;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FlammableTrait;
import com.hbm.inventory.fluid.trait.FluidReleaseType;
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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import static com.hbm.api.redstoneoverradio.RadioCommandParser.FUN;
import static com.hbm.api.redstoneoverradio.RadioCommandParser.NAME_SEP;
import static com.hbm.api.redstoneoverradio.RadioCommandParser.VAL;

public class HeaterOilburnerBlockEntity extends PollutingMachineBlockEntity
        implements FluidTankAccess,
                FluidTankHandler,
                HeatHandler,
                FluidSettingsHandler,
                MenuProvider,
                RadioValueProvider,
                RadioControlHandler,
                SyncUnitSchema {

    public static final String[] ROR =
            new String[] {
                VAL + "heat",
                VAL + "fuel",
                VAL + "burnRate",
                VAL + "state",
                FUN + "setstate" + NAME_SEP + "active",
                FUN + "setburnrate" + NAME_SEP + "rate"
            };
    public static final int SLOT_FLUID_IN = 0;
    public static final int SLOT_FLUID_OUT = 1;
    public static final int SLOT_FLUID_ID = 2;

    public static final int MAX_HEAT_ENERGY = 100_000;
    public static final int TANK_CAPACITY = 16_000;
    public final FluidTankNTM tank;
    private final FluidTankNTM[] receiving;
    public boolean isOn;

    {
        hbm$syncSet$HeaterOilburnerBlockEntity$isOn(false);
    }

    public int setting;

    {
        hbm$syncSet$HeaterOilburnerBlockEntity$setting(1);
    }

    public int heatEnergy;

    private final FluidFlushOutputs flush = new FluidFlushOutputs();

    public HeaterOilburnerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.OILBURNER.get(), pos, state, 3, 100);
        tank = new FluidTankNTM(NTMFluids.HEATINGOIL, TANK_CAPACITY);
        receiving = new FluidTankNTM[] {tank};
    }

    @Override
    public void tickServer() {
        boolean changed = tank.loadTank(SLOT_FLUID_IN, SLOT_FLUID_OUT, inventory);
        changed |= tank.setType(SLOT_FLUID_ID, SLOT_FLUID_ID, inventory);
        if (changed) setChanged();

        boolean shouldCool = true;

        if (isOn && heatEnergy < MAX_HEAT_ENERGY) {
            FlammableTrait trait =
                    NTMFluidProperties.getTrait(tank.getTankType(), FlammableTrait.class);
            if (trait != null) {
                int burnRate = setting;
                int toBurn = Math.min(burnRate, tank.getFill());
                if (toBurn > 0 ? breatheAir(toBurn) : !Environment.installed()) {
                    tank.setFill(tank.getFill() - toBurn);

                    int heat = (int) (trait.heatPerBucket() / 1000L);
                    this.hbm$syncSet$HeaterOilburnerBlockEntity$heatEnergy(
                            (int) (this.heatEnergy + (heat * toBurn)));
                    if (toBurn > 0) markChanged();

                    if (TickPhase.every(this, 5) && toBurn > 0) {
                        pollute(tank.getTankType(), FluidReleaseType.BURN, toBurn * 5);
                    }

                    shouldCool = false;
                }
            }
        }

        if (this.heatEnergy >= MAX_HEAT_ENERGY) shouldCool = false;

        if (shouldCool) {
            if (this.heatEnergy > 0) markChanged();
            this.hbm$syncSet$HeaterOilburnerBlockEntity$heatEnergy(
                    Math.max(this.heatEnergy - Math.max(this.heatEnergy / 1000, 1), 0));
        }

        flush.provide((ServerLevel) level, this);

        networkPackNT(25);
    }

    public void toggleSetting() {
        hbm$syncSet$HeaterOilburnerBlockEntity$setting((int) (setting + 1));
        if (setting > 10) hbm$syncSet$HeaterOilburnerBlockEntity$setting(1);
    }

    @Override
    public int getHeatStored() {
        return heatEnergy;
    }

    @Override
    public void setHeatStored(int remaining) {
        hbm$syncSet$HeaterOilburnerBlockEntity$heatEnergy(remaining);
        markChanged();
    }

    public void toggleFromControl() {
        hbm$syncSet$HeaterOilburnerBlockEntity$isOn(!isOn);
        setChanged();
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return receiving;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return getSmokeTanks();
    }

    @Override
    public void declareFlush(FlushLanes out) {
        for (FluidTankNTM tank : getSendingTanks()) out.add(tank, FlushFaces.own());
    }

    @Override
    public int tankCount() {
        return 1;
    }

    @Override
    public FluidTankNTM tank(int index) {
        if (index != 0) throw new IndexOutOfBoundsException(index);
        return tank;
    }

    @Override
    public CompoundTag copySettings(Level level, BlockPos pos) {
        CompoundTag tag = FluidSettingsHandler.super.copySettings(level, pos);
        tag.putInt("burnRate", setting);
        tag.putBoolean("isOn", isOn);
        return tag;
    }

    @Override
    public void pasteSettings(
            CompoundTag nbt, int index, Level level, Player player, BlockPos pos) {
        FluidSettingsHandler.super.pasteSettings(nbt, index, level, player, pos);
        if (nbt.contains("isOn"))
            hbm$syncSet$HeaterOilburnerBlockEntity$isOn(nbt.getBooleanOr("isOn", isOn));
        if (nbt.contains("burnRate"))
            hbm$syncSet$HeaterOilburnerBlockEntity$setting(nbt.getIntOr("burnRate", setting));
        markChanged();
    }

    @Override
    public String[] getFunctionInfo() {
        return ROR;
    }

    @Override
    public @Nullable String getRadioValue(String name) {
        if ((VAL + "heat").equals(name)) return "" + heatEnergy;
        if ((VAL + "fuel").equals(name)) return "" + tank.getFill();
        if ((VAL + "burnrate").equals(name)) return "" + setting;
        if ((VAL + "state").equals(name)) return isOn ? "1" : "0";
        return null;
    }

    @Override
    public void runRadioFunction(String name, String[] params) {
        if ((FUN + "setstate").equals(name) && params.length > 0) {
            this.hbm$syncSet$HeaterOilburnerBlockEntity$isOn(params[0].equals("1"));
            markChanged();
            return;
        }
        if ((FUN + "setburnrate").equals(name) && params.length > 0) {
            this.hbm$syncSet$HeaterOilburnerBlockEntity$setting(
                    RadioCommandParser.intParam(params[0], 1, 10));
            markChanged();
            return;
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_FLUID_ID -> stack.getItem() instanceof FluidIdentifierItem;
            case SLOT_FLUID_IN -> true;
            default -> false;
        };
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.heaterOilburner");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new OilburnerMenu(containerId, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("tank").ifPresent(tank::deserialize);
        hbm$syncSet$HeaterOilburnerBlockEntity$isOn(input.getBooleanOr("isOn", isOn));
        hbm$syncSet$HeaterOilburnerBlockEntity$heatEnergy(input.getIntOr("heatEnergy", heatEnergy));
        hbm$syncSet$HeaterOilburnerBlockEntity$setting(input.getIntOr("setting", setting));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tank.serialize(output.child("tank"));
        output.putBoolean("isOn", isOn);
        output.putInt("heatEnergy", heatEnergy);
        output.putInt("setting", setting);
    }

    private void writeSetting(ByteBuf output) {
        output.writeByte(setting);
    }

    private void readSetting(ByteBuf input) {
        hbm$syncSet$HeaterOilburnerBlockEntity$setting(input.readByte());
    }

    @Override
    public long syncUnitMask() {
        return 0xfL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> this.tank.packetSerialize(output);
            case 1 -> output.writeBoolean(this.isOn);
            case 2 -> output.writeInt(this.heatEnergy);
            case 3 -> writeSetting(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.tank.packetDeserialize(input);
            case 1 -> this.hbm$syncSet$HeaterOilburnerBlockEntity$isOn(input.readBoolean());
            case 2 -> this.hbm$syncSet$HeaterOilburnerBlockEntity$heatEnergy(input.readInt());
            case 3 -> readSetting(input);
            default -> throw new IllegalArgumentException();
        }
    }

    public final void hbm$syncSet$HeaterOilburnerBlockEntity$isOn(boolean next) {
        if (this.isOn != next) {
            this.isOn = next;
            if ((1L << 1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 1);
        }
    }

    public final void hbm$syncSet$HeaterOilburnerBlockEntity$setting(int next) {
        if (this.setting != next) {
            this.setting = next;
            if ((1L << 3) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 3);
        }
    }

    public final void hbm$syncSet$HeaterOilburnerBlockEntity$heatEnergy(int next) {
        if (this.heatEnergy != next) {
            this.heatEnergy = next;
            if ((1L << 2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 2);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(HeaterOilburnerBlockEntity.class, "tank", 3, 1L << 0);
    }
}
