// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.generic.BlockDoorGeneric;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.BlockMultiblockGeometryCell;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncWire;
import com.hbm.packet.WireReplayServer;
import com.hbm.registration.RegistryHandle;
import com.hbm.render.anim.HbmAnimations;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import com.hbm.tileentity.machine.BlockEntityLockableBase;
import io.netty.buffer.ByteBuf;
import java.util.HashSet;
import java.util.List;
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

public class BlockEntityDoorGeneric extends BlockEntityLockableBase implements Audible {

    public static final byte STATE_CLOSED = 0, STATE_OPEN = 1, STATE_CLOSING = 2, STATE_OPENING = 3;
    private final Set<BlockPos> activatedBlocks = new HashSet<>(4);

    @SyncField(units = 1L << 4)
    public byte state = STATE_CLOSED;

    @SyncField(value = 2, units = 0)
    public int openTicks;

    public int redstonePower;

    public long animStartTime;

    public HbmAnimations.@Nullable Animation currentAnimation;
    private @Nullable DoorDecl doorType;
    private @Nullable AudioWrapper audio;
    private @Nullable AudioWrapper audio2;
    private byte incomingState;

    private boolean resumeOwed;

    public BlockEntityDoorGeneric(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NTM_DOOR.get(), pos, state, 0);
    }

    private static BlockPos rotate(int x, int z, Direction direction) {
        return switch (direction) {
            case SOUTH -> new BlockPos(-x, 0, -z);
            case EAST -> new BlockPos(-z, 0, x);
            case WEST -> new BlockPos(z, 0, -x);
            default -> new BlockPos(x, 0, z);
        };
    }

    public @Nullable DoorDecl getDoorType() {
        if (doorType == null && getBlockState().getBlock() instanceof BlockDoorGeneric door)
            doorType = door.type;
        return doorType;
    }

    public void tickServer() {
        DoorDecl door = getDoorType();
        if (door.onDoorUpdate() != null) door.onDoorUpdate().accept(this);

        advanceOpenTicks(door);
        updateDoorCells(door);

        if (state == STATE_OPENING && openTicks == door.timeToOpen()) state = STATE_OPEN;
        if (state == STATE_CLOSING && openTicks == 0) state = STATE_CLOSED;
        syncCoreOpen();

        networkPackNT(100);

        if (redstonePower == -1 && state == STATE_OPEN) tryToggle(-1);
        else if (redstonePower > 0 && state == STATE_CLOSED) tryToggle(-1);
        if (redstonePower == -1) redstonePower = 0;
    }

    public void tickClient() {
        if (resumeOwed) {
            resumeOwed = false;
            resumeTransition(getDoorType());
        }
        advanceOpenTicks(getDoorType());
    }

    private void resumeTransition(DoorDecl door) {
        if (state != STATE_OPENING && state != STATE_CLOSING) return;
        startAudio(
                door,
                state == STATE_OPENING ? door.getOpenSoundLoop() : door.getCloseSoundLoop(),
                null,
                door);
        stampAnimation(door, state);
    }

    private void advanceOpenTicks(DoorDecl door) {
        if (state == STATE_OPENING) {
            openTicks++;
            if (openTicks >= door.timeToOpen()) openTicks = door.timeToOpen();
        } else if (state == STATE_CLOSING) {
            openTicks--;
            if (openTicks <= 0) openTicks = 0;
        }
    }

    private void updateDoorCells(DoorDecl door) {
        if (state != STATE_OPENING && state != STATE_CLOSING) return;
        ServerLevel server = (ServerLevel) level;
        boolean opening = state == STATE_OPENING;
        int[][] ranges = door.getDoorOpenRanges();

        for (int i = 0; i < ranges.length; i++) {
            int[] range = ranges[i];
            float time = door.getDoorRangeOpenTime(openTicks, i);
            int length = Math.abs(range[3]);

            int divisor = Math.abs(range[3] - 1);
            if (opening) {
                for (int j = 0; j < length; j++) {
                    if ((float) j / divisor > time) break;
                    updateColumn(server, range, j, true);
                }
            } else {
                for (int j = length - 1; j >= 0; j--) {
                    if ((float) j / divisor < time) break;
                    updateColumn(server, range, j, false);
                }
            }
        }
    }

    private void updateColumn(ServerLevel server, int[] range, int j, boolean opening) {
        int signum = Integer.signum(range[3]);
        for (int k = 0; k < range[4]; k++) {
            BlockPos local =
                    switch (range[5]) {
                        case 1 -> new BlockPos(range[0] + k, range[1] + signum * j, range[2]);
                        case 2 -> new BlockPos(range[0] + signum * j, range[1] + k, range[2]);
                        default -> new BlockPos(range[0], range[1] + k, range[2] + signum * j);
                    };
            BlockPos rotated = rotate(local.getX(), local.getZ(), facing());
            BlockPos pos = worldPosition.offset(rotated.getX(), local.getY(), rotated.getZ());
            if (pos.equals(worldPosition)) continue;
            BlockMultiblockGeometryCell.setCellOpen(server, pos, worldPosition, opening);
        }
    }

    private void syncCoreOpen() {
        BlockState core = getBlockState();
        if (!core.hasProperty(BlockDoorGeneric.OPEN)) return;
        boolean open = state != STATE_CLOSED;
        if (core.getValue(BlockDoorGeneric.OPEN) == open) return;
        level.setBlock(
                worldPosition, core.setValue(BlockDoorGeneric.OPEN, open), Block.UPDATE_CLIENTS);
    }

    public Direction facing() {
        return getBlockState().getValue(BlockMultiblockCore.FACING);
    }

    public boolean tryToggle(@Nullable Player player) {
        if (isLocked() && player == null) return false;

        if (state == STATE_CLOSED && redstonePower > 0) return false;
        if (state == STATE_CLOSED) {
            if (canAccess(player)) {
                state = STATE_OPENING;
                setChanged();
            }
            return true;
        }
        if (state == STATE_OPEN) {
            if (canAccess(player)) {
                state = STATE_CLOSING;
                setChanged();
            }
            return true;
        }
        return false;
    }

    public boolean tryToggle(int passcode) {
        if (isLocked() && passcode != lock) return false;
        if (state == STATE_CLOSED) {
            state = STATE_OPENING;
            setChanged();
            return true;
        }
        if (state == STATE_OPEN) {
            state = STATE_CLOSING;
            setChanged();
            return true;
        }
        return false;
    }

    public void open() {
        if (state == STATE_CLOSED) state = STATE_OPENING;
    }

    public void close() {
        if (state == STATE_OPEN) state = STATE_CLOSING;
    }

    public void updateRedstonePower(BlockPos pos) {
        boolean powered = level.hasNeighborSignal(pos);
        boolean contained = activatedBlocks.contains(pos);
        if (!contained && powered) {
            activatedBlocks.add(pos);
            if (redstonePower == -1) redstonePower = 0;
            redstonePower++;
        } else if (contained && !powered) {
            activatedBlocks.remove(pos);
            redstonePower--;
            if (redstonePower == 0) redstonePower = -1;
        }
    }

    public void playCue(RegistryHandle<SoundEvent> sound) {
        level.playSound(null, worldPosition, sound.get(), SoundSource.BLOCKS, 1F, 1F);
    }

    public byte getSkinIndex() {
        BlockState state = getBlockState();
        return state.hasProperty(BlockDoorGeneric.SKIN)
                ? state.getValue(BlockDoorGeneric.SKIN).byteValue()
                : 0;
    }

    public boolean cycleSkinIndex() {
        DoorDecl door = getDoorType();
        if (!door.hasSkins()) return false;
        int next = (getSkinIndex() + 1) % door.getSkinCount();
        level.setBlock(
                worldPosition,
                getBlockState().setValue(BlockDoorGeneric.SKIN, next),
                Block.UPDATE_ALL);
        return true;
    }

    private void handleNewState(byte next) {
        if (state == next) return;
        DoorDecl door = getDoorType();

        if (state == STATE_CLOSED && next == STATE_OPENING) {
            startAudio(door, door.getOpenSoundLoop(), door.getOpenSoundStart(), door);
        }
        if (state == STATE_OPEN && next == STATE_CLOSING) {
            startAudio(door, door.getCloseSoundLoop(), door.getCloseSoundStart(), door);
        }
        if (next == STATE_OPEN || next == STATE_CLOSED) stopAudio();
        if (state == STATE_OPENING && next == STATE_OPEN) playOneShot(door, door.getOpenSoundEnd());
        if (state == STATE_CLOSING && next == STATE_CLOSED)
            playOneShot(door, door.getCloseSoundEnd());

        state = next;
        if (next == STATE_OPENING || next == STATE_CLOSING) stampAnimation(door, next);
    }

    private void stampAnimation(DoorDecl door, byte next) {
        animStartTime = transitionStart(door, next);
        currentAnimation = door.getSEDNAAnim(next, getSkinIndex(), animStartTime);
    }

    private long transitionStart(DoorDecl door, byte next) {
        int elapsed = next == STATE_OPENING ? openTicks : door.timeToOpen() - openTicks;
        return (level.getGameTime() - elapsed) * 50L;
    }

    private void startAudio(
            DoorDecl door,
            @Nullable RegistryHandle<SoundEvent> loop,
            @Nullable RegistryHandle<SoundEvent> oneShot,
            DoorDecl decl) {
        if (audio != null) audio.stopSound();
        audio = loop == null ? null : loop(door, loop);
        if (audio != null) audio.startSound();
        playOneShot(door, oneShot);
        if (decl.getSoundLoop2() != null) {
            if (audio2 != null) audio2.stopSound();
            audio2 = loop(door, decl.getSoundLoop2());
            if (audio2 != null) audio2.startSound();
        }
    }

    private @Nullable AudioWrapper loop(DoorDecl door, RegistryHandle<SoundEvent> sound) {
        return AudioSystem.getLoopedSound(
                sound.get(),
                SoundSource.BLOCKS,
                worldPosition.getX(),
                worldPosition.getY(),
                worldPosition.getZ(),
                getVolume(door.getSoundVolume()),
                10F,
                1F);
    }

    private void playOneShot(DoorDecl door, @Nullable RegistryHandle<SoundEvent> sound) {
        if (sound == null) return;
        level.playLocalSound(
                worldPosition.getX(),
                worldPosition.getY(),
                worldPosition.getZ(),
                sound.get(),
                SoundSource.BLOCKS,
                getVolume(door.getSoundVolume()),
                1F,
                false);
    }

    private void stopAudio() {
        if (audio != null) {
            audio.stopSound();
            audio = null;
        }
        if (audio2 != null) {
            audio2.stopSound();
            audio2 = null;
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        stopAudio();
    }

    @Override
    protected boolean syncMuffled() {
        return true;
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 1L << 4;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        if (unit == 4) {
            output.writeByte(state);
            return;
        }
        super.writeSyncUnit(unit, output);
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        if (unit == 4) {
            incomingState = input.readByte();
            return;
        }
        super.readSyncUnit(unit, input);
    }

    @Override
    public void afterSyncUnits(long units) {
        if ((units & (1L << 4)) != 0) handleNewState(incomingState);
        if ((units & (1L << MUFFLE_UNIT)) != 0 && (audio != null || audio2 != null)) {
            float volume = getVolume(getDoorType().getSoundVolume());
            if (audio != null) audio.updateVolume(volume);
            if (audio2 != null) audio2.updateVolume(volume);
        }
    }

    @Override
    public void afterInitialSyncUnits() {
        stopAudio();
        currentAnimation = null;
        state = incomingState;
        resumeOwed = state == STATE_OPENING || state == STATE_CLOSING;
    }

    @Override
    public void writeInitialExtras(ByteBuf buf) {
        boolean relayed =
                SyncWire.replayCompat()
                        && level instanceof ServerLevel server
                        && server.getServer() instanceof WireReplayServer;
        buf.writeInt(relayed ? relayedOpenTicks(getDoorType()) : openTicks);
    }

    @Override
    public boolean afterRelayedSyncUnits(long units, boolean initial) {
        if (!initial && (units & (1L << 4)) == 0) return false;
        DoorDecl door = getDoorType();
        if (!initial)
            openTicks =
                    switch (incomingState) {
                        case STATE_OPEN -> door.timeToOpen();
                        case STATE_CLOSED -> 0;
                        default -> relayedOpenTicks(door);
                    };
        state = incomingState;
        animStartTime = transitionStart(door, state);

        return !initial;
    }

    private int relayedOpenTicks(DoorDecl door) {
        int elapsed = (int) (level.getGameTime() - animStartTime / 50L);
        return switch (state) {
            case STATE_OPENING -> Math.min(elapsed, door.timeToOpen());
            case STATE_CLOSING -> Math.max(door.timeToOpen() - elapsed, 0);
            default -> openTicks;
        };
    }

    @Override
    public void readInitialExtras(ByteBuf buf) {
        openTicks = buf.readInt();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        state = input.getByteOr("state", state);
        openTicks = input.getIntOr("openTicks", openTicks);
        redstonePower = input.getIntOr("redstoned", redstonePower);
        input.read("activatedBlocks", BlockPos.CODEC.listOf()).ifPresent(activatedBlocks::addAll);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putByte("state", state);
        output.putInt("openTicks", openTicks);
        output.putInt("redstoned", redstonePower);
        output.store("activatedBlocks", BlockPos.CODEC.listOf(), List.copyOf(activatedBlocks));
    }
}
