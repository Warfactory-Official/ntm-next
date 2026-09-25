// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.network;

import com.hbm.api.energymk2.CableData;
import com.hbm.api.energymk2.PowerGraph;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.FoldedCoreResident;
import com.hbm.tileentity.GraphResident;
import com.hbm.tileentity.Synced;
import com.hbm.uninos.graph.GraphNode;
import com.hbm.util.ChunkUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public abstract class BlockEntityPylonBase extends BlockEntity
        implements Synced, GraphResident, FoldedCoreResident, SyncUnitSchema {

    @SyncField(units = 1L)
    public long[] connections = new long[0];

    @SyncField(units = 1L << 1)
    public int color;

    protected BlockEntityPylonBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static int canConnect(BlockEntityPylonBase first, BlockEntityPylonBase second) {
        if (first.connectionType() != second.connectionType()) return 1;
        if (first == second) return 2;
        double reach = Math.min(first.maxWireLength(), second.maxWireLength());
        return reach >= first.connectionPoint().distanceTo(second.connectionPoint()) ? 0 : 3;
    }

    public abstract ConnectionType connectionType();

    public abstract Vec3[] mounts();

    public abstract double maxWireLength();

    public double familyLift() {
        return switch (connectionType()) {
            case SINGLE -> 5.5D;
            case TRIPLE -> 7.5D;
            case QUAD -> 12.3125D;
        };
    }

    public Vec3 connectionPoint() {
        Vec3[] mounts = mounts();
        if (mounts.length == 0) return Vec3.atCenterOf(worldPosition);
        return mounts[0].add(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
    }

    public void refreshConnections() {
        GraphNode<CableData> node =
                PowerGraph.get((ServerLevel) level).getNode(worldPosition.asLong());
        this.connections =
                node != null && node.hasRemoteLinks()
                        ? node.remoteLinks.toLongArray()
                        : new long[0];
        networkPackNTTracking();
    }

    public static void refreshIfLoaded(ServerLevel level, long pos) {
        BlockEntityPylonBase pylon =
                ChunkUtil.blockEntityIfLoaded(BlockEntityPylonBase.class, level, BlockPos.of(pos));
        if (pylon != null) pylon.refreshConnections();
    }

    @Override
    public void onGraphLoad(ServerLevel server) {
        refreshConnections();
    }

    public void setColor(int dyed, ItemStack stack, Player player) {
        stack.consume(1, player);
        this.color = dyed;
        setChanged();
        networkPackNTTracking();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        color = input.getIntOr("color", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("color", color);
    }

    public enum ConnectionType {
        SINGLE,
        TRIPLE,
        QUAD
    }

    private void writeConnections(ByteBuf output) {
        output.writeInt(connections.length);
        for (long connection : connections) output.writeLong(connection);
    }

    private void readConnections(ByteBuf input) {
        long[] values = new long[input.readInt()];
        for (int i = 0; i < values.length; i++) values[i] = input.readLong();
        connections = values;
    }

    @Override
    public long syncUnitMask() {
        return 0x3L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> writeConnections(output);
            case 1 -> output.writeInt(this.color);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> readConnections(input);
            case 1 -> this.color = input.readInt();
            default -> throw new IllegalArgumentException();
        }
    }
}
