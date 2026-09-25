// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.generic.BlockDoorGeneric;
import com.hbm.blocks.machine.BlastDoor;
import com.hbm.blocks.multiblock.BlockMultiblockGeometryCell;
import com.hbm.packet.SyncField;
import com.hbm.sound.ModSounds;
import io.netty.buffer.ByteBuf;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BlockEntityBlastDoor extends BlockEntityLockableBase {

    public static final int STATE_CLOSED = 0, STATE_MOVING = 1, STATE_OPEN = 2;

    public static final int TRAVEL_TICKS = 100;

    public static final int STEP_TICKS = 20;

    @SyncField(units = 1L << 4)
    public boolean isOpening;

    @SyncField(units = 1L << 5)
    public int state = STATE_CLOSED;

    @SyncField(units = 1L << 6)
    public int timer;

    private boolean redstoned;

    public BlockEntityBlastDoor(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BLAST_DOOR.get(), pos, state, 0);
    }

    public void tickServer() {

        boolean powered =
                level.hasNeighborSignal(worldPosition)
                        || level.hasNeighborSignal(worldPosition.above(BlastDoor.LEAF_TOP + 1));
        if (!isLocked() && powered) {
            if (!redstoned) tryToggle(null);
            redstoned = true;
        } else {
            redstoned = false;
        }

        if (state != STATE_MOVING) {
            timer = 0;
        } else {
            timer++;
            travel((ServerLevel) level);
            if (timer >= TRAVEL_TICKS) {
                if (isOpening) finishOpen();
                else finishClose();
            }
        }

        syncCoreOpen();
        networkPackNT(100);
    }

    private void syncCoreOpen() {
        BlockState core = getBlockState();
        boolean open = state != STATE_CLOSED;
        if (core.getValue(BlockDoorGeneric.OPEN) == open) return;
        level.setBlock(
                worldPosition, core.setValue(BlockDoorGeneric.OPEN, open), Block.UPDATE_CLIENTS);
    }

    private void travel(ServerLevel server) {
        for (int cell = 1; cell <= BlastDoor.LEAF_TOP; cell++) {
            int due =
                    isOpening
                            ? (cell - 1) * STEP_TICKS
                            : (BlastDoor.LEAF_TOP + 1 - cell) * STEP_TICKS;
            if (timer < due) continue;
            BlockMultiblockGeometryCell.setCellOpen(
                    server, worldPosition.above(cell), worldPosition, isOpening);
        }
    }

    public boolean canOpen() {
        return state == STATE_CLOSED;
    }

    public boolean canClose() {
        return state == STATE_OPEN;
    }

    public void tryToggle(@Nullable Player player) {
        if (player != null && isLocked() && !canAccess(player)) return;
        if (canOpen()) {
            open();
            spread(true);
        } else if (canClose()) {
            close();
            spread(false);
        }
    }

    public void open() {
        if (state != STATE_CLOSED) return;
        isOpening = true;
        state = STATE_MOVING;
        cue(ModSounds.REACTOR_START.get(), 0.75F);
    }

    public void close() {
        if (state != STATE_OPEN) return;
        isOpening = false;
        state = STATE_MOVING;
        cue(ModSounds.REACTOR_START.get(), 0.75F);
    }

    private void finishOpen() {
        state = STATE_OPEN;
        cue(ModSounds.REACTOR_STOP.get(), 1.0F);
    }

    private void finishClose() {
        state = STATE_CLOSED;
        cue(ModSounds.REACTOR_STOP.get(), 1.0F);
    }

    private void cue(SoundEvent sound, float pitch) {
        level.playSound(null, worldPosition, sound, SoundSource.BLOCKS, 0.5F, pitch);
    }

    private void spread(boolean opening) {
        Set<BlockPos> seen = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        seen.add(worldPosition);
        queue.add(worldPosition);

        while (!queue.isEmpty()) {
            BlockPos at = queue.removeFirst();
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos next = at.relative(dir);
                if (!seen.add(next)) continue;
                if (!(level.getBlockEntity(next) instanceof BlockEntityBlastDoor door)) continue;
                if (door.isLocked() && door.getPins() != getPins()) continue;
                if (opening ? !door.canOpen() : !door.canClose()) continue;
                if (opening) door.open();
                else door.close();
                queue.add(next);
            }
        }
    }

    @Override
    public void lock() {
        super.lock();
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (!(level.getBlockEntity(worldPosition.relative(dir))
                    instanceof BlockEntityBlastDoor door)) continue;
            if (door.isLocked()) continue;
            door.setPins(getPins());
            door.lock();
            door.setMod(getMod());
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        isOpening = input.getBooleanOr("isOpening", false);
        state = input.getIntOr("state", STATE_CLOSED);
        timer = input.getIntOr("timer", 0);
        redstoned = input.getBooleanOr("redstoned", false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("isOpening", isOpening);
        output.putInt("state", state);
        output.putInt("timer", timer);
        output.putBoolean("redstoned", redstoned);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x70L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 4 -> output.writeBoolean(this.isOpening);
            case 5 -> output.writeInt(this.state);
            case 6 -> output.writeInt(this.timer);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 4 -> this.isOpening = input.readBoolean();
            case 5 -> this.state = input.readInt();
            case 6 -> this.timer = input.readInt();
            default -> super.readSyncUnit(unit, input);
        }
    }
}
