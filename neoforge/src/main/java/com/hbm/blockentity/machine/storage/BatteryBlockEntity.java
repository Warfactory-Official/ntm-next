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

package com.hbm.blockentity.machine.storage;

import com.hbm.api.energymk2.CableData;
import com.hbm.api.energymk2.EnergyCaps;
import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.api.energymk2.EnergyItem;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.energymk2.PowerGraph;
import com.hbm.api.energymk2.PowerNetwork;
import com.hbm.api.redstoneoverradio.RadioCommandParser;
import com.hbm.blockentity.PersistentDrop;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.storage.BatteryBlock;
import com.hbm.inventory.container.BatteryMenu;
import com.hbm.items.ModDataComponents;
import com.hbm.platform.BlockLookupCache;
import com.hbm.platform.Caps;
import com.hbm.uninos.ConnectionPriority;
import com.hbm.uninos.graph.GraphNode;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.hbm.util.ChunkUtil;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BatteryBlockEntity extends EnergyStoreBlockEntity implements PersistentDrop {
    public static final int SLOT_IN = 0;
    public static final int SLOT_OUT = 1;
    public static final int SLOT_COUNT = 2;
    public static final long STANDARD_CAPACITY = 1_000_000L;

    private static final int[] SLOTS_DOWN = {SLOT_IN, SLOT_OUT};
    private static final int[] SLOTS_UP = {SLOT_IN};
    private static final int[] SLOTS_SIDE = {SLOT_OUT};
    private static final int ALL_FACES = 0x3F;
    private static final long UNIT_STOCK = 1L;
    private static final long UNIT_DELTA = 1L << 1;
    private static final String[] ROR = modeDescriptors("fill", "fillpercent", "delta");
    private static final String[] PERSISTENT_KEYS = {"power", "redLow", "redHigh", "priority"};

    protected long power;
    private final long capacity;
    private long prevPower;
    private boolean prevPowerPlaced;
    private int comparator;
    private @Nullable BlockLookupCache<EnergyHandler>[] receivers;
    private int foreignChunkFaces;
    private final ArrayList<EnergyHandler> touching = new ArrayList<>(6);

    public BatteryBlockEntity(BlockPos pos, BlockState state) {
        this(
                ModBlockEntities.BATTERY.get(),
                pos,
                state,
                ((BatteryBlock) state.getBlock()).maxPower);
    }

    protected BatteryBlockEntity(
            BlockEntityType<?> type, BlockPos pos, BlockState state, long capacity) {
        super(type, pos, state, SLOT_COUNT);
        this.capacity = capacity;
    }

    @Override
    public void tickServer() {
        long began = prevPowerPlaced ? prevPower : getPower();
        prevPowerPlaced = false;
        prevPower = began;
        long charged = ItemEnergyTransfer.insert(this, SLOT_OUT, getPower(), false);
        if (charged > 0L) usePower(charged);
        if (pushes(getActiveMode())) provideToTouching((ServerLevel) level);
        long discharged = ItemEnergyTransfer.extract(this, SLOT_IN, inboundRoom(), false);
        if (discharged > 0L) absorb(discharged);
        meter(meanStock(began, getPower()));
        updateComparator();
        networkPackNT(20);
    }

    protected long inboundRoom() {
        return getMaxPower() - getPower();
    }

    protected void absorb(long amount) {
        setPower(getPower() + amount);
        onEnergyChanged();
    }

    protected long meanStock(long began, long ended) {
        return mean(began, ended);
    }

    protected boolean pushes(TransferMode mode) {
        return mode == TransferMode.SEND;
    }

    protected boolean pushesThrough(Direction side) {
        return true;
    }

    private void provideToTouching(ServerLevel server) {
        if (receivers == null) receivers = receiverCaches(server);
        touching.clear();
        for (Direction side : Direction.VALUES) {
            BlockLookupCache<EnergyHandler> cache = receivers[side.ordinal()];
            if (cache == null) continue;
            if ((foreignChunkFaces & 1 << side.ordinal()) != 0
                    && ChunkUtil.chunkIfLoaded(server, worldPosition.relative(side)) == null)
                continue;
            EnergyHandler receiver = cache.find();
            if (receiver != null && receiver != this && receiver.canReceiveDirectly())
                touching.add(receiver);
        }
        if (!touching.isEmpty()) PowerNetwork.provideDirect(this, touching);
        touching.clear();
    }

    @SuppressWarnings("unchecked")
    private BlockLookupCache<EnergyHandler>[] receiverCaches(ServerLevel server) {
        BlockLookupCache<EnergyHandler>[] caches = new BlockLookupCache[Direction.VALUES.length];
        for (Direction side : Direction.VALUES) {
            if (!pushesThrough(side)) continue;
            BlockPos neighbour = worldPosition.relative(side);
            BlockLookupCache<EnergyHandler> cache =
                    Caps.createCache(EnergyCaps.RECEIVER, server, neighbour, side.getOpposite());
            caches[side.ordinal()] = cache;
            if (neighbour.getX() >> 4 != worldPosition.getX() >> 4
                    || neighbour.getZ() >> 4 != worldPosition.getZ() >> 4) {
                foreignChunkFaces |= 1 << side.ordinal();
            }
        }
        return caches;
    }

    protected void updateComparator() {
        int now = StorageComparatorSignal.rising(getPower(), getMaxPower());
        if (now == comparator) return;
        comparator = now;
        setChanged();
    }

    @Override
    public int getComparatorPower() {
        return comparator;
    }

    protected boolean conducts(TransferMode mode) {
        return mode == TransferMode.BUFFER;
    }

    @Override
    public void onGraphLoad(ServerLevel server) {
        refreshMode();
        placeConductor(server, conducts(getActiveMode()));
    }

    @Override
    public void onGraphDetach(ServerLevel server) {
        placeConductor(server, false);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel server) onGraphDetach(server);
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    protected void activeModeChanged(TransferMode before) {
        if (level instanceof ServerLevel server
                && !isRemoved()
                && conducts(before) != conducts(getActiveMode())) {
            placeConductor(server, conducts(getActiveMode()));
        }
    }

    private void placeConductor(ServerLevel server, boolean present) {
        LevelNodeGraph<CableData> graph = PowerGraph.get(server);
        long key = worldPosition.asLong();
        if (present) {
            graph.addMachineNode(key, CableData.INSTANCE, ALL_FACES, true);
            return;
        }
        GraphNode<CableData> node = graph.getNode(key);
        if (node != null && node.machineOwned) graph.removeNode(key);
    }

    @Override
    protected boolean signalled(ServerLevel level) {
        return level.hasNeighborSignal(worldPosition);
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long stored) {
        hbm$syncSet$BatteryBlockEntity$power(Math.clamp(stored, 0L, getMaxPower()));
    }

    @Override
    public long getMaxPower() {
        return capacity;
    }

    @Override
    public long getSendRate() {
        return getActiveMode().sends() ? capacity / 600L : 0L;
    }

    @Override
    public long getReceiveRate() {
        return getActiveMode().receives() ? capacity / 200L : 0L;
    }

    public long fillScaled(long units) {
        return getPower() * units / getMaxPower();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return EnergyItem.matches(stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? SLOTS_DOWN : side == Direction.UP ? SLOTS_UP : SLOTS_SIDE;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        if (!(stack.getItem() instanceof EnergyItem item)) return false;
        return slot == SLOT_IN
                ? item.getCharge(stack) == 0L
                : item.getCharge(stack) == item.getMaxCharge(stack);
    }

    @Override
    public String[] getFunctionInfo() {
        return ROR;
    }

    @Override
    public @Nullable String getRadioValue(String key) {
        return switch (key) {
            case RadioCommandParser.VAL + "fill" -> Long.toString(getPower());
            case RadioCommandParser.VAL + "fillpercent" -> Long.toString(fillScaled(100L));
            case RadioCommandParser.VAL + "delta" -> Long.toString(delta);
            default -> null;
        };
    }

    @Override
    public void runRadioFunction(String key, String[] params) {
        runModeFunction(key, params);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.battery");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new BatteryMenu(containerId, inventory, this);
    }

    @Override
    public void writePersistent(DataComponentMap.Builder components) {
        components.set(
                ModDataComponents.MACHINE_BATTERY_STATE.get(),
                new BatteryCharge(
                        power,
                        prevPower,
                        getLowMode().code(),
                        getHighMode().code(),
                        getPriority().ordinal()));
    }

    @Override
    public void readPersistent(DataComponentGetter components) {
        BatteryCharge charge = components.get(ModDataComponents.MACHINE_BATTERY_STATE.get());
        if (charge == null) return;
        hbm$syncSet$BatteryBlockEntity$power(charge.power());
        prevPower = charge.prevPower();
        prevPowerPlaced = true;
        setModes(TransferMode.of(charge.redLow()), TransferMode.of(charge.redHigh()));
        setPriority(ConnectionPriority.VALUES[charge.priority()]);
    }

    @Override
    public String[] persistentKeys() {
        return PERSISTENT_KEYS;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$BatteryBlockEntity$power(input.getLongOr("power", 0L));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | UNIT_STOCK | UNIT_DELTA;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(power);
            case 1 -> output.writeLong(delta);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$BatteryBlockEntity$power(input.readLong());
            case 1 -> hbm$syncSet$EnergyStoreBlockEntity$delta(input.readLong());
            default -> super.readSyncUnit(unit, input);
        }
    }

    public final void hbm$syncSet$BatteryBlockEntity$power(long next) {
        if (this.power != next) {
            this.power = next;
            if ((1L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L);
        }
    }

    static {
        com.hbm.inventory.container.SyncedData.fields(BatteryBlockEntity.class, "power");
    }

    public void stripPersistent(ValueOutput output) {
        for (String key : persistentKeys()) output.discard(key);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        writePersistent(components);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        readPersistent(components);
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        stripPersistent(output);
    }
}
