// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler.neutron;

import com.hbm.blocks.machine.rbmk.RBMKBase;
import com.hbm.handler.neutron.NeutronNodeWorld.StreamWorld;
import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.tileentity.machine.rbmk.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class RBMKNeutronHandler {

    static double moderatorEfficiency;
    static double reflectorEfficiency;
    static double absorberEfficiency;
    static int columnHeight;
    static int fluxRange;

    private static BlockEntity blockPosToTE(Level world, BlockPos pos) {
        return world.getBlockEntity(pos);
    }

    public static RBMKNeutronNode makeNode(StreamWorld streamWorld, BlockEntityRBMKBase tile) {
        NeutronNode node = streamWorld.getNode(tile.getBlockPos());
        return node != null
                ? (RBMKNeutronNode) node
                : new RBMKNeutronNode(tile, tile.getRBMKType(), tile.hasLid());
    }

    public enum RBMKType {
        ROD,
        MODERATOR,
        CONTROL_ROD,
        REFLECTOR,
        ABSORBER,
        OUTGASSER,
        OTHER
    }

    public static class RBMKNeutronNode extends NeutronNode {

        public static final Key<Boolean> HAS_LID = new Key<>("hasLid", Boolean.class);
        public static final Key<RBMKType> COLUMN_TYPE = new Key<>("type", RBMKType.class);

        public RBMKNeutronNode(BlockEntityRBMKBase tile, RBMKType type, boolean hasLid) {
            super(tile, NeutronStream.NeutronType.RBMK);
            set(HAS_LID, hasLid);
            set(COLUMN_TYPE, type);
        }

        public void addLid() {
            set(HAS_LID, true);
        }

        public void removeLid() {
            set(HAS_LID, false);
        }

        public List<BlockPos> checkNode(StreamWorld streamWorld) {
            List<BlockPos> list = new ArrayList<>();
            BlockPos pos = tile.getBlockPos();

            RBMKNeutronStream[] streams = new RBMKNeutronStream[BlockEntityRBMKRod.fluxDirs.length];
            for (int i = 0; i < streams.length; i++) {
                streams[i] = new RBMKNeutronStream(this, BlockEntityRBMKRod.fluxDirs[i]);
            }

            if (tile instanceof BlockEntityRBMKRod rod
                    && (!rod.hasRod || rod.lastFluxQuantity == 0)) {
                for (RBMKNeutronStream stream : streams) {
                    for (NeutronNode node : stream.getNodes(streamWorld, false)) {
                        if (node != null) list.add(node.tile.getBlockPos());
                    }
                }
                return list;
            }

            for (RBMKNeutronStream stream : streams) {
                for (NeutronNode node : stream.getNodes(streamWorld, false)) {
                    if (node != null && node.tile instanceof BlockEntityRBMKRod) return list;
                }
            }

            list.add(pos);
            return list;
        }
    }

    public static class RBMKNeutronStream extends NeutronStream {

        public RBMKNeutronStream(NeutronNode origin, Vec3 vector) {
            super(origin, vector);
        }

        public RBMKNeutronStream(NeutronNode origin, Vec3 vector, double flux, double ratio) {
            super(origin, vector, flux, ratio, NeutronType.RBMK);
        }

        private static boolean neutronOpaque(BlockState state) {
            return state.canOcclude();
        }

        public NeutronNode[] getNodes(StreamWorld streamWorld, boolean addNode) {
            NeutronNode[] positions = new RBMKNeutronNode[fluxRange];
            BlockPos ori = origin.tile.getBlockPos();
            Level world = origin.tile.getLevel();

            for (int i = 1; i <= fluxRange; i++) {
                int x = (int) Math.floor(0.5 + vector.x * i);
                int z = (int) Math.floor(0.5 + vector.z * i);
                BlockPos pos = new BlockPos(ori.getX() + x, ori.getY(), ori.getZ() + z);

                NeutronNode node = streamWorld.getNode(pos);
                if (node instanceof RBMKNeutronNode) {
                    positions[i - 1] = node;
                } else if (origin.tile.getBlockState().getBlock() instanceof RBMKBase) {
                    if (blockPosToTE(world, pos) instanceof BlockEntityRBMKBase rbmkBase) {
                        node = makeNode(streamWorld, rbmkBase);
                        positions[i - 1] = node;
                        if (addNode) streamWorld.addNode(node);
                    }
                }
            }
            return positions;
        }

        public void runStreamInteraction(Level worldObj, StreamWorld streamWorld) {

            if (fluxQuantity == 0D) return;

            BlockPos originPos = origin.tile.getBlockPos();

            BlockEntityRBMKBase originTE;
            NeutronNode node = streamWorld.getNode(originPos);
            if (node != null) {
                originTE = (BlockEntityRBMKBase) node.tile;
            } else {
                if (!(blockPosToTE(worldObj, originPos) instanceof BlockEntityRBMKBase b)) return;
                originTE = b;
                streamWorld.addNode(
                        new RBMKNeutronNode(originTE, originTE.getRBMKType(), originTE.hasLid()));
            }

            int moderatedCount = 0;
            Iterator<BlockPos> iterator = getBlocks(fluxRange);

            while (iterator.hasNext()) {
                BlockPos targetPos = iterator.next();
                if (fluxQuantity == 0D) return;

                NeutronNode targetNode = streamWorld.getNode(targetPos);
                if (targetNode == null) {
                    if (blockPosToTE(worldObj, targetPos) instanceof BlockEntityRBMKBase rbmk) {
                        targetNode = makeNode(streamWorld, rbmk);
                        streamWorld.addNode(targetNode);
                    } else {
                        int hits = getHits(targetPos);
                        if (hits == columnHeight) return;
                        else if (hits > 0) {
                            irradiateFromFlux(originPos, hits);
                            fluxQuantity *= 1 - ((double) hits / columnHeight);
                            continue;
                        } else {
                            irradiateFromFlux(originPos, 0);
                            continue;
                        }
                    }
                }

                RBMKType type = targetNode.get(RBMKNeutronNode.COLUMN_TYPE);
                if (type == RBMKType.OTHER || type == null) continue;

                BlockEntityRBMKBase nodeTE = (BlockEntityRBMKBase) targetNode.tile;

                if (Boolean.FALSE.equals(targetNode.get(RBMKNeutronNode.HAS_LID))) {
                    RadiationSystemNT.incrementRad(
                            (ServerLevel) worldObj,
                            targetPos,
                            this.fluxQuantity * 0.05D,
                            this.fluxQuantity * 0.05D * 1024D);
                }

                if (type == RBMKType.MODERATOR || nodeTE.isModerated()) {
                    moderatedCount++;
                    moderateStream();
                }

                if (nodeTE instanceof IRBMKFluxReceiver column) {

                    if (type == RBMKType.ROD) {
                        BlockEntityRBMKRod rod = (BlockEntityRBMKRod) column;
                        if (rod.hasRod) {
                            rod.receiveFlux(this);
                            return;
                        }
                    } else if (type == RBMKType.OUTGASSER) {

                        BlockEntityRBMKOutgasser outgasser = (BlockEntityRBMKOutgasser) column;
                        if (outgasser.canProcess()) {
                            outgasser.receiveFlux(this);
                            return;
                        }
                    }

                } else if (type == RBMKType.CONTROL_ROD) {
                    BlockEntityRBMKControl rod = (BlockEntityRBMKControl) nodeTE;
                    if (rod.level > 0.0D) {
                        this.fluxQuantity *= rod.getMult();
                        continue;
                    }
                    return;
                } else if (type == RBMKType.REFLECTOR) {
                    if (originTE.isModerated()) moderatedCount++;
                    if (this.fluxRatio > 0 && moderatedCount > 0) {
                        for (int i = 0; i < moderatedCount; i++) moderateStream();
                    }
                    if (reflectorEfficiency != 1.0D) {
                        this.fluxQuantity *= reflectorEfficiency;
                        continue;
                    }
                    if (originTE instanceof BlockEntityRBMKRod rod) rod.receiveFlux(this);
                    return;
                } else if (type == RBMKType.ABSORBER) {
                    nodeTE.heat +=
                            RBMKConfig.getAbsorberHeatConversion(worldObj) * this.fluxQuantity;
                    if (absorberEfficiency == 1) return;
                    this.fluxQuantity *= absorberEfficiency;
                }
            }

            NeutronNode[] nodes = getNodes(streamWorld, true);
            NeutronNode lastNode = nodes[nodes.length - 1];

            if (lastNode == null) {
                irradiateFromFlux(
                        new BlockPos(
                                (int) (originPos.getX() + this.vector.x),
                                originPos.getY(),
                                (int) (originPos.getZ() + this.vector.z)));
                return;
            }

            RBMKType lastNodeType = lastNode.get(RBMKNeutronNode.COLUMN_TYPE);
            if (lastNodeType == RBMKType.CONTROL_ROD) {
                BlockEntityRBMKControl rod = (BlockEntityRBMKControl) lastNode.tile;
                if (rod.getMult() > 0.0D) {
                    this.fluxQuantity *= rod.getMult();
                    BlockPos posAfter =
                            new BlockPos(
                                    (int) (lastNode.tile.getBlockPos().getX() + this.vector.x),
                                    lastNode.tile.getBlockPos().getY(),
                                    (int) (lastNode.tile.getBlockPos().getZ() + this.vector.z));

                    if (NeutronNodeWorld.getNode(worldObj, originPos) == null) {
                        if (blockPosToTE(worldObj, posAfter) instanceof BlockEntityRBMKBase b) {
                            StreamWorld sw = NeutronNodeWorld.getOrAddWorld(worldObj);
                            sw.addNode(makeNode(sw, b));
                        } else {
                            irradiateFromFlux(posAfter);
                        }
                    }
                }
            }
        }

        public int getHits(BlockPos pos) {
            int hits = 0;
            Level world = origin.tile.getLevel();
            for (int h = 0; h < columnHeight; h++) {
                if (neutronOpaque(world.getBlockState(pos.above(h)))) hits += 1;
            }
            return hits;
        }

        public void irradiateFromFlux(BlockPos pos) {
            double amount = fluxQuantity * 0.05D * (1 - (double) getHits(pos) / columnHeight);
            RadiationSystemNT.incrementRad(
                    (ServerLevel) origin.tile.getLevel(),
                    pos,
                    amount,
                    Math.abs(amount) * 1024D + 1D);
        }

        public void irradiateFromFlux(BlockPos pos, int hits) {
            double amount = fluxQuantity * 0.05D * (1 - (double) hits / columnHeight);
            RadiationSystemNT.incrementRad(
                    (ServerLevel) origin.tile.getLevel(),
                    pos,
                    amount,
                    Math.abs(amount) * 1024D + 1D);
        }

        public void moderateStream() {
            fluxRatio *= (1 - moderatorEfficiency);
        }
    }
}
