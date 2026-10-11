// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.uninos.graph;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.capability.NtmCapabilities.CapRole;
import com.hbm.capability.NtmCapabilities;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jspecify.annotations.Nullable;

public abstract class EndpointDomain<D, H, E, C, F extends GatherFace<H, ?, E>>
        implements LevelNodeGraph.FaceProbe<D> {

    private static final Direction[] DIRS = Direction.VALUES;

    private final Class<H> type;
    private final CapRole in;
    private final CapRole out;

    protected EndpointDomain(Class<H> type, CapRole in, CapRole out) {
        this.type = type;
        this.in = in;
        this.out = out;
    }

    protected abstract BlockCapability<H, C> providerToken();

    protected abstract BlockCapability<H, C> receiverToken();

    protected abstract C context(LevelNodeGraph<D> graph, long cellPos, @Nullable Direction side);

    protected abstract F newFace(
            ServerLevel level, long posKey, @Nullable Direction side, C context);

    protected boolean fits(F face, C context) {
        return true;
    }

    protected abstract boolean foreign(
            ServerLevel level, BlockPos pos, Direction side, BlockState state);

    protected boolean declaredForeign(
            ServerLevel level, EndpointRegistry.Endpoint declared, BlockPos pos, Direction side) {
        return false;
    }

    public interface Gatherer<H, E, F> {
        void provider(H provider);

        void receiver(H receiver);

        void foreign(F face, E endpoint);
    }

    @Override
    public final long present(
            ServerLevel level,
            LevelNodeGraph<D> graph,
            long cellPos,
            LevelChunk cellChunk,
            @Nullable EdgeFaces ef,
            int faceIndex) {
        boolean self = faceIndex == EdgeFaces.SELF;
        long posKey = self ? cellPos : LevelNodeGraph.offset(cellPos, DIRS[faceIndex]);
        Direction side = self ? null : DIRS[faceIndex].getOpposite();
        C context = context(graph, cellPos, side);
        @SuppressWarnings("unchecked")
        F fc = ef == null ? null : (F) ef.attachments[faceIndex];
        if (fc != null && !fits(fc, context)) {
            ef.attachments[faceIndex] = null;
            fc = null;
        }
        if (fc != null) fc.disarm();
        LevelChunk faceChunk =
                self
                        ? cellChunk
                        : BlockMultiblockCore.readableChunk(
                                level, BlockPos.getX(posKey), BlockPos.getZ(posKey), cellChunk);
        if (faceChunk == null) return LevelNodeGraph.chunkOf(posKey);
        if (!self && graph.containsCell(posKey)) return LevelNodeGraph.ABSENT;

        BlockPos npos = BlockPos.of(posKey);
        EndpointRegistry endpoints = level.hbm$endpoints();
        BlockEntity owner = endpoints.resolve(level, posKey, side, in.bit | out.bit);
        int declaredBits = owner == null ? 0 : endpoints.declared(posKey).domains();
        boolean p = (declaredBits & out.bit) != 0;
        boolean r = (declaredBits & in.bit) != 0;
        if (!p && !r) {
            owner = null;
            EndpointRegistry.Endpoint declared = endpoints.declared(posKey);
            if (declared != null) {
                if (self || !declaredForeign(level, declared, npos, side))
                    return absent(ef, faceIndex);
            } else {
                if (!faceChunk.getFullStatus().isOrAfter(FullChunkStatus.FULL))
                    return LevelNodeGraph.chunkOf(posKey);
                BlockState nstate = faceChunk.getBlockState(npos);
                if (MultiblockSurface.isSurface(nstate)) {
                    p = NtmCapabilities.coreCap(level, npos, nstate, context, type, out) != null;
                    r = NtmCapabilities.coreCap(level, npos, nstate, context, type, in) != null;
                    if (p || r) owner = MultiblockSurface.resolveOwner(level, npos, nstate, side);
                } else {
                    p =
                            providerToken().getCapability((Level) level, npos, null, null, context)
                                    != null;
                    r =
                            receiverToken().getCapability((Level) level, npos, null, null, context)
                                    != null;
                    assert self || !(p || r)
                            : "Undeclared endpoint at "
                                    + npos
                                    + " state="
                                    + nstate
                                    + " side="
                                    + side
                                    + " provider="
                                    + p
                                    + " receiver="
                                    + r;
                }
                if (!p && !r && (self || !foreign(level, npos, side, nstate))) {
                    long blocker =
                            self
                                    ? LevelNodeGraph.NO_BLOCKER
                                    : MultiblockSurface.coreBlockingChunk(level, npos);
                    if (blocker != LevelNodeGraph.NO_BLOCKER) return blocker;
                    return absent(ef, faceIndex);
                }
            }
        }
        int bit = 1 << faceIndex;
        if (ef == null) {
            if (owner == null) return LevelNodeGraph.PRESENT;
            p &= providerToken().getCapability((Level) level, npos, null, null, context) != null;
            r &= receiverToken().getCapability((Level) level, npos, null, null, context) != null;
            return p || r ? LevelNodeGraph.PRESENT : LevelNodeGraph.ABSENT;
        }
        if (fc == null) ef.attachments[faceIndex] = fc = newFace(level, posKey, side, context);
        fc.memoise(level, owner);
        if (owner != null) {
            p &= fc.provider.find() != null;
            r &= fc.receiver.find() != null;
            if (!p && !r) return absent(ef, faceIndex);
        }
        if (p) ef.roleA |= bit;
        else ef.roleA &= ~bit;
        if (r) ef.roleB |= bit;
        else ef.roleB &= ~bit;
        return LevelNodeGraph.PRESENT;
    }

    private static long absent(@Nullable EdgeFaces ef, int faceIndex) {
        if (ef != null) ef.attachments[faceIndex] = null;
        return LevelNodeGraph.ABSENT;
    }

    public final void gather(
            ServerLevel level,
            LevelNodeGraph<D> graph,
            NodeNetwork<D> net,
            Iterable<GraphNode<D>> faces,
            Gatherer<H, E, F> gatherer) {
        for (GraphNode<D> fe : faces) {
            EdgeFaces ef = fe.ef;
            if (ef == null) continue;
            for (int d = 0; d <= EdgeFaces.SELF; d++) {
                int bit = 1 << d;
                if ((ef.activeMask & bit) == 0) continue;
                F fc = face(level, graph, ef, fe.posKey, d);
                boolean roles = (ef.probedMask & bit) != 0;
                boolean memo = fc.memoLive();
                H p =
                        !roles || (ef.roleA & bit) != 0
                                ? (memo ? fc.memoProvider : fc.provider.find())
                                : null;
                if (p != null) gatherer.provider(p);
                H r =
                        !roles || (ef.roleB & bit) != 0
                                ? (memo ? fc.memoReceiver : fc.receiver.find())
                                : null;
                if (r != null) gatherer.receiver(r);
                if (p != null || r != null) continue;
                E foreign = fc.side == null ? null : fc.foreign(level);
                if (foreign == null) graph.markFaceStale(net, fe, ef, d);
                else gatherer.foreign(fc, foreign);
            }
        }
    }

    public final NetCensus census(LevelNodeGraph<D> graph, NodeNetwork<D> net) {
        ReferenceOpenHashSet<H> provs = new ReferenceOpenHashSet<>();
        ReferenceOpenHashSet<H> recs = new ReferenceOpenHashSet<>();
        for (GraphNode<D> fe : graph.activeFaces(net)) {
            EdgeFaces ef = fe.ef;
            if (ef == null) continue;
            for (int d = 0; d <= EdgeFaces.SELF; d++) {
                int bit = 1 << d;
                if ((ef.activeMask & bit) == 0) continue;
                @SuppressWarnings("unchecked")
                F fc = (F) ef.attachments[d];
                if (fc == null) continue;
                boolean roles = (ef.probedMask & bit) != 0;
                if (!roles || (ef.roleA & bit) != 0) {
                    H prov = fc.provider.find();
                    if (prov != null) provs.add(prov);
                }
                if (!roles || (ef.roleB & bit) != 0) {
                    H rec = fc.receiver.find();
                    if (rec != null) recs.add(rec);
                }
            }
        }
        return new NetCensus(net.nodeCount(), net.size(), provs.size(), recs.size(), net.tracker);
    }

    private F face(ServerLevel level, LevelNodeGraph<D> graph, EdgeFaces ef, long nodeKey, int d) {
        @SuppressWarnings("unchecked")
        F fc = (F) ef.attachments[d];
        if (fc != null) return fc;
        boolean self = d == EdgeFaces.SELF;
        long posKey = self ? nodeKey : LevelNodeGraph.offset(nodeKey, DIRS[d]);
        Direction side = self ? null : DIRS[d].getOpposite();
        fc = newFace(level, posKey, side, context(graph, nodeKey, side));
        ef.attachments[d] = fc;
        return fc;
    }
}
