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

package com.hbm.blockentity.machine;

import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankHandler;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.inventory.container.StrandCasterMenu;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.material.MaterialStack;
import com.hbm.items.machine.MoldItem.Mold;
import com.hbm.items.machine.MoldItem;
import com.hbm.items.machine.ScrapsItem;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.util.InventoryRange;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class ContinuousCaster extends FoundryCastingBaseBlockEntity
        implements FluidTankHandler, MenuProvider, SyncUnitSchema {

    public static final int SLOT_MOLD = 0;
    public static final int SLOT_OUTPUT_START = 1;
    public static final int SLOT_OUTPUT_END = 6;
    public static final int SLOT_COUNT = 7;
    public static final int BATCH = 9;
    public static final int IDLE_FLUSH = 200;
    private static final int[] OUTPUT_SLOTS = {1, 2, 3, 4, 5, 6};
    private static final InventoryRange PRODUCTS =
            new InventoryRange(SLOT_OUTPUT_START, SLOT_OUTPUT_END);
    private static final FlushFaces STEAM_CONTACTS = FlushFaces.own();

    public final FluidTankNTM water = new FluidTankNTM(NTMFluids.WATER, 64_000);
    public final FluidTankNTM steam = new FluidTankNTM(NTMFluids.SPENTSTEAM, 64_000);
    private final FluidTankNTM[] intake = {water};
    private final FluidTankNTM[] sending = {steam};
    private final FluidTankNTM[] tanks = {water, steam};
    private final FluidFlushOutputs steamOutput = new FluidFlushOutputs();
    private long lastProgress;

    public ContinuousCaster(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STRAND_CASTER.get(), pos, state, SLOT_COUNT);
    }

    @Override
    public void tickServer() {
        int capacity = getCapacity();
        if (amount > capacity) {
            var excess = ScrapsItem.create(new MaterialStack(type, amount - capacity));
            level.addFreshEntity(
                    new ItemEntity(
                            level,
                            worldPosition.getX() + 0.5D,
                            worldPosition.getY() + 2D,
                            worldPosition.getZ() + 0.5D,
                            excess));
            hbm$syncSet$FoundryBaseBlockEntity$amount(capacity);
            markChanged();
        }
        if (amount == 0 && type != null) {
            hbm$syncSet$FoundryBaseBlockEntity$type(null);
            markChanged();
        }

        steamOutput.provide((ServerLevel) level, this);
        castBatch();
        networkPackNT(150);
    }

    private void castBatch() {
        var mold = getInstalledMold();
        if (mold == null || type == null || amount == 0) return;
        var product = mold.getOutput(type);
        if (product == null || product.isEmpty()) return;

        int cost = mold.getCost();
        int coolant = cost * 5;
        int room = PRODUCTS.place(inventory, product, Integer.MAX_VALUE, true);
        int count = Math.min(amount / cost, room / product.getCount());
        count = Math.min(count, water.getFill() / coolant);
        count = Math.min(count, (steam.getMaxFill() - steam.getFill()) / coolant);
        if (count == 0) return;

        long now = level.getGameTime();
        if (count < BATCH && now - lastProgress < IDLE_FLUSH) return;

        hbm$syncSet$FoundryBaseBlockEntity$amount((int) (amount - (count * cost)));
        PRODUCTS.place(inventory, product, count * product.getCount(), false);
        water.setFill(water.getFill() - count * coolant);
        steam.setFill(steam.getFill() + count * coolant);
        lastProgress = now;
        markChanged();
    }

    @Override
    public @Nullable Mold getInstalledMold() {
        var item = inventory.get(SLOT_MOLD).getItem();
        return item instanceof MoldItem mold ? mold.mold : null;
    }

    @Override
    public int getMoldSize() {
        return -1;
    }

    @Override
    public int getCapacity() {
        var mold = getInstalledMold();
        return mold == null ? 50_000 : mold.getCost() * 10;
    }

    public int getWaterRequired() {
        var mold = getInstalledMold();
        return mold == null ? 50 : mold.getCost() * 5;
    }

    public BlockPos[] getMetalPourPos() {
        var facing = getBlockState().getValue(MultiblockCoreBlock.FACING);
        var clockwise = facing.getClockWise();
        var roof = worldPosition.above(2);
        var rear = roof.relative(facing.getOpposite());
        return new BlockPos[] {roof, roof.relative(clockwise), rear, rear.relative(clockwise)};
    }

    private boolean isPourCell(BlockPos pos) {
        if (pos.getY() != worldPosition.getY() + 2) return false;
        var facing = getBlockState().getValue(MultiblockCoreBlock.FACING);
        var clockwise = facing.getClockWise();
        int x = pos.getX() - worldPosition.getX();
        int z = pos.getZ() - worldPosition.getZ();
        int along = x * facing.getStepX() + z * facing.getStepZ();
        int across = x * clockwise.getStepX() + z * clockwise.getStepZ();
        return (along == 0 || along == -1) && (across == 0 || across == 1);
    }

    private boolean admits(MaterialStack offered) {
        var mold = getInstalledMold();
        return mold != null
                && (amount == 0 || type == offered.material)
                && amount < BATCH * mold.getCost();
    }

    @Override
    public boolean standardCheck(Level level, BlockPos pos, Direction side, MaterialStack offered) {
        return admits(offered);
    }

    @Override
    public boolean canAcceptPour(
            Level level, BlockPos pos, Vec3 impact, Direction side, MaterialStack offered) {
        return side == Direction.UP && isPourCell(pos) && admits(offered);
    }

    @Override
    public @Nullable MaterialStack acceptPour(
            Level level, BlockPos pos, Vec3 impact, Direction side, MaterialStack offered) {
        return standardAdd(level, pos, side, offered);
    }

    @Override
    public @Nullable MaterialStack standardAdd(
            Level level, BlockPos pos, Direction side, MaterialStack offered) {
        var mold = getInstalledMold();
        int accepted = Math.min(offered.amount, BATCH * mold.getCost() - amount);
        boolean changed = type != offered.material || accepted != 0;
        hbm$syncSet$FoundryBaseBlockEntity$type(offered.material);
        hbm$syncSet$FoundryBaseBlockEntity$amount((int) (amount + (accepted)));
        if (accepted == offered.amount) {
            if (changed) markChanged();
            return null;
        }
        offered.hbm$syncSet$MaterialStack$amount((int) (offered.amount - (accepted)));
        long now = level.getGameTime();
        if (lastProgress != now) changed = true;
        lastProgress = now;
        if (changed) markChanged();
        return offered;
    }

    @Override
    public boolean canAcceptFlow(Level level, BlockPos pos, Direction side, MaterialStack offered) {
        return false;
    }

    @Override
    public MaterialStack acceptFlow(
            Level level, BlockPos pos, Direction side, MaterialStack offered) {
        return offered;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == SLOT_MOLD && stack.getItem() instanceof MoldItem;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return OUTPUT_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= SLOT_OUTPUT_START && slot <= SLOT_OUTPUT_END;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.machineStrandCaster");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new StrandCasterMenu(containerId, inventory, this);
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return intake;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return sending;
    }

    @Override
    public int tankCount() {
        return tanks.length;
    }

    @Override
    public FluidTankNTM tank(int index) {
        return tanks[index];
    }

    @Override
    public void declareFlush(FlushLanes out) {
        out.add(steam, STEAM_CONTACTS);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("water").ifPresent(water::deserialize);
        input.child("steam").ifPresent(steam::deserialize);
        lastProgress = input.getLongOr("lastProgress", 0L);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        water.serialize(output.child("water"));
        steam.serialize(output.child("steam"));
        output.putLong("lastProgress", lastProgress);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 31L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 2 -> serializeMold(output);
            case 3 -> water.packetSerialize(output);
            case 4 -> steam.packetSerialize(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 2 -> deserializeMold(input);
            case 3 -> water.packetDeserialize(input);
            case 4 -> steam.packetDeserialize(input);
            default -> super.readSyncUnit(unit, input);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(ContinuousCaster.class, "water", 3, 8L);
        com.hbm.packet.SyncBindings.field(ContinuousCaster.class, "steam", 3, 16L);
    }
}
