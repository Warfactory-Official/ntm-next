// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.network;

import com.hbm.particle.helper.ParticleCreators;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.network.RequestNetwork.PathNode;
import com.hbm.util.TickPhase;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public abstract class BlockEntityRequestNetwork extends BlockEntityMachineBase {

    private static final int DISCOVERY_BUDGET = 5;
    private static final int LOCAL_CHUNK_RANGE = 2;
    private static final int SCAN_PERIOD = 20;
    private static final int LINK_PARTICLE_RANGE = 150;

    protected final Set<BlockPos> known = new HashSet<>();
    protected final Set<BlockPos> reachable = new HashSet<>();

    protected @Nullable PathNode published;

    protected BlockEntityRequestNetwork(
            BlockEntityType<?> type, BlockPos pos, BlockState state, int slots) {
        super(type, pos, state, slots);
    }

    public BlockPos nodePos() {
        return worldPosition.above();
    }

    protected abstract PathNode createNode(BlockPos pos);

    @Override
    public void tickServer() {
        if (!(level instanceof ServerLevel server) || !TickPhase.every(this, SCAN_PERIOD)) return;

        BlockPos node = nodePos();
        PathNode fresh = createNode(node);
        fresh.active = !server.hasNeighborSignal(worldPosition);
        RequestNetwork.push(server, fresh);
        published = fresh;

        Map<BlockPos, PathNode> local =
                new HashMap<>(RequestNetwork.localNodes(server, worldPosition, LOCAL_CHUNK_RANGE));
        local.remove(node);

        known.removeIf(
                pos -> {
                    if (local.containsKey(pos)) return false;
                    reachable.remove(pos);
                    return true;
                });

        for (BlockPos pos : known) {
            if (reachable.contains(pos)) drawLink(server, node, pos);
        }

        for (BlockPos pos : known) {
            if (RequestNetwork.hasPath(server, node, pos)) reachable.add(pos);
            else reachable.remove(pos);
        }

        int budget = DISCOVERY_BUDGET;
        for (PathNode candidate : local.values()) {
            if (!connectable(candidate, fresh)) continue;

            if (known.add(candidate.pos)) {
                budget--;
                if (RequestNetwork.hasPath(server, node, candidate.pos))
                    reachable.add(candidate.pos);
            }

            if (budget <= 0) break;
        }
    }

    private static boolean connectable(PathNode one, PathNode other) {
        return one.waypoint || other.waypoint;
    }

    private static void drawLink(ServerLevel server, BlockPos from, BlockPos to) {
        ParticleCreators.droneLine(
                server,
                from.getX() + 0.5,
                from.getY() + 0.5,
                from.getZ() + 0.5,
                (to.getX() - from.getX()) / 2D,
                (to.getY() - from.getY()) / 2D,
                (to.getZ() - from.getZ()) / 2D,
                0x00ff00,
                LINK_PARTICLE_RANGE);
    }
}
