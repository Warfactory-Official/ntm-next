// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.rail;

import com.hbm.blocks.rail.IRailNTM.MoveContext;
import com.hbm.blocks.rail.IRailNTM.RailContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

public class RailStandardCurveBase extends BlockRailNTM {

    protected int width = 4;

    public static final int[][] STAIRCASE = {
        {1, 0, 0},
        {0, 1, 1},
        {1, 1, 1},
        {1, 2, 1},
        {2, 1, 0},
        {2, 2, 0},
        {3, 1, 0},
        {3, 2, 0},
        {2, 3, 1},
        {3, 3, 1},
        {4, 3, 0},
        {3, 4, 1},
        {4, 4, 1},
    };

    public RailStandardCurveBase(Properties props) {
        super(props);
    }

    protected int[][] staircase() {
        return STAIRCASE;
    }

    protected double turnRadius() {
        return width;
    }

    protected double lift() {
        return 0.1875;
    }

    @Override
    public void getTravelLocation(
            Level level,
            int x,
            int y,
            int z,
            double trainX,
            double trainY,
            double trainZ,
            double motionX,
            double motionY,
            double motionZ,
            double speed,
            RailContext info,
            MoveContext context) {
        Owner owner = info.owner;
        int cX = owner.coreX;
        int cZ = owner.coreZ;
        Direction dir = owner.facing;
        Direction rot = dir.getClockWise();

        double turnRadius = turnRadius();
        double axisDist = width + 0.5D;

        double axisX = cX + 0.5 + dir.getStepX() * 0.5 + rot.getStepX() * axisDist;
        double axisZ = cZ + 0.5 + dir.getStepZ() * 0.5 + rot.getStepZ() * axisDist;

        double offX = trainX - axisX;
        double offZ = trainZ - axisZ;
        double offLen = Math.sqrt(offX * offX + offZ * offZ);
        if (offLen < 1.0E-4D) {
            offX = 0;
            offZ = 0;
        } else {
            offX = offX / offLen * turnRadius;
            offZ = offZ / offLen * turnRadius;
        }

        double moveAngle = Math.atan2(motionX, motionZ) * 180D / Math.PI + 90;

        if (speed == 0) {
            info.dist(0).pos(x, y, z).yaw((float) moveAngle);
            info.at(axisX + offX, y, axisZ + offZ);
            return;
        }

        double angleDeg = Math.atan2(offX, offZ) * 180D / Math.PI + 90;
        if (dir == Direction.WEST) angleDeg -= 90;
        if (dir == Direction.EAST) angleDeg += 90;
        if (dir == Direction.SOUTH) angleDeg += 180;
        angleDeg = Mth.wrapDegrees(angleDeg);
        double length90Deg = turnRadius * Math.PI / 2D;
        double angularChange = speed / length90Deg * 90D;

        Direction moveDir;

        if (Math.abs(motionX) > Math.abs(motionZ)) {
            moveDir = motionX > 0 ? Direction.EAST : Direction.WEST;
        } else {
            moveDir = motionZ > 0 ? Direction.SOUTH : Direction.NORTH;
        }

        if (moveDir == dir || moveDir == rot.getOpposite()) {
            angularChange *= -1;
        }

        double effAngle = angleDeg + angularChange;
        moveAngle += angularChange;

        if (effAngle > 90) {
            double angleOvershoot = effAngle - 90D;
            moveAngle -= angleOvershoot;
            double lengthOvershoot = angleOvershoot * length90Deg / 90D;
            info.dist(lengthOvershoot * Math.signum(speed * angularChange))
                    .pos(
                            cX - dir.getStepX() * width + rot.getStepX() * (width + 1),
                            y,
                            cZ - dir.getStepZ() * width + rot.getStepZ() * (width + 1))
                    .yaw((float) moveAngle);
            info.at(
                    axisX - dir.getStepX() * turnRadius,
                    y + lift(),
                    axisZ - dir.getStepZ() * turnRadius);
            return;
        }

        if (effAngle < 0) {
            double angleOvershoot = -effAngle;
            moveAngle -= angleOvershoot;
            double lengthOvershoot = angleOvershoot * length90Deg / 90D;
            info.dist(-lengthOvershoot * Math.signum(speed * angularChange))
                    .pos(cX + dir.getStepX(), y, cZ + dir.getStepZ())
                    .yaw((float) moveAngle);
            info.at(
                    axisX - rot.getStepX() * turnRadius,
                    y + lift(),
                    axisZ - rot.getStepZ() * turnRadius);
            return;
        }

        float radianChange = (float) (angularChange * Math.PI / 180D);
        float sin = Mth.sin(radianChange);
        float cos = Mth.cos(radianChange);
        info.at(axisX + offX * cos + offZ * sin, y + lift(), axisZ + offZ * cos - offX * sin);
    }

    @Override
    public TrackGauge getGauge(Level level, int x, int y, int z) {
        return TrackGauge.STANDARD;
    }

    @Override
    public int[] getDimensions() {
        return new int[] {0, 0, width, 0, width, 0};
    }

    @Override
    public int getOffset() {
        return 0;
    }

    public static BlockPos staircaseCell(BlockPos core, Direction facing, int[] off) {
        Direction dir = facing.getOpposite();
        Direction rot = facing.getClockWise();
        return core.offset(
                dir.getStepX() * off[0] + rot.getStepX() * off[1],
                0,
                dir.getStepZ() * off[0] + rot.getStepZ() * off[1]);
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        for (int[] off : staircase()) {
            visitor.cell(staircaseCell(core, facing, off), MASK_NONE);
        }
    }

    @Override
    public boolean checkRequirement(Level level, BlockPos placed, Direction dir, int o) {
        BlockPos core = placed.offset(dir.getStepX() * o, dir.getStepY() * o, dir.getStepZ() * o);
        for (int[] off : staircase()) {
            if (!level.getBlockState(staircaseCell(core, dir, off)).canBeReplaced()) return false;
        }
        return true;
    }
}
