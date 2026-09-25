// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.albion;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.FoldedCoreResident;
import com.hbm.tileentity.GraphResident;
import com.hbm.tileentity.Synced;
import com.hbm.tileentity.machine.albion.BlockEntityPASource.Particle;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityPABeamline extends BlockEntity
        implements Synced, GraphResident, FoldedCoreResident, IParticleUser, SyncUnitSchema {

    private long flashTick = Long.MIN_VALUE / 2;

    @SyncField(units = 1L << 0)
    private int passCount;

    private int lastSeenPass = -1;

    public BlockEntityPABeamline(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PA_BEAMLINE.get(), pos, state);
    }

    public float flash(float partialTick) {
        return Mth.clamp(2F - 0.25F * (level.getGameTime() - flashTick - 1 + partialTick), 0F, 2F);
    }

    private Direction beamlineDir() {

        return BlockMultiblockCore.coreFacing(getBlockState()).getCounterClockWise();
    }

    @Override
    public boolean canParticleEnter(Particle particle, Direction dir, BlockPos pos) {
        Direction beamlineDir = beamlineDir();
        return worldPosition.relative(beamlineDir, -1).equals(pos) && beamlineDir == dir;
    }

    @Override
    public void onEnter(Particle particle, Direction dir) {
        particle.addDistance(3);
        this.passCount++;
        networkPackNT(150);
    }

    @Override
    public BlockPos getExitPos(Particle particle) {
        return worldPosition.relative(beamlineDir(), 2);
    }

    private void readPassCount(ByteBuf input) {
        int count = input.readInt();
        if (lastSeenPass != -1 && count != lastSeenPass) flashTick = level.getGameTime();
        lastSeenPass = count;
    }

    @Override
    public long syncUnitMask() {
        return 1L << 0;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeInt(this.passCount);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> readPassCount(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
