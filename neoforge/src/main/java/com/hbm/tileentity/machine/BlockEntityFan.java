// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.MachineFan;
import com.hbm.capability.NtmContracts;
import com.hbm.interfaces.IBlowable;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.FoldedCoreResident;
import com.hbm.tileentity.GraphResident;
import com.hbm.tileentity.Synced;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

public class BlockEntityFan extends BlockEntity
        implements Synced, GraphResident, FoldedCoreResident, SyncUnitSchema {

    public float spin;
    public float prevSpin;

    @SyncField(units = 1L << 0)
    public boolean falloff = true;

    @SyncField(units = 1L << 1)
    public boolean suck = false;

    public BlockEntityFan(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FAN.get(), pos, state);
    }

    @Override
    public void setBlockState(BlockState state) {
        super.setBlockState(state);
        if (!state.getValue(MachineFan.POWERED)) prevSpin = spin;
    }

    public void tick() {

        this.prevSpin = this.spin;

        Direction dir = getBlockState().getValue(MachineFan.FACING);

        int range = 10;
        int effRange = 0;
        double push = 0.1;

        for (int i = 1; i <= range; i++) {
            BlockPos probe = worldPosition.relative(dir, i);
            BlockState state = level.getBlockState(probe);
            IBlowable blowable = NtmContracts.BLOWABLE.at(level, probe, state);

            if ((state.isCollisionShapeFullBlock(level, probe)
                            && state.canOcclude()
                            && !state.isSignalSource())
                    || blowable != null) {
                if (!level.isClientSide() && blowable != null) {
                    blowable.applyFan(level, probe, dir, i);
                }

                break;
            }

            effRange = i;
        }

        int x = dir.getStepX() * effRange;
        int y = dir.getStepY() * effRange;
        int z = dir.getStepZ() * effRange;

        List<Entity> affected =
                level.getEntitiesOfClass(
                        Entity.class,
                        new AABB(
                                        worldPosition.getX() + 0.5 + Math.min(x, 0),
                                        worldPosition.getY() + 0.5 + Math.min(y, 0),
                                        worldPosition.getZ() + 0.5 + Math.min(z, 0),
                                        worldPosition.getX() + 0.5 + Math.max(x, 0),
                                        worldPosition.getY() + 0.5 + Math.max(y, 0),
                                        worldPosition.getZ() + 0.5 + Math.max(z, 0))
                                .inflate(0.5, 0.5, 0.5));

        for (Entity e : affected) {

            double coeff = push;

            if (falloff) {
                double dist =
                        Math.sqrt(
                                e.distanceToSqr(
                                        worldPosition.getX() + 0.5,
                                        worldPosition.getY() + 0.5,
                                        worldPosition.getZ() + 0.5));
                coeff *= 1.5 * (1 - dist / range / 2);
            }
            if (suck) coeff *= -1;

            e.setDeltaMovement(
                    e.getDeltaMovement()
                            .add(
                                    dir.getStepX() * coeff,
                                    dir.getStepY() * coeff,
                                    dir.getStepZ() * coeff));
        }

        if (level.isClientSide() && level.getRandom().nextInt(30) == 0) {
            double speed = suck ? -0.2 : 0.2;
            level.addParticle(
                    ParticleTypes.CLOUD,
                    worldPosition.getX() + 0.5 + dir.getStepX() * 0.5,
                    worldPosition.getY() + 0.5 + dir.getStepY() * 0.5,
                    worldPosition.getZ() + 0.5 + dir.getStepZ() * 0.5,
                    dir.getStepX() * speed,
                    dir.getStepY() * speed,
                    dir.getStepZ() * speed);
        }

        this.spin += 30;

        if (this.spin >= 360) {
            this.prevSpin -= 360;
            this.spin -= 360;
        }

        if (!level.isClientSide()) {
            networkPackNT(150);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        falloff = input.getBooleanOr("falloff", falloff);
        suck = input.getBooleanOr("suck", suck);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("falloff", falloff);
        output.putBoolean("suck", suck);
    }

    @Override
    public long syncUnitMask() {
        return 0x3L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeBoolean(this.falloff);
            case 1 -> output.writeBoolean(this.suck);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.falloff = input.readBoolean();
            case 1 -> this.suck = input.readBoolean();
            default -> throw new IllegalArgumentException();
        }
    }
}
