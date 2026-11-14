/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
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

package com.hbm.blockentity.machine.dfc;

import com.hbm.api.block.LaserHandler;
import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.api.fluidmk2.FluidTankHandler;
import com.hbm.api.redstoneoverradio.RadioCommandParser;
import com.hbm.api.redstoneoverradio.RadioControlHandler;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.CoreComponentBlock;
import com.hbm.inventory.container.CoreEmitterMenu;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.ModDamageTypes;
import com.hbm.packet.SyncUnitSchema;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

public class DfcEmitterBlockEntity extends MachineBlockEntity
        implements EnergyHandler,
                FluidTankHandler,
                LaserHandler,
                MenuProvider,
                SyncUnitSchema,
                RadioControlHandler {
    public static final long MAX_POWER = 1_000_000_000L;
    public static final int RANGE = 50;
    private static final String[] RADIO = {
        "FUN:setpower!percent", "FUN:toggle", "FUN:switch!on/off"
    };
    public long power;
    public int watts;
    public long prev;
    public int beam;
    public boolean isOn;
    public final FluidTankNTM tank = new FluidTankNTM(NTMFluids.CRYOGEL, 64_000);
    public long joules;
    private final FluidTankNTM[] intake = {tank};
    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

    public DfcEmitterBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.CORE_EMITTER.get(), pos, state);
    }

    protected DfcEmitterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 0);
        tank.bindOwner(this);
    }

    @Override
    public void tickServer() {
        setWatts(watts);
        hbm$syncSet$DfcEmitterBlockEntity$beam(0);
        if (joules > 0 || prev > 0) {
            if (tank.getFill() < 20) {
                level.setBlockAndUpdate(worldPosition, Blocks.LAVA.defaultBlockState());
                return;
            }
            tank.setFill(tank.getFill() - 20);
            setChanged();
        }
        if (isOn) {
            long demand = MAX_POWER * watts / 2000;
            if (power >= demand) {
                hbm$syncSet$DfcEmitterBlockEntity$power((long) (power - (demand)));
                joules += watts * 100L;
                setChanged();
            }
            if (prev != joules) {
                hbm$syncSet$DfcEmitterBlockEntity$prev(joules);
                setChanged();
            }
            if (joules > 0) {
                fire(joules * 95 / 100);
                joules = 0;
                setChanged();
            }
        } else if (joules != 0 || prev != 0) {
            joules = 0;
            hbm$syncSet$DfcEmitterBlockEntity$prev(0);
            setChanged();
        }
        networkPackNT(250);
    }

    private void fire(long energy) {
        var server = (ServerLevel) level;
        var facing = getBlockState().getValue(CoreComponentBlock.FACING);
        cursor.set(worldPosition);
        for (int distance = 1; distance <= RANGE; distance++) {
            cursor.move(facing);
            hbm$syncSet$DfcEmitterBlockEntity$beam(distance);
            var target = server.getBlockEntity(cursor);
            if (target instanceof DfcChamberBlockEntity chamber) {
                energy = chamber.amplify(energy);
                continue;
            }
            if (target instanceof LaserHandler receiver) {
                receiver.receiveLaserEnergy(server, cursor, energy, facing);
                break;
            }
            var state = server.getBlockState(cursor);
            if (state.isAir()) continue;
            if (state.getBlock() instanceof LiquidBlock) {
                server.removeBlock(cursor, false);
                server.playSound(
                        null, cursor, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1, 1);
            } else if (state.getBlock().getExplosionResistance() < 6000
                    && server.getRandom().nextInt(20) == 0) {
                server.destroyBlock(cursor, false);
            }
            break;
        }
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        int tx = x + facing.getStepX() * beam,
                ty = y + facing.getStepY() * beam,
                tz = z + facing.getStepZ() * beam;
        var damage = server.damageSources().source(ModDamageTypes.AMS_CORE);
        for (var entity :
                server.getEntities(
                        (Entity) null,
                        new AABB(
                                Math.min(x, tx) + .2,
                                Math.min(y, ty) + .2,
                                Math.min(z, tz) + .2,
                                Math.max(x, tx) + .8,
                                Math.max(y, ty) + .8,
                                Math.max(z, tz) + .8))) {
            entity.hurtServer(server, damage, 50);
            entity.igniteForSeconds(10);
        }
    }

    @Override
    public void receiveLaserEnergy(Level level, BlockPos pos, long energy, Direction travel) {
        if (travel.getOpposite() == getBlockState().getValue(CoreComponentBlock.FACING)) return;
        if (energy != 0) {
            joules += energy;
            setChanged();
        }
    }

    public void setWatts(int value) {
        int next = Math.clamp(value, 1, 100);
        if (watts != next) {
            hbm$syncSet$DfcEmitterBlockEntity$watts(next);
            setChanged();
        }
    }

    public void setInputPercent(int value) {
        int next = Math.clamp(value, 0, 100);
        if (watts != next) {
            hbm$syncSet$DfcEmitterBlockEntity$watts(next);
            setChanged();
        }
    }

    public void setActive(boolean active) {
        if (isOn != active) {
            hbm$syncSet$DfcEmitterBlockEntity$isOn(active);
            setChanged();
        }
    }

    public void toggle() {
        setActive(!isOn);
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
        hbm$syncSet$DfcEmitterBlockEntity$power(stored);
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
        return intake[index];
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return intake;
    }

    @Override
    public String[] getFunctionInfo() {
        return RADIO;
    }

    @Override
    public void runRadioFunction(String key, String[] params) {
        switch (key) {
            case "FUN:setpower" -> {
                if (params.length == 0) return;
                setInputPercent(RadioCommandParser.intParam(params[0], 0, 100));
            }
            case "FUN:toggle" -> toggle();
            case "FUN:switch" -> {
                if (params.length == 0) return;
                if (params[0].equals("on") && !isOn || params[0].equals("off") && isOn) toggle();
            }
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.dfcEmitter");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new CoreEmitterMenu(id, inventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$DfcEmitterBlockEntity$power(input.getLongOr("power", 0));
        hbm$syncSet$DfcEmitterBlockEntity$watts(input.getIntOr("watts", 0));
        joules = input.getLongOr("joules", 0);
        hbm$syncSet$DfcEmitterBlockEntity$prev(input.getLongOr("prev", 0));
        hbm$syncSet$DfcEmitterBlockEntity$isOn(input.getBooleanOr("isOn", false));
        input.child("tank").ifPresent(tank::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putInt("watts", watts);
        output.putLong("joules", joules);
        output.putLong("prev", prev);
        output.putBoolean("isOn", isOn);
        tank.serialize(output.child("tank"));
    }

    @Override
    public long syncUnitMask() {
        return 63;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(power);
            case 1 -> output.writeInt(watts);
            case 2 -> output.writeLong(prev);
            case 3 -> output.writeInt(beam);
            case 4 -> output.writeBoolean(isOn);
            case 5 -> tank.packetSerialize(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$DfcEmitterBlockEntity$power(input.readLong());
            case 1 -> hbm$syncSet$DfcEmitterBlockEntity$watts(input.readInt());
            case 2 -> hbm$syncSet$DfcEmitterBlockEntity$prev(input.readLong());
            case 3 -> hbm$syncSet$DfcEmitterBlockEntity$beam(input.readInt());
            case 4 -> hbm$syncSet$DfcEmitterBlockEntity$isOn(input.readBoolean());
            case 5 -> tank.packetDeserialize(input);
            default -> throw new IllegalArgumentException();
        }
    }

    public final void hbm$syncSet$DfcEmitterBlockEntity$power(long next) {
        if (this.power != next) {
            this.power = next;
            if ((1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1);
        }
    }

    public final void hbm$syncSet$DfcEmitterBlockEntity$watts(int next) {
        if (this.watts != next) {
            this.watts = next;
            if ((2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 2);
        }
    }

    public final void hbm$syncSet$DfcEmitterBlockEntity$prev(long next) {
        if (this.prev != next) {
            this.prev = next;
            if ((4) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 4);
        }
    }

    public final void hbm$syncSet$DfcEmitterBlockEntity$beam(int next) {
        if (this.beam != next) {
            this.beam = next;
            if ((8) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 8);
        }
    }

    public final void hbm$syncSet$DfcEmitterBlockEntity$isOn(boolean next) {
        if (this.isOn != next) {
            this.isOn = next;
            if ((16) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 16);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(DfcEmitterBlockEntity.class, "tank", 3, 32);
    }
}
