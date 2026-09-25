// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.rail;

import com.hbm.blocks.rail.IRailNTM.MoveContext;
import com.hbm.blocks.rail.IRailNTM.RailContext;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

public class RailStandardBuffer extends BlockRailNTM {

    private static final int[] DIMENSIONS = {0, 0, 2, 2, 1, 0};

    public RailStandardBuffer(Properties props) {
        super(props);
    }

    @Override
    public int[] getDimensions() {
        return DIMENSIONS;
    }

    @Override
    public int getOffset() {
        return 2;
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
            double vecX = Mth.clamp(targetX, cX - 2, cX + 3);
            double vecY = y + 0.1875;
            double vecZ = cZ + 0.5 + rot.getStepZ() * 0.5;

            double nX = (dir == Direction.EAST ? -1 - context.collisionBogieDistance : 2);
            double pX = (dir == Direction.WEST ? 0 - context.collisionBogieDistance : 3);
            double buffer = Mth.clamp(targetX, cX - nX, cX + pX);

            if (buffer != vecX) {
                context.collision = true;
                context.overshoot = Math.abs(buffer - vecX);
                info.at(buffer, vecY, vecZ);
                return;
            }

            info.dist(Math.abs(targetX - vecX) * Math.signum(speed));
            info.pos(cX + (motionX * speed > 0 ? 3 : -3), y, cZ);
            info.at(vecX, vecY, vecZ);
        } else {
            double targetZ = trainZ;
            if (motionZ > 0) {
                targetZ += speed;
                info.yaw(0F);
            } else {
                targetZ -= speed;
                info.yaw(180F);
            }
            double vecX = cX + 0.5 + rot.getStepX() * 0.5;
            double vecY = y + 0.1875;
            double vecZ = Mth.clamp(targetZ, cZ - 2, cZ + 3);

            double nZ = (dir == Direction.SOUTH ? -1 - context.collisionBogieDistance : 2);
            double pZ = (dir == Direction.NORTH ? 0 - context.collisionBogieDistance : 3);
            double buffer = Mth.clamp(targetZ, cZ - nZ, cZ + pZ);

            if (buffer != vecZ) {
                context.collision = true;
                context.overshoot = Math.abs(buffer - vecZ);
                info.at(vecX, vecY, buffer);
                return;
            }

            info.dist(Math.abs(targetZ - vecZ) * Math.signum(speed));
            info.pos(cX, y, cZ + (motionZ * speed > 0 ? 3 : -3));
            info.at(vecX, vecY, vecZ);
        }
    }

    @Override
    public TrackGauge getGauge(Level level, int x, int y, int z) {
        return TrackGauge.STANDARD;
    }
}
