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

package com.hbm.blockentity.machine.reactor;

import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blockentity.machine.ControlCurve;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.inventory.container.ReactorControlMenu;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.packet.SyncUnitSchema;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class ReactorControlBlockEntity extends MachineBlockEntity
        implements SyncUnitSchema, MenuProvider {

    public static final int SENSOR_SLOT = 0;

    public int internalHeat;
    public double actualRodPosition;
    public int flux;
    public boolean linked;
    public double lowerRodPercent;
    public double upperRodPercent;
    public double lowerHeat;
    public double upperHeat;
    public ControlCurve curve;

    {
        hbm$syncSet$ReactorControlBlockEntity$curve(ControlCurve.LINEAR);
    }

    public ReactorControlBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REACTOR_CONTROL.get(), pos, state, 1);
    }

    @Override
    public void tickServer() {
        var sensor = getItem(SENSOR_SLOT);
        var destination =
                sensor.is(ModItems.REACTOR_SENSOR.get())
                        ? sensor.get(ModDataComponents.REACTOR_POS.get())
                        : null;
        ResearchReactorBlockEntity reactor = null;
        if (destination != null) {
            var pos = BlockPos.of(destination);
            if (level.hasChunkAt(pos)) {
                var owner =
                        MultiblockSurface.resolveOwner(level, pos, level.getBlockState(pos), null);
                if (owner == null) owner = level.getBlockEntity(pos);
                if (owner instanceof ResearchReactorBlockEntity column) reactor = column;
            }
        }
        boolean nowLinked = reactor != null;
        if (linked != nowLinked) {
            hbm$syncSet$ReactorControlBlockEntity$linked(nowLinked);
            setChanged();
        }
        if (reactor != null) {
            int oldOutput = comparatorOutput();
            hbm$syncSet$ReactorControlBlockEntity$internalHeat(reactor.heat);
            hbm$syncSet$ReactorControlBlockEntity$actualRodPosition(reactor.rodPosition);
            hbm$syncSet$ReactorControlBlockEntity$flux(reactor.totalFlux);
            if (oldOutput != comparatorOutput()) {
                level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
            }
            double requested =
                    curve.target(
                            internalHeat, lowerHeat, upperHeat, lowerRodPercent, upperRodPercent);
            if (requested != actualRodPosition) {
                reactor.requestRodPosition(requested);
            }
        }
        networkPackNT(150);
    }

    public int displayRodPercent() {
        return linked ? (int) (actualRodPosition * 100D) : 0;
    }

    public int displayFlux() {
        return linked ? flux : 0;
    }

    public int displayCelsius() {
        return linked ? (int) Math.round(internalHeat * 0.00002D * 980D + 20D) : 0;
    }

    public int comparatorOutput() {
        return (int) Math.ceil(internalHeat * 15D / 50000D);
    }

    @Override
    public int getComparatorPower() {
        return comparatorOutput();
    }

    public void setCurve(ControlCurve requested) {
        if (curve == requested) return;
        hbm$syncSet$ReactorControlBlockEntity$curve(requested);
        setChanged();
    }

    public void setBand(
            double lowerRod, double upperRod, double lowerTemperature, double upperTemperature) {
        if (lowerRodPercent == lowerRod
                && upperRodPercent == upperRod
                && lowerHeat == lowerTemperature
                && upperHeat == upperTemperature) return;
        hbm$syncSet$ReactorControlBlockEntity$lowerRodPercent(lowerRod);
        hbm$syncSet$ReactorControlBlockEntity$upperRodPercent(upperRod);
        hbm$syncSet$ReactorControlBlockEntity$lowerHeat(lowerTemperature);
        hbm$syncSet$ReactorControlBlockEntity$upperHeat(upperTemperature);
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == SENSOR_SLOT && stack.is(ModItems.REACTOR_SENSOR.get());
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.reactorControl");
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new ReactorControlMenu(containerId, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$ReactorControlBlockEntity$linked(input.getBooleanOr("isLinked", false));
        hbm$syncSet$ReactorControlBlockEntity$lowerRodPercent(
                savedBound(input, "levelLower", 100D));
        hbm$syncSet$ReactorControlBlockEntity$upperRodPercent(
                savedBound(input, "levelUpper", 100D));
        hbm$syncSet$ReactorControlBlockEntity$lowerHeat(savedBound(input, "heatLower", 50000D));
        hbm$syncSet$ReactorControlBlockEntity$upperHeat(savedBound(input, "heatUpper", 50000D));
        hbm$syncSet$ReactorControlBlockEntity$curve(
                ControlCurve.VALUES[input.getIntOr("function", 0)]);
        hbm$syncSet$ReactorControlBlockEntity$internalHeat(0);
        hbm$syncSet$ReactorControlBlockEntity$actualRodPosition(0D);
        hbm$syncSet$ReactorControlBlockEntity$flux(0);
    }

    private static double savedBound(ValueInput input, String key, double maximum) {
        double value = input.getDoubleOr(key, 0D);
        if (!Double.isFinite(value) || value < 0D || value > maximum)
            throw new IllegalArgumentException(key);
        return value;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("isLinked", linked);
        output.putDouble("levelLower", lowerRodPercent);
        output.putDouble("levelUpper", upperRodPercent);
        output.putDouble("heatLower", lowerHeat);
        output.putDouble("heatUpper", upperHeat);
        output.putInt("function", curve.ordinal());
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x1ffL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeInt(internalHeat);
            case 1 -> output.writeDouble(actualRodPosition);
            case 2 -> output.writeInt(flux);
            case 3 -> output.writeBoolean(linked);
            case 4 -> output.writeDouble(lowerRodPercent);
            case 5 -> output.writeDouble(upperRodPercent);
            case 6 -> output.writeDouble(lowerHeat);
            case 7 -> output.writeDouble(upperHeat);
            case 8 -> output.writeByte(curve.ordinal());
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$ReactorControlBlockEntity$internalHeat(input.readInt());
            case 1 -> hbm$syncSet$ReactorControlBlockEntity$actualRodPosition(input.readDouble());
            case 2 -> hbm$syncSet$ReactorControlBlockEntity$flux(input.readInt());
            case 3 -> hbm$syncSet$ReactorControlBlockEntity$linked(input.readBoolean());
            case 4 -> hbm$syncSet$ReactorControlBlockEntity$lowerRodPercent(input.readDouble());
            case 5 -> hbm$syncSet$ReactorControlBlockEntity$upperRodPercent(input.readDouble());
            case 6 -> hbm$syncSet$ReactorControlBlockEntity$lowerHeat(input.readDouble());
            case 7 -> hbm$syncSet$ReactorControlBlockEntity$upperHeat(input.readDouble());
            case 8 ->
                    hbm$syncSet$ReactorControlBlockEntity$curve(
                            ControlCurve.VALUES[input.readUnsignedByte()]);
            default -> super.readSyncUnit(unit, input);
        }
    }

    public final void hbm$syncSet$ReactorControlBlockEntity$internalHeat(int next) {
        if (this.internalHeat != next) {
            this.internalHeat = next;
            if ((1L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L);
        }
    }

    public final void hbm$syncSet$ReactorControlBlockEntity$actualRodPosition(double next) {
        if (Double.doubleToRawLongBits(this.actualRodPosition)
                != Double.doubleToRawLongBits(next)) {
            this.actualRodPosition = next;
            if ((1L << 1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 1);
        }
    }

    public final void hbm$syncSet$ReactorControlBlockEntity$flux(int next) {
        if (this.flux != next) {
            this.flux = next;
            if ((1L << 2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 2);
        }
    }

    public final void hbm$syncSet$ReactorControlBlockEntity$linked(boolean next) {
        if (this.linked != next) {
            this.linked = next;
            if ((1L << 3) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 3);
        }
    }

    public final void hbm$syncSet$ReactorControlBlockEntity$lowerRodPercent(double next) {
        if (Double.doubleToRawLongBits(this.lowerRodPercent) != Double.doubleToRawLongBits(next)) {
            this.lowerRodPercent = next;
            if ((1L << 4) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 4);
        }
    }

    public final void hbm$syncSet$ReactorControlBlockEntity$upperRodPercent(double next) {
        if (Double.doubleToRawLongBits(this.upperRodPercent) != Double.doubleToRawLongBits(next)) {
            this.upperRodPercent = next;
            if ((1L << 5) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 5);
        }
    }

    public final void hbm$syncSet$ReactorControlBlockEntity$lowerHeat(double next) {
        if (Double.doubleToRawLongBits(this.lowerHeat) != Double.doubleToRawLongBits(next)) {
            this.lowerHeat = next;
            if ((1L << 6) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 6);
        }
    }

    public final void hbm$syncSet$ReactorControlBlockEntity$upperHeat(double next) {
        if (Double.doubleToRawLongBits(this.upperHeat) != Double.doubleToRawLongBits(next)) {
            this.upperHeat = next;
            if ((1L << 7) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 7);
        }
    }

    public final void hbm$syncSet$ReactorControlBlockEntity$curve(ControlCurve next) {
        if (this.curve != next) {
            boolean bound = syncBound();
            if (bound) com.hbm.packet.SyncBindings.unbind(this, this.curve);
            this.curve = next;
            if (bound) bindSyncValue(next, 3, 1L << 8);
            if ((1L << 8) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 8);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(ReactorControlBlockEntity.class, "curve", 3, 1L << 8);
    }
}
