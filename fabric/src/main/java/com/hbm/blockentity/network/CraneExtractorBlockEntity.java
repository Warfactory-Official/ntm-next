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
import com.hbm.api.conveyor.ConveyorInputHandler;
import com.hbm.blockentity.GhostFilters;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.capability.NtmContracts;
import com.hbm.entity.item.MovingItemEntity;
import com.hbm.inventory.container.CraneExtractorMenu;
import com.hbm.items.machine.upgrade.MachineUpgradeItem;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.module.ModulePatternMatcher;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.platform.BlockLookupCache;
import com.hbm.platform.Caps;
import com.hbm.platform.Platform;
import com.hbm.sound.ModSounds;
import com.hbm.util.InventoryUtil;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import java.util.function.ToIntFunction;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class CraneExtractorBlockEntity extends CraneBaseBlockEntity
        implements MenuProvider, SyncUnitSchema, GhostFilters, ToIntFunction<ItemStack> {

    public static final int FILTER_SLOTS = 9;
    public static final int SLOT_BUFFER = 9;
    public static final int SLOT_UPGRADE_STACK = 18;
    public static final int SLOT_UPGRADE_EJECTOR = 19;
    public static final int SLOT_COUNT = 20;

    private static final Window FILTER_WINDOW = new Window(0, FILTER_SLOTS);
    private static final int[] BUFFER_ACCESS = {9, 10, 11, 12, 13, 14, 15, 16, 17};

    public final ModulePatternMatcher matcher = new ModulePatternMatcher(FILTER_SLOTS);
    public boolean isWhitelist;
    public boolean maxEject;

    private @Nullable Direction sourceSide;
    private @Nullable Direction beltSide;
    private BlockLookupCache<Storage<ItemVariant>> source;
    private BlockLookupCache<ConveyorBelt> belt;
    private BlockLookupCache<ConveyorInputHandler> intake;
    private BlockPos beltPos;
    private boolean powered;
    private boolean emitting;
    private int amount;

    public CraneExtractorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRANE_EXTRACTOR.get(), pos, state, SLOT_COUNT);
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
        var output = getOutputSide();
        if (sourceSide != output) {
            sourceSide = output;
            source =
                    Caps.createItemStoreCache(
                            (ServerLevel) level,
                            worldPosition.relative(output),
                            output.getOpposite());
        }
        var input = getInputSide();
        if (beltSide != input) {
            beltSide = input;
            beltPos = worldPosition.relative(input);
            belt = NtmContracts.CONVEYOR_BELT.cacheAt((ServerLevel) level, beltPos);
            intake = NtmContracts.ENTERABLE.cacheAt((ServerLevel) level, beltPos);
        }
        powered = level.hasNeighborSignal(worldPosition);
    }

    @Override
    public void tickServer() {
        if (!powered
                && TickPhase.every(this, CraneUpgrades.delayFor(getItem(SLOT_UPGRADE_EJECTOR))))
            transfer();
        networkPackNT(15);
    }

    private void transfer() {
        amount = CraneUpgrades.amountFor(getItem(SLOT_UPGRADE_STACK));
        var surface = belt.find();
        emitting = surface != null;
        var store = source.find();
        var extracted = store == null ? ItemStack.EMPTY : Caps.extract(store, this);
        if (!extracted.isEmpty()) {
            if (surface != null) emit(surface, extracted);
            else retain(extracted);
            return;
        }
        if (surface == null) return;
        for (int slot = SLOT_BUFFER; slot < SLOT_UPGRADE_STACK; slot++) {
            var stack = getItem(slot);
            if (stack.isEmpty() || !hasBatch(stack)) continue;
            var outgoing = stack.copyWithCount(Math.min(amount, stack.getCount()));
            removeItem(slot, outgoing.getCount());
            emit(surface, outgoing);
            return;
        }
    }

    @Override
    public int applyAsInt(ItemStack stack) {
        if (isWhitelist != matchesFilter(stack) || !hasBatch(stack)) return 0;
        return emitting ? amount : receivable(stack);
    }

    private boolean hasBatch(ItemStack stack) {
        return !maxEject || stack.getCount() >= Math.min(amount, stack.getMaxStackSize());
    }

    private int receivable(ItemStack stack) {
        if (!Platform.canFitInsideContainerItems(stack)) return 0;
        int room = 0;
        int limit = getMaxStackSize(stack);
        boolean emptySlot = false;
        for (int slot = SLOT_BUFFER; slot < SLOT_UPGRADE_STACK; slot++) {
            var held = getItem(slot);
            if (held.isEmpty()) emptySlot = true;
            else if (ItemStack.isSameItemSameComponents(held, stack))
                room += Math.max(0, limit - held.getCount());
            if (room >= amount) return amount;
        }
        return Math.min(amount, room + (emptySlot ? limit : 0));
    }

    private void retain(ItemStack stack) {
        InventoryUtil.tryAddItemToInventory(this, SLOT_BUFFER, SLOT_UPGRADE_STACK - 1, stack);
    }

    private void emit(ConveyorBelt surface, ItemStack stack) {
        var input = getInputSide();
        var mouth =
                Vec3.atCenterOf(worldPosition)
                        .add(
                                input.getStepX() * 0.55,
                                input.getStepY() * 0.55,
                                input.getStepZ() * 0.55);
        var at = surface.getSnappingPosition(level, beltPos, mouth);
        var rider = new MovingItemEntity(level);
        rider.setItemStack(stack);
        rider.snapTo(at.x, at.y, at.z, 0, 0);
        level.addFreshEntity(rider);
        var enterable = intake.find();
        var face = input.getOpposite();
        if (enterable != null && enterable.canItemEnter(level, beltPos, face, rider)) {
            enterable.onItemEnter(level, beltPos, face, rider);
            rider.discard();
        }
    }

    public boolean matchesFilter(ItemStack stack) {
        for (int slot = 0; slot < FILTER_SLOTS; slot++) {
            var pattern = getItem(slot);
            if (!pattern.isEmpty() && matcher.isValidForFilter(pattern, slot, stack)) return true;
        }
        return false;
    }

    @Override
    public Window filterWindow() {
        return FILTER_WINDOW;
    }

    @Override
    public void advanceFilterMode(int slot) {
        matcher.nextMode(level, getItem(slot), slot);
        setChanged();
    }

    public void toggleWhitelist() {
        hbm$syncSet$CraneExtractorBlockEntity$isWhitelist(!isWhitelist);
        setChanged();
    }

    public void toggleMaxEject() {
        hbm$syncSet$CraneExtractorBlockEntity$maxEject(!maxEject);
        setChanged();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return BUFFER_ACCESS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot >= SLOT_BUFFER
                && slot < SLOT_UPGRADE_STACK
                && isWhitelist == matchesFilter(stack)
                && Platform.canFitInsideContainerItems(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= SLOT_BUFFER && slot < SLOT_UPGRADE_STACK;
    }

    @Override
    public boolean dropsSlot(int slot) {
        return slot >= SLOT_BUFFER;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        boolean changed = !ItemStack.matches(getItem(slot), stack);
        super.setItem(slot, stack);
        if (changed
                && level instanceof ServerLevel server
                && (slot == SLOT_UPGRADE_STACK
                                && MachineUpgradeItem.getLevel(stack, UpgradeType.STACK) > 0
                        || slot == SLOT_UPGRADE_EJECTOR
                                && MachineUpgradeItem.getLevel(stack, UpgradeType.EJECTOR) > 0)) {
            server.playSound(
                    null, worldPosition, ModSounds.UPGRADE_PLUG.get(), SoundSource.BLOCKS, 1, 1);
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.craneExtractor");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new CraneExtractorMenu(containerId, inventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        matcher.load(input);
        hbm$syncSet$CraneExtractorBlockEntity$isWhitelist(input.getBooleanOr("isWhitelist", false));
        hbm$syncSet$CraneExtractorBlockEntity$maxEject(input.getBooleanOr("maxEject", false));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        matcher.save(output);
        output.putBoolean("isWhitelist", isWhitelist);
        output.putBoolean("maxEject", maxEject);
    }

    @Override
    public long syncUnitMask() {
        return 7;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> matcher.serialize(output);
            case 1 -> output.writeBoolean(isWhitelist);
            case 2 -> output.writeBoolean(maxEject);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> matcher.deserialize(input);
            case 1 -> hbm$syncSet$CraneExtractorBlockEntity$isWhitelist(input.readBoolean());
            case 2 -> hbm$syncSet$CraneExtractorBlockEntity$maxEject(input.readBoolean());
            default -> throw new IllegalArgumentException();
        }
    }

    public final void hbm$syncSet$CraneExtractorBlockEntity$isWhitelist(boolean next) {
        if (this.isWhitelist != next) {
            this.isWhitelist = next;
            if ((1L << 1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 1);
        }
    }

    public final void hbm$syncSet$CraneExtractorBlockEntity$maxEject(boolean next) {
        if (this.maxEject != next) {
            this.maxEject = next;
            if ((1L << 2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 2);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(CraneExtractorBlockEntity.class, "matcher", 3, 1L << 0);
    }
}
