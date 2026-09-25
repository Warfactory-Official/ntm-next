// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.generic.BlockEmitter;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.packet.toclient.PlasmaBlastPayload;
import com.hbm.platform.Services;
import io.netty.buffer.ByteBuf;
import java.awt.Color;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntityEmitter extends BlockEntity implements Synced, SyncUnitSchema {

    public static final int RANGE = 100;
    public static final int EFFECT_COUNT = 5;

    private static final int EFFECT_PLASMA = 4;

    public static final int CAST_PERIOD = 20;
    public static final float MIN_GIRTH = 0.125F;

    @SyncField(units = 1L << 0)
    public int color;

    @SyncField(units = 1L << 1)
    public int beam;

    @SyncField(units = 1L << 2)
    public float girth = 0.5F;

    @SyncField(units = 1L << 3)
    public int effect;

    public BlockEntityEmitter(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EMITTER.get(), pos, state);
    }

    public void setColor(int color) {
        this.color = color;
        setChanged();
    }

    public void addGirth(float delta) {
        girth = Math.max(girth + delta, MIN_GIRTH);
        setChanged();
    }

    public void cycleEffect() {
        effect = (effect + 1) % EFFECT_COUNT;
        setChanged();
    }

    public static int cycledColor(long gameTime) {
        return Color.HSBtoRGB(gameTime / 50.0F, 0.5F, 0.25F) & 0xFFFFFF;
    }

    public static void tickServer(
            Level level, BlockPos pos, BlockState state, BlockEntityEmitter be) {
        Direction dir = state.getValue(BlockEmitter.FACING);

        if (level.getGameTime() % CAST_PERIOD == 0) be.castBeam(level, pos, dir);
        if (be.effect == EFFECT_PLASMA && be.beam > 0) be.plasma((ServerLevel) level, pos, dir);

        be.networkPackNT(150);
    }

    private void castBeam(Level level, BlockPos pos, Direction dir) {
        for (int i = 1; i <= RANGE; i++) {
            beam = i;
            BlockPos at = pos.relative(dir, i);

            if (level.getBlockState(at).isFaceSturdy(level, at, dir)) break;
        }
    }

    private void plasma(ServerLevel level, BlockPos pos, Direction dir) {
        if (level.getGameTime() % 5 != 0) return;

        long step = level.getGameTime() / 5L;
        double x = (int) (pos.getX() + dir.getStepX() * step % beam) + 0.5D;
        double y = (int) (pos.getY() + dir.getStepY() * step % beam) + 0.5D;
        double z = (int) (pos.getZ() + dir.getStepZ() * step % beam) + 0.5D;

        int tint = color == 0 ? cycledColor(level.getGameTime()) : color;

        float r = ((tint & 0xFF0000) >> 16) / 256F;
        float g = ((tint & 0x00FF00) >> 8) / 256F;
        float b = (tint & 0x0000FF) / 256F;

        float pitch = 0F;
        float yaw = 0F;
        switch (dir) {
            case NORTH -> pitch = 90F;
            case SOUTH -> pitch = -90F;
            case WEST -> {
                pitch = 90F;
                yaw = 90F;
            }
            case EAST -> {
                pitch = -90F;
                yaw = 90F;
            }
            default -> {}
        }

        Services.NETWORK.sendToAllAround(
                new PlasmaBlastPayload(x, y, z, r, g, b, pitch, yaw, girth * 5F),
                new TargetPoint(level, x, y, z, RANGE));
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putInt("color", color);
        out.putFloat("girth", girth);
        out.putInt("effect", effect);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        color = in.getIntOr("color", 0);
        girth = in.getFloatOr("girth", 0.5F);
        effect = in.getIntOr("effect", 0);
    }

    @Override
    public long syncUnitMask() {
        return 0xfL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeInt(this.color);
            case 1 -> output.writeInt(this.beam);
            case 2 -> output.writeFloat(this.girth);
            case 3 -> output.writeInt(this.effect);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.color = input.readInt();
            case 1 -> this.beam = input.readInt();
            case 2 -> this.girth = input.readFloat();
            case 3 -> this.effect = input.readInt();
            default -> throw new IllegalArgumentException();
        }
    }
}
