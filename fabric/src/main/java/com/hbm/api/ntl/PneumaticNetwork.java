// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.ntl;

import com.hbm.platform.Services;
import com.hbm.tileentity.machine.BlockEntityMachineAutocrafter;
import com.hbm.tileentity.network.pneumatic.BlockEntityPneumaticStorageBase;
import com.hbm.tileentity.network.pneumatic.BlockEntityPneumoTube;
import com.hbm.uninos.graph.EdgeFaces;
import com.hbm.uninos.graph.GraphNode;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.hbm.uninos.graph.NodeNetwork;
import com.hbm.util.BobMathUtil;
import com.hbm.util.ForeignItems;
import com.hbm.util.InventoryUtil;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenCustomHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

public final class PneumaticNetwork {

    public static final byte SEND_FIRST = 0;
    public static final byte SEND_LAST = 1;
    public static final byte SEND_RANDOM = 2;
    public static final byte RECEIVE_ROBIN = 0;
    public static final byte RECEIVE_RANDOM = 1;
    public static final int ITEMS_PER_TRANSFER = 64;

    private static final int MAX_ATTEMPTS = 5;
    public final RandomSource rand = RandomSource.create();

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

    private @Nullable NodeNetwork<PneumaticData> net;

    public static void init() {
        Services.SERVER.onServerTickPost(PneumaticNetwork::onServerTick);
    }

    public static @Nullable PneumaticNetwork at(ServerLevel level, BlockPos pos) {
        NodeNetwork<PneumaticData> net = netAt(level, pos);
        if (net == null) return null;
        if (net.attachment instanceof PneumaticNetwork state) return state;
        PneumaticNetwork state = new PneumaticNetwork();
        state.net = net;
        net.attachment = state;
        return state;
    }

    private static void onServerTick(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            LevelNodeGraph<PneumaticData> graph = get(level);
            graph.applyCompiledSwaps();
        }
    }

    private static LevelNodeGraph.Probe isDestination(ServerLevel level, long nodeKey, int face) {
        if (face != EdgeFaces.SELF) return LevelNodeGraph.Probe.ABSENT;
        BlockPos pos = BlockPos.of(nodeKey);
        if (!(level.getBlockEntity(pos) instanceof BlockEntityPneumoTube tube))
            return LevelNodeGraph.Probe.ABSENT;
        if (tube.ejectionDir == null) return LevelNodeGraph.Probe.ABSENT;
        BlockPos target = pos.relative(tube.ejectionDir);
        if (!LevelNodeGraph.resident(level, target.asLong())) return LevelNodeGraph.Probe.UNKNOWN;
        return presentAt(level, target, tube.ejectionDir.getOpposite())
                ? LevelNodeGraph.Probe.PRESENT
                : LevelNodeGraph.Probe.ABSENT;
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
                        (lvl, g, cellPos, faces, face) -> isDestination(lvl, cellPos, face))) {
            EdgeFaces ef = member.ef;
            if (ef == null || (ef.activeMask & (1 << EdgeFaces.SELF)) == 0) continue;
            BlockPos node = BlockPos.of(member.posKey);
            if (!(level.getBlockEntity(node) instanceof BlockEntityPneumoTube tube)) continue;
            if (tube.ejectionDir == null) continue;
            BlockPos at = node.relative(tube.ejectionDir);
            if (presentAt(level, at, tube.ejectionDir.getOpposite())) {
                out.add(new Destination(at, tube.ejectionDir, tube));
            }
        }
        return out;
    }

    public static boolean presentAt(ServerLevel level, BlockPos pos, @Nullable Direction face) {
        if (level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) == null)
            return false;
        return ForeignItems.present(level, pos, face);
    }

    public List<BlockEntityPneumaticStorageBase> drives(ServerLevel level, BlockPos viewer) {
        if (net == null) return List.of();
        List<BlockEntityPneumaticStorageBase> out = new ArrayList<>();
        for (GraphNode<PneumaticData> node : net.nodes) {
            BlockPos pos = BlockPos.of(node.posKey);
            if (level.getBlockEntity(pos) instanceof BlockEntityPneumaticStorageBase drive
                    && drive.isAvailable()
                    && drive.reaches(viewer)) {
                out.add(drive);
            }
        }
        return out;
    }

    public Object2LongMap<ItemStack> contents(ServerLevel level, BlockPos viewer) {
        Object2LongOpenCustomHashMap<ItemStack> out =
                new Object2LongOpenCustomHashMap<>(TYPE_AND_COMPONENTS);
        for (BlockEntityPneumaticStorageBase drive : drives(level, viewer)) {
            for (BlockEntityPneumaticStorageBase.Holding held : drive.index().values()) {
                out.addTo(held.display, held.total);
            }
        }
        return out;
    }

    public long take(ServerLevel level, BlockPos viewer, ItemStack type, long amount) {
        if (type.isEmpty() || amount <= 0) return 0L;
        long left = amount;
        for (BlockEntityPneumaticStorageBase drive : drives(level, viewer)) {
            left = drive.take(type, left);
            if (left <= 0) break;
        }
        return amount - left;
    }

    public long give(ServerLevel level, BlockPos viewer, ItemStack stack, long amount) {
        if (stack.isEmpty() || amount <= 0 || !Services.PLATFORM.canFitInsideContainerItems(stack))
            return 0L;
        List<BlockEntityPneumaticStorageBase> reachable = drives(level, viewer);
        long left = amount;
        for (BlockEntityPneumaticStorageBase drive : reachable) {
            left = drive.giveExisting(stack, left);
            if (left <= 0) return amount;
        }
        for (BlockEntityPneumaticStorageBase drive : reachable) {
            left = drive.giveFresh(stack, left);
            if (left <= 0) break;
        }
        return amount - left;
    }

    public boolean send(
            ServerLevel level,
            BlockPos sourcePos,
            Direction sourceFace,
            BlockEntityPneumoTube tube,
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

    private static boolean passesEndpoint(BlockEntityPneumoTube endpoint, List<ItemStack> offered) {
        for (ItemStack stack : offered) if (passes(endpoint, stack)) return true;
        return false;
    }

    private boolean deliver(
            ServerLevel level,
            BlockPos sourcePos,
            Direction sourceFace,
            BlockEntityPneumoTube tube,
            Destination candidate,
            List<ItemStack> offered) {
        int budget = ITEMS_PER_TRANSFER;
        int hardCap =
                level.getBlockEntity(candidate.pos()) instanceof BlockEntityMachineAutocrafter
                        ? 1
                        : ITEMS_PER_TRANSFER;
        boolean moved = false;

        for (ItemStack stack : offered) {
            if (candidate.tube() != tube && !passes(candidate.tube(), stack)) continue;
            int proportional = Mth.clamp(64 / stack.getMaxStackSize(), 1, 64);
            int wanted = BobMathUtil.min(stack.getCount(), budget / proportional, hardCap);
            if (wanted <= 0) continue;

            ItemStack taken =
                    ForeignItems.extract(
                            level,
                            sourcePos,
                            sourceFace,
                            held -> ItemStack.isSameItemSameComponents(held, stack),
                            wanted);
            if (taken.isEmpty()) continue;

            Direction destFace = candidate.pipeDir().getOpposite();
            ItemStack refused = ForeignItems.insert(level, candidate.pos(), destFace, taken);
            if (!refused.isEmpty()) ForeignItems.insert(level, sourcePos, sourceFace, refused);
            int sent = taken.getCount() - refused.getCount();
            if (sent <= 0) continue;

            budget -= sent * proportional;
            moved = true;
            if (budget <= 0) break;
        }
        return moved;
    }

    public static final SavedDataType<LevelNodeGraph<PneumaticData>> GRAPH =
            LevelNodeGraph.type(PneumaticGraphProvider.INSTANCE, "pneumatic_graph");

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
                .thenComparingInt(d -> BlockEntityPneumoTube.getIdentifier(d.pos()));
    }

    private static boolean passes(BlockEntityPneumoTube tube, ItemStack stack) {
        return tube.matchesFilter(stack) == tube.whitelist;
    }

    public record Destination(BlockPos pos, Direction pipeDir, BlockEntityPneumoTube tube) {}
}
