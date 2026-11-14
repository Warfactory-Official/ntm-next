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

import com.hbm.api.block.DroneInventoryProvider;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.entity.item.RequestDroneEntity.Step;
import com.hbm.entity.item.RequestDroneEntity;
import com.hbm.inventory.container.DroneDockMenu;
import com.hbm.items.ModItems;
import com.hbm.items.tool.DroneItem.EnumDroneType;
import com.hbm.logistics.courier.CourierEndpoint;
import com.hbm.logistics.courier.CourierEndpointRegistry;
import com.hbm.logistics.courier.CourierItemRequest;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import com.hbm.util.TickPhase;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public final class DroneDockBlockEntity extends RequestNetworkBlockEntity
        implements DroneInventoryProvider.Dock, MenuProvider {

    public static final int SLOT_COUNT = 9;
    private static final int DISPATCH_CHUNK_RANGE = 5;
    private static final int DISPATCH_ATTEMPTS = 5;

    public DroneDockBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DRONE_DOCK.get(), pos, state, SLOT_COUNT);
    }

    @Override
    public void tickServer() {
        super.tickServer();
        if (!TickPhase.every(this, CourierEndpointRegistry.SCAN_INTERVAL) || published == null)
            return;

        int parked = parkedSlot();
        if (parked < 0) return;

        var server = (ServerLevel) level;
        var snapshot =
                CourierEndpointRegistry.getNearbyEndpoints(
                        server, worldPosition, DISPATCH_CHUNK_RANGE);
        var requests = new ArrayList<CourierEndpoint.Requests>();
        var offers = new ArrayList<CourierEndpoint.Stock>();
        for (var node : snapshot.values()) {
            if (node instanceof CourierEndpoint.Requests request) requests.add(request);
            else if (node instanceof CourierEndpoint.Stock offer) offers.add(offer);
        }

        RequestRoutes routes = null;
        for (int attempt = 0; attempt < DISPATCH_ATTEMPTS; attempt++) {
            Collections.shuffle(requests);
            Collections.shuffle(offers);
            var requester = selectRequest(requests);
            if (requester == null) continue;

            var demand =
                    requester.demands().get(server.getRandom().nextInt(requester.demands().size()));
            var provider = selectOffer(offers, demand);
            if (provider == null) continue;
            if (routes == null) routes = new RequestRoutes(snapshot);

            var outbound = routes.between(published, provider);
            if (outbound == null) continue;
            var delivery = routes.between(provider, requester);
            if (delivery == null) continue;
            var returning = routes.between(requester, published);
            if (returning == null) continue;

            var program = new ArrayList<Step>();
            appendFlight(program, outbound, provider.position());
            program.add(new Step.Load(demand));
            appendFlight(program, delivery, requester.position());
            program.add(new Step.Unload());
            appendFlight(program, returning, published.position());
            program.add(new Step.Dock());

            removeItem(parked, 1);
            var courier = new RequestDroneEntity(server);
            courier.setPos(
                    worldPosition.getX() + 0.5,
                    worldPosition.getY() + 1,
                    worldPosition.getZ() + 0.5);
            courier.program.addAll(program);
            server.addFreshEntity(courier);
            WorldSounds.machine(
                    server,
                    null,
                    worldPosition,
                    ModSounds.STORAGE_OPEN.get(),
                    SoundSource.BLOCKS,
                    2F,
                    1F);
            return;
        }
    }

    private int parkedSlot() {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            if (inventory.get(slot).is(ModItems.DRONE.get(EnumDroneType.REQUEST))) return slot;
        }
        return -1;
    }

    private static CourierEndpoint.@Nullable Requests selectRequest(
            List<CourierEndpoint.Requests> nodes) {
        for (var node : nodes) {
            if (node.enabled() && !node.demands().isEmpty()) return node;
        }
        return null;
    }

    private static CourierEndpoint.@Nullable Stock selectOffer(
            List<CourierEndpoint.Stock> nodes, CourierItemRequest demand) {
        for (var node : nodes) {
            if (!node.enabled()) continue;
            for (var stock : node.items()) {
                if (!stock.isEmpty() && demand.accepts(stock)) return node;
            }
        }
        return null;
    }

    private static void appendFlight(
            List<Step> program, List<CourierEndpoint> intermediates, BlockPos destination) {
        for (var node : intermediates) program.add(new Step.Goto(node.position()));
        program.add(new Step.Goto(destination));
    }

    @Override
    protected CourierEndpoint createNode(BlockPos pos) {
        return new CourierEndpoint.Transit(pos, reachable);
    }

    @Override
    public Container droneDockInventory() {
        return this;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.droneDock");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new DroneDockMenu(containerId, inventory, this);
    }
}
