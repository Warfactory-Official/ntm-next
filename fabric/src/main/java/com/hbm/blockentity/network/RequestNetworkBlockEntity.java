/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.blockentity.network;

import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.logistics.courier.CourierEndpoint;
import com.hbm.logistics.courier.CourierEndpointRegistry;
import com.hbm.particle.helper.ParticleCreators;
import com.hbm.util.TickPhase;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class RequestNetworkBlockEntity extends MachineBlockEntity {
    private static final int DISCOVERY_BUDGET = 5;
    private static final int LOCAL_CHUNK_RANGE = 2;
    private static final int LINK_PARTICLE_RANGE = 150;

    protected final Set<BlockPos> known = new HashSet<>();
    protected final Set<BlockPos> reachable = new HashSet<>();
    protected @Nullable CourierEndpoint published;

    protected RequestNetworkBlockEntity(
            BlockEntityType<?> type, BlockPos pos, BlockState state, int slots) {
        super(type, pos, state, slots);
    }

    public BlockPos nodePos() {
        return worldPosition.above();
    }

    protected abstract CourierEndpoint createNode(BlockPos pos);

    @Override
    public void tickServer() {
        if (!(level instanceof ServerLevel server)
                || !TickPhase.every(this, CourierEndpointRegistry.SCAN_INTERVAL)) return;

        BlockPos self = nodePos();
        CourierEndpoint fresh = createNode(self);
        fresh.enabled(!server.hasNeighborSignal(worldPosition));
        CourierEndpointRegistry.updateEndpoint(server, fresh);
        published = fresh;

        Map<BlockPos, CourierEndpoint> snapshot =
                CourierEndpointRegistry.getNearbyEndpoints(
                        server, worldPosition, LOCAL_CHUNK_RANGE);
        for (var it = known.iterator(); it.hasNext(); ) {
            BlockPos peer = it.next();
            if (!snapshot.containsKey(peer)) {
                it.remove();
                reachable.remove(peer);
                continue;
            }
            if (reachable.contains(peer)) drawLink(server, self, peer);
            setReachable(peer, CourierEndpointRegistry.hasLineOfSight(server, self, peer));
        }

        int budget = DISCOVERY_BUDGET;
        for (CourierEndpoint candidate : snapshot.values()) {
            if (candidate.position().equals(self) || !(candidate.transit() || fresh.transit()))
                continue;
            if (known.add(candidate.position())) {
                budget--;
                if (CourierEndpointRegistry.hasLineOfSight(server, self, candidate.position()))
                    reachable.add(candidate.position());
            }
            if (budget <= 0) break;
        }
    }

    private void setReachable(BlockPos peer, boolean linked) {
        if (linked) reachable.add(peer);
        else reachable.remove(peer);
    }

    private static void drawLink(ServerLevel server, BlockPos from, BlockPos to) {
        Vec3 start = Vec3.atCenterOf(from);
        Vec3 span = Vec3.atLowerCornerOf(to.subtract(from)).scale(0.5);
        ParticleCreators.droneLine(
                server,
                start.x,
                start.y,
                start.z,
                span.x,
                span.y,
                span.z,
                0x00ff00,
                LINK_PARTICLE_RANGE);
    }
}
