// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.FoldedCoreResident;
import com.hbm.tileentity.GraphResident;
import com.hbm.tileentity.Synced;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class BlockEntityMachineTeleporter extends BlockEntity
        implements Synced, GraphResident, FoldedCoreResident, IEnergyHandlerMK2, SyncUnitSchema {

    public static final long MAX_POWER = 1_500_000;
    public static final long CONSUMPTION = 1_000_000;

    private static final DustParticleOptions EXIT_MOTE = new DustParticleOptions(0x66CCFF, 1.0F);

    @SyncField(units = 1L << 0)
    public long power;

    @SyncField(units = 1L << 1)
    public @Nullable GlobalPos target;

    public BlockEntityMachineTeleporter(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TELEBLOCK.get(), pos, state);
    }

    public void tickServer() {
        if (target != null) {
            AABB box =
                    new AABB(
                            worldPosition.getX() + 0.25,
                            worldPosition.getY(),
                            worldPosition.getZ() + 0.25,
                            worldPosition.getX() + 0.75,
                            worldPosition.getY() + 2,
                            worldPosition.getZ() + 0.75);
            for (Entity entity : level.getEntitiesOfClass(Entity.class, box)) teleport(entity);
        }
        networkPackNT(15);
    }

    public void tickClient() {
        if (target == null || power < CONSUMPTION) return;
        level.addParticle(
                EXIT_MOTE,
                worldPosition.getX() + 0.5 + level.getRandom().nextGaussian() * 0.25D,
                worldPosition.getY() + 1 + level.getRandom().nextDouble() * 2D,
                worldPosition.getZ() + 0.5 + level.getRandom().nextGaussian() * 0.25D,
                0D,
                0D,
                0D);
    }

    public void teleport(Entity entity) {
        if (target == null || power < CONSUMPTION) return;

        level.playSound(
                null,
                worldPosition.getX() + 0.5,
                worldPosition.getY() + 1.5,
                worldPosition.getZ() + 0.5,
                SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.BLOCKS,
                1.0F,
                1.0F);

        ServerLevel destination = ((ServerLevel) level).getServer().getLevel(target.dimension());
        if (destination != null) {
            BlockPos exit = target.pos();

            TeleportTransition.PostTeleportTransition arrival =
                    destination == level
                            ? TeleportTransition.DO_NOTHING
                            : TeleportTransition.PLACE_PORTAL_TICKET;

            entity.teleport(
                    new TeleportTransition(
                            destination,
                            new Vec3(exit.getX() + 0.5D, exit.getY() + 1.5D, exit.getZ() + 0.5D),
                            entity.getDeltaMovement(),
                            entity.getYRot(),
                            entity.getXRot(),
                            arrival));
        }

        level.playSound(
                null,
                entity.getX(),
                entity.getY(),
                entity.getZ(),
                SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.BLOCKS,
                1.0F,
                1.0F);

        this.power -= CONSUMPTION;
        setChanged();
    }

    public void setTarget(GlobalPos target) {
        this.target = target;
        setChanged();
    }

    private void writeTarget(ByteBuf output) {
        output.writeBoolean(target != null);
        if (target != null) GlobalPos.STREAM_CODEC.encode(output, target);
    }

    private void readTarget(ByteBuf input) {
        target = input.readBoolean() ? GlobalPos.STREAM_CODEC.decode(input) : null;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.power = input.getLongOr("power", 0L);
        this.target = input.read("target", GlobalPos.CODEC).orElse(null);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.storeNullable("target", GlobalPos.CODEC, target);
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
        return 0x3L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(this.power);
            case 1 -> writeTarget(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.power = input.readLong();
            case 1 -> readTarget(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
