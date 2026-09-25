// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.machine;

import com.hbm.blocks.machine.BlockPWR;
import com.hbm.blocks.machine.MachinePWRController;
import com.hbm.util.ChunkUtil;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongArrays;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public record PwrPrintData(
        BlockPos min, BlockPos max, Direction direction, long[] positions, BlockState[] states) {

    public static @Nullable PwrPrintData capture(ServerLevel level, BlockPos core) {
        Direction direction =
                level.getBlockState(core).getValue(MachinePWRController.FACING).getOpposite();
        LongOpenHashSet visited = new LongOpenHashSet();
        LongArrayList pending = new LongArrayList();
        visited.add(core.asLong());
        pending.add(core.relative(direction).asLong());
        int minX = core.getX(), minY = core.getY(), minZ = core.getZ();
        int maxX = minX, maxY = minY, maxZ = minZ;
        for (int cursor = 0; cursor < pending.size(); cursor++) {
            long packed = pending.getLong(cursor);
            if (!visited.add(packed)) continue;
            BlockPos pos = BlockPos.of(packed);
            BlockState state = ChunkUtil.blockStateIfLoaded(level, pos);
            if (state == null) return null;
            if (!(state.getBlock() instanceof BlockPWR)) continue;
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
            pending.add(pos.east().asLong());
            pending.add(pos.west().asLong());
            pending.add(pos.above().asLong());
            pending.add(pos.below().asLong());
            pending.add(pos.south().asLong());
            pending.add(pos.north().asLong());
        }

        Long2ObjectOpenHashMap<BlockState> blocks = new Long2ObjectOpenHashMap<>();
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockState state = ChunkUtil.blockStateIfLoaded(level, at.set(x, y, z));
                    if (state == null) return null;
                    if (state.getBlock() instanceof BlockPWR) {
                        blocks.put(at.asLong(), state.getValue(BlockPWR.PART).original());
                    } else if (state.getBlock() instanceof MachinePWRController) {
                        blocks.put(at.asLong(), state);
                    }
                }
            }
        }
        long[] positions = blocks.keySet().toLongArray();
        LongArrays.quickSort(
                positions,
                (a, b) -> {
                    int comparison = Integer.compare(BlockPos.getX(a), BlockPos.getX(b));
                    if (comparison == 0)
                        comparison = Integer.compare(BlockPos.getY(a), BlockPos.getY(b));
                    return comparison == 0
                            ? Integer.compare(BlockPos.getZ(a), BlockPos.getZ(b))
                            : comparison;
                });
        BlockState[] states = new BlockState[positions.length];
        for (int i = 0; i < positions.length; i++) states[i] = blocks.get(positions[i]);
        return new PwrPrintData(
                new BlockPos(minX, minY, minZ),
                new BlockPos(maxX, maxY, maxZ),
                direction,
                positions,
                states);
    }

    public int sizeX() {
        return max.getX() - min.getX() + 1;
    }

    public int sizeY() {
        return max.getY() - min.getY() + 1;
    }

    public int sizeZ() {
        return max.getZ() - min.getZ() + 1;
    }

    public Rotation rotation() {
        return switch (direction) {
            case NORTH -> Rotation.NONE;
            case WEST -> Rotation.CLOCKWISE_90;
            case SOUTH -> Rotation.CLOCKWISE_180;
            case EAST -> Rotation.COUNTERCLOCKWISE_90;
            default -> throw new IllegalStateException();
        };
    }

    public int displayX(int index) {
        int x = BlockPos.getX(positions[index]) - min.getX();
        int z = BlockPos.getZ(positions[index]) - min.getZ();
        return switch (direction) {
            case NORTH -> x;
            case WEST -> sizeZ() - 1 - z;
            case SOUTH -> sizeX() - 1 - x;
            case EAST -> z;
            default -> throw new IllegalStateException();
        };
    }

    public int displayZ(int index) {
        int x = BlockPos.getX(positions[index]) - min.getX();
        int z = BlockPos.getZ(positions[index]) - min.getZ();
        return switch (direction) {
            case NORTH -> z;
            case WEST -> x;
            case SOUTH -> sizeZ() - 1 - z;
            case EAST -> sizeX() - 1 - x;
            default -> throw new IllegalStateException();
        };
    }
}
