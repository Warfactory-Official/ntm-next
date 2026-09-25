// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler.neutron;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class NeutronNodeWorld {

    public static final HashMap<Level, StreamWorld> streamWorlds = new HashMap<>();

    public static NeutronNode getNode(Level world, BlockPos pos) {
        StreamWorld streamWorld = streamWorlds.get(world);
        return streamWorld != null ? streamWorld.nodeCache.get(pos) : null;
    }

    public static void removeNode(Level world, BlockPos pos) {
        StreamWorld streamWorld = streamWorlds.get(world);
        if (streamWorld == null) return;
        streamWorld.removeNode(pos);
    }

    public static StreamWorld getOrAddWorld(Level world) {
        return streamWorlds.computeIfAbsent(world, w -> new StreamWorld());
    }

    public static void removeAllWorlds() {
        streamWorlds.clear();
    }

    public static void removeEmptyWorlds() {
        streamWorlds.values().removeIf(streamWorld -> streamWorld.streams.isEmpty());
    }

    public static class StreamWorld {

        private final List<NeutronStream> streams = new ArrayList<>();
        private final HashMap<BlockPos, NeutronNode> nodeCache = new HashMap<>();

        public void runStreamInteractions(Level world) {
            for (NeutronStream stream : streams) {
                stream.runStreamInteraction(world, this);
            }
        }

        public void addStream(NeutronStream stream) {
            streams.add(stream);
        }

        public void removeAllStreams() {
            streams.clear();
        }

        public void cleanNodes() {
            List<BlockPos> toRemove = new ArrayList<>();
            for (NeutronNode cachedNode : nodeCache.values()) {
                if (cachedNode.type == NeutronStream.NeutronType.RBMK) {
                    toRemove.addAll(
                            ((RBMKNeutronHandler.RBMKNeutronNode) cachedNode).checkNode(this));
                }
            }
            for (BlockPos pos : toRemove) {
                nodeCache.remove(pos);
            }
        }

        public NeutronNode getNode(BlockPos pos) {
            return nodeCache.get(pos);
        }

        public void addNode(NeutronNode node) {
            nodeCache.put(node.pos, node);
        }

        public void removeNode(BlockPos pos) {
            nodeCache.remove(pos);
        }
    }
}
