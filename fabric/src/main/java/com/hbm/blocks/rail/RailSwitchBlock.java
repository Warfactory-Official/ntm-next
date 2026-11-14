/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.blocks.rail;

import com.hbm.blockentity.RailBlockEntity;
import com.hbm.items.special.TrainItem;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class RailSwitchBlock extends NtmRailBlock {

    private static final int MAIN = 0;
    private static final int SIDE = 1;
    private static final int UNKNOWN = -1;
    private static final int FROG = 7;
    private static final double RIDE = 0.1875;
    private static final double[] MAIN_PATH = {-8.5, 0.5, -7.5, 0.5, 6.5, 0.5, 7.5, 0.5, 8.5, 0.5};
    private static final double[] CLOCKWISE_SIDE_PATH = {
        -8.5, 4.5, -7.5, 4.5, -6.5, 4.5, -5.5, 4.5, -4.5, 4.5, -3.5, 4.5, -2.5, 4.5, -1.5, 4.5,
        -0.5, 4.25, 0.5, 3.9375, 1.5, 3.375, 2.5, 2.4625, 3.5, 1.75, 4.5, 1.1875, 5.5, 0.875, 6.5,
        0.625, 7.5, 0.5, 8.5, 0.5
    };
    private static final double[] COUNTER_CLOCKWISE_SIDE_PATH = {
        -8.5, -3.5, -7.5, -3.5, -6.5, -3.5, -5.5, -3.5, -4.5, -3.5, -3.5, -3.5, -2.5, -3.5, -1.5,
        -3.5, -0.5, -3.25, 0.5, -2.9375, 1.5, -2.375, 2.5, -1.4625, 3.5, -0.75, 4.5, -0.1875, 5.5,
        0.175, 6.5, 0.375, 7.5, 0.5, 8.5, 0.5
    };
    private static final ThreadLocal<BlockPos.MutableBlockPos> CORE =
            ThreadLocal.withInitial(BlockPos.MutableBlockPos::new);

    private final boolean mirrored;
    private final RailPolyline[][] paths = new RailPolyline[4][];

    private RailSwitchBlock(Properties props, boolean mirrored) {
        super(props);
        this.mirrored = mirrored;
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            paths[facing.get2DDataValue()] =
                    new RailPolyline[] {
                        laid(MAIN_PATH, facing),
                        laid(mirrored ? COUNTER_CLOCKWISE_SIDE_PATH : CLOCKWISE_SIDE_PATH, facing)
                    };
        }
    }

    public static RailSwitchBlock clockwise(Properties props) {
        return new RailSwitchBlock(props, false);
    }

    public static RailSwitchBlock counterClockwise(Properties props) {
        return new RailSwitchBlock(props, true);
    }

    public boolean mirrored() {
        return mirrored;
    }

    private static RailPolyline laid(double[] path, Direction facing) {
        Direction clockwise = facing.getClockWise();
        var nodes = new ArrayList<Vec3>(path.length / 2);
        for (int i = 0; i < path.length; i += 2) {
            double ahead = path[i], aside = path[i + 1];
            nodes.add(
                    new Vec3(
                            ahead * facing.getStepX() + aside * clockwise.getStepX(),
                            RIDE,
                            ahead * facing.getStepZ() + aside * clockwise.getStepZ()));
        }
        return new RailPolyline(nodes, 0F);
    }

    @Override
    public RailGauge gauge(Level level, int x, int y, int z) {
        return RailGauge.STANDARD;
    }

    @Override
    public void travel(
            Level level,
            int x,
            int y,
            int z,
            double trainX,
            double trainY,
            double trainZ,
            double headingX,
            double headingY,
            double headingZ,
            double speed,
            RailStep step,
            double collisionReach) {
        Owner owner = step.owner;
        Direction facing = owner.facing;
        boolean alongX = facing.getAxis() == Direction.Axis.X;
        int sense = facing.getAxisDirection().getStep();
        double frog = (alongX ? owner.coreX : owner.coreZ) + 0.5 + FROG * sense;
        int route =
                level.getBlockEntity(CORE.get().set(owner.coreX, owner.coreY, owner.coreZ))
                                instanceof RailBlockEntity rail
                        ? rail.isSwitched ? SIDE : MAIN
                        : UNKNOWN;
        double originX = owner.coreX + 0.5, originY = owner.coreY, originZ = owner.coreZ + 0.5;
        RailPolyline[] laid = paths[facing.get2DDataValue()];
        double trainAlong = alongX ? trainX : trainZ;

        int nearestPath = -1, nearestLink = 0;
        double nearest = Double.POSITIVE_INFINITY, snapX = trainX, snapY = trainY, snapZ = trainZ;
        boolean backward = false;
        for (int path = MAIN; path <= SIDE; path++) {
            RailPolyline line = laid[path];
            for (int link = 0; link < line.links(); link++) {
                double t = Mth.clamp(line.project(link, originX, originZ, trainX, trainZ), 0, 1);
                double qx = line.along(link, RailPolyline.X, originX, t);
                double qy = line.along(link, RailPolyline.Y, originY, t);
                double qz = line.along(link, RailPolyline.Z, originZ, t);
                if (!crosses(path, route, sense, frog, trainAlong, alongX ? qx : qz)) continue;
                double linkX =
                        line.node(link + 1, RailPolyline.X, 0) - line.node(link, RailPolyline.X, 0);
                double linkZ =
                        line.node(link + 1, RailPolyline.Z, 0) - line.node(link, RailPolyline.Z, 0);
                if (linkX * headingX + linkZ * headingZ < 0) backward = true;
                double dx = qx - trainX, dy = qy - trainY, dz = qz - trainZ;
                double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (distance < nearest) {
                    nearest = distance;
                    nearestPath = path;
                    nearestLink = link;
                    snapX = qx;
                    snapY = qy;
                    snapZ = qz;
                }
            }
        }
        if (nearestPath < 0) {
            step.reach(trainX, trainY, trainZ);
            return;
        }

        RailPolyline line = laid[nearestPath];
        double atX = snapX, atY = snapY, atZ = snapZ, left = speed;
        int stride = backward ? -1 : 1;
        for (int node = backward ? nearestLink : nearestLink + 1;
                node >= 0 && node <= line.links();
                node += stride) {
            double nodeX = line.node(node, RailPolyline.X, originX);
            double nodeY = line.node(node, RailPolyline.Y, originY);
            double nodeZ = line.node(node, RailPolyline.Z, originZ);
            if (!crosses(
                    nearestPath, route, sense, frog, alongX ? atX : atZ, alongX ? nodeX : nodeZ))
                break;
            double dx = nodeX - atX, dy = nodeY - atY, dz = nodeZ - atZ;
            double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (length < left) {
                left -= length;
                atX = nodeX;
                atY = nodeY;
                atZ = nodeZ;
                continue;
            }
            if (length > 0) {
                double share = left / length;
                atX += dx * share;
                atY += dy * share;
                atZ += dz * share;
            }
            step.reach(atX, atY, atZ);
            return;
        }
        step.reach(atX, atY, atZ)
                .leftover(left)
                .exitAt(Mth.floor(atX), Mth.floor(atY), Mth.floor(atZ));
    }

    private static boolean crosses(
            int path, int route, int sense, double frog, double from, double to) {
        return route == UNKNOWN
                || (to - from) * sense > 0
                || (to - frog) * sense < 0
                || path == route;
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (player.isSecondaryUseActive()) return InteractionResult.SUCCESS;
        if (player.getMainHandItem().getItem() instanceof TrainItem) return InteractionResult.PASS;
        if (!level.isClientSide() && level.getBlockEntity(core) instanceof RailBlockEntity rail)
            rail.toggleSwitch();
        return InteractionResult.SUCCESS;
    }
}
