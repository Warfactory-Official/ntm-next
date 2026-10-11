// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.network;

import com.hbm.api.fluidmk2.*;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockMaskTable;
import com.hbm.capability.MachineCaps;
import com.hbm.capability.NtmCapabilities.CapRole;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.items.ModDataComponents;
import com.hbm.uninos.graph.GraphNode;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.hbm.uninos.graph.PhysicalGraph;
import com.hbm.util.AutoRotate;
import com.hbm.util.ChunkUtil;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

@AutoRotate
public class FluidPipeBlock extends FluidDuctBlockBase implements SimpleWaterloggedBlock {

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

    private static final VoxelShape[] SHAPES = buildShapes();

    public FluidPipeBlock(BlockBehaviour.Properties props) {
        super(props);
        registerDefaultState(
                stateDefinition
                        .any()
                        .setValue(NORTH, false)
                        .setValue(EAST, false)
                        .setValue(SOUTH, false)
                        .setValue(WEST, false)
                        .setValue(UP, false)
                        .setValue(DOWN, false)
                        .setValue(WATERLOGGED, false));
    }

    private static VoxelShape[] buildShapes() {
        VoxelShape core = Shapes.box(5 / 16D, 5 / 16D, 5 / 16D, 11 / 16D, 11 / 16D, 11 / 16D);
        VoxelShape[] arm = new VoxelShape[6];
        arm[Direction.DOWN.ordinal()] =
                Shapes.box(5 / 16D, 0, 5 / 16D, 11 / 16D, 5 / 16D, 11 / 16D);
        arm[Direction.UP.ordinal()] = Shapes.box(5 / 16D, 11 / 16D, 5 / 16D, 11 / 16D, 1, 11 / 16D);
        arm[Direction.NORTH.ordinal()] =
                Shapes.box(5 / 16D, 5 / 16D, 0, 11 / 16D, 11 / 16D, 5 / 16D);
        arm[Direction.SOUTH.ordinal()] =
                Shapes.box(5 / 16D, 5 / 16D, 11 / 16D, 11 / 16D, 11 / 16D, 1);
        arm[Direction.WEST.ordinal()] =
                Shapes.box(0, 5 / 16D, 5 / 16D, 5 / 16D, 11 / 16D, 11 / 16D);
        arm[Direction.EAST.ordinal()] =
                Shapes.box(11 / 16D, 5 / 16D, 5 / 16D, 1, 11 / 16D, 11 / 16D);

        VoxelShape[] shapes = new VoxelShape[64];
        for (int mask = 0; mask < 64; mask++) {
            VoxelShape shape = core;
            for (int o = 0; o < 6; o++) {
                if (mask == 0 || (mask & (1 << o)) != 0) shape = Shapes.or(shape, arm[o]);
            }
            shapes[mask] = shape;
        }
        return shapes;
    }

    protected static BooleanProperty propertyFor(Direction dir) {
        return PipeBlock.PROPERTY_BY_DIRECTION.get(dir);
    }

    public static Fluid stampedFluid(ItemStack stack) {
        FluidStackNTM content = stack.get(ModDataComponents.FLUID_CONTENT.get());
        return content == null ? Fluids.EMPTY : content.type();
    }

    public static Fluid pipeFluidAt(LevelReader level, BlockPos pos) {
        if (level instanceof ServerLevel sl) {
            PipeData data = FluidPipeGraph.dataAt(sl, pos.asLong());
            return data != null ? data.fluid() : Fluids.EMPTY;
        }
        if (level instanceof Level l && l.isClientSide()) {
            return FluidPipeTintData.fluidAt(l, pos);
        }
        return Fluids.EMPTY;
    }

    public static boolean retypeNode(ServerLevel level, long key, Fluid fluid) {
        if (retypeNodeNoSync(level, key, fluid)) {
            FluidPipeTintData.sync(level, BlockPos.of(key), fluid);
            return true;
        }
        return false;
    }

    public static boolean retypeNodeNoSync(ServerLevel level, long key, Fluid fluid) {
        try (var batch = PhysicalGraph.begin(level)) {
            LevelNodeGraph<PipeData> from = FluidPipeGraph.graphAt(level, key);
            if (from == null) return false;
            GraphNode<PipeData> node = from.firstClass(key);
            if (node.machineOwned) return false;
            if (node.data.fluid() == fluid) return false;
            PipeData newData = new PipeData(fluid);
            LevelNodeGraph<PipeData> to = FluidPipeGraph.get(level, fluid);
            if (from == to) {
                from.replaceNodeData(key, newData);
            } else {
                int mask = node.openConnections;
                boolean device = node.device;
                long[] links = from.removeNodeKeepingLinks(key);
                to.addNode(key, newData, mask);
                if (device) to.firstClass(key).device = true;
                for (long peer : links) {
                    if (to.containsCell(peer)) to.addRemoteLink(key, peer);
                    else to.addDormantLink(key, peer);
                }
            }
            return true;
        }
    }

    public static void refreshAround(ServerLevel level, BlockPos pos) {
        refreshConnections(level, pos);
        for (Direction d : Direction.VALUES) refreshConnections(level, pos.relative(d));
    }

    public static void scheduleConnections(BlockEntity owner) {
        if (owner.getLevel() instanceof ServerLevel level)
            LevelNodeGraph.invalidateTransportAround(level, owner.getBlockPos());
        ChunkDuctNodes.schedule(owner);
    }

    static void queueConnections(BlockEntity owner) {
        if (!(owner.getLevel() instanceof ServerLevel level) || owner.isRemoved()) return;
        BlockPos origin = owner.getBlockPos();
        int domains = MachineCaps.declaredDomains(owner);
        if ((domains & (MachineCaps.FLUID_IN | MachineCaps.FLUID_OUT)) != 0
                && owner.getBlockState().getBlock() instanceof BlockMultiblockCore core) {
            Direction facing = owner.getBlockState().getValue(BlockMultiblockCore.FACING);
            for (long contact : fluidContacts(core, facing, domains)) {
                BlockPos position =
                        origin.offset(
                                BlockPos.getX(contact),
                                BlockPos.getY(contact),
                                BlockPos.getZ(contact));
                LevelNodeGraph.invalidateEndpointsAt(level, position);
                ChunkDuctNodes.schedule(level, position);
            }
        } else if ((domains & (MachineCaps.FLUID_IN | MachineCaps.FLUID_OUT)) != 0) {
            for (Direction side : Direction.VALUES)
                ChunkDuctNodes.schedule(level, origin.relative(side));
        }
        level.hbm$endpoints()
                .forEachRemoteCell(
                        origin.asLong(),
                        MachineCaps.FLUID_IN | MachineCaps.FLUID_OUT,
                        (cellKey, faceMask) -> {
                            BlockPos cell = BlockPos.of(cellKey);
                            LevelNodeGraph.invalidateTransportAround(level, cell);
                            for (Direction side : Direction.VALUES) {
                                if ((faceMask & (1 << side.ordinal())) != 0) {
                                    ChunkDuctNodes.schedule(level, cell.relative(side));
                                }
                            }
                        });
    }

    private static long[] fluidContacts(BlockMultiblockCore core, Direction facing, int domains) {
        long[][] byFacing = core.fluidContactCache();
        long[] contacts = byFacing[facing.get2DDataValue()];
        if (contacts != null) return contacts;
        LongLinkedOpenHashSet out = new LongLinkedOpenHashSet();
        core.maskTable(facing)
                .forEachLocalCell(
                        (lx, ly, lz) -> {
                            int dx = MultiblockMaskTable.worldX(lx, ly, lz, facing);
                            int dz = MultiblockMaskTable.worldZ(lx, ly, lz, facing);
                            for (Direction side : Direction.VALUES) {
                                boolean receives =
                                        (domains & MachineCaps.FLUID_IN) != 0
                                                && core.isOpenAt(
                                                        facing,
                                                        dx,
                                                        ly,
                                                        dz,
                                                        side,
                                                        BlockMultiblockCore.PASSIVE_FLUID_IN,
                                                        CapRole.FLUID_IN);
                                boolean provides =
                                        (domains & MachineCaps.FLUID_OUT) != 0
                                                && core.isOpenAt(
                                                        facing,
                                                        dx,
                                                        ly,
                                                        dz,
                                                        side,
                                                        BlockMultiblockCore.PASSIVE_NONE,
                                                        CapRole.FLUID_OUT);
                                if (receives || provides) {
                                    out.add(
                                            BlockPos.asLong(
                                                    dx + side.getStepX(),
                                                    ly + side.getStepY(),
                                                    dz + side.getStepZ()));
                                }
                            }
                        });
        contacts = out.toLongArray();
        byFacing[facing.get2DDataValue()] = contacts;
        return contacts;
    }

    public static void retypeRun(ServerLevel level, long key, Fluid fluid) {
        try (var batch = PhysicalGraph.begin(level)) {
            LevelNodeGraph<PipeData> fromGraph = FluidPipeGraph.graphAt(level, key);
            PipeData keyData = fromGraph == null ? null : fromGraph.dataAt(key);
            if (keyData == null) return;
            Fluid original = keyData.fluid();
            LongOpenHashSet visited = new LongOpenHashSet();
            LongArrayList frontier = new LongArrayList();
            LongArrayList retyped = new LongArrayList();
            frontier.add(key);
            visited.add(key);
            for (int depth = 0; depth <= 64 && !frontier.isEmpty(); depth++) {
                LongArrayList next = new LongArrayList();
                for (int i = 0; i < frontier.size(); i++) {
                    long cur = frontier.getLong(i);
                    LevelNodeGraph<PipeData> graph = FluidPipeGraph.graphAt(level, cur);
                    if (graph == null) continue;
                    PipeData curData = graph.dataAt(cur);
                    GraphNode<PipeData> node = graph.getNode(cur);
                    if (curData == null || curData.fluid() != original) continue;
                    if (node != null && (node.machineOwned || node.selfEndpoint)) continue;
                    long[] links =
                            node != null && node.hasRemoteLinks()
                                    ? node.remoteLinks.toLongArray()
                                    : null;
                    retypeNodeNoSync(level, cur, fluid);
                    retyped.add(cur);
                    BlockPos p = BlockPos.of(cur);
                    for (Direction d : Direction.VALUES) {
                        long n = p.relative(d).asLong();
                        if (visited.add(n)) next.add(n);
                    }
                    if (links != null) {
                        for (long peer : links) {
                            if (visited.add(peer)) next.add(peer);
                        }
                    }
                }
                frontier = next;
            }
            long[] touched = retyped.toLongArray();
            FluidPipeGraph.get(level, fluid).compactAround(touched);
            fromGraph.compactAround(touched);
            FluidPipeTintData.sync(level, retyped.toLongArray(), fluid);
        }
    }

    public static void refreshConnections(ServerLevel level, BlockPos pos) {
        BlockState state = ChunkUtil.blockStateIfLoaded(level, pos);
        if (state == null || !(state.getBlock() instanceof FluidPipeBlock pipe)) return;
        Fluid self = pipeFluidAt(level, pos);
        BlockState updated = state;
        for (Direction dir : Direction.VALUES) {
            updated =
                    updated.setValue(
                            propertyFor(dir), pipe.connectsVisually(level, pos, dir, self));
        }
        if (updated != state)
            level.setBlock(pos, updated, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN, WATERLOGGED);
    }

    protected boolean connectsVisually(
            LevelReader level, BlockPos pos, Direction dir, Fluid selfFluid) {
        return PhysicalGraph.fluidContact(level, pos.relative(dir), dir.getOpposite(), selfFluid);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState state =
                defaultBlockState()
                        .setValue(
                                WATERLOGGED,
                                ctx.getLevel().getFluidState(ctx.getClickedPos()).is(Fluids.WATER));
        Fluid self = stampedFluid(ctx.getItemInHand());
        for (Direction dir : Direction.VALUES) {
            state =
                    state.setValue(
                            propertyFor(dir),
                            connectsVisually(ctx.getLevel(), ctx.getClickedPos(), dir, self));
        }
        return state;
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess ticks,
            BlockPos pos,
            Direction dir,
            BlockPos neighbourPos,
            BlockState neighbourState,
            RandomSource random) {
        if (state.getValue(WATERLOGGED))
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        boolean joins = connectsVisually(level, pos, dir, pipeFluidAt(level, pos));
        if (level.isClientSide())
            return PhysicalGraph.predictArm(state, propertyFor(dir), neighbourState, joins);
        return state.setValue(propertyFor(dir), joins);
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block block,
            @Nullable Orientation orientation,
            boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
        if (level instanceof ServerLevel sl) PhysicalGraph.redraw(state, sl, pos);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        int mask = 0;
        for (Direction dir : Direction.VALUES) {
            if (state.getValue(propertyFor(dir))) mask |= 1 << dir.ordinal();
        }
        return SHAPES[mask];
    }

    @Override
    public void setPlacedBy(
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable LivingEntity by,
            ItemStack stack) {
        super.setPlacedBy(level, pos, state, by, stack);
        if (!(level instanceof ServerLevel sl)) return;
        Fluid fluid = stampedFluid(stack);
        if (fluid == Fluids.EMPTY) return;
        createNodes(sl, pos, state);
        if (!retypeNode(sl, pos.asLong(), fluid)) {
            PhysicalGraph.redraw(sl.getBlockState(pos), sl, pos);
            PhysicalGraph.refreshAround(sl, pos);
        }
    }

    @Override
    protected ItemStack getCloneItemStack(
            LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        ItemStack stack = super.getCloneItemStack(level, pos, state, includeData);
        Fluid fluid = pipeFluidAt(level, pos);
        if (fluid != Fluids.EMPTY) {
            stack.set(ModDataComponents.FLUID_CONTENT.get(), new FluidStackNTM(fluid, 0, 0));
        }
        return stack;
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED)
                ? Fluids.WATER.getSource(false)
                : super.getFluidState(state);
    }
}
