// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.packet.toclient.GasFlamePayload;
import com.hbm.particle.CoolingTowerParticleOptions;
import com.hbm.platform.Services;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntityPartEmitter extends BlockEntity implements Synced, SyncUnitSchema {

    public static final int EFFECT_COUNT = 4;

    private static final int RANGE = 150;

    @SyncField(units = 1L)
    public int effect;

    public BlockEntityPartEmitter(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PART_EMITTER.get(), pos, state);
    }

    public void cycleEffect() {
        effect = (effect + 1) % EFFECT_COUNT;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    public static void tickServer(
            Level level, BlockPos pos, BlockState state, BlockEntityPartEmitter be) {
        if (be.effect != 1) return;
        RandomSource rand = level.getRandom();
        double x = pos.getX() + rand.nextDouble();
        double y = pos.getY() + 4.5D + rand.nextDouble();
        double z = pos.getZ() + rand.nextDouble();
        Services.NETWORK.sendToAllAround(
                new GasFlamePayload(
                        x,
                        y,
                        z,
                        rand.nextGaussian() * 0.2D,
                        0.1D,
                        rand.nextGaussian() * 0.2D,
                        GasFlamePayload.DEFAULT_SCALE),
                new TargetPoint((ServerLevel) level, x, y, z, RANGE));
    }

    public static void tickClient(
            Level level, BlockPos pos, BlockState state, BlockEntityPartEmitter be) {
        RandomSource rand = level.getRandom();

        if (be.effect == 2) {
            CoolingTowerParticleOptions opts =
                    new CoolingTowerParticleOptions.Builder()
                            .setLift(5F)
                            .setBaseScale(0.25F)
                            .setMaxScale(5F)
                            .setLife(560 + rand.nextInt(20))
                            .setColor(0x404040)
                            .build();
            level.addParticle(
                    opts, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 0, 0, 0);
        }

        if (be.effect == 3) {
            CoolingTowerParticleOptions opts =
                    new CoolingTowerParticleOptions.Builder()
                            .setLift(0.5F)
                            .setBaseScale(1F)
                            .setMaxScale(10F)
                            .setLife(750 + rand.nextInt(250))
                            .build();
            level.addParticle(
                    opts,
                    pos.getX() + 0.5D + rand.nextDouble() * 3 - 1.5D,
                    pos.getY() + 1,
                    pos.getZ() + 0.5D + rand.nextDouble() * 3 - 1.5D,
                    0,
                    0,
                    0);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putInt("effect", effect);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        effect = in.getIntOr("effect", 0);
    }

    @Override
    public long syncUnitMask() {
        return 1L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        if (unit != 0) throw new IllegalArgumentException();
        output.writeInt(effect);
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        if (unit != 0) throw new IllegalArgumentException();
        effect = input.readInt();
    }
}
