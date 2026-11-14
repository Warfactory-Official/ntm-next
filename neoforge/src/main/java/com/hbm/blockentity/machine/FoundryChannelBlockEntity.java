/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.blockentity.machine;

import com.hbm.api.block.MoltenMetalHandler;
import com.hbm.api.foundry.FoundryChannelGraph;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.material.Material;
import com.hbm.inventory.material.MaterialShape;
import com.hbm.inventory.material.MaterialStack;
import com.hbm.uninos.graph.GraphNode;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.hbm.uninos.graph.NodeNetwork;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class FoundryChannelBlockEntity extends FoundryBaseBlockEntity {
    private static final int[][] FLOW_PERMUTATIONS = permutations();
    private final int[] flowOrder = new int[4];
    public int nextUpdate;
    public int lastFlow = 0;
    private @Nullable Material nodeMaterial;
    private boolean nodeMaterialSeeded;

    public FoundryChannelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FOUNDRY_CHANNEL.get(), pos, state);
    }

    private static int[][] permutations() {
        int[] base = {
            Direction.NORTH.get3DDataValue(),
            Direction.SOUTH.get3DDataValue(),
            Direction.WEST.get3DDataValue(),
            Direction.EAST.get3DDataValue()
        };
        int[][] out = new int[24][];
        int n = 0;
        for (int a = 0; a < 4; a++) {
            for (int b = 0; b < 4; b++) {
                if (b == a) continue;
                for (int c = 0; c < 4; c++) {
                    if (c == a || c == b) continue;
                    int d = 6 - a - b - c;
                    out[n++] = new int[] {base[a], base[b], base[c], base[d]};
                }
            }
        }
        return out;
    }

    private static Reference2IntOpenHashMap<Material> census(NodeNetwork<@Nullable Material> net) {
        if (net.attachment instanceof Reference2IntOpenHashMap<?> raw) {
            @SuppressWarnings("unchecked")
            Reference2IntOpenHashMap<Material> cached = (Reference2IntOpenHashMap<Material>) raw;
            return cached;
        }
        Reference2IntOpenHashMap<Material> fresh = new Reference2IntOpenHashMap<>();
        for (GraphNode<@Nullable Material> member : net.nodes) {
            if (member.data != null) fresh.addTo(member.data, 1);
        }
        net.attachment = fresh;
        return fresh;
    }

    @Override
    public void tickServer() {
        if (!nodeMaterialSeeded) seedNodeMaterial();

        if (nodeMaterial != null && this.amount == 0) {
            setChannelMaterial(null);
        }

        if (this.type == null && this.amount != 0) {
            this.hbm$syncSet$FoundryBaseBlockEntity$amount(0);
            markChanged();
        }

        nextUpdate--;

        if (nextUpdate <= 0 && this.amount > 0 && this.type != null) {

            boolean hasOp = false;
            nextUpdate = 5;

            int[] dirs = nextFlowOrder();

            for (int i = 0; i < dirs.length; i++) {
                Direction dir = Direction.from3DDataValue(dirs[i]);
                BlockPos target = worldPosition.relative(dir);
                Block b = level.getBlockState(target).getBlock();
                MoltenMetalHandler acc = acceptorAt(dir, target);

                if (acc != null && b != ModBlocks.FOUNDRY_CHANNEL.get()) {
                    if (acc.canAcceptFlow(
                            level,
                            target,
                            dir.getOpposite(),
                            new MaterialStack(this.type, this.amount))) {
                        MaterialStack left =
                                acc.acceptFlow(
                                        level,
                                        target,
                                        dir.getOpposite(),
                                        new MaterialStack(this.type, this.amount));
                        if (left == null) {
                            this.hbm$syncSet$FoundryBaseBlockEntity$type(null);
                            this.hbm$syncSet$FoundryBaseBlockEntity$amount(0);
                            setChannelMaterial(null);
                        } else {
                            this.hbm$syncSet$FoundryBaseBlockEntity$amount(left.amount);
                        }
                        markChanged();
                        hasOp = true;
                        break;
                    }
                }
            }

            if (!hasOp) {
                for (int i = 0; i < dirs.length; i++) {
                    Direction dir = Direction.from3DDataValue(dirs[i]);
                    BlockEntity b = level.getBlockEntity(worldPosition.relative(dir));

                    if (b instanceof FoundryChannelBlockEntity acc) {
                        if (acc.type == null || acc.type == this.type || acc.amount == 0) {
                            int flow = dir.getOpposite().get3DDataValue();
                            if (acc.type != this.type || acc.lastFlow != flow) acc.markChanged();
                            acc.hbm$syncSet$FoundryBaseBlockEntity$type(this.type);
                            acc.setChannelMaterial(this.type);

                            acc.lastFlow = flow;
                            int before = this.amount;

                            if (level.getRandom().nextInt(5) == 0 || this.amount == 1) {
                                int buf = this.amount;
                                this.hbm$syncSet$FoundryBaseBlockEntity$amount(acc.amount);
                                acc.hbm$syncSet$FoundryBaseBlockEntity$amount(buf);
                            } else {
                                int diff = this.amount - acc.amount;
                                if (diff > 0) {
                                    diff /= 2;
                                    this.hbm$syncSet$FoundryBaseBlockEntity$amount(
                                            (int) (this.amount - (diff)));
                                    acc.hbm$syncSet$FoundryBaseBlockEntity$amount(
                                            (int) (acc.amount + (diff)));
                                }
                            }
                            if (this.amount != before) {
                                markChanged();
                                acc.markChanged();
                            }
                        }
                    }
                }
            }
        }

        if (this.amount == 0) {
            if (this.lastFlow != 0) markChanged();
            this.lastFlow = 0;
            this.nextUpdate = 5;
        }

        super.tickServer();
    }

    private int[] nextFlowOrder() {
        int[] perm = FLOW_PERMUTATIONS[level.getRandom().nextInt(FLOW_PERMUTATIONS.length)];
        System.arraycopy(perm, 0, flowOrder, 0, flowOrder.length);
        if (lastFlow > 0) {
            for (int i = 0; i < flowOrder.length; i++) {
                if (flowOrder[i] != lastFlow) continue;
                flowOrder[i] = flowOrder[flowOrder.length - 1];
                flowOrder[flowOrder.length - 1] = lastFlow;
                break;
            }
        }
        return flowOrder;
    }

    private void seedNodeMaterial() {
        if (!(level instanceof ServerLevel sl)) return;
        nodeMaterial = FoundryChannelGraph.get(sl).dataAt(worldPosition.asLong());
        nodeMaterialSeeded = true;
    }

    private void setChannelMaterial(@Nullable Material material) {
        if (nodeMaterialSeeded && nodeMaterial == material) return;
        if (!(level instanceof ServerLevel server)) return;
        LevelNodeGraph<@Nullable Material> graph = FoundryChannelGraph.get(server);
        GraphNode<@Nullable Material> node = graph.getNode(worldPosition.asLong());
        if (node == null) return;
        Material previous = node.data;
        nodeMaterial = material;
        nodeMaterialSeeded = true;
        if (previous == material) return;
        node.data = material;
        graph.setDirty();
        NodeNetwork<@Nullable Material> net = node.net;
        if (net == null || !(net.attachment instanceof Reference2IntOpenHashMap<?> raw)) return;
        @SuppressWarnings("unchecked")
        Reference2IntOpenHashMap<Material> census = (Reference2IntOpenHashMap<Material>) raw;
        if (previous != null && census.addTo(previous, -1) <= 1) census.removeInt(previous);
        if (material != null) census.addTo(material, 1);
    }

    @Override
    public int getCapacity() {
        return MaterialShape.INGOT.amount(2);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.lastFlow = input.getIntOr("flow", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("flow", this.lastFlow);
    }

    @Override
    public boolean canAcceptPour(
            Level level, BlockPos pos, Vec3 hit, Direction side, MaterialStack stack) {
        if (!(level instanceof ServerLevel sl)) return false;
        LevelNodeGraph<@Nullable Material> graph = FoundryChannelGraph.get(sl);
        NodeNetwork<@Nullable Material> net = graph.networkAt(worldPosition.asLong());
        if (net == null) return false;

        Reference2IntOpenHashMap<Material> census = census(net);
        if (!census.isEmpty() && !(census.size() == 1 && census.containsKey(stack.material)))
            return false;

        return super.canAcceptPour(level, pos, hit, side, stack);
    }

    @Override
    public MaterialStack acceptFlow(
            Level level, BlockPos pos, Direction side, MaterialStack stack) {
        setChannelMaterial(stack.material);
        return super.acceptFlow(level, pos, side, stack);
    }

    @Override
    public MaterialStack acceptPour(
            Level level, BlockPos pos, Vec3 hit, Direction side, MaterialStack stack) {
        setChannelMaterial(stack.material);
        return super.acceptPour(level, pos, hit, side, stack);
    }
}
