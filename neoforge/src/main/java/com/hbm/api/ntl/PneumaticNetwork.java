/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.api.ntl;

import com.hbm.blockentity.machine.TemplateCrafterBlockEntity;
import com.hbm.blockentity.network.pneumatic.PneumaticStorageBankBlockEntity;
import com.hbm.blockentity.network.pneumatic.PneumaticTubeBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.platform.Platform;
import com.hbm.uninos.graph.EdgeFaces;
import com.hbm.uninos.graph.GraphNode;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.hbm.uninos.graph.NodeNetwork;
import com.hbm.util.ForeignItems;
import com.hbm.util.InventoryUtil;
import com.hbm.util.NtmMath;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenCustomHashMap;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

public final class PneumaticNetwork {

    public static final byte SEND_FIRST = 0;
    public static final byte SEND_LAST = 1;
    public static final byte SEND_RANDOM = 2;
    public static final byte RECEIVE_ROBIN = 0;
    public static final byte RECEIVE_RANDOM = 1;
    public static final int ITEMS_PER_TRANSFER = 64;
    public static final Hash.Strategy<ItemStack> TYPE_AND_COMPONENTS =
            new Hash.Strategy<>() {

                @Override
                public int hashCode(ItemStack stack) {
                    return ItemStack.hashItemAndComponents(stack);
                }

                @Override
                public boolean equals(@Nullable ItemStack a, @Nullable ItemStack b) {
                    if (a == b) return true;
                    if (a == null || b == null) return false;
                    return a.isEmpty() == b.isEmpty() && ItemStack.isSameItemSameComponents(a, b);
                }
            };
    public static final LevelNodeGraph.Kind<PneumaticData> GRAPH =
            LevelNodeGraph.kind(PneumaticGraphProvider.INSTANCE, "pneumatic_graph");
    private static final int MAX_ATTEMPTS = 5;
    public final RandomSource rand = RandomSource.create();
    private @Nullable NodeNetwork<PneumaticData> network;

    public static @Nullable PneumaticNetwork at(ServerLevel level, BlockPos pos) {
        NodeNetwork<PneumaticData> net = netAt(level, pos);
        if (net == null) return null;
        if (net.attachment instanceof PneumaticNetwork state) return state;
        PneumaticNetwork state = new PneumaticNetwork();
        state.network = net;
        net.attachment = state;
        return state;
    }

    private static long isDestination(
            ServerLevel level, long nodeKey, LevelChunk cellChunk, int face) {
        if (face != EdgeFaces.SELF) return LevelNodeGraph.ABSENT;
        BlockPos pos = BlockPos.of(nodeKey);
        if (!(cellChunk.getBlockEntity(pos) instanceof PneumaticTubeBlockEntity tube))
            return LevelNodeGraph.ABSENT;
        if (tube.outletFace == null) return LevelNodeGraph.ABSENT;
        BlockPos target = pos.relative(tube.outletFace);
        if (MultiblockCoreBlock.readableChunk(level, target.getX(), target.getZ(), cellChunk)
                == null) {
            return LevelNodeGraph.chunkOf(target.asLong());
        }
        return ForeignItems.present(level, target, tube.outletFace.getOpposite())
                ? LevelNodeGraph.PRESENT
                : LevelNodeGraph.ABSENT;
    }

    public static List<Destination> destinations(
            ServerLevel level,
            LevelNodeGraph<PneumaticData> graph,
            NodeNetwork<PneumaticData> net) {
        List<Destination> out = new ArrayList<>();
        for (var member :
                graph.drainProbes(
                        net,
                        level,
                        (lvl, g, cellPos, cellChunk, faces, face) ->
                                isDestination(lvl, cellPos, cellChunk, face))) {
            EdgeFaces ef = member.ef;
            if (ef == null || (ef.activeMask & (1 << EdgeFaces.SELF)) == 0) continue;
            BlockPos node = BlockPos.of(member.posKey);
            LevelChunk nodeChunk =
                    MultiblockCoreBlock.readableChunk(level, node.getX(), node.getZ());
            if (nodeChunk == null
                    || !(nodeChunk.getBlockEntity(node) instanceof PneumaticTubeBlockEntity tube))
                continue;
            if (tube.outletFace == null) continue;
            BlockPos at = node.relative(tube.outletFace);
            if (presentAt(level, at, tube.outletFace.getOpposite())) {
                out.add(new Destination(at, tube.outletFace, tube));
            }
        }
        return out;
    }

    public static boolean presentAt(ServerLevel level, BlockPos pos, @Nullable Direction face) {
        if (MultiblockCoreBlock.readableChunk(level, pos.getX(), pos.getZ()) == null) return false;
        return ForeignItems.present(level, pos, face);
    }

    private static boolean passesEndpoint(
            PneumaticTubeBlockEntity endpoint, List<ItemStack> offered) {
        for (ItemStack stack : offered) if (passes(endpoint, stack)) return true;
        return false;
    }

    public static LevelNodeGraph<PneumaticData> get(ServerLevel level) {
        return LevelNodeGraph.getOrCreate(level, GRAPH);
    }

    public static void addNode(ServerLevel level, BlockPos pos) {
        get(level).addNode(pos.asLong(), PneumaticData.INSTANCE, 0x3F);
    }

    public static void removeNode(ServerLevel level, BlockPos pos) {
        get(level).removeNode(pos.asLong());
    }

    public static void setEndpoint(ServerLevel level, BlockPos pos, boolean on) {
        get(level).setSelfEndpoint(pos.asLong(), on);
    }

    public static @Nullable NodeNetwork<PneumaticData> netAt(ServerLevel level, BlockPos pos) {
        return get(level).networkAt(pos.asLong());
    }

    public static Comparator<Destination> nearestFirst(BlockPos origin) {
        return Comparator.comparingDouble((Destination d) -> d.pos().distSqr(origin))
                .thenComparingInt(d -> PneumaticTubeBlockEntity.locationKey(d.pos()));
    }

    private static boolean passes(PneumaticTubeBlockEntity tube, ItemStack stack) {
        return tube.filterMatches(stack) == tube.matchOnly;
    }

    public List<PneumaticStorageBankBlockEntity> drives(ServerLevel level, BlockPos viewer) {
        if (network == null) return List.of();
        List<PneumaticStorageBankBlockEntity> out = new ArrayList<>();
        for (GraphNode<PneumaticData> node : network.nodes) {
            BlockPos pos = BlockPos.of(node.posKey);
            LevelChunk chunk = MultiblockCoreBlock.readableChunk(level, pos.getX(), pos.getZ());
            if (chunk != null
                    && chunk.getBlockEntity(pos) instanceof PneumaticStorageBankBlockEntity drive
                    && drive.hasAir()
                    && drive.reaches(viewer)) {
                out.add(drive);
            }
        }
        return out;
    }

    public Object2LongMap<ItemStack> contents(ServerLevel level, BlockPos viewer) {
        Object2LongOpenCustomHashMap<ItemStack> out =
                new Object2LongOpenCustomHashMap<>(TYPE_AND_COMPONENTS);
        for (PneumaticStorageBankBlockEntity drive : drives(level, viewer)) {
            for (PneumaticStorageBankBlockEntity.Stock held : drive.stocks().values()) {
                out.addTo(held.display, held.amount);
            }
        }
        return out;
    }

    public long take(ServerLevel level, BlockPos viewer, ItemStack type, long amount) {
        if (type.isEmpty() || amount <= 0) return 0L;
        long left = amount;
        for (PneumaticStorageBankBlockEntity drive : drives(level, viewer)) {
            left = drive.withdraw(type, left);
            if (left <= 0) break;
        }
        return amount - left;
    }

    public long give(ServerLevel level, BlockPos viewer, ItemStack stack, long amount) {
        if (stack.isEmpty() || amount <= 0 || !Platform.canFitInsideContainerItems(stack))
            return 0L;
        List<PneumaticStorageBankBlockEntity> reachable = drives(level, viewer);
        long left = amount;
        for (PneumaticStorageBankBlockEntity drive : reachable) {
            left = drive.fillExisting(stack, left);
            if (left <= 0) return amount;
        }
        for (PneumaticStorageBankBlockEntity drive : reachable) {
            left = drive.fillVacant(stack, left);
            if (left <= 0) break;
        }
        return amount - left;
    }

    public boolean send(
            ServerLevel level,
            BlockPos sourcePos,
            Direction sourceFace,
            PneumaticTubeBlockEntity tube,
            int sendOrder,
            int receiveOrder,
            int maxRange,
            int nextReceiver) {

        NodeNetwork<PneumaticData> at = netAt(level, tube.getBlockPos());
        if (at == null) return false;
        List<Destination> receivers = destinations(level, get(level), at);
        if (receivers.isEmpty()) return false;

        List<ItemStack> offered = ForeignItems.contents(level, sourcePos, sourceFace);
        offered.removeIf(stack -> !passes(tube, stack));
        if (offered.isEmpty()) return false;

        if (sendOrder == SEND_LAST) Collections.reverse(offered);
        if (sendOrder == SEND_RANDOM) Collections.shuffle(offered, new Random(rand.nextLong()));

        if (receiveOrder == RECEIVE_ROBIN) receivers.sort(nearestFirst(tube.getBlockPos()));
        if (receiveOrder == RECEIVE_RANDOM) Collections.shuffle(receivers);

        int maxAttempts = Math.min(receivers.size(), MAX_ATTEMPTS);
        Container sourceContainer = InventoryUtil.containerAt(level, sourcePos);

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            int index =
                    receiveOrder == RECEIVE_ROBIN
                            ? Math.floorMod(nextReceiver + attempt, receivers.size())
                            : attempt;

            Destination candidate = receivers.get(index);
            if (sourcePos.equals(candidate.pos())
                    || (sourceContainer != null
                            && sourceContainer
                                    == InventoryUtil.containerAt(level, candidate.pos()))) continue;
            if (sourcePos.distSqr(candidate.pos()) > (long) maxRange * maxRange) continue;
            if (candidate.tube() != tube && !passesEndpoint(candidate.tube(), offered)) continue;

            if (deliver(level, sourcePos, sourceFace, tube, candidate, offered)) return true;
        }
        return false;
    }

    private boolean deliver(
            ServerLevel level,
            BlockPos sourcePos,
            Direction sourceFace,
            PneumaticTubeBlockEntity tube,
            Destination candidate,
            List<ItemStack> offered) {
        int budget = ITEMS_PER_TRANSFER;
        int hardCap =
                level.getBlockEntity(candidate.pos()) instanceof TemplateCrafterBlockEntity
                        ? 1
                        : ITEMS_PER_TRANSFER;
        boolean moved = false;

        for (ItemStack stack : offered) {
            if (candidate.tube() != tube && !passes(candidate.tube(), stack)) continue;
            int proportional = Mth.clamp(64 / stack.getMaxStackSize(), 1, 64);
            int wanted = NtmMath.min(stack.getCount(), budget / proportional, hardCap);
            if (wanted <= 0) continue;

            Direction destFace = candidate.pipeDir().getOpposite();
            int sent =
                    ForeignItems.move(
                            level, sourcePos, sourceFace, candidate.pos(), destFace, stack, wanted);
            if (sent <= 0) continue;

            budget -= sent * proportional;
            moved = true;
            if (budget <= 0) break;
        }
        return moved;
    }

    public record Destination(BlockPos pos, Direction pipeDir, PneumaticTubeBlockEntity tube) {}
}
