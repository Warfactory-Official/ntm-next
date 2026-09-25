// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.block.ICrucibleAcceptor;
import com.hbm.api.foundry.FoundryChannelGraph;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.inventory.material.NTMMaterial;
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

public class BlockEntityFoundryChannel extends BlockEntityFoundryBase {

    private static final int[][] FLOW_PERMUTATIONS = permutations();

    private final int[] flowOrder = new int[4];
    public int nextUpdate;
    public int lastFlow = 0;

    private @Nullable NTMMaterial nodeMaterial;
    private boolean nodeMaterialSeeded;

    public BlockEntityFoundryChannel(BlockPos pos, BlockState state) {
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

    private static Reference2IntOpenHashMap<NTMMaterial> census(
            NodeNetwork<@Nullable NTMMaterial> net) {
        if (net.attachment instanceof Reference2IntOpenHashMap<?> raw) {
            @SuppressWarnings("unchecked")
            Reference2IntOpenHashMap<NTMMaterial> cached =
                    (Reference2IntOpenHashMap<NTMMaterial>) raw;
            return cached;
        }
        Reference2IntOpenHashMap<NTMMaterial> fresh = new Reference2IntOpenHashMap<>();
        for (GraphNode<@Nullable NTMMaterial> member : net.nodes) {
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
            this.amount = 0;
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
                ICrucibleAcceptor acc = acceptorAt(dir, target);

                if (acc != null && b != ModBlocks.FOUNDRY_CHANNEL.get()) {
                    if (acc.canAcceptPartialFlow(
                            level,
                            target,
                            dir.getOpposite(),
                            new MaterialStack(this.type, this.amount))) {
                        MaterialStack left =
                                acc.flow(
                                        level,
                                        target,
                                        dir.getOpposite(),
                                        new MaterialStack(this.type, this.amount));
                        if (left == null) {
                            this.type = null;
                            this.amount = 0;
                            setChannelMaterial(null);
                        } else {
                            this.amount = left.amount;
                        }
                        hasOp = true;
                        break;
                    }
                }
            }

            if (!hasOp) {
                for (int i = 0; i < dirs.length; i++) {
                    Direction dir = Direction.from3DDataValue(dirs[i]);
                    BlockEntity b = level.getBlockEntity(worldPosition.relative(dir));

                    if (b instanceof BlockEntityFoundryChannel acc) {
                        if (acc.type == null || acc.type == this.type || acc.amount == 0) {
                            acc.type = this.type;
                            acc.setChannelMaterial(this.type);

                            acc.lastFlow = dir.getOpposite().get3DDataValue();

                            if (level.getRandom().nextInt(5) == 0 || this.amount == 1) {
                                int buf = this.amount;
                                this.amount = acc.amount;
                                acc.amount = buf;
                            } else {
                                int diff = this.amount - acc.amount;
                                if (diff > 0) {
                                    diff /= 2;
                                    this.amount -= diff;
                                    acc.amount += diff;
                                }
                            }
                        }
                    }
                }
            }
        }

        if (this.amount == 0) {
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

    private @Nullable GraphNode<@Nullable NTMMaterial> node() {
        if (!(level instanceof ServerLevel sl)) return null;

        return FoundryChannelGraph.get(sl).getNode(worldPosition.asLong());
    }

    private void seedNodeMaterial() {
        if (!(level instanceof ServerLevel sl)) return;
        nodeMaterial = FoundryChannelGraph.get(sl).dataAt(worldPosition.asLong());
        nodeMaterialSeeded = true;
    }

    private void setChannelMaterial(@Nullable NTMMaterial material) {
        if (nodeMaterialSeeded && nodeMaterial == material) return;
        GraphNode<@Nullable NTMMaterial> node = node();
        if (node == null) return;
        NTMMaterial previous = node.data;
        nodeMaterial = material;
        nodeMaterialSeeded = true;
        if (previous == material) return;
        node.data = material;
        NodeNetwork<@Nullable NTMMaterial> net = node.net;
        if (net == null || !(net.attachment instanceof Reference2IntOpenHashMap<?> raw)) return;
        @SuppressWarnings("unchecked")
        Reference2IntOpenHashMap<NTMMaterial> census = (Reference2IntOpenHashMap<NTMMaterial>) raw;
        if (previous != null && census.addTo(previous, -1) <= 1) census.removeInt(previous);
        if (material != null) census.addTo(material, 1);
    }

    @Override
    public int getCapacity() {
        return MaterialShapes.INGOT.q(2);
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
    public boolean canAcceptPartialPour(
            Level level, BlockPos pos, Vec3 hit, Direction side, MaterialStack stack) {
        if (!(level instanceof ServerLevel sl)) return false;
        LevelNodeGraph<@Nullable NTMMaterial> graph = FoundryChannelGraph.get(sl);
        NodeNetwork<@Nullable NTMMaterial> net = graph.networkAt(worldPosition.asLong());
        if (net == null) return false;

        Reference2IntOpenHashMap<NTMMaterial> census = census(net);
        if (!census.isEmpty() && !(census.size() == 1 && census.containsKey(stack.material)))
            return false;

        return super.canAcceptPartialPour(level, pos, hit, side, stack);
    }

    @Override
    public MaterialStack flow(Level level, BlockPos pos, Direction side, MaterialStack stack) {
        setChannelMaterial(stack.material);
        return super.flow(level, pos, side, stack);
    }

    @Override
    public MaterialStack pour(
            Level level, BlockPos pos, Vec3 hit, Direction side, MaterialStack stack) {
        setChannelMaterial(stack.material);
        return super.pour(level, pos, hit, side, stack);
    }
}
