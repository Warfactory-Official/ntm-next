// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.network;

import com.hbm.api.block.IDroneContainer;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.entity.item.EntityRequestDrone.Step;
import com.hbm.entity.item.EntityRequestDrone;
import com.hbm.inventory.container.MenuDroneDock;
import com.hbm.items.ModItems;
import com.hbm.items.tool.ItemDrone.EnumDroneType;
import com.hbm.items.tool.ItemDrone;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.network.RequestNetwork.Demand;
import com.hbm.tileentity.network.RequestNetwork.OfferNode;
import com.hbm.tileentity.network.RequestNetwork.PathNode;
import com.hbm.tileentity.network.RequestNetwork.RequestNode;
import com.hbm.util.TickPhase;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class BlockEntityDroneDock extends BlockEntityRequestNetwork
        implements MenuProvider, IDroneContainer.Dock {

    public static final int SLOT_COUNT = 9;

    private static final int PATHING_DEPTH = 10;
    private static final int ATTEMPTS = 5;
    private static final int LOCAL_CHUNK_RANGE = 5;

    private static final int ITERATION_BRAKE = 1000;
    private static final int SCAN_PERIOD = 20;

    public BlockEntityDroneDock(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DRONE_DOCK.get(), pos, state, SLOT_COUNT);
    }

    private static boolean isRequestDrone(ItemStack stack) {
        return ModItems.DRONE.is(stack, EnumDroneType.REQUEST);
    }

    @Override
    public void tickServer() {
        super.tickServer();

        if (!(level instanceof ServerLevel server) || !TickPhase.every(this, SCAN_PERIOD)) return;
        if (published == null || !hasDrone()) return;

        Map<BlockPos, PathNode> local =
                RequestNetwork.localNodes(server, worldPosition, LOCAL_CHUNK_RANGE);
        List<RequestNode> requests = new ArrayList<>();
        List<OfferNode> offers = new ArrayList<>();

        for (PathNode node : local.values()) {
            if (node instanceof RequestNode request) requests.add(request);
            if (node instanceof OfferNode offer) offers.add(offer);
        }

        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            Collections.shuffle(requests);
            Collections.shuffle(offers);

            RequestNode target = null;
            for (RequestNode request : requests) {
                if (request.active && !request.request.isEmpty()) {
                    target = request;
                    break;
                }
            }
            if (target == null) continue;

            Demand demand = target.request.get(server.getRandom().nextInt(target.request.size()));

            offers:
            for (OfferNode offer : offers) {
                for (ItemStack stack : offer.offer) {
                    if (!offer.active || stack.isEmpty() || !demand.test(stack)) continue;
                    if (tryEmbark(server, target, offer, demand, local)) return;
                    break offers;
                }
            }
        }
    }

    private boolean tryEmbark(
            ServerLevel server,
            RequestNode request,
            OfferNode offer,
            Demand demand,
            Map<BlockPos, PathNode> local) {
        PathNode dock = published;
        List<PathNode> toOffer = generatePath(dock, offer, local);
        if (toOffer == null) return false;
        List<PathNode> toRequest = generatePath(offer, request, local);
        if (toRequest == null) return false;
        List<PathNode> home = generatePath(request, dock, local);
        if (home == null) return false;

        for (int i = 0; i < inventory.size(); i++) {
            if (isRequestDrone(inventory.get(i))) {
                removeItem(i, 1);
                break;
            }
        }

        EntityRequestDrone drone = new EntityRequestDrone(server);
        drone.setPos(
                worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5);

        for (PathNode node : toOffer) drone.program.add(new Step.Goto(node.pos));
        drone.program.add(new Step.Goto(offer.pos));
        drone.program.add(new Step.Load(demand));
        for (PathNode node : toRequest) drone.program.add(new Step.Goto(node.pos));
        drone.program.add(new Step.Goto(request.pos));
        drone.program.add(new Step.Unload());
        for (PathNode node : home) drone.program.add(new Step.Goto(node.pos));
        drone.program.add(new Step.Goto(dock.pos));
        drone.program.add(new Step.Dock());

        server.addFreshEntity(drone);
        server.playSound(
                null, worldPosition, ModSounds.CRATE_OPEN.get(), SoundSource.BLOCKS, 2.0F, 1.0F);
        return true;
    }

    private @Nullable List<PathNode> generatePath(
            PathNode start, PathNode end, Map<BlockPos, PathNode> local) {
        List<List<PathNode>> paths = new ArrayList<>();
        paths.add(new ArrayList<>(List.of(start)));

        for (int depth = 0; depth < PATHING_DEPTH; depth++) {
            int brake = ITERATION_BRAKE;
            List<List<PathNode>> extended = new ArrayList<>();

            for (List<PathNode> path : paths) {
                for (BlockPos next : path.getLast().reachable) {
                    PathNode node = local.get(next);

                    if (node != null) {
                        List<PathNode> longer = new ArrayList<>(path);

                        if (node.pos.equals(end.pos)) {
                            longer.removeFirst();
                            return longer;
                        }

                        longer.add(node);
                        extended.add(longer);
                    }

                    if (--brake <= 0) break;
                }
                if (brake <= 0) break;
            }

            paths = extended;
        }

        return null;
    }

    public boolean hasDrone() {
        for (ItemStack stack : inventory) if (isRequestDrone(stack)) return true;
        return false;
    }

    @Override
    protected PathNode createNode(BlockPos pos) {
        return new PathNode(pos, reachable);
    }

    @Override
    public Container droneDockInventory() {
        return this;
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.droneDock");
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuDroneDock(containerId, playerInventory, this);
    }
}
