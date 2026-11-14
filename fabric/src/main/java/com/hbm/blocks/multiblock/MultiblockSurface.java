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

package com.hbm.blocks.multiblock;

import com.hbm.capability.NtmCapabilities.CapRole;
import com.hbm.capability.NtmCapabilities;
import com.hbm.interfaces.injected.ClientCoreHintAccess;
import com.hbm.registration.Registrar;
import com.hbm.registration.RegistryHandle;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.hbm.util.BlockReadBounds;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrays;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.jspecify.annotations.Nullable;

public final class MultiblockSurface {
    public static final long NO_CORE = Long.MIN_VALUE;
    public static final long NO_CORE_TRUNCATED = Long.MIN_VALUE + 1;
    private static volatile @Nullable Long2ObjectOpenHashMap<long[]> clientCandidates;
    private static final ThreadLocal<ForeignHint> foreignHints =
            ThreadLocal.withInitial(ForeignHint::new);

    private MultiblockSurface() {}

    public static void forEachActiveFace(
            MultiblockCoreBlock core,
            BlockPos corePos,
            Direction facing,
            @Nullable CapRole role,
            ActiveFaceVisitor visitor) {
        MultiblockMaskTable table = core.maskTable(facing);
        table.forEachLocalCell(
                (lx, ly, lz) -> {
                    int mask =
                            role == null
                                    ? table.maskAtLocal(lx, ly, lz)
                                    : table.maskAtLocal(lx, ly, lz, role);
                    if (mask <= MultiblockCoreBlock.MASK_NONE) return;
                    BlockPos cell =
                            corePos.offset(
                                    MultiblockMaskTable.worldX(lx, ly, lz, facing),
                                    ly,
                                    MultiblockMaskTable.worldZ(lx, ly, lz, facing));
                    for (Direction side : Direction.VALUES) {
                        if ((mask & maskBit(MultiblockMaskTable.toLocal(side, facing))) != 0)
                            visitor.face(cell, side);
                    }
                });
    }

    public static int maskBit(Direction local) {
        return switch (local) {
            case DOWN -> MultiblockCoreBlock.MASK_DOWN;
            case UP -> MultiblockCoreBlock.MASK_UP;
            case NORTH -> MultiblockCoreBlock.MASK_NORTH;
            case SOUTH -> MultiblockCoreBlock.MASK_SOUTH;
            case WEST -> MultiblockCoreBlock.MASK_WEST;
            case EAST -> MultiblockCoreBlock.MASK_EAST;
        };
    }

    @FunctionalInterface
    public interface ActiveFaceVisitor {

        void face(BlockPos cell, Direction side);
    }

    public static boolean hasCore(long packed) {
        return packed != NO_CORE && packed != NO_CORE_TRUNCATED;
    }

    public static boolean isSurface(BlockState state) {
        return state.getBlock() instanceof MultiblockCoreBlock
                || state.getBlock() instanceof MultiblockCellBlock;
    }

    public static @Nullable MultiblockCoreBlock foldedCore(BlockState state) {
        return state.getBlock() instanceof MultiblockCoreBlock core && !core.isCellState(state)
                ? core
                : null;
    }

    public static boolean isFoldedCell(BlockState state) {
        return state.getBlock() instanceof MultiblockCellBlock
                || (state.getBlock() instanceof MultiblockCoreBlock core
                        && core.isCellState(state));
    }

    public static long indexedCorePacked(ServerLevel level, int x, int y, int z) {
        return lookup(level, x, y, z, HIT.get());
    }

    private static final class CoreHit {
        @Nullable LevelChunk chunk;
        @Nullable BlockState state;
        @Nullable MultiblockCoreBlock block;
        int x;
        int y;
        int z;

        Direction facing() {
            return state.getValue(MultiblockCoreBlock.FACING);
        }
    }

    private static final ThreadLocal<CoreHit> HIT = ThreadLocal.withInitial(CoreHit::new);

    private static long lookup(ServerLevel level, int x, int y, int z, CoreHit hit) {
        LevelChunk chunk = MultiblockCoreBlock.readableChunk(level, x, z);
        if (chunk == null) return NO_CORE;
        long entry = MultiblockCoreIndex.lookup(chunk, level, x, y, z);
        if (entry == MultiblockCoreIndex.NO_ENTRY) return NO_CORE;

        int cx = x + MultiblockCoreIndex.dxOf(entry);
        int cy = y + MultiblockCoreIndex.dyOf(entry);
        int cz = z + MultiblockCoreIndex.dzOf(entry);

        LevelChunk coreChunk;
        if (MultiblockCoreIndex.coreInSameChunk(x, z, entry)) {
            coreChunk = chunk;
        } else {
            coreChunk =
                    level.getChunkSource()
                            .getChunkNow(
                                    SectionPos.blockToSectionCoord(cx),
                                    SectionPos.blockToSectionCoord(cz));
            if (coreChunk == null) return NO_CORE_TRUNCATED;
        }
        BlockState coreState = stateAt(coreChunk, cx, cy, cz);
        MultiblockCoreBlock block = coreState == null ? null : foldedCore(coreState);
        if (block == null) return NO_CORE;
        hit.chunk = coreChunk;
        hit.state = coreState;
        hit.block = block;
        hit.x = cx;
        hit.y = cy;
        hit.z = cz;
        return BlockPos.asLong(cx, cy, cz);
    }

    private static @Nullable BlockEntity ownerAt(ServerLevel level, CoreHit hit) {
        if (!level.getServer().isSameThread()) return null;
        BlockEntity be =
                hit.chunk.getBlockEntity(
                        new BlockPos(hit.x, hit.y, hit.z), LevelChunk.EntityCreationType.IMMEDIATE);
        return be == null || be.isRemoved() ? null : be;
    }

    public static long recordedCorePacked(ServerLevel level, int x, int y, int z) {
        LevelChunk chunk = MultiblockCoreBlock.readableChunk(level, x, z);
        if (chunk == null) return NO_CORE;
        long entry = MultiblockCoreIndex.lookup(chunk, level, x, y, z);
        if (entry == MultiblockCoreIndex.NO_ENTRY) return NO_CORE;
        return MultiblockCoreIndex.corePacked(entry, x, y, z);
    }

    public static @Nullable BlockPos indexedCore(ServerLevel level, BlockPos pos) {
        long packed = indexedCorePacked(level, pos.getX(), pos.getY(), pos.getZ());
        return hasCore(packed) ? BlockPos.of(packed) : null;
    }

    private static @Nullable BlockState stateAt(LevelChunk chunk, int x, int y, int z) {
        int index = chunk.getSectionIndex(y);
        if (index < 0 || index >= chunk.getSections().length) return null;
        LevelChunkSection section = chunk.getSection(index);
        return section.hasOnlyAir() ? null : section.getBlockState(x & 15, y & 15, z & 15);
    }

    public static @Nullable BlockEntity resolveOwner(
            Level level, BlockPos pos, BlockState state, @Nullable Direction side) {
        return resolveOwner(level, pos, state, side, MultiblockCoreBlock.PASSIVE_NONE);
    }

    public static @Nullable BlockEntity resolveOwner(
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable Direction side,
            int passiveDomains) {
        return resolveOwner(level, pos, state, side, passiveDomains, null);
    }

    public static @Nullable BlockEntity resolveOwner(
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable Direction side,
            int passiveDomains,
            @Nullable CapRole role) {
        MultiblockCoreBlock self = foldedCore(state);
        if (self != null) {
            if (side != null
                    && !self.isOpenAt(
                            state.getValue(MultiblockCoreBlock.FACING),
                            0,
                            0,
                            0,
                            side,
                            passiveDomains,
                            role)) {
                return null;
            }
            BlockEntity be = level.getBlockEntity(pos);
            return be == null || be.isRemoved() ? null : be;
        }
        if (isFoldedCell(state)) {
            if (!(level instanceof ServerLevel server)) return null;
            CoreHit hit = HIT.get();
            if (!hasCore(lookup(server, pos.getX(), pos.getY(), pos.getZ(), hit))) return null;
            if (side != null
                    && !hit.block.isOpenAt(
                            hit.facing(),
                            pos.getX() - hit.x,
                            pos.getY() - hit.y,
                            pos.getZ() - hit.z,
                            side,
                            passiveDomains,
                            role)) {
                return null;
            }
            return ownerAt(server, hit);
        }
        return null;
    }

    public static @Nullable BlockEntity resolveOwnerIfOpenForItems(
            Level level, BlockPos pos, BlockState state, Direction side) {
        MultiblockCoreBlock self = foldedCore(state);
        if (self != null) {
            if (!self.isPassiveOnlyOpenAt(
                    state.getValue(MultiblockCoreBlock.FACING),
                    0,
                    0,
                    0,
                    side,
                    NtmCapabilities.ITEM_PLANE)) {
                return null;
            }
            BlockEntity be = level.getBlockEntity(pos);
            return be == null || be.isRemoved() ? null : be;
        }
        if (isFoldedCell(state)) {
            if (!(level instanceof ServerLevel server)) return null;
            CoreHit hit = HIT.get();
            if (!hasCore(lookup(server, pos.getX(), pos.getY(), pos.getZ(), hit))) return null;
            if (!hit.block.isPassiveOnlyOpenAt(
                    hit.facing(),
                    pos.getX() - hit.x,
                    pos.getY() - hit.y,
                    pos.getZ() - hit.z,
                    side,
                    NtmCapabilities.ITEM_PLANE)) {
                return null;
            }
            return ownerAt(server, hit);
        }
        return null;
    }

    public static @Nullable BlockPos proxyCoreOfCell(Level level, BlockPos pos, BlockState state) {
        return proxyCoreOfCell(level, pos, state, MultiblockCoreBlock.PASSIVE_ANY);
    }

    public static @Nullable BlockPos proxyCoreOfCell(
            Level level, BlockPos pos, BlockState state, int passiveDomains) {
        return proxyCoreOfCell(level, pos, state, passiveDomains, false);
    }

    public static @Nullable BlockPos rorCoreOfCell(Level level, BlockPos pos, BlockState state) {
        return proxyCoreOfCell(level, pos, state, MultiblockCoreBlock.PASSIVE_ANY, true);
    }

    private static @Nullable BlockPos proxyCoreOfCell(
            Level level, BlockPos pos, BlockState state, int passiveDomains, boolean proxyCells) {
        if (!isFoldedCell(state)) return null;
        BlockPos core;
        BlockState coreState;
        if (level instanceof ServerLevel server) {
            CoreHit hit = HIT.get();
            long packed = lookup(server, pos.getX(), pos.getY(), pos.getZ(), hit);
            if (!hasCore(packed)) return null;
            core = BlockPos.of(packed);
            coreState = hit.state;
        } else {
            core = clientCoreOf(level, pos);
            if (core == null) return null;
            coreState = level.getBlockState(core);
        }
        MultiblockCoreBlock block = foldedCore(coreState);
        if (block == null) return null;
        Direction facing = coreState.getValue(MultiblockCoreBlock.FACING);
        int dx = pos.getX() - core.getX();
        int dy = pos.getY() - core.getY();
        int dz = pos.getZ() - core.getZ();
        int lx = MultiblockMaskTable.localX(dx, dy, dz, facing);
        int ly = MultiblockMaskTable.localY(dx, dy, dz, facing);
        int lz = MultiblockMaskTable.localZ(dx, dy, dz, facing);
        if (proxyCells
                && (block.maskTable(facing).flagsAtLocal(lx, ly, lz)
                                & MultiblockMaskTable.FLAG_PROXY)
                        != 0) {
            return core;
        }
        int mask = block.passiveMaskAt(lx, ly, lz, facing, passiveDomains);
        if (mask == MultiblockMaskTable.NOT_A_CELL || mask == MultiblockCoreBlock.MASK_NONE)
            return null;
        return core;
    }

    public static boolean isOpen(Level level, BlockPos pos, BlockState state, Direction side) {
        return isOpen(level, pos, state, side, MultiblockCoreBlock.PASSIVE_NONE);
    }

    public static boolean isOpen(
            Level level, BlockPos pos, BlockState state, Direction side, int passiveDomains) {
        return isOpen(level, pos, state, side, passiveDomains, false, null);
    }

    public static boolean isOpen(
            Level level,
            BlockPos pos,
            BlockState state,
            Direction side,
            int passiveDomains,
            @Nullable CapRole role) {
        return isOpen(level, pos, state, side, passiveDomains, false, role);
    }

    public static boolean isOpenForItems(
            Level level, BlockPos pos, BlockState state, Direction side) {
        return isOpen(level, pos, state, side, NtmCapabilities.ITEM_PLANE, true, null);
    }

    private static boolean isOpen(
            Level level,
            BlockPos pos,
            BlockState state,
            Direction side,
            int passiveDomains,
            boolean passiveOnly,
            @Nullable CapRole role) {
        MultiblockCoreBlock self = foldedCore(state);
        if (self != null) {
            return open(
                    self,
                    state.getValue(MultiblockCoreBlock.FACING),
                    0,
                    0,
                    0,
                    side,
                    passiveDomains,
                    passiveOnly,
                    role);
        }
        if (isFoldedCell(state) && level instanceof ServerLevel server) {
            CoreHit hit = HIT.get();
            if (!hasCore(lookup(server, pos.getX(), pos.getY(), pos.getZ(), hit))) return false;
            return open(
                    hit.block,
                    hit.facing(),
                    pos.getX() - hit.x,
                    pos.getY() - hit.y,
                    pos.getZ() - hit.z,
                    side,
                    passiveDomains,
                    passiveOnly,
                    role);
        }
        return false;
    }

    private static boolean open(
            MultiblockCoreBlock block,
            Direction facing,
            int dx,
            int dy,
            int dz,
            Direction side,
            int passiveDomains,
            boolean passiveOnly,
            @Nullable CapRole role) {
        return passiveOnly
                ? block.isPassiveOnlyOpenAt(facing, dx, dy, dz, side, passiveDomains)
                : block.isOpenAt(facing, dx, dy, dz, side, passiveDomains, role);
    }

    public static boolean ownerDeclares(BlockEntity owner, int bits) {
        if (!(owner.getBlockState().getBlock() instanceof MultiblockCoreBlock core)) return true;
        if (!core.usesSharedCells()) return true;
        return core.declares(bits);
    }

    public static @Nullable BlockPos clientCoreOf(BlockGetter level, BlockPos cell) {
        ClientCoreHintAccess hints =
                level instanceof ClientCoreHintAccess own ? own : foreignHints.get().forView(level);
        BlockPos hint = hints.hbm$coreHint();
        if (hint != null && coreClaims(level, hint, cell)) return hint;

        if (!BlockReadBounds.canRead(level, cell)) return null;

        BlockState cellState = level.getBlockState(cell);
        if (cellState.getBlock() instanceof MultiblockCoreBlock own && own.isCellState(cellState)) {
            BlockPos core = own.findClientCore(level, cell);
            if (core != null && coreClaims(level, core, cell)) {
                hints.hbm$coreHint(core);
                return core;
            }
        }

        long packed = clientCandidateCore(level, cell, cellState, new BlockPos.MutableBlockPos());
        if (!hasCore(packed)) return null;
        BlockPos core = BlockPos.of(packed);
        hints.hbm$coreHint(core);
        return core;
    }

    public static long clientCorePacked(
            BlockGetter level, BlockPos cell, BlockPos.MutableBlockPos probe) {
        ClientCoreHintAccess hints =
                level instanceof ClientCoreHintAccess own ? own : foreignHints.get().forView(level);
        BlockPos hint = hints.hbm$coreHint();
        if (hint != null && coreClaims(level, hint, cell)) return hint.asLong();
        if (!BlockReadBounds.canRead(level, cell)) return NO_CORE;
        BlockState state = level.getBlockState(cell);
        if (state.getBlock() instanceof MultiblockCoreBlock own && own.isCellState(state)) {
            BlockPos core = own.findClientCore(level, cell);
            if (core != null && coreClaims(level, core, cell)) return core.asLong();
        }
        return clientCandidateCore(level, cell, state, probe);
    }

    private static long clientCandidateCore(
            BlockGetter level,
            BlockPos cell,
            BlockState cellState,
            BlockPos.MutableBlockPos probe) {
        var byCell = clientCandidates;
        if (byCell == null) byCell = clientCandidates = buildClientCandidates();
        long[] candidates = byCell.get(candidateKey(cellState));
        if (candidates == null) return NO_CORE;
        for (long delta : candidates) {
            probe.set(
                    cell.getX() + BlockPos.getX(delta),
                    cell.getY() + BlockPos.getY(delta),
                    cell.getZ() + BlockPos.getZ(delta));
            if (!BlockReadBounds.canRead(level, probe.getX(), probe.getY(), probe.getZ())) continue;
            BlockState state = level.getBlockState(probe);
            MultiblockCoreBlock block = foldedCore(state);
            if (block == null
                    || !block.claimsCell(
                            level, probe, cell, state.getValue(MultiblockCoreBlock.FACING))) {
                continue;
            }
            return probe.asLong();
        }
        return NO_CORE;
    }

    public static @Nullable BlockState particleState(Level level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof MultiblockCellBlock)) return state;
        BlockPos core = coreOfFoldedCell(level, pos, state);
        return core == null ? null : level.getBlockState(core);
    }

    public static BlockState particleStateOrAir(Level level, BlockPos pos, BlockState state) {
        var owner = particleState(level, pos, state);
        return owner == null ? Blocks.AIR.defaultBlockState() : owner;
    }

    public static @Nullable ParticleOptions particleOptions(
            Level level, BlockPos pos, BlockState state, ParticleOptions fallback) {
        BlockState owner = particleState(level, pos, state);
        if (owner == state) return fallback;
        if (owner == null) return null;
        return new BlockParticleOption(ParticleTypes.BLOCK, owner);
    }

    public static boolean coreClaims(BlockGetter level, BlockPos core, BlockPos cell) {
        if (!BlockReadBounds.canRead(level, core) || !BlockReadBounds.canRead(level, cell))
            return false;
        BlockState state = level.getBlockState(core);
        MultiblockCoreBlock block = foldedCore(state);
        if (block == null) return false;
        Direction facing = state.getValue(MultiblockCoreBlock.FACING);
        return block.claimsCell(level, core, cell, facing);
    }

    public static void invalidateClientCandidates() {
        clientCandidates = null;
    }

    private static long candidateKey(BlockState state) {
        int shape =
                state.getBlock() instanceof MultiblockGeometryCellBlock
                        ? MultiblockGeometryCellBlock.shapeId(state)
                        : 0;
        return ((long) BuiltInRegistries.BLOCK.getId(state.getBlock()) << 32) | shape;
    }

    private static Long2ObjectOpenHashMap<long[]> buildClientCandidates() {
        var deltas = new Long2ObjectOpenHashMap<LongOpenHashSet>();
        for (RegistryHandle<? extends Block> handle : Registrar.registeredBlocks) {
            if (!(handle.get() instanceof MultiblockCoreBlock core)) continue;
            for (Direction facing : Direction.VALUES) {
                if (facing.getAxis() == Direction.Axis.Y) continue;
                var layout = core.footprint(facing);
                for (int i = 0; i < layout.offsets.length; i++) {
                    long offset = layout.offsets[i];
                    long delta =
                            BlockPos.asLong(
                                    -BlockPos.getX(offset),
                                    -BlockPos.getY(offset),
                                    -BlockPos.getZ(offset));
                    deltas.computeIfAbsent(
                                    candidateKey(layout.states[i]), key -> new LongOpenHashSet())
                            .add(delta);
                    if (layout.swapped == null || layout.swapped[i] == null) continue;
                    deltas.computeIfAbsent(
                                    candidateKey(layout.swapped[i]), key -> new LongOpenHashSet())
                            .add(delta);
                }
            }
        }
        var result = new Long2ObjectOpenHashMap<long[]>(deltas.size());
        for (var entry : deltas.long2ObjectEntrySet()) {
            long[] offsets = entry.getValue().toLongArray();
            LongArrays.quickSort(
                    offsets,
                    (a, b) -> {
                        int order = Long.compare(distanceSquared(a), distanceSquared(b));
                        return order != 0 ? order : Long.compare(a, b);
                    });
            result.put(entry.getLongKey(), offsets);
        }
        return result;
    }

    private static long distanceSquared(long offset) {
        long x = BlockPos.getX(offset), y = BlockPos.getY(offset), z = BlockPos.getZ(offset);
        return x * x + y * y + z * z;
    }

    private static final class ForeignHint implements ClientCoreHintAccess {
        private WeakReference<BlockGetter> view = new WeakReference<>(null);
        private @Nullable BlockPos pos;

        private ForeignHint forView(BlockGetter next) {
            if (view.get() != next) {
                view = new WeakReference<>(next);
                pos = null;
            }
            return this;
        }

        @Override
        public @Nullable BlockPos hbm$coreHint() {
            return pos;
        }

        @Override
        public void hbm$coreHint(@Nullable BlockPos next) {
            pos = next;
        }
    }

    public static @Nullable BlockPos coreOfFoldedCell(Level level, BlockPos cell) {
        return coreOfFoldedCell(level, cell, level.getBlockState(cell));
    }

    public static @Nullable BlockPos coreOfFoldedCell(
            Level level, BlockPos cell, BlockState state) {
        if (!isFoldedCell(state)) return null;
        if (level instanceof ServerLevel server) {
            long packed = indexedCorePacked(server, cell.getX(), cell.getY(), cell.getZ());
            return hasCore(packed) ? BlockPos.of(packed) : null;
        }
        return clientCoreOf(level, cell);
    }

    public static @Nullable BlockPos coreOfAny(Level level, BlockPos pos) {
        return coreOfAny(level, pos, level.getBlockState(pos));
    }

    public static @Nullable BlockPos coreOfAny(Level level, BlockPos pos, BlockState state) {
        return foldedCore(state) != null ? pos : coreOfFoldedCell(level, pos, state);
    }

    public static @Nullable BlockPos coreOfAny(BlockGetter view, BlockPos pos, BlockState state) {
        if (view instanceof Level level) return coreOfAny(level, pos, state);
        if (foldedCore(state) != null) return pos;
        return isFoldedCell(state) ? clientCoreOf(view, pos) : null;
    }

    public static boolean sustainsPlant(BlockGetter level, BlockPos soil, BlockState soilState) {
        BlockPos core = coreOfAny(level, soil, soilState);
        return core != null && (cellFlags(level, core, soil) & MultiblockMaskTable.FLAG_SOIL) != 0;
    }

    public static @Nullable BlockPos heatCoreOfCell(Level level, BlockPos cell, BlockState state) {
        BlockPos core = coreOfFoldedCell(level, cell, state);
        return core != null && (cellFlags(level, core, cell) & MultiblockMaskTable.FLAG_HEAT) != 0
                ? core
                : null;
    }

    private static int cellFlags(BlockGetter level, BlockPos core, BlockPos pos) {
        BlockState coreState = level.getBlockState(core);
        MultiblockCoreBlock block = foldedCore(coreState);
        if (block == null) return 0;
        Direction facing = coreState.getValue(MultiblockCoreBlock.FACING);
        int dx = pos.getX() - core.getX(),
                dy = pos.getY() - core.getY(),
                dz = pos.getZ() - core.getZ();
        return block.maskTable(facing)
                .flagsAtLocal(
                        MultiblockMaskTable.localX(dx, dy, dz, facing),
                        MultiblockMaskTable.localY(dx, dy, dz, facing),
                        MultiblockMaskTable.localZ(dx, dy, dz, facing));
    }

    public static List<BlockPos> rigidStructureBlocks(Level level, BlockPos core) {
        BlockState state = level.getBlockState(core);
        MultiblockCoreBlock block = foldedCore(state);
        if (block == null) return List.of();
        List<BlockPos> out = new ArrayList<>();
        out.add(core.immutable());
        long corePacked = core.asLong();
        ServerLevel server = level instanceof ServerLevel s ? s : null;
        block.visitPlacedCells(
                level,
                core,
                state.getValue(MultiblockCoreBlock.FACING),
                pos -> {
                    if (server != null
                            && recordedCorePacked(server, pos.getX(), pos.getY(), pos.getZ())
                                    != corePacked) {
                        return;
                    }
                    out.add(pos.immutable());
                });
        return out;
    }

    public static long coreBlockingChunk(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!isFoldedCell(state)) return LevelNodeGraph.NO_BLOCKER;
        LevelChunk chunk = MultiblockCoreBlock.readableChunk(level, pos.getX(), pos.getZ());
        if (chunk == null) return LevelNodeGraph.NO_BLOCKER;
        long entry = MultiblockCoreIndex.lookup(chunk, level, pos.getX(), pos.getY(), pos.getZ());
        if (entry == MultiblockCoreIndex.NO_ENTRY) return LevelNodeGraph.NO_BLOCKER;
        if (MultiblockCoreIndex.coreInSameChunk(pos.getX(), pos.getZ(), entry))
            return LevelNodeGraph.NO_BLOCKER;
        int cx = SectionPos.blockToSectionCoord(pos.getX() + MultiblockCoreIndex.dxOf(entry));
        int cz = SectionPos.blockToSectionCoord(pos.getZ() + MultiblockCoreIndex.dzOf(entry));
        return level.getChunkSource().getChunkNow(cx, cz) == null
                ? ChunkPos.pack(cx, cz)
                : LevelNodeGraph.NO_BLOCKER;
    }
}
