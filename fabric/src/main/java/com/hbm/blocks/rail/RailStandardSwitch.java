// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.rail;

import com.hbm.items.ModItems;
import com.hbm.tileentity.BlockEntityRail;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class RailStandardSwitch extends BlockRailWaypointSystem {

    private static final int[][] SIDE_TRACK = {
        {2, 2}, {3, 2}, {4, 2}, {5, 2}, {4, 3}, {5, 3}, {5, 4}, {6, 3}, {6, 4}, {7, 3}, {7, 4},
        {7, 5}, {8, 4}, {9, 4}, {10, 4}, {11, 4}, {12, 4}, {13, 4}, {14, 4}, {8, 5}, {9, 5},
        {10, 5}, {11, 5}, {12, 5}, {13, 5}, {14, 5},
    };

    public RailStandardSwitch(Properties props) {
        super(props);

        RailDef main = new RailDef("main");
        RailDef side = new RailDef("side");
        railDefs.add(main);
        railDefs.add(side);

        main.nodes.add(new Vec3(-8.5, 0.1875, 0.5));
        main.nodes.add(new Vec3(-7.5, 0.1875, 0.5));
        main.nodes.add(new Vec3(6.5, 0.1875, 0.5));
        main.nodes.add(new Vec3(7.5, 0.1875, 0.5));
        main.nodes.add(new Vec3(8.5, 0.1875, 0.5));

        side.nodes.add(new Vec3(-8.5, 0.1875, 4.5));
        side.nodes.add(new Vec3(-7.5, 0.1875, 4.5));
        side.nodes.add(new Vec3(-6.5, 0.1875, 4.5));
        side.nodes.add(new Vec3(-5.5, 0.1875, 4.5));
        side.nodes.add(new Vec3(-4.5, 0.1875, 4.5));
        side.nodes.add(new Vec3(-3.5, 0.1875, 4.5));
        side.nodes.add(new Vec3(-2.5, 0.1875, 4.5));
        side.nodes.add(new Vec3(-1.5, 0.1875, 4.5));
        side.nodes.add(new Vec3(-0.5, 0.1875, 4.25));
        side.nodes.add(new Vec3(0.5, 0.1875, 3.9375));
        side.nodes.add(new Vec3(1.5, 0.1875, 3.375));
        side.nodes.add(new Vec3(2.5, 0.1875, 2.4625));
        side.nodes.add(new Vec3(3.5, 0.1875, 1.75));
        side.nodes.add(new Vec3(4.5, 0.1875, 1.1875));
        side.nodes.add(new Vec3(5.5, 0.1875, 0.875));
        side.nodes.add(new Vec3(6.5, 0.1875, 0.625));
        side.nodes.add(new Vec3(7.5, 0.1875, 0.5));
        side.nodes.add(new Vec3(8.5, 0.1875, 0.5));
    }

    protected Direction rotFor(Direction facing) {
        return facing.getClockWise();
    }

    protected int[][] sideTrack() {
        return SIDE_TRACK;
    }

    protected BlockPos sideTrackCell(BlockPos core, Direction facing, int[] off) {
        Direction dir = facing.getOpposite();
        Direction rot = rotFor(facing);
        return core.offset(
                dir.getStepX() * off[0] + rot.getStepX() * off[1],
                0,
                dir.getStepZ() * off[0] + rot.getStepZ() * off[1]);
    }

    @Override
    public TrackGauge getGauge(Level level, int x, int y, int z) {
        return TrackGauge.STANDARD;
    }

    private static final int[] DIMENSIONS = {0, 0, 7, 7, 1, 0};

    @Override
    public int[] getDimensions() {
        return DIMENSIONS;
    }

    @Override
    public int getOffset() {
        return 7;
    }

    protected static final int ROUTE_UNKNOWN = -1;
    protected static final int ROUTE_MAIN = 0;
    protected static final int ROUTE_SIDE = 1;

    @Override
    protected int routeState(Level level, int coreX, int coreY, int coreZ) {
        if (!(level.getBlockEntity(new BlockPos(coreX, coreY, coreZ))
                instanceof BlockEntityRail rail)) return ROUTE_UNKNOWN;
        return rail.isSwitched ? ROUTE_SIDE : ROUTE_MAIN;
    }

    @Override
    public boolean canCross(
            Direction dir,
            int x,
            int y,
            int z,
            double fromX,
            double fromZ,
            double toX,
            double toZ,
            RailDef def,
            int route) {

        if (route == ROUTE_UNKNOWN) return true;

        if (dir == Direction.EAST && fromX < toX) return true;
        if (dir == Direction.WEST && fromX > toX) return true;
        if (dir == Direction.SOUTH && fromZ < toZ) return true;
        if (dir == Direction.NORTH && fromZ > toZ) return true;

        if (dir == Direction.EAST && toX < x + 0.5 + 7) return true;
        if (dir == Direction.WEST && toX > x + 0.5 - 7) return true;
        if (dir == Direction.SOUTH && toZ < z + 0.5 + 7) return true;
        if (dir == Direction.NORTH && toZ > z + 0.5 - 7) return true;

        if (route == ROUTE_SIDE) {
            if ("side".equals(def.name)) return true;
        } else {
            if ("main".equals(def.name)) return true;
        }

        return false;
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (ModItems.TRAIN.typeOf(player.getMainHandItem()) != null) return InteractionResult.PASS;

        if (!level.isClientSide() && level.getBlockEntity(core) instanceof BlockEntityRail rail) {
            rail.toggleSwitch();
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        for (int[] off : sideTrack()) {
            visitor.cell(sideTrackCell(core, facing, off), MASK_NONE);
        }
    }

    @Override
    public boolean checkRequirement(Level level, BlockPos placed, Direction dir, int o) {
        if (!super.checkRequirement(level, placed, dir, o)) return false;
        BlockPos core = placed.offset(dir.getStepX() * o, dir.getStepY() * o, dir.getStepZ() * o);
        for (int[] off : sideTrack()) {
            if (!level.getBlockState(sideTrackCell(core, dir, off)).canBeReplaced()) return false;
        }
        return true;
    }
}
