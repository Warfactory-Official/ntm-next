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

import com.hbm.api.fluidmk2.FluidTankHandler;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.CoreComponentBlock;
import com.hbm.inventory.container.CoreInjectorMenu;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
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

public class DfcInjectorBlockEntity extends MachineBlockEntity
        implements FluidTankHandler, MenuProvider, SyncUnitSchema {
    public static final int SLOT_FILL_A = 0,
            SLOT_DRAIN_A = 1,
            SLOT_FILL_B = 2,
            SLOT_DRAIN_B = 3,
            SLOT_COUNT = 4;
    public static final int RANGE = 15;
    public int beam;
    public final FluidTankNTM[] tanks = {
        new FluidTankNTM(NTMFluids.DEUTERIUM, 128_000), new FluidTankNTM(NTMFluids.TRITIUM, 128_000)
    };
    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

    public DfcInjectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CORE_INJECTOR.get(), pos, state, SLOT_COUNT);
        for (var tank : tanks) tank.bindOwner(this);
    }

    @Override
    public void tickServer() {
        if (tanks[0].setType(SLOT_FILL_A, SLOT_DRAIN_A, inventory)) setChanged();
        if (tanks[1].setType(SLOT_FILL_B, SLOT_DRAIN_B, inventory)) setChanged();
        hbm$syncSet$DfcInjectorBlockEntity$beam(0);
        var chamber =
                DfcBeamPath.chamber(
                        level,
                        worldPosition,
                        getBlockState().getValue(CoreComponentBlock.FACING),
                        cursor,
                        RANGE);
        if (chamber != null) {
            hbm$syncSet$DfcInjectorBlockEntity$beam(cursor.distManhattan(worldPosition));
            transfer(tanks[0], chamber.tanks[0], chamber);
            transfer(tanks[1], chamber.tanks[1], chamber);
        }
        networkPackNT(250);
    }

    private void transfer(FluidTankNTM source, FluidTankNTM target, DfcChamberBlockEntity chamber) {
        if (target.getDeclaredFluid() != source.getDeclaredFluid()) {
            if (target.getFill() > 0) return;
            target.setTankTypeByIdentifier(source.getDeclaredFluid());
            chamber.setChanged();
        }
        var fluid = source.getFluid();
        if (!target.accepts(fluid, source.getComponents())) return;
        int moved = target.receive(fluid, source.getComponents(), source.getFill());
        if (moved == 0) return;
        source.setFill(source.getFill() - moved);
        setChanged();
        chamber.setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == SLOT_FILL_A || slot == SLOT_FILL_B;
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
    public FluidTankNTM[] getReceivingTanks() {
        return tanks;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.dfcInjector");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new CoreInjectorMenu(id, inventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("fuel1").ifPresent(tanks[0]::deserialize);
        input.child("fuel2").ifPresent(tanks[1]::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tanks[0].serialize(output.child("fuel1"));
        tanks[1].serialize(output.child("fuel2"));
    }

    @Override
    public long syncUnitMask() {
        return 3;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeInt(beam);
            case 1 -> {
                tanks[0].packetSerialize(output);
                tanks[1].packetSerialize(output);
            }
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$DfcInjectorBlockEntity$beam(input.readInt());
            case 1 -> {
                tanks[0].packetDeserialize(input);
                tanks[1].packetDeserialize(input);
            }
            default -> throw new IllegalArgumentException();
        }
    }

    public final void hbm$syncSet$DfcInjectorBlockEntity$beam(int next) {
        if (this.beam != next) {
            this.beam = next;
            if ((1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1);
        }
    }

    public final FluidTankNTM hbm$syncArraySet$DfcInjectorBlockEntity$tanks(
            FluidTankNTM[] array, int index, FluidTankNTM next) {
        if (array[index] != next) {
            if (array == this.tanks && syncBound())
                com.hbm.packet.SyncBindings.unbind(this, array[index]);
            array[index] = next;
            if (array == this.tanks) {
                syncArrayChanged(array, index, 3, 2);
            }
        }
        return next;
    }

    static {
        com.hbm.packet.SyncBindings.field(DfcInjectorBlockEntity.class, "tanks", 3, 2);
    }
}
