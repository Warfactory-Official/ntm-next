// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.rail;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

public class RailStandardStraightShort extends BlockRailNTM {

    private static final int[] DIMENSIONS = {0, 0, 0, 0, 1, 0};

    public RailStandardStraightShort(Properties props) {
        super(props);
    }

    @Override
    public int[] getDimensions() {
        return DIMENSIONS;
    }

    @Override
    public int getOffset() {
        return 0;
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

        if (dir == Direction.EAST || dir == Direction.WEST) {
            double targetX = trainX;
            if (motionX > 0) {
                targetX += speed;
                info.yaw(-90F);
            } else {
                targetX -= speed;
                info.yaw(90F);
            }
            double vecX = Mth.clamp(targetX, cX, cX + 1);
            info.dist(Math.abs(targetX - vecX) * Math.signum(speed));
            info.pos(cX + (motionX * speed > 0 ? 1 : -1), y, cZ);
            info.at(vecX, y + 0.1875, cZ + 0.5 + rot.getStepZ() * 0.5);
        } else {
            double targetZ = trainZ;
            if (motionZ > 0) {
                targetZ += speed;
                info.yaw(0F);
            } else {
                targetZ -= speed;
                info.yaw(180F);
            }
            double vecZ = Mth.clamp(targetZ, cZ, cZ + 1);
            info.dist(Math.abs(targetZ - vecZ) * Math.signum(speed));
            info.pos(cX, y, cZ + (motionZ * speed > 0 ? 1 : -1));
            info.at(cX + 0.5 + rot.getStepX() * 0.5, y + 0.1875, vecZ);
        }
    }

    @Override
    public TrackGauge getGauge(Level level, int x, int y, int z) {
        return TrackGauge.STANDARD;
    }
}
