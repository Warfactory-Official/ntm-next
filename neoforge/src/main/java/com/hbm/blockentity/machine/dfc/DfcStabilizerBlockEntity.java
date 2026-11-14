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

package com.hbm.blockentity.machine.dfc;

import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.api.redstoneoverradio.RadioCommandParser;
import com.hbm.api.redstoneoverradio.RadioControlHandler;
import com.hbm.api.redstoneoverradio.RadioValueProvider;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.CoreComponentBlock;
import com.hbm.inventory.container.CoreStabilizerMenu;
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
import org.jspecify.annotations.Nullable;

public class DfcStabilizerBlockEntity extends MachineBlockEntity
        implements EnergyHandler,
                MenuProvider,
                SyncUnitSchema,
                RadioControlHandler,
                RadioValueProvider {
    public static final int SLOT_LENS = 0, SLOT_COUNT = 1, RANGE = 15;
    public static final long MAX_POWER = 2_500_000_000L;
    private static final String[] RADIO = {
        "VAL:durability", "VAL:durabilitypercent", "FUN:setpower!percent"
    };
    public long power;
    public int watts;
    public int beam;
    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

    public DfcStabilizerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CORE_STABILIZER.get(), pos, state, SLOT_COUNT);
    }

    public static boolean usableLens(ItemStack stack) {
        return stack.is(ModItems.AMS_LENS.get()) && stack.getDamageValue() < stack.getMaxDamage();
    }

    @Override
    public void tickServer() {
        setWatts(watts);
        hbm$syncSet$DfcStabilizerBlockEntity$beam(0);
        int square = watts * watts, demand = square * square;
        var lens = getItem(SLOT_LENS);
        if (power >= demand && usableLens(lens)) {
            var chamber =
                    DfcBeamPath.chamber(
                            level,
                            worldPosition,
                            getBlockState().getValue(CoreComponentBlock.FACING),
                            cursor,
                            RANGE);
            if (chamber != null) {
                if (chamber.field < watts) {
                    chamber.hbm$syncSet$DfcChamberBlockEntity$field(watts);
                    chamber.setChanged();
                }
                hbm$syncSet$DfcStabilizerBlockEntity$power((long) (power - (demand)));
                hbm$syncSet$DfcStabilizerBlockEntity$beam(cursor.distManhattan(worldPosition));
                int wear = lens.getDamageValue() + watts;
                if (wear >= lens.getMaxDamage()) inventory.set(SLOT_LENS, ItemStack.EMPTY);
                else lens.setDamageValue(wear);
                setChanged();
            }
        }
        networkPackNT(250);
    }

    public void setWatts(int value) {
        int next = Math.clamp(value, 1, 100);
        if (watts != next) {
            hbm$syncSet$DfcStabilizerBlockEntity$watts(next);
            setChanged();
        }
    }

    public void setInputPercent(int value) {
        int next = Math.clamp(value, 0, 100);
        if (watts != next) {
            hbm$syncSet$DfcStabilizerBlockEntity$watts(next);
            setChanged();
        }
    }

    public long getPowerScaled(long scale) {
        return power * scale / MAX_POWER;
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long stored) {
        hbm$syncSet$DfcStabilizerBlockEntity$power(stored);
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == SLOT_LENS && stack.is(ModItems.AMS_LENS.get());
    }

    @Override
    public String[] getFunctionInfo() {
        return RADIO;
    }

    @Override
    public @Nullable String getRadioValue(String key) {
        var lens = getItem(SLOT_LENS);
        long remaining =
                lens.is(ModItems.AMS_LENS.get())
                        ? (long) lens.getMaxDamage() - lens.getDamageValue()
                        : 0;
        return switch (key) {
            case "VAL:durability" -> Long.toString(remaining);
            case "VAL:durabilitypercent" ->
                    Long.toString(
                            lens.is(ModItems.AMS_LENS.get())
                                    ? remaining * 100 / lens.getMaxDamage()
                                    : 0);
            default -> null;
        };
    }

    @Override
    public void runRadioFunction(String key, String[] params) {
        if (key.equals("FUN:setpower") && params.length > 0) {
            setInputPercent(RadioCommandParser.intParam(params[0], 0, 100));
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.dfcStabilizer");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new CoreStabilizerMenu(id, inventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$DfcStabilizerBlockEntity$power(input.getLongOr("power", 0));
        hbm$syncSet$DfcStabilizerBlockEntity$watts(input.getIntOr("watts", 0));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putInt("watts", watts);
    }

    @Override
    public long syncUnitMask() {
        return 7;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(power);
            case 1 -> output.writeInt(watts);
            case 2 -> output.writeInt(beam);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$DfcStabilizerBlockEntity$power(input.readLong());
            case 1 -> hbm$syncSet$DfcStabilizerBlockEntity$watts(input.readInt());
            case 2 -> hbm$syncSet$DfcStabilizerBlockEntity$beam(input.readInt());
            default -> throw new IllegalArgumentException();
        }
    }

    public final void hbm$syncSet$DfcStabilizerBlockEntity$power(long next) {
        if (this.power != next) {
            this.power = next;
            if ((1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1);
        }
    }

    public final void hbm$syncSet$DfcStabilizerBlockEntity$watts(int next) {
        if (this.watts != next) {
            this.watts = next;
            if ((2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 2);
        }
    }

    public final void hbm$syncSet$DfcStabilizerBlockEntity$beam(int next) {
        if (this.beam != next) {
            this.beam = next;
            if ((4) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 4);
        }
    }
}
