// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.network.ConveyorBend;
import com.hbm.blocks.network.ConveyorBendableBlock;
import com.hbm.blocks.network.ConveyorBlockBase;
import com.hbm.blocks.network.ConveyorChuteBlock;
import com.hbm.blocks.network.ConveyorLiftBlock;
import com.hbm.blocks.network.CraneBlockBase;
import com.hbm.items.tool.ItemConveyorWand.ConveyorType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.jspecify.annotations.Nullable;

public final class ConveyorRoute {

    private ConveyorRoute() {}

    public static int construct(
            Level route,
            @Nullable ConveyorPlacer build,
            ConveyorType type,
            Player player,
            BlockPos anchor,
            Direction anchorSide,
            BlockPos end,
            Direction endSide,
            int max) {
        Direction dir = anchorSide;
        Direction targetDir = endSide;

        if (anchor.equals(end) && anchorSide == endSide && dir.getAxis().isVertical()) {
            BlockPos at = anchor.relative(dir);
            if (!route.getBlockState(at).canBeReplaced()) return -1;
            if (build != null)
                place(
                        build,
                        at,
                        ItemConveyorWand.getConveyorBlock(type),
                        player.getDirection(),
                        ConveyorBend.STRAIGHT);
            return 1;
        }

        boolean hasVertical = ItemConveyorWand.hasSnakesAndLadders(type);

        BlockPos target = end.relative(targetDir);
        BlockPos cursor = anchor.relative(dir);

        if (dir.getAxis().isVertical()) dir = targetDirection(cursor, end, hasVertical);

        Block targetBlock = route.getBlockState(end).getBlock();
        boolean shouldTurnToTarget =
                targetDir.getAxis().isHorizontal()
                        || targetBlock instanceof CraneBlockBase
                        || targetBlock == ModBlocks.CONVEYOR_LIFT.get()
                        || targetBlock == ModBlocks.CONVEYOR_CHUTE.get();

        Direction horDir = dir.getAxis().isVertical() ? player.getDirection() : dir;

        if (hasVertical
                && cursor.getY() > target.getY()
                && route.getBlockState(cursor.below()).canBeReplaced()) {
            dir = Direction.DOWN;
        }

        for (int laid = 1; laid <= max; laid++) {
            if (!route.getBlockState(cursor).canBeReplaced()) return -1;

            Block block = conveyorForDirection(type, dir);

            Direction facing = facingFor(block, dir, targetDir, horDir);
            ConveyorBend bend = ConveyorBend.STRAIGHT;

            BlockPos ahead = cursor.relative(dir);

            int fromDistance = taxi(cursor, target);
            int toDistance = taxi(ahead, target);
            int finalDistance = taxi(ahead, end);
            boolean notAtTarget = (shouldTurnToTarget ? finalDistance : fromDistance) > 0;
            boolean willBeObstructed = notAtTarget && !route.getBlockState(ahead).canBeReplaced();
            boolean shouldTurn = (toDistance >= fromDistance && notAtTarget) || willBeObstructed;

            if (shouldTurn) {
                Direction newDir =
                        targetDirection(
                                cursor,
                                shouldTurnToTarget ? end : target,
                                target,
                                dir,
                                willBeObstructed,
                                hasVertical);

                if (newDir == Direction.UP) block = ModBlocks.CONVEYOR_LIFT.get();
                else if (newDir == Direction.DOWN) block = ModBlocks.CONVEYOR_CHUTE.get();
                else if (dir.getClockWise(Direction.Axis.Y) == newDir) bend = ConveyorBend.RIGHT;
                else if (dir.getCounterClockWise(Direction.Axis.Y) == newDir)
                    bend = ConveyorBend.LEFT;

                dir = newDir;
                if (dir.getAxis().isHorizontal()) horDir = dir;
            }

            if (build != null) place(build, cursor, block, facing, bend);

            if (cursor.equals(target)) return laid;

            cursor = cursor.relative(dir);
        }

        return 0;
    }

    public static ConveyorPlacer into(Level level) {
        return new LevelPlacer(level);
    }

    private static void place(
            ConveyorPlacer build, BlockPos pos, Block block, Direction facing, ConveyorBend bend) {
        BlockState state = block.defaultBlockState().setValue(ConveyorBlockBase.FACING, facing);

        if (state.hasProperty(ConveyorBendableBlock.BEND))
            state = state.setValue(ConveyorBendableBlock.BEND, bend);
        build.place(pos, columnState(build, pos, state));
        for (int dy = -1; dy <= 1; dy += 2) {
            BlockPos neighbor = pos.above(dy);
            BlockState before = build.getBlockState(neighbor);
            BlockState after = columnState(build, neighbor, before);
            if (after != before) build.place(neighbor, after);
        }
    }

    private static BlockState columnState(ConveyorPlacer build, BlockPos pos, BlockState state) {

        BlockGetter view = build instanceof LevelPlacer live ? live.level() : build;
        if (state.hasProperty(ConveyorLiftBlock.PART)) {
            return state.setValue(ConveyorLiftBlock.PART, ConveyorLiftBlock.partAt(view, pos));
        }
        if (state.hasProperty(ConveyorChuteBlock.FEEDS_DOWN)) {
            return state.setValue(
                    ConveyorChuteBlock.FEEDS_DOWN, ConveyorChuteBlock.feedsOn(view, pos));
        }
        return state;
    }

    private static Block conveyorForDirection(ConveyorType type, Direction dir) {
        if (dir == Direction.UP) return ModBlocks.CONVEYOR_LIFT.get();
        if (dir == Direction.DOWN) return ModBlocks.CONVEYOR_CHUTE.get();
        return ItemConveyorWand.getConveyorBlock(type);
    }

    private static Direction facingFor(
            Block block, Direction dir, Direction targetDir, Direction horDir) {
        if (block != ModBlocks.CONVEYOR_CHUTE.get() && block != ModBlocks.CONVEYOR_LIFT.get())
            return dir;
        if (targetDir.getAxis().isVertical()) return horDir;
        return targetDir.getOpposite();
    }

    private static Direction targetDirection(BlockPos from, BlockPos to, boolean hasVertical) {
        return targetDirection(from, to, to, null, false, hasVertical);
    }

    private static Direction targetDirection(
            BlockPos from,
            BlockPos to,
            BlockPos target,
            @Nullable Direction heading,
            boolean willBeObstructed,
            boolean hasVertical) {
        if (hasVertical
                && (from.getY() != to.getY() || from.getY() != target.getY())
                && (willBeObstructed
                        || (from.getX() == to.getX() && from.getZ() == to.getZ())
                        || (from.getX() == target.getX() && from.getZ() == target.getZ()))) {
            return from.getY() > to.getY() ? Direction.DOWN : Direction.UP;
        }

        if (Math.abs(from.getX() - to.getX()) > Math.abs(from.getZ() - to.getZ())) {
            if (heading == Direction.EAST || heading == Direction.WEST) {
                return from.getZ() > to.getZ() ? Direction.NORTH : Direction.SOUTH;
            }
            return from.getX() > to.getX() ? Direction.WEST : Direction.EAST;
        }

        if (heading == Direction.NORTH || heading == Direction.SOUTH) {
            return from.getX() > to.getX() ? Direction.WEST : Direction.EAST;
        }
        return from.getZ() > to.getZ() ? Direction.NORTH : Direction.SOUTH;
    }

    private static int taxi(BlockPos a, BlockPos b) {
        return Math.abs(a.getX() - b.getX())
                + Math.abs(a.getY() - b.getY())
                + Math.abs(a.getZ() - b.getZ());
    }

    private record LevelPlacer(Level level) implements ConveyorPlacer {

        @Override
        public void place(BlockPos pos, BlockState state) {
            level.setBlock(pos, state, Block.UPDATE_ALL);
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            return level.getBlockState(pos);
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return level.getFluidState(pos);
        }

        @Override
        public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
            return level.getBlockEntity(pos);
        }

        @Override
        public int getHeight() {
            return level.getHeight();
        }

        @Override
        public int getMinY() {
            return level.getMinY();
        }
    }
}
