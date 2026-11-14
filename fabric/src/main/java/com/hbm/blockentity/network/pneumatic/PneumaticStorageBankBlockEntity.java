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

package com.hbm.blockentity.network.pneumatic;

import com.hbm.api.fluidmk2.PlainFluidReceiver;
import com.hbm.api.ntl.PneumaticData;
import com.hbm.api.ntl.PneumaticNetwork;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.platform.Platform;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public abstract class PneumaticStorageBankBlockEntity extends PneumaticMachineBaseBlockEntity
        implements PlainFluidReceiver, SyncUnitSchema {
    public final FluidTankNTM airTank = new FluidTankNTM(NTMFluids.AIR, 4_000).withPressure(1);

    private @Nullable Object2ObjectMap<ItemStack, Stock> cachedStock;
    private long stockTick = Long.MIN_VALUE;

    protected PneumaticStorageBankBlockEntity(
            BlockEntityType<?> type, BlockPos pos, BlockState state, int slots) {
        super(type, pos, state, slots);
        airTank.bindOwner(this);
    }

    @Override
    public void onGraphLoad(ServerLevel server) {
        PneumaticNetwork.get(server)
                .addMachineNode(worldPosition.asLong(), PneumaticData.INSTANCE, 0x3F, false);
    }

    public static final class Stock {
        public final ItemStack display;
        public long amount;
        public final IntArrayList slots = new IntArrayList();

        private Stock(ItemStack display) {
            this.display = display;
        }
    }

    public void tickServer() {
        int fill = airTank.getFill();
        if (fill > 0) {
            airTank.setFill(fill - (fill * 9 / airTank.getMaxFill() + 1));
            setChanged();
        }
        networkPackNT(15);
    }

    public void cyclePressure() {
        airTank.setTankType(NTMFluids.AIR);
        int pressure = airTank.getPressure();
        airTank.withPressure(pressure >= 5 ? 1 : pressure + 1);
        setChanged();
    }

    public boolean hasAir() {
        return !isRemoved() && airTank.getFill() > 0;
    }

    public boolean reaches(BlockPos viewer) {
        int range = PneumaticTubeBlockEntity.rangeForPressure(airTank.getPressure());
        return worldPosition.distSqr(viewer) <= (double) range * range;
    }

    public ItemStack getSlotAt(int slot) {
        return getItem(slot);
    }

    public abstract long quantityAt(int slot);

    public abstract boolean canAssignTypes();

    public abstract long removeFromSlot(int slot, long amount);

    public abstract long fillExistingSlot(int slot, long amount);

    public abstract long assignSlotType(int slot, ItemStack type, long amount);

    public Object2ObjectMap<ItemStack, Stock> stocks() {
        long tick = level.getGameTime();
        var stock = cachedStock;
        if (stock != null && stockTick == tick) return stock;

        stock = new Object2ObjectOpenCustomHashMap<>(PneumaticNetwork.TYPE_AND_COMPONENTS);
        for (int slot = 0; slot < inventory.size(); slot++) {
            var stack = getSlotAt(slot);
            if (stack.isEmpty()) continue;
            var group = stock.get(stack);
            if (group == null) {
                group = new Stock(stack.copyWithCount(1));
                stock.put(group.display, group);
            }
            group.amount += quantityAt(slot);
            group.slots.add(slot);
        }
        cachedStock = stock;
        stockTick = tick;
        return stock;
    }

    public long withdraw(ItemStack type, long amount) {
        var group = stocks().get(type);
        if (group != null) {
            for (int i = 0; i < group.slots.size() && amount > 0; i++) {
                int slot = group.slots.getInt(i);
                if (PneumaticNetwork.TYPE_AND_COMPONENTS.equals(type, getSlotAt(slot))) {
                    amount = removeFromSlot(slot, amount);
                }
            }
        }
        cachedStock = null;
        return amount;
    }

    public long fillExisting(ItemStack type, long amount) {
        var group = stocks().get(type);
        if (group != null) {
            for (int i = 0; i < group.slots.size() && amount > 0; i++) {
                int slot = group.slots.getInt(i);
                if (PneumaticNetwork.TYPE_AND_COMPONENTS.equals(type, getSlotAt(slot))) {
                    amount = fillExistingSlot(slot, amount);
                }
            }
        }
        cachedStock = null;
        return amount;
    }

    public long fillVacant(ItemStack type, long amount) {
        if (canAssignTypes()) {
            for (int slot = 0; slot < inventory.size() && amount > 0; slot++) {
                if (getSlotAt(slot).isEmpty()) amount = assignSlotType(slot, type, amount);
            }
        }
        cachedStock = null;
        return amount;
    }

    @Override
    public void setChanged() {
        cachedStock = null;
        super.setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return Platform.canFitInsideContainerItems(stack);
    }

    @Override
    public long getDemand(Fluid type, int pressure) {
        return pressure == airTank.getPressure() && airTank.accepts(type)
                ? airTank.getMaxFill() - airTank.getFill()
                : 0L;
    }

    @Override
    public long transferFluid(Fluid type, int pressure, long amount) {
        if (pressure != airTank.getPressure()) return amount;
        int accepted = airTank.fill(type, (int) Math.min(amount, Integer.MAX_VALUE), true);
        if (accepted > 0) setChanged();
        return amount - accepted;
    }

    @Override
    public int receivingPressureMask(Fluid type) {
        return 1 << airTank.getPressure();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("tank").ifPresent(airTank::deserialize);
        cachedStock = null;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        airTank.serialize(output.child("tank"));
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 1L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        if (unit == 0) airTank.packetSerialize(output);
        else super.writeSyncUnit(unit, output);
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        if (unit == 0) airTank.packetDeserialize(input);
        else super.readSyncUnit(unit, input);
    }

    static {
        com.hbm.packet.SyncBindings.field(PneumaticStorageBankBlockEntity.class, "airTank", 3, 1L);
    }
}
