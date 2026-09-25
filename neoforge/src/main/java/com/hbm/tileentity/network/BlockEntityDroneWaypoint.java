// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.network;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.network.DroneWaypointBlock;
import com.hbm.entity.item.EntityDeliveryDrone;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.particle.helper.ParticleCreators;
import com.hbm.tileentity.FoldedCoreResident;
import com.hbm.tileentity.GraphResident;
import com.hbm.tileentity.Synced;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public class BlockEntityDroneWaypoint extends BlockEntity
        implements Synced, GraphResident, FoldedCoreResident, IDroneLinkable, SyncUnitSchema {

    private static final int LINE_PARTICLE_RANGE = 150;
    private static final int LINE_COLOUR = 0x0000ff;

    @SyncField(units = 1L)
    public int height = 5;

    @SyncField(units = 1L << 1)
    public @Nullable BlockPos next;

    public BlockEntityDroneWaypoint(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DRONE_WAYPOINT.get(), pos, state);
    }

    public void tickServer() {
        BlockPos point = getPoint();

        if (next != null) {
            for (EntityDeliveryDrone drone :
                    ((ServerLevel) level)
                            .getEntitiesOfClass(EntityDeliveryDrone.class, new AABB(point))) {
                if (drone.getDeltaMovement().length() >= 0.05) continue;
                drone.setTarget(next.getX() + 0.5, next.getY(), next.getZ() + 0.5);
            }

            if (level.getGameTime() % 2 == 0) {
                ParticleCreators.droneLine(
                        level,
                        point.getX() + 0.5,
                        point.getY() + 0.5,
                        point.getZ() + 0.5,
                        next.getX() - point.getX(),
                        next.getY() - point.getY(),
                        next.getZ() - point.getZ(),
                        LINE_COLOUR,
                        LINE_PARTICLE_RANGE);
            }
        }

        networkPackNT(15);
    }

    public void tickClient() {
        if (next == null || level.getGameTime() % 2 != 0) return;

        BlockPos point = getPoint();
        level.addParticle(
                DustParticleOptions.REDSTONE,
                point.getX() + 0.5,
                point.getY() + 0.5,
                point.getZ() + 0.5,
                0,
                0,
                0);
    }

    @Override
    public BlockPos getPoint() {
        return worldPosition.relative(getBlockState().getValue(DroneWaypointBlock.FACING), height);
    }

    @Override
    public void setNextTarget(BlockPos target) {
        this.next = target;
        setChanged();
    }

    public void addHeight(int delta) {
        height = Mth.clamp(height + delta, 1, 15);
        setChanged();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        height = input.getIntOr("height", 5);
        next = input.read("next", BlockPos.CODEC).orElse(null);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("height", height);
        if (next != null) output.store("next", BlockPos.CODEC, next);
    }

    private void writeNext(ByteBuf output) {
        output.writeBoolean(next != null);
        if (next != null) {
            output.writeInt(next.getX());
            output.writeInt(next.getY());
            output.writeInt(next.getZ());
        }
    }

    private void readNext(ByteBuf input) {
        next =
                input.readBoolean()
                        ? new BlockPos(input.readInt(), input.readInt(), input.readInt())
                        : null;
    }

    @Override
    public long syncUnitMask() {
        return 0x3L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeInt(this.height);
            case 1 -> writeNext(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.height = input.readInt();
            case 1 -> readNext(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
