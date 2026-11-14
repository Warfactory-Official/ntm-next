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

package com.hbm.blockentity.network;

import com.hbm.api.conveyor.ConveyorBelt;
import com.hbm.blockentity.GhostFilters;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.capability.NtmContracts;
import com.hbm.entity.item.MovingItemEntity;
import com.hbm.inventory.container.CraneGrabberMenu;
import com.hbm.module.ModulePatternMatcher;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.platform.BlockLookupCache;
import com.hbm.platform.Caps;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

public final class CraneGrabberBlockEntity extends CraneBaseBlockEntity
        implements GhostFilters, MenuProvider, SyncUnitSchema {

    public static final int FILTER_SLOTS = 9;
    public static final int SLOT_UPGRADE_STACK = 9;
    public static final int SLOT_UPGRADE_EJECTOR = 10;
    public static final int SLOT_COUNT = 11;
    private static final Window FILTER_WINDOW = new Window(0, FILTER_SLOTS);

    public final ModulePatternMatcher matcher = new ModulePatternMatcher(FILTER_SLOTS);
    public boolean isWhitelist;
    public long lastGrabbedTick;

    private boolean powered;
    private Direction windowSide;
    private Block lane;
    private AABB pickupWindow;
    private Direction destinationSide;
    private BlockPos destinationPos;
    private Vec3 destinationNear;
    private BlockLookupCache<ConveyorBelt> belt;
    private BlockLookupCache<ResourceHandler<ItemResource>> items;

    public CraneGrabberBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRANE_GRABBER.get(), pos, state, SLOT_COUNT);
    }

    @Override
    public void onGraphLoad(ServerLevel server) {
        refreshNeighbors();
    }

    @Override
    protected void directionStateChanged() {
        refreshNeighbors();
    }

    public void refreshNeighbors() {
        var server = (ServerLevel) level;
        powered = server.hasNeighborSignal(worldPosition);
        var input = getInputSide();
        var inputPos = worldPosition.relative(input);
        var chunk = MultiblockCoreBlock.readableChunk(server, inputPos.getX(), inputPos.getZ());
        var nextLane = chunk == null ? null : chunk.getBlockState(inputPos).getBlock();
        if (windowSide != input || lane != nextLane) {
            windowSide = input;
            lane = nextLane;
            double reach = 1;
            if (input.getAxis().isHorizontal()) {
                if (lane == ModBlocks.CONVEYOR_DOUBLE.get()) reach = 0.5;
                else if (lane == ModBlocks.CONVEYOR_TRIPLE.get()) reach = 0.33;
            }
            double x = worldPosition.getX() + reach * input.getStepX();
            double y = worldPosition.getY() + reach * input.getStepY();
            double z = worldPosition.getZ() + reach * input.getStepZ();
            pickupWindow =
                    new AABB(
                            x + 0.1875, y + 0.1875, z + 0.1875, x + 0.8125, y + 0.8125, z + 0.8125);
        }
        var output = getOutputSide();
        if (destinationSide != output) {
            destinationSide = output;
            destinationPos = worldPosition.relative(output);
            destinationNear = Vec3.atCenterOf(worldPosition).relative(output, 0.55);
            belt = NtmContracts.CONVEYOR_BELT.cacheAt(server, destinationPos);
            items = Caps.createItemStoreCache(server, destinationPos, output.getOpposite());
        }
    }

    @Override
    public void tickServer() {
        networkPackNT(15);
        long now = level.getGameTime();
        int delay = CraneUpgrades.delayFor(inventory.get(SLOT_UPGRADE_EJECTOR));
        if (powered || now < lastGrabbedTick + delay) return;
        var outputBelt = belt.find();
        var outputItems = outputBelt == null ? items.find() : null;
        if (outputBelt == null && outputItems == null) return;

        var riders = level.getEntitiesOfClass(MovingItemEntity.class, pickupWindow);
        int remaining = CraneUpgrades.amountFor(inventory.get(SLOT_UPGRADE_STACK));
        for (var rider : riders) {
            if (!rider.isAlive()) continue;
            var stack = rider.getItemStack();
            if (stack.isEmpty() || !matchesFilter(stack)) continue;
            noteAttempt(now);
            if (outputBelt != null) {
                var point = outputBelt.getSnappingPosition(level, destinationPos, destinationNear);
                var moved = new MovingItemEntity(level);
                moved.setItemStack(stack.copy());
                moved.snapTo(point.x, point.y, point.z, 0, 0);
                rider.discard();
                level.addFreshEntity(moved);
                return;
            }
            int offered = Math.min(stack.getCount(), remaining);
            var offer = stack.copyWithCount(offered);
            var refused = Caps.insert(outputItems, offer);
            int accepted = offered - refused.getCount();
            if (accepted == 0) continue;
            remaining -= accepted;
            if (accepted == stack.getCount()) rider.discard();
            else rider.setItemStack(stack.copyWithCount(stack.getCount() - accepted));
            if (remaining == 0) return;
        }
    }

    private void noteAttempt(long now) {
        if (lastGrabbedTick == now) return;
        lastGrabbedTick = now;
        setChanged();
    }

    public boolean matchesFilter(ItemStack stack) {
        for (int slot = 0; slot < FILTER_SLOTS; slot++) {
            var template = inventory.get(slot);
            if (!template.isEmpty() && matcher.isValidForFilter(template, slot, stack))
                return isWhitelist;
        }
        return !isWhitelist;
    }

    @Override
    public Window filterWindow() {
        return FILTER_WINDOW;
    }

    @Override
    public void advanceFilterMode(int slot) {
        matcher.nextMode(level, inventory.get(slot), slot);
        setChanged();
    }

    public void toggleWhitelist() {
        hbm$syncSet$CraneGrabberBlockEntity$isWhitelist(!isWhitelist);
        setChanged();
    }

    @Override
    public boolean dropsSlot(int slot) {
        return slot >= SLOT_UPGRADE_STACK;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.craneGrabber");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new CraneGrabberMenu(containerId, inventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        matcher.load(input);
        hbm$syncSet$CraneGrabberBlockEntity$isWhitelist(input.getBooleanOr("isWhitelist", false));
        lastGrabbedTick = input.getLongOr("lastGrabbedTick", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        matcher.save(output);
        output.putBoolean("isWhitelist", isWhitelist);
        output.putLong("lastGrabbedTick", lastGrabbedTick);
    }

    @Override
    public long syncUnitMask() {
        return 3;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> matcher.serialize(output);
            case 1 -> output.writeBoolean(isWhitelist);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> matcher.deserialize(input);
            case 1 -> hbm$syncSet$CraneGrabberBlockEntity$isWhitelist(input.readBoolean());
            default -> throw new IllegalArgumentException();
        }
    }

    public final void hbm$syncSet$CraneGrabberBlockEntity$isWhitelist(boolean next) {
        if (this.isWhitelist != next) {
            this.isWhitelist = next;
            if ((2L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 2L);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(CraneGrabberBlockEntity.class, "matcher", 3, 1L);
    }
}
