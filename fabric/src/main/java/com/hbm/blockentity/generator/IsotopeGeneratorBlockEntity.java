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

package com.hbm.blockentity.generator;

import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.RTGMenu;
import com.hbm.items.machine.RTGPelletItem;
import com.hbm.items.machine.RtgFuelHandler;
import com.hbm.packet.SyncUnitSchema;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class IsotopeGeneratorBlockEntity extends MachineBlockEntity
        implements EnergyHandler, MenuProvider, SyncUnitSchema {

    public static final int SLOT_COUNT = 15;
    public static final int[] SLOT_IO = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14};
    public static final long MAX_POWER = 100_000;
    public static final int POWER_PER_HEAT = 5;

    public int heat;
    public long power;

    public IsotopeGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_RTG.get(), pos, state, SLOT_COUNT);
    }

    public static int maxHeat() {
        return RtgFuelHandler.current().decays() ? 600 : 200;
    }

    public boolean hasHeat() {
        return RTGPelletItem.anyIn(inventory, SLOT_IO);
    }

    @Override
    public void tickServer() {
        var policy = RtgFuelHandler.current();
        int previousHeat = heat;
        long previousPower = power;
        heat = Math.min(policy.tickPellets(this, SLOT_IO), policy.decays() ? 600 : 200);
        hbm$syncSet$IsotopeGeneratorBlockEntity$power(
                Math.min(MAX_POWER, power + (long) POWER_PER_HEAT * heat));
        if (heat != previousHeat || power != previousPower) setChanged();
        networkPackNT(50);
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long stored) {
        hbm$syncSet$IsotopeGeneratorBlockEntity$power(stored);
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOT_IO;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.getItem() instanceof RTGPelletItem;
    }

    @Override
    protected double interactionRangeSq() {
        return 64;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.rtg");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new RTGMenu(id, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$IsotopeGeneratorBlockEntity$power(input.getLongOr("power", power));
        heat = input.getIntOr("heat", heat);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putInt("heat", heat);
    }

    @Override
    public long syncUnitMask() {
        return 1;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        if (unit == 0) output.writeLong(power);
        else super.writeSyncUnit(unit, output);
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        if (unit == 0) hbm$syncSet$IsotopeGeneratorBlockEntity$power(input.readLong());
        else super.readSyncUnit(unit, input);
    }

    public final void hbm$syncSet$IsotopeGeneratorBlockEntity$power(long next) {
        if (this.power != next) {
            this.power = next;
            if ((1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1);
        }
    }

    static {
        com.hbm.inventory.container.SyncedData.fields(IsotopeGeneratorBlockEntity.class, "heat");
    }
}
