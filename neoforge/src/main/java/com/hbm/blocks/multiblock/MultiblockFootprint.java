// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.multiblock;

import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import java.util.Arrays;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public final class MultiblockFootprint {
    private MultiblockFootprint() {}

    static Layout bake(BlockMultiblockCore block, Direction facing) {
        LongLinkedOpenHashSet unique = new LongLinkedOpenHashSet();
        block.visitCells(
                BlockPos.ZERO,
                facing,
                (pos, mask) -> {
                    if (!pos.equals(BlockPos.ZERO)) unique.add(pos.asLong());
                });
        long[] offsets = unique.toLongArray();
        BlockState[] states = new BlockState[offsets.length];
        for (int i = 0; i < offsets.length; i++) {
            long offset = offsets[i];
            int x = BlockPos.getX(offset), y = BlockPos.getY(offset), z = BlockPos.getZ(offset);
            int lx = MultiblockMaskTable.localX(x, y, z, facing),
                    lz = MultiblockMaskTable.localZ(x, y, z, facing);
            states[i] =
                    block.cellStateFor(
                            lx, y, lz, facing, MultiblockCellShapes.idAt(block, lx, y, lz, facing));
        }
        return new Layout(offsets, states);
    }

    public static final class Layout {
        public final long[] offsets;
        public final BlockState[] states;
        private final long[] sorted;
        private final int minX, maxX, minZ, maxZ;

        private Layout(long[] offsets, BlockState[] states) {
            this.offsets = offsets;
            this.states = states;
            sorted = offsets.clone();
            Arrays.sort(sorted);
            int minX = 0, maxX = 0, minZ = 0, maxZ = 0;
            for (long offset : offsets) {
                minX = Math.min(minX, BlockPos.getX(offset));
                maxX = Math.max(maxX, BlockPos.getX(offset));
                minZ = Math.min(minZ, BlockPos.getZ(offset));
                maxZ = Math.max(maxZ, BlockPos.getZ(offset));
            }
            this.minX = minX;
            this.maxX = maxX;
            this.minZ = minZ;
            this.maxZ = maxZ;
        }

        public boolean contains(int x, int y, int z) {
            return (x == 0 && y == 0 && z == 0)
                    || Arrays.binarySearch(sorted, BlockPos.asLong(x, y, z)) >= 0;
        }

        public boolean intersectsChunk(BlockPos core, int x, int z) {
            return x >= ((core.getX() + minX) >> 4)
                    && x <= ((core.getX() + maxX) >> 4)
                    && z >= ((core.getZ() + minZ) >> 4)
                    && z <= ((core.getZ() + maxZ) >> 4);
        }

        public void visit(BlockPos core, BlockMultiblockCore.CellVisitor visitor) {
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (long offset : offsets) {
                visitor.cell(
                        pos.setWithOffset(
                                core,
                                BlockPos.getX(offset),
                                BlockPos.getY(offset),
                                BlockPos.getZ(offset)),
                        BlockMultiblockCore.MASK_NONE);
            }
        }
    }

    public static boolean hasDetailedShape(BlockState coreState) {
        BlockMultiblockCore block = (BlockMultiblockCore) coreState.getBlock();
        return !block.bounding.isEmpty()
                || !block.usesSharedCells()
                || block.cellsOpen()
                || MultiblockCellShapes.hasGeometry(block);
    }

    public static void visitPresent(
            Level level,
            BlockPos core,
            BlockState coreState,
            BiConsumer<BlockPos, BlockState> visitor) {
        BlockMultiblockCore block = (BlockMultiblockCore) coreState.getBlock();
        Direction facing = coreState.getValue(BlockMultiblockCore.FACING);
        visitor.accept(core, coreState);
        if (!block.hasVariableFootprint()) {
            Layout layout = block.footprint(facing);
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            ChunkAccess chunk = null;
            int chunkX = Integer.MIN_VALUE, chunkZ = Integer.MIN_VALUE;
            for (int i = 0; i < layout.offsets.length; i++) {
                long offset = layout.offsets[i];
                pos.setWithOffset(
                        core, BlockPos.getX(offset), BlockPos.getY(offset), BlockPos.getZ(offset));
                int x = pos.getX() >> 4, z = pos.getZ() >> 4;
                if (x != chunkX || z != chunkZ) {
                    chunk = level.getChunk(x, z, ChunkStatus.FULL, false);
                    chunkX = x;
                    chunkZ = z;
                }
                if (chunk == null) continue;
                BlockState state = chunk.getBlockState(pos);
                if (!block.isCellOf(state)) continue;
                if (level instanceof ServerLevel server) {
                    if (MultiblockSurface.recordedCorePacked(
                                    server, pos.getX(), pos.getY(), pos.getZ())
                            != core.asLong()) continue;
                } else if (!matches(state, layout.states[i])) continue;
                visitor.accept(pos, state);
            }
            return;
        }
        block.visitPlacedCells(
                level,
                core,
                facing,
                (pos, mask) -> {
                    var chunk =
                            level.getChunk(
                                    pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.FULL, false);
                    if (chunk == null) return;
                    BlockState state = chunk.getBlockState(pos);
                    if (!block.isCellOf(state)) return;
                    if (level instanceof ServerLevel server) {
                        if (MultiblockSurface.recordedCorePacked(
                                        server, pos.getX(), pos.getY(), pos.getZ())
                                != core.asLong()) return;
                    } else {
                        int dx = pos.getX() - core.getX(),
                                dy = pos.getY() - core.getY(),
                                dz = pos.getZ() - core.getZ();
                        int lx = MultiblockMaskTable.localX(dx, dy, dz, facing);
                        int lz = MultiblockMaskTable.localZ(dx, dy, dz, facing);
                        int shape = MultiblockCellShapes.idAt(block, lx, dy, lz, facing);
                        BlockState expected = block.cellStateFor(lx, dy, lz, facing, shape);
                        if (!state.is(expected.getBlock())) return;
                        if (state.getBlock() instanceof BlockMultiblockGeometryCell
                                && BlockMultiblockGeometryCell.shapeId(state) != shape) return;
                    }
                    visitor.accept(pos, state);
                });
    }

    public static boolean matches(BlockState actual, BlockState expected) {
        return actual.is(expected.getBlock())
                && (!(actual.getBlock() instanceof BlockMultiblockGeometryCell)
                        || BlockMultiblockGeometryCell.shapeId(actual)
                                == BlockMultiblockGeometryCell.shapeId(expected));
    }
}
