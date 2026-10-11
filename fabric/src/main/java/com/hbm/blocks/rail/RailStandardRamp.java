// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.rail;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

public class RailStandardRamp extends BlockRailNTM {
    public RailStandardRamp(Properties props) {
        super(props);
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
        int cY = owner.coreY;
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
            double dist = (cX + 0.5 - targetX + 2.5) / 5;
            double vecX = Mth.clamp(targetX, cX - 2, cX + 3);
            double vecY =
                    Mth.clamp(dir == Direction.EAST ? cY + dist : cY + 1 - dist, cY, cY + 1)
                            + 0.1875;
            info.dist(Math.abs(targetX - vecX) * Math.signum(speed));
            info.pos(
                    cX + (motionX * speed > 0 ? 3 : -3),
                    cY + (motionX * speed > 0 ^ dir == Direction.EAST ? 1 : 0),
                    cZ);
            info.at(vecX, vecY, cZ + 0.5 + rot.getStepZ() * 0.5);
        } else {
            double targetZ = trainZ;
            if (motionZ > 0) {
                targetZ += speed;
                info.yaw(0F);
            } else {
                targetZ -= speed;
                info.yaw(180F);
            }
            double dist = (cZ + 0.5 - targetZ + 2.5) / 5;
            double vecY =
                    Mth.clamp(dir == Direction.SOUTH ? cY + dist : cY + 1 - dist, cY, cY + 1)
                            + 0.1875;
            double vecZ = Mth.clamp(targetZ, cZ - 2, cZ + 3);
            info.dist(Math.abs(targetZ - vecZ) * Math.signum(speed));
            info.pos(
                    cX,
                    cY + (motionZ * speed > 0 ^ dir == Direction.SOUTH ? 1 : 0),
                    cZ + (motionZ * speed > 0 ? 3 : -3));
            info.at(cX + 0.5 + rot.getStepX() * 0.5, vecY, vecZ);
        }
    }

    @Override
    public TrackGauge getGauge(Level level, int x, int y, int z) {
        return TrackGauge.STANDARD;
    }
}
