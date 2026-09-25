// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.rail;

import com.hbm.entity.train.EntityRailCarBase;
import com.hbm.util.BobMathUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public abstract class BlockRailWaypointSystem extends BlockRailNTM {

    public List<RailDef> railDefs = new ArrayList<>();

    private volatile double[][][] baked;

    public BlockRailWaypointSystem(Properties props) {
        super(props);
    }

    protected int routeState(Level level, int coreX, int coreY, int coreZ) {
        return 0;
    }

    public boolean canCross(
            Direction facing,
            int coreX,
            int coreY,
            int coreZ,
            double fromX,
            double fromZ,
            double toX,
            double toZ,
            RailDef def,
            int routeState) {
        return true;
    }

    private double[][] nodesFor(Direction facing) {
        double[][][] table = baked;
        if (table == null) {
            table = new double[Direction.values().length][][];
            for (Direction dir : Direction.values()) {
                float rotation = 0;
                if (dir == Direction.NORTH) rotation = 90F / 180F * (float) Math.PI;
                if (dir == Direction.WEST) rotation = (float) Math.PI;
                if (dir == Direction.SOUTH) rotation = 270F / 180F * (float) Math.PI;

                double[][] defs = new double[railDefs.size()][];
                for (int d = 0; d < railDefs.size(); d++) {
                    List<Vec3> nodes = railDefs.get(d).nodes;
                    double[] flat = new double[nodes.size() * 3];
                    for (int i = 0; i < nodes.size(); i++) {
                        Vec3 rotated = nodes.get(i).yRot(rotation);
                        flat[i * 3] = rotated.x;
                        flat[i * 3 + 1] = rotated.y;
                        flat[i * 3 + 2] = rotated.z;
                    }
                    defs[d] = flat;
                }
                table[dir.ordinal()] = defs;
            }
            baked = table;
        }
        return table[facing.ordinal()];
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

        double[][] defs = nodesFor(dir);
        double originX = cX + 0.5;
        double originY = cY;
        double originZ = cZ + 0.5;
        double moveAngle = Math.atan2(motionX, motionZ) * 180D / Math.PI + 90;
        int route = routeState(level, cX, cY, cZ);

        int closestDef = -1;
        int closestLink = -1;
        double startX = 0, startY = 0, startZ = 0;
        double dist = Double.MAX_VALUE;

        boolean d = true;

        for (int di = 0; di < defs.length; di++) {
            double[] nodes = defs[di];
            RailDef def = railDefs.get(di);

            for (int li = 0; li < nodes.length / 3 - 1; li++) {
                double aX = originX + nodes[li * 3],
                        aY = originY + nodes[li * 3 + 1],
                        aZ = originZ + nodes[li * 3 + 2];
                double bX = originX + nodes[li * 3 + 3],
                        bY = originY + nodes[li * 3 + 4],
                        bZ = originZ + nodes[li * 3 + 5];

                double abX = bX - aX;
                double abZ = bZ - aZ;
                double along =
                        ((trainX - aX) * abX + (trainZ - aZ) * abZ) / (abX * abX + abZ * abZ);
                double pX, pY, pZ;
                if (along < 0) {
                    pX = aX;
                    pY = aY;
                    pZ = aZ;
                } else if (along > 1) {
                    pX = bX;
                    pY = bY;
                    pZ = bZ;
                } else {
                    pX = aX + abX * along;
                    pY = aY + (bY - aY) * along;
                    pZ = aZ + abZ * along;
                }

                double deltaX = pX - trainX;
                double deltaY = pY - trainY;
                double deltaZ = pZ - trainZ;
                double length = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);

                if (!canCross(dir, cX, cY, cZ, trainX, trainZ, pX, pZ, def, route)) continue;

                double linkAngle = EntityRailCarBase.generateYaw(bX, bZ, aX, aZ);
                double angularDiff = BobMathUtil.angularDifference(linkAngle, -moveAngle);
                if (angularDiff < -180) {
                    d = false;
                }
                if (angularDiff > 0) {
                    d = false;
                }

                if (length < dist) {
                    closestDef = di;
                    closestLink = li;
                    startX = pX;
                    startY = pY;
                    startZ = pZ;
                    dist = length;
                }
            }
        }

        if (closestDef == -1) {
            info.at(trainX, trainY, trainZ);
            return;
        }

        double[] chain = defs[closestDef];
        RailDef chainDef = railDefs.get(closestDef);
        int links = chain.length / 3 - 1;

        double distRemaining = speed;
        boolean engaged = false;
        double currentX = startX, currentY = startY, currentZ = startZ;

        for (int i = d ? 0 : links - 1; d ? (i < links) : (i >= 0); i += d ? 1 : -1) {

            if (!engaged) {
                if (i == closestLink) {
                    engaged = true;
                } else {
                    continue;
                }
            }

            int node = d ? i + 1 : i;
            double nextX = originX + chain[node * 3];
            double nextY = originY + chain[node * 3 + 1];
            double nextZ = originZ + chain[node * 3 + 2];

            double deltaX = nextX - currentX;
            double deltaY = nextY - currentY;
            double deltaZ = nextZ - currentZ;

            if (!canCross(dir, cX, cY, cZ, currentX, currentZ, nextX, nextZ, chainDef, route))
                break;

            double len = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
            if (len >= distRemaining) {
                info.overshoot = 0;
                double newYaw = EntityRailCarBase.generateYaw(nextX, nextZ, currentX, currentZ);
                if (Math.abs(BobMathUtil.angularDifference(newYaw, moveAngle)) < 45)
                    info.yaw = (float) newYaw;
                else info.yaw = (float) moveAngle;
                double unit = len < 1.0E-4D ? 0 : distRemaining / len;

                info.at(
                        currentX + deltaX * unit,
                        currentY + deltaY * unit,
                        currentZ + deltaZ * unit);
                return;
            }

            distRemaining -= len;
            currentX = nextX;
            currentY = nextY;
            currentZ = nextZ;
        }

        info.overshoot = distRemaining;
        info.pos(Mth.floor(currentX), Mth.floor(currentY), Mth.floor(currentZ));
        info.at(currentX, currentY, currentZ);
    }

    public class RailDef {
        final String name;
        public List<Vec3> nodes = new ArrayList<>();

        public RailDef(String name) {
            this.name = name;
        }
    }
}
