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

import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.PistonInserterBlock;
import com.hbm.capability.NtmContracts;
import com.hbm.interfaces.ItemInsertionHandler;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.platform.BlockLookupCache;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import com.hbm.util.SyncInterpolation;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public final class PistonInserterBlockEntity extends MachineBlockEntity implements SyncUnitSchema {
    public static final int TRAVEL_TICKS = 25;
    public static final int REST_TICKS = 5;
    private static final int[] PAYLOAD_SLOT = {0};

    public int extension;
    public boolean retracting = true;
    public boolean powered;
    public double previousExtension;
    public double displayedExtension;
    private int restTicks;
    private final SyncInterpolation easing = new SyncInterpolation();
    private @Nullable Direction receiverDirection;
    private @Nullable BlockPos receiverPosition;
    private @Nullable BlockLookupCache<ItemInsertionHandler> receiver;

    public PistonInserterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PISTON_INSERTER.get(), pos, state, 1);
    }

    public Direction facing() {
        return getBlockState().getValue(PistonInserterBlock.FACING);
    }

    public void onSignalChanged() {
        boolean signal = level.hasNeighborSignal(worldPosition);
        if (signal && !powered && extension <= 0) retracting = false;
        if (powered != signal) {
            powered = signal;
            setChanged();
        }
    }

    @Override
    public void tickServer() {
        if (restTicks > 0) {
            restTicks--;
        } else if (!retracting) {
            hbm$syncSet$PistonInserterBlockEntity$extension((int) (extension + 1));
            if (extension >= TRAVEL_TICKS) {
                offerPayload();
                retracting = true;
                restTicks = REST_TICKS;
            }
            setChanged();
        } else if (extension > 0) {
            hbm$syncSet$PistonInserterBlockEntity$extension((int) (extension - 1));
            setChanged();
        }
        networkPackNT(25);
    }

    private void offerPayload() {
        WorldSounds.machine(
                level,
                null,
                worldPosition.getX() + 0.5,
                worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5,
                ModSounds.PRESS_OPERATE.get(),
                SoundSource.BLOCKS,
                1F,
                1.5F);
        var direction = facing();
        if (receiverDirection != direction) {
            receiverDirection = direction;
            receiverPosition = worldPosition.relative(direction, 2);
            receiver = NtmContracts.INSERTABLE.cacheAt((ServerLevel) level, receiverPosition);
        }
        var target = receiver.find();
        if (target != null && target.insertItem(level, receiverPosition, direction, getItem(0)))
            removeItem(0, 1);
    }

    public void ejectPayload() {
        var payload = removeItemNoUpdate(0);
        if (payload.isEmpty()) return;
        setChanged();
        var direction = facing();
        var item =
                new ItemEntity(
                        level,
                        worldPosition.getX() + 0.5 + direction.getStepX() * 0.75,
                        worldPosition.getY() + 0.5 + direction.getStepY() * 0.75,
                        worldPosition.getZ() + 0.5 + direction.getStepZ() * 0.75,
                        payload);
        item.setDeltaMovement(
                direction.getStepX() * 0.25,
                direction.getStepY() * 0.25,
                direction.getStepZ() * 0.25);
        level.addFreshEntity(item);
    }

    @Override
    public void tickClient() {
        previousExtension = displayedExtension;
        displayedExtension = easing.step(displayedExtension, extension);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return PAYLOAD_SLOT;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == 0;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == 0;
    }

    @Override
    public boolean stillValid(Player player) {
        return false;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$PistonInserterBlockEntity$extension(input.getIntOr("extend", 0));
        retracting = input.getBooleanOr("retract", true);
        powered = input.getBooleanOr("state", false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("extend", extension);
        output.putBoolean("retract", retracting);
        output.putBoolean("state", powered);
    }

    @Override
    public long syncUnitMask() {
        return 3;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeInt(extension);
            case 1 -> {
                var payload = getItem(0);
                output.writeBoolean(!payload.isEmpty());
                if (!payload.isEmpty()) {
                    var ops = level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
                    FriendlyByteBuf.writeNbt(
                            output, ItemStack.CODEC.encodeStart(ops, payload).getOrThrow());
                }
            }
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$PistonInserterBlockEntity$extension(input.readInt());
            case 1 -> {
                var payload = ItemStack.EMPTY;
                if (input.readBoolean()) {
                    var ops = level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
                    payload =
                            ItemStack.CODEC.parse(ops, FriendlyByteBuf.readNbt(input)).getOrThrow();
                }
                inventory.set(0, payload);
            }
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void afterSyncUnits(long units) {
        easing.restart(2);
    }

    public final void hbm$syncSet$PistonInserterBlockEntity$extension(int next) {
        if (this.extension != next) {
            this.extension = next;
            if ((1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1);
        }
    }

    static {
        com.hbm.packet.SyncInventory.slots(
                PistonInserterBlockEntity.class, new int[] {0}, 2, false, true);
    }
}
