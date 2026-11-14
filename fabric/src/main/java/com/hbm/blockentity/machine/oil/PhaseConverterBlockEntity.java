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

package com.hbm.blockentity.machine.oil;

import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.api.energymk2.EnergyItem;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.FluidTankHandler;
import com.hbm.blockentity.FluidSettingsHandler;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blockentity.UpgradeInfoProvider;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.machine.upgrade.MachineUpgradeItem;
import com.hbm.items.machine.upgrade.UpgradeInfo;
import com.hbm.items.machine.upgrade.UpgradeLimits;
import com.hbm.items.machine.upgrade.UpgradeManager;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.packet.SyncUnitSchema;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public abstract sealed class PhaseConverterBlockEntity extends MachineBlockEntity
        implements FluidTankAccess,
                EnergyHandler,
                FluidTankHandler,
                MenuProvider,
                FluidSettingsHandler,
                UpgradeInfoProvider,
                SyncUnitSchema
        permits LiquefactorBlockEntity, SolidifierBlockEntity {

    public static final int SLOT_BATTERY = 1;
    public static final int SLOT_UPGRADE_START = 2;
    public static final int SLOT_UPGRADE_END = 3;

    public static final long MAX_POWER = 100_000L;
    public static final int BASE_DRAW = 250;
    public static final int BASE_CYCLE = 60;

    private static final int CYCLE_PER_SPEED = 15;
    private static final int TANK_CAPACITY = 24_000;
    private static final UpgradeLimits UPGRADES =
            UpgradeLimits.of(UpgradeType.SPEED, 3).and(UpgradeType.POWER, 3);

    public long power;
    public int progress;
    public int draw;
    public int cycle;
    public final FluidTankNTM tank = new FluidTankNTM(TANK_CAPACITY);

    protected final FluidTankNTM[] tankArray = {tank};
    private final UpgradeManager upgrades = new UpgradeManager(this);

    PhaseConverterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int slots) {
        super(type, pos, state, slots);
    }

    @Override
    public final void tickServer() {
        long charged = ItemEnergyTransfer.extract(this, SLOT_BATTERY, MAX_POWER - power, false);
        if (charged != 0L) hbm$syncSet$PhaseConverterBlockEntity$power((long) (power + (charged)));
        prepare();
        upgrades.scan(this, SLOT_UPGRADE_START, SLOT_UPGRADE_END);
        int speed = upgrades.getLevel(UpgradeType.SPEED);
        int nextCycle = BASE_CYCLE - CYCLE_PER_SPEED * speed;
        int nextDraw = (BASE_DRAW + BASE_DRAW * speed) / (upgrades.getLevel(UpgradeType.POWER) + 1);
        if (cycle != nextCycle) hbm$syncSet$PhaseConverterBlockEntity$cycle(nextCycle);
        if (draw != nextDraw) hbm$syncSet$PhaseConverterBlockEntity$draw(nextDraw);

        if (admits()) {
            hbm$syncSet$PhaseConverterBlockEntity$power((long) (power - (draw)));
            hbm$syncSet$PhaseConverterBlockEntity$progress((int) (progress + 1));
            if (progress >= cycle) {
                convert();
                hbm$syncSet$PhaseConverterBlockEntity$progress(0);
            }
            markChanged();
        } else if (progress != 0) {
            hbm$syncSet$PhaseConverterBlockEntity$progress(0);
            markChanged();
        }
        push();
        networkPackNT(50);
    }

    void prepare() {}

    public final boolean admits() {
        return power >= draw && ready();
    }

    abstract boolean ready();

    abstract void convert();

    void push() {}

    @Override
    public UpgradeLimits upgradeCaps() {
        return UPGRADES;
    }

    @Override
    public void appendUpgradeInfo(UpgradeType type, int level, List<Component> tooltip) {
        if (type == UpgradeType.SPEED) UpgradeInfo.speedAndPower(tooltip, -25 * level, 100 * level);
        else if (type == UpgradeType.POWER)
            UpgradeInfo.percent(
                    tooltip, "upgrade.consumption", UpgradeInfo.reduction(level + 1), true);
    }

    boolean canPlaceSharedSlot(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) return EnergyItem.matches(stack);
        return slot >= SLOT_UPGRADE_START
                && slot <= SLOT_UPGRADE_END
                && MachineUpgradeItem.isUpgrade(stack);
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long stored) {
        hbm$syncSet$PhaseConverterBlockEntity$power(Math.clamp(stored, 0L, MAX_POWER));
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    public int tankCount() {
        return 1;
    }

    @Override
    public FluidTankNTM tank(int index) {
        return tankArray[index];
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$PhaseConverterBlockEntity$power(input.getLongOr("power", 0L));
        hbm$syncSet$PhaseConverterBlockEntity$progress(input.getIntOr("progress", 0));
        input.child("tank").ifPresent(tank::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putInt("progress", progress);
        tank.serialize(output.child("tank"));
    }

    @Override
    public long syncUnitMask() {
        return 0x1fL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(power);
            case 1 -> output.writeInt(progress);
            case 2 -> output.writeInt(draw);
            case 3 -> output.writeInt(cycle);
            case 4 -> tank.packetSerialize(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$PhaseConverterBlockEntity$power(input.readLong());
            case 1 -> hbm$syncSet$PhaseConverterBlockEntity$progress(input.readInt());
            case 2 -> hbm$syncSet$PhaseConverterBlockEntity$draw(input.readInt());
            case 3 -> hbm$syncSet$PhaseConverterBlockEntity$cycle(input.readInt());
            case 4 -> tank.packetDeserialize(input);
            default -> throw new IllegalArgumentException();
        }
    }

    public final void hbm$syncSet$PhaseConverterBlockEntity$power(long next) {
        if (this.power != next) {
            this.power = next;
            if ((1L << 0) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 0);
        }
    }

    public final void hbm$syncSet$PhaseConverterBlockEntity$progress(int next) {
        if (this.progress != next) {
            this.progress = next;
            if ((1L << 1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 1);
        }
    }

    public final void hbm$syncSet$PhaseConverterBlockEntity$draw(int next) {
        if (this.draw != next) {
            this.draw = next;
            if ((1L << 2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 2);
        }
    }

    public final void hbm$syncSet$PhaseConverterBlockEntity$cycle(int next) {
        if (this.cycle != next) {
            this.cycle = next;
            if ((1L << 3) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 3);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(PhaseConverterBlockEntity.class, "tank", 3, 1L << 4);
    }
}
