// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.PistonInserter;
import com.hbm.capability.NtmContracts;
import com.hbm.interfaces.IInsertable;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncSlots;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.platform.BlockLookupCache;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.BlockEntityMachineBase;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
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

@SyncSlots(
        value = {0},
        units = 1L << 1)
public class BlockEntityPistonInserter extends BlockEntityMachineBase implements SyncUnitSchema {

    public static final int MAX_EXTEND = 25;

    private static final int[] SLOTS = {0};

    @SyncField(units = 1L << 0)
    public int extend;

    public boolean isRetracting = true;
    public int delay;

    public boolean lastState;

    public double renderExtend;
    public double lastExtend;
    private int syncExtend;
    private int turnProgress;

    public BlockEntityPistonInserter(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PISTON_INSERTER.get(), pos, state, 1);
    }

    @Override
    public void tickServer() {

        if (delay <= 0) {

            if (this.isRetracting && this.extend > 0) {
                this.extend--;
            } else if (!this.isRetracting) {
                this.extend++;

                if (this.extend >= MAX_EXTEND) {
                    level.playSound(
                            null,
                            worldPosition,
                            ModSounds.PRESS_OPERATE.get(),
                            SoundSource.BLOCKS,
                            1.0F,
                            1.5F);

                    Direction dir = facing();
                    BlockPos target = worldPosition.relative(dir, 2);

                    IInsertable insertable = insertable(target);

                    if (insertable != null
                            && insertable.insertItem(level, target, dir, getItem(0))) {
                        this.removeItem(0, 1);
                    }

                    this.isRetracting = true;
                    this.delay = 5;
                }
            }

        } else {
            delay--;
        }

        networkPackNT(25);
    }

    @Override
    public void tickClient() {
        this.lastExtend = this.renderExtend;

        if (this.turnProgress > 0) {
            this.renderExtend += (this.syncExtend - this.renderExtend) / (double) this.turnProgress;
            this.turnProgress--;
        } else {
            this.renderExtend = this.syncExtend;
        }
    }

    public Direction facing() {
        return getBlockState().getValue(PistonInserter.FACING);
    }

    public void eject(Direction dir) {
        ItemStack stack = getItem(0);
        if (stack.isEmpty()) return;
        setItem(0, ItemStack.EMPTY);

        ItemEntity dust =
                new ItemEntity(
                        level,
                        worldPosition.getX() + 0.5D + dir.getStepX() * 0.75D,
                        worldPosition.getY() + 0.5D + dir.getStepY() * 0.75D,
                        worldPosition.getZ() + 0.5D + dir.getStepZ() * 0.75D,
                        stack);
        dust.setDeltaMovement(dir.getStepX() * 0.25, dir.getStepY() * 0.25, dir.getStepZ() * 0.25);
        level.addFreshEntity(dust);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        this.extend = input.getIntOr("extend", extend);
        this.isRetracting = input.getBooleanOr("retract", isRetracting);
        this.lastState = input.getBooleanOr("state", lastState);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("extend", extend);
        output.putBoolean("retract", isRetracting);
        output.putBoolean("state", lastState);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return true;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return false;
    }

    private @Nullable BlockLookupCache<IInsertable> insertCache;
    private @Nullable BlockPos insertPos;

    private @Nullable IInsertable insertable(BlockPos target) {
        if (!(level instanceof ServerLevel server)) return null;
        if (insertCache == null || !target.equals(insertPos)) {
            insertPos = target.immutable();
            insertCache = NtmContracts.INSERTABLE.cacheAt(server, insertPos);
        }
        return insertCache.find();
    }

    private void readExtend(ByteBuf input) {
        syncExtend = input.readInt();
    }

    private void writeHeldStack(ByteBuf output) {
        ItemStack stack = getItem(0);
        output.writeBoolean(!stack.isEmpty());
        if (!stack.isEmpty()) {
            FriendlyByteBuf.writeNbt(
                    output, ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, stack).getOrThrow());
        }
    }

    private void readHeldStack(ByteBuf input) {
        if (input.readBoolean()) {
            Tag tag = FriendlyByteBuf.readNbt(input);
            setItem(0, ItemStack.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow());
        } else {
            setItem(0, ItemStack.EMPTY);
        }
    }

    @Override
    public void afterSyncUnits(long units) {
        turnProgress = 2;
    }

    @Override
    public long syncUnitMask() {
        return 0x3L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeInt(this.extend);
            case 1 -> writeHeldStack(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> readExtend(input);
            case 1 -> readHeldStack(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
