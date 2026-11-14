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

import com.hbm.api.energymk2.EnergyItem;
import com.hbm.api.energymk2.PowerGraph;
import com.hbm.api.redstoneoverradio.RadioCommandParser;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.BatterySocketBlock;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.inventory.container.BatterySocketMenu;
import com.hbm.items.machine.BatterySCItem;
import com.hbm.items.machine.SelfChargingBatteryType;
import com.hbm.uninos.ConnectionPriority;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public final class SocketBlockEntity extends EnergyStoreBlockEntity {

    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_COUNT = 1;

    private static final int[] SLOTS = {SLOT_BATTERY};
    private static final String[] ROR = modeDescriptors("fill", "maxfill", "fillpercent", "delta");

    public final BatterySocketDischarge discharge = new BatterySocketDischarge(this);
    private ItemStack shownItem = ItemStack.EMPTY;
    private long shownStock;
    private long shownCapacity;
    private @Nullable Item publishedItem;
    private int comparator;
    public boolean frame;

    public SocketBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BATTERY_SOCKET.get(), pos, state, SLOT_COUNT);
    }

    public static boolean drawsItem(ItemStack stack) {
        return EnergyItem.matches(stack);
    }

    private static boolean loadedCell(ItemStack stack) {
        return stack.getItem() instanceof BatterySCItem cell
                && cell.tier != SelfChargingBatteryType.EMPTY;
    }

    private ItemStack seated() {
        return inventory.get(SLOT_BATTERY);
    }

    @Override
    public void tickServer() {
        long began = getPower();
        discharge.tickServer(seated());
        meter(mean(getPower(), began));
        int now = StorageComparatorSignal.rounded(getPower(), getMaxPower());
        if (now != comparator) {
            comparator = now;
            ServerLevel server = (ServerLevel) level;
            ((MultiblockCoreBlock) getBlockState().getBlock())
                    .updateComparatorOutput(server, worldPosition);
        }
        networkPackNT(100);
    }

    @Override
    public void tickClient() {
        if (TickPhase.every(level, worldPosition, 20))
            frame = !level.getBlockState(worldPosition.above(2)).isAir();
    }

    @Override
    public int getComparatorPower() {
        return StorageComparatorSignal.rounded(getPower(), getMaxPower());
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level == null || level.isClientSide()) return;
        Item seatedItem = seated().isEmpty() ? null : seated().getItem();
        if (seatedItem != publishedItem) {
            publishedItem = seatedItem;
            syncToTracking();
        }
    }

    @Override
    public long getPower() {
        return level != null && level.isClientSide() ? shownStock : itemStock();
    }

    private long itemStock() {
        ItemStack stack = seated();
        if (!(stack.getItem() instanceof EnergyItem item)) return 0L;
        long charge = item.getCharge(stack);
        return loadedCell(stack) ? (long) (charge * discharge.powerMult()) : charge;
    }

    @Override
    public void setPower(long stored) {
        ItemStack stack = seated();
        if (!(stack.getItem() instanceof EnergyItem item)) return;
        item.setCharge(stack, stored);
        markChanged();
    }

    @Override
    public long getMaxPower() {
        return level != null && level.isClientSide() ? shownCapacity : itemCapacity();
    }

    private long itemCapacity() {
        ItemStack stack = seated();
        return stack.getItem() instanceof EnergyItem item ? item.getMaxCharge(stack) : 0L;
    }

    @Override
    public long getSendRate() {
        ItemStack stack = seated();
        return getActiveMode().sends() && stack.getItem() instanceof EnergyItem item
                ? item.getDischargeRate(stack)
                : 0L;
    }

    @Override
    public long getReceiveRate() {
        ItemStack stack = seated();
        return getActiveMode().receives() && stack.getItem() instanceof EnergyItem item
                ? item.getChargeRate(stack)
                : 0L;
    }

    @Override
    public boolean canReceiveDirectly() {
        return false;
    }

    public ItemStack getShownItem() {
        return shownItem;
    }

    public long getShownStock() {
        return shownStock;
    }

    public long getShownCapacity() {
        return shownCapacity;
    }

    @Override
    protected boolean signalled(ServerLevel level) {
        for (BlockPos port : BatterySocketBlock.ports(worldPosition, facing())) {
            if (level.hasNeighborSignal(port)) return true;
        }
        return false;
    }

    @Override
    protected void poweredChanged() {
        setChanged();
    }

    private Direction facing() {
        return getBlockState().getValue(MultiblockCoreBlock.FACING);
    }

    @Override
    public void onGraphLoad(ServerLevel server) {
        refreshMode();
        BatterySocketBlock.mintNodes(server, worldPosition, getBlockState());
        PowerGraph.get(server).setSelfEndpoint(worldPosition.asLong(), true);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return EnergyItem.matches(stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        if (slot != SLOT_BATTERY || !(stack.getItem() instanceof EnergyItem item)) return false;
        return switch (getActiveMode()) {
            case SEND -> item.getCharge(stack) == 0L;
            case RECEIVE -> item.getCharge(stack) == item.getMaxCharge(stack);
            default -> false;
        };
    }

    @Override
    public String[] getFunctionInfo() {
        return ROR;
    }

    @Override
    public @Nullable String getRadioValue(String key) {
        return switch (key) {
            case RadioCommandParser.VAL + "fill" -> Long.toString(getPower());
            case RadioCommandParser.VAL + "maxfill" -> Long.toString(getMaxPower());
            case RadioCommandParser.VAL + "fillpercent" ->
                    Long.toString(
                            StorageComparatorSignal.percent(
                                    getPower(), Math.max(getMaxPower(), 1L)));
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
        return Component.translatable("container.batterySocket");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new BatterySocketMenu(containerId, inventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        restorePowered(input.getBooleanOr("redstone", false));
        discharge.load(input);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("redstone", isPowered());
        discharge.save(output);
    }

    @Override
    public long syncUnitMask() {
        return 0b1111111L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(delta);
            case 1 ->
                    output.writeInt(
                            seated().isEmpty()
                                    ? -1
                                    : BuiltInRegistries.ITEM.getId(seated().getItem()));
            case 2 -> output.writeLong(itemStock());
            case 3 -> output.writeLong(itemCapacity());
            case 4 -> output.writeShort(getLowMode().code());
            case 5 -> output.writeShort(getHighMode().code());
            case 6 -> output.writeByte(getPriority().ordinal());
            default -> throw new IllegalArgumentException("socket sync unit " + unit);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$EnergyStoreBlockEntity$delta(input.readLong());
            case 1 -> shownItem = readItem(input.readInt());
            case 2 -> shownStock = input.readLong();
            case 3 -> shownCapacity = input.readLong();
            case 4 ->
                    readSettingsUnit(
                            TransferMode.of(input.readShort()), getHighMode(), getPriority());
            case 5 ->
                    readSettingsUnit(
                            getLowMode(), TransferMode.of(input.readShort()), getPriority());
            case 6 ->
                    readSettingsUnit(
                            getLowMode(),
                            getHighMode(),
                            ConnectionPriority.getStoragePriority(
                                    ConnectionPriority.VALUES[input.readByte()]));
            default -> throw new IllegalArgumentException("socket sync unit " + unit);
        }
    }

    private static ItemStack readItem(int id) {
        if (id == -1) return ItemStack.EMPTY;
        Item item = BuiltInRegistries.ITEM.byId(id);
        if (item == Items.AIR) throw new DecoderException("socket item id " + id);
        return new ItemStack(item);
    }

    static {
        com.hbm.packet.SyncBindings.field(SocketBlockEntity.class, "discharge", 3, 1L << 2);
        com.hbm.packet.SyncInventory.slots(
                SocketBlockEntity.class,
                new int[] {SocketBlockEntity.SLOT_BATTERY},
                7L << 1,
                false,
                true);
    }
}
