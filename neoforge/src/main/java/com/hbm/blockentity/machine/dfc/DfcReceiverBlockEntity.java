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

import com.hbm.api.block.LaserHandler;
import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.api.fluidmk2.FluidTankHandler;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.CoreComponentBlock;
import com.hbm.inventory.container.CoreReceiverMenu;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.packet.SyncUnitSchema;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class DfcReceiverBlockEntity extends MachineBlockEntity
        implements EnergyHandler, FluidTankHandler, LaserHandler, MenuProvider, SyncUnitSchema {
    public static final long HE_PER_SPK = 5000L;
    public long power;
    public long joules;
    public final FluidTankNTM tank = new FluidTankNTM(NTMFluids.CRYOGEL, 64_000);
    private final FluidTankNTM[] intake = {tank};

    public DfcReceiverBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.CORE_RECEIVER.get(), pos, state);
    }

    protected DfcReceiverBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 0);
        tank.bindOwner(this);
    }

    @Override
    public void tickServer() {
        long next = joules * HE_PER_SPK;
        if (power != next) {
            power = next;
            setChanged();
        }
        if (joules > 0) {
            if (tank.getFill() < 20) {
                level.setBlockAndUpdate(worldPosition, Blocks.LAVA.defaultBlockState());
                return;
            }
            tank.setFill(tank.getFill() - 20);
            setChanged();
        }
        networkPackNT(50);
        if (joules != 0) {
            hbm$syncSet$DfcReceiverBlockEntity$joules(0);
            setChanged();
        }
    }

    @Override
    public void receiveLaserEnergy(Level level, BlockPos pos, long energy, Direction travel) {
        if (travel.getOpposite() == getBlockState().getValue(CoreComponentBlock.FACING)) {
            if (energy != 0) {
                hbm$syncSet$DfcReceiverBlockEntity$joules((long) (joules + (energy)));
                setChanged();
            }
        } else {
            level.destroyBlock(worldPosition, false);
            level.explode(
                    null,
                    worldPosition.getX() + .5,
                    worldPosition.getY() + .5,
                    worldPosition.getZ() + .5,
                    2.5F,
                    Level.ExplosionInteraction.BLOCK);
        }
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long stored) {
        power = stored;
    }

    @Override
    public long getMaxPower() {
        return power;
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
    protected Component getDefaultName() {
        return Component.translatable("container.dfcReceiver");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new CoreReceiverMenu(id, inventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        power = input.getLongOr("power", 0);
        hbm$syncSet$DfcReceiverBlockEntity$joules(input.getLongOr("joules", 0));
        input.child("tank").ifPresent(tank::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putLong("joules", joules);
        tank.serialize(output.child("tank"));
    }

    @Override
    public long syncUnitMask() {
        return 3;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(joules);
            case 1 -> tank.packetSerialize(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$DfcReceiverBlockEntity$joules(input.readLong());
            case 1 -> tank.packetDeserialize(input);
            default -> throw new IllegalArgumentException();
        }
    }

    public final void hbm$syncSet$DfcReceiverBlockEntity$joules(long next) {
        if (this.joules != next) {
            this.joules = next;
            if ((1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(DfcReceiverBlockEntity.class, "tank", 3, 2);
    }
}
