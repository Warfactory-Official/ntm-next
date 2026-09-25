// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.rail;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public interface IRailNTM {

    default Vec3 getSnappingPos(
            Level level, int x, int y, int z, double trainX, double trainY, double trainZ) {
        RailContext info = new RailContext();
        if (!BlockRailNTM.railAt(level, x, y, z, info.owner))
            return new Vec3(trainX, trainY, trainZ);
        getTravelLocation(
                level,
                x,
                y,
                z,
                trainX,
                trainY,
                trainZ,
                0,
                0,
                0,
                0,
                info,
                new MoveContext(RailCheckType.OTHER, 0));
        return new Vec3(info.x, info.y, info.z);
    }

    void getTravelLocation(
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
            MoveContext context);

    TrackGauge getGauge(Level level, int x, int y, int z);

    enum TrackGauge {
        STANDARD,
        NARROW
    }

    final class RailContext {

        public float yaw;

        public double overshoot;

        public int posX, posY, posZ;
        public boolean hasPos;

        public double x, y, z;

        public final BlockRailNTM.Owner owner = new BlockRailNTM.Owner();

        public RailContext reset() {
            this.overshoot = 0;
            this.hasPos = false;
            return this;
        }

        public RailContext yaw(float y) {
            this.yaw = y;
            return this;
        }

        public RailContext dist(double d) {
            this.overshoot = d;
            return this;
        }

        public RailContext pos(int x, int y, int z) {
            this.posX = x;
            this.posY = y;
            this.posZ = z;
            this.hasPos = true;
            return this;
        }

        public RailContext at(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
            return this;
        }
    }

    final class MoveContext {
        public RailCheckType type;
        public double collisionBogieDistance;

        public boolean collision = false;

        public double overshoot;

        public MoveContext(RailCheckType type, double collisionBogieDistance) {
            set(type, collisionBogieDistance);
        }

        public MoveContext set(RailCheckType type, double collisionBogieDistance) {
            this.type = type;
            this.collisionBogieDistance = collisionBogieDistance;
            this.collision = false;
            this.overshoot = 0;
            return this;
        }
    }

    enum RailCheckType {
        CORE,
        FRONT,
        BACK,
        OTHER
    }
}
