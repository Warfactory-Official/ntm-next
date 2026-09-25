// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.Floodlight;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.GraphResident;
import com.hbm.tileentity.Synced;
import com.hbm.util.ChunkUtil;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class BlockEntityFloodlight extends BlockEntity
        implements IEnergyHandlerMK2, Synced, GraphResident, SyncUnitSchema {

    public static final long MAX_POWER = 5_000L;
    private static final long POWER_DRAW = 100L;
    private static final int BEAM_COUNT = 15;
    private static final int MAX_BEAM_DISTANCE = 64;
    private static final int LIGHT_DAMPENING_CUTOFF = 8;
    private final BlockPos[] lightPositions = new BlockPos[BEAM_COUNT];

    @SyncField(units = 1L << 0)
    public float rotation;

    @SyncField(units = 1L << 1)
    public long power;

    @SyncField(units = 1L << 2)
    public boolean isOn;

    private int delay;

    public BlockEntityFloodlight(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLOODLIGHT.get(), pos, state);
    }

    private static float[] getVariation(int index) {
        return new float[] {
            (((index / 3) - 2) * 7.5F) / 180F * (float) Math.PI,
            (((index % 3) - 1) * 15F) / 180F * (float) Math.PI
        };
    }

    public void tickServer() {
        if (delay > 0) {
            delay--;
            return;
        }

        if (power >= POWER_DRAW) {
            power -= POWER_DRAW;
            if (!isOn) {
                isOn = true;
                castLights();
                syncVisualState();
            } else if (TickPhase.every(this, 5)) {
                long timer = level.getGameTime() / 5L;
                castLight((int) Math.abs(timer % lightPositions.length));
            }
        } else if (isOn) {
            isOn = false;
            delay = 60;
            destroyLights();
            syncVisualState();
        }
    }

    public Direction inputDirection() {
        return Direction.from3DDataValue(getBlockState().getValue(Floodlight.FACING) % 6)
                .getOpposite();
    }

    private void castLight(int index) {
        BlockPos newPos = getRayEndpoint(index);
        BlockPos oldPos = lightPositions[index];
        lightPositions[index] = null;

        if (newPos == null || !newPos.equals(oldPos)) {
            if (oldPos != null
                    && ChunkUtil.blockEntityIfLoaded(level, oldPos)
                            instanceof BlockEntityFloodlightBeam beam
                    && beam.isFrom(worldPosition)) {
                level.setBlock(oldPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }

        if (newPos == null || ChunkUtil.chunkIfLoaded(level, newPos) == null) return;

        if (level.getBlockState(newPos).isAir()) {
            level.setBlock(
                    newPos,
                    ModBlocks.FLOODLIGHT_BEAM.get().defaultBlockState(),
                    Block.UPDATE_CLIENTS);
            if (level.getBlockEntity(newPos) instanceof BlockEntityFloodlightBeam beam) {
                beam.setSource(this, index);
            }
            lightPositions[index] = newPos;
        }

        if (level.getBlockState(newPos).is(ModBlocks.FLOODLIGHT_BEAM.get())) {
            lightPositions[index] = newPos;
        }
    }

    private @Nullable BlockPos getRayEndpoint(int index) {
        if (index < 0 || index >= lightPositions.length) return null;

        int meta = getBlockState().getValue(Floodlight.FACING);
        float[] angles = getVariation(index);
        float adjustedRotation = rotation;
        if (meta == 1 || meta == 7) adjustedRotation = 180F - adjustedRotation;
        if (meta == 6) adjustedRotation = 180F - adjustedRotation;

        Vec3 direction =
                new Vec3(1D, 0D, 0D).zRot((float) (adjustedRotation / 180D * Math.PI) + angles[0]);
        if (meta == 6 || meta == 7 || meta == 2) direction = direction.yRot((float) (Math.PI / 2D));
        if (meta == 3) direction = direction.yRot((float) -(Math.PI / 2D));
        if (meta == 4) direction = direction.yRot((float) Math.PI);
        direction = direction.yRot(angles[1]);

        for (int distance = 1; distance < MAX_BEAM_DISTANCE; distance++) {
            BlockPos tested =
                    BlockPos.containing(
                            worldPosition.getX() + 0.5D + direction.x * distance,
                            worldPosition.getY() + 0.5D + direction.y * distance,
                            worldPosition.getZ() + 0.5D + direction.z * distance);
            if (tested.equals(worldPosition)) continue;
            BlockState blocking = ChunkUtil.blockStateIfLoaded(level, tested);
            if (blocking == null) return null;

            if (blocking.getLightDampening() < LIGHT_DAMPENING_CUTOFF) continue;

            if (distance > 1) {
                return BlockPos.containing(
                        worldPosition.getX() + 0.5D + direction.x * (distance - 1),
                        worldPosition.getY() + 0.5D + direction.y * (distance - 1),
                        worldPosition.getZ() + 0.5D + direction.z * (distance - 1));
            }
        }

        return null;
    }

    private void castLights() {
        for (int index = 0; index < lightPositions.length; index++) castLight(index);
    }

    private void destroyLight(int index) {
        BlockPos pos = lightPositions[index];
        BlockState beam = pos == null ? null : ChunkUtil.blockStateIfLoaded(level, pos);
        if (beam != null && beam.is(ModBlocks.FLOODLIGHT_BEAM.get())) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public void destroyLights() {
        for (int index = 0; index < lightPositions.length; index++) destroyLight(index);
    }

    boolean ownsBeam(BlockPos pos, int index) {
        return pos.equals(lightPositions[index]);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null && !level.isClientSide()) destroyLights();
    }

    private void syncVisualState() {
        setChanged();
        BlockState state = getBlockState();
        level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putFloat("rotation", rotation);
        out.putLong("power", power);
        out.putBoolean("isOn", isOn);
        int cast = 0;
        List<BlockPos> lights = new ArrayList<>();
        for (int i = 0; i < lightPositions.length; i++) {
            if (lightPositions[i] == null) continue;
            cast |= 1 << i;
            lights.add(lightPositions[i]);
        }
        out.putInt("lightMask", cast);
        out.store("lights", BlockPos.CODEC.listOf(), lights);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        rotation = in.getFloatOr("rotation", 0F);
        power = in.getLongOr("power", 0L);
        isOn = in.getBooleanOr("isOn", false);
        int cast = in.getIntOr("lightMask", 0);
        List<BlockPos> lights = in.read("lights", BlockPos.CODEC.listOf()).orElse(List.of());
        for (int i = 0, next = 0; i < lightPositions.length; i++) {
            lightPositions[i] =
                    (cast & 1 << i) != 0 && next < lights.size() ? lights.get(next++) : null;
        }
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long power) {
        this.power = power;
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    public long syncUnitMask() {
        return 0x7L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeFloat(this.rotation);
            case 1 -> output.writeLong(this.power);
            case 2 -> output.writeBoolean(this.isOn);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.rotation = input.readFloat();
            case 1 -> this.power = input.readLong();
            case 2 -> this.isOn = input.readBoolean();
            default -> throw new IllegalArgumentException();
        }
    }
}
