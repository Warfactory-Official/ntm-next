// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.network.pneumatic;

import com.hbm.api.ntl.IPneumaticConnector;
import com.hbm.api.ntl.PneumaticNetwork;
import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.IToolable;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.lib.Library;
import com.hbm.tileentity.network.pneumatic.BlockEntityPneumoTube;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.hbm.util.AutoRotate;
import com.hbm.util.InventoryUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

@AutoRotate
public class PneumoTubeBlock extends Block
        implements ITickingBlock, IToolable, ICapabilityBlock, SimpleWaterloggedBlock {

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private static final double LOWER = 5 / 16D;
    private static final double UPPER = 11 / 16D;

    private static final VoxelShape CORE = Shapes.box(LOWER, LOWER, LOWER, UPPER, UPPER, UPPER);
    private static final VoxelShape[] ARMS = buildArms();
    private static final VoxelShape[] COLLISION = buildCollisionShapes();

    public PneumoTubeBlock(Properties props) {
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

    private static final BooleanProperty[] ARM_BY_DIRECTION = armByDirection();

    private static BooleanProperty[] armByDirection() {
        BooleanProperty[] out = new BooleanProperty[Direction.values().length];
        for (Direction dir : Direction.values())
            out[dir.ordinal()] = PipeBlock.PROPERTY_BY_DIRECTION.get(dir);
        return out;
    }

    public static BooleanProperty faceProperty(Direction dir) {
        return ARM_BY_DIRECTION[dir.ordinal()];
    }

    public static int faceMask(BlockState state) {
        int mask = 0;
        for (Direction dir : Direction.VALUES)
            if (state.getValue(faceProperty(dir))) mask |= 1 << dir.ordinal();
        return mask;
    }

    private static VoxelShape[] buildArms() {
        VoxelShape[] arm = new VoxelShape[6];
        arm[Direction.DOWN.ordinal()] = Shapes.box(LOWER, 0, LOWER, UPPER, LOWER, UPPER);
        arm[Direction.UP.ordinal()] = Shapes.box(LOWER, UPPER, LOWER, UPPER, 1, UPPER);
        arm[Direction.NORTH.ordinal()] = Shapes.box(LOWER, LOWER, 0, UPPER, UPPER, LOWER);
        arm[Direction.SOUTH.ordinal()] = Shapes.box(LOWER, LOWER, UPPER, UPPER, UPPER, 1);
        arm[Direction.WEST.ordinal()] = Shapes.box(0, LOWER, LOWER, LOWER, UPPER, UPPER);
        arm[Direction.EAST.ordinal()] = Shapes.box(UPPER, LOWER, LOWER, 1, UPPER, UPPER);
        return arm;
    }

    private static VoxelShape[] buildCollisionShapes() {
        VoxelShape[] shapes = new VoxelShape[64];
        shapes[0] = CORE;
        for (int mask = 1; mask < 64; mask++) {
            int last = Integer.highestOneBit(mask);
            shapes[mask] =
                    Shapes.or(shapes[mask ^ last], ARMS[Integer.numberOfTrailingZeros(last)]);
        }
        return shapes;
    }

    public static boolean canConnectTo(BlockGetter level, BlockPos pos, Direction dir) {
        BlockPos npos = pos.relative(dir);
        if (level.getBlockState(npos).getBlock() instanceof PneumoTubeBlock) return true;

        return level.getBlockState(npos).getBlock() instanceof IPneumaticConnector;
    }

    public static boolean canConnectToAir(LevelReader level, BlockPos pos, Direction dir) {
        if (!(level.getBlockEntity(pos) instanceof BlockEntityPneumoTube tube)) return false;
        if (!tube.isCompressor()) return false;
        if (tube.ejectionDir == dir || tube.insertionDir == dir) return false;
        BlockPos npos = pos.relative(dir);
        if (level.getBlockState(npos).getBlock() instanceof PneumoTubeBlock) return false;
        return Library.canConnectFluid(level, pos.relative(dir), dir.getOpposite(), NTMFluids.AIR);
    }

    public static void refreshConnections(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof PneumoTubeBlock)) return;
        BlockState updated = state;
        for (Direction dir : Direction.VALUES) {
            updated =
                    updated.setValue(
                            faceProperty(dir),
                            canConnectTo(level, pos, dir) || canConnectToAir(level, pos, dir));
        }
        if (updated != state) level.setBlock(pos, updated, Block.UPDATE_ALL);
        refreshAir(level, pos);
    }

    private static void refreshAir(Level level, BlockPos pos) {
        if (level.isClientSide()
                || !(level.getBlockEntity(pos) instanceof BlockEntityPneumoTube tube)) return;
        int air = 0;
        for (Direction dir : Direction.VALUES)
            if (canConnectToAir(level, pos, dir)) air |= 1 << dir.ordinal();
        tube.setAirMask(air);
    }

    private static boolean bit(int mask, Direction dir) {
        return (mask & (1 << dir.ordinal())) != 0;
    }

    public static @Nullable Direction nextDir(@Nullable Direction dir) {
        int ordinal = dir == null ? 6 : dir.ordinal();
        int next = (ordinal + 1) % 7;
        return next == 6 ? null : Direction.from3DDataValue(next);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN, WATERLOGGED);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState state =
                defaultBlockState()
                        .setValue(
                                WATERLOGGED,
                                ctx.getLevel().getFluidState(ctx.getClickedPos()).is(Fluids.WATER));
        for (Direction dir : Direction.VALUES) {
            state =
                    state.setValue(
                            faceProperty(dir),
                            canConnectTo(ctx.getLevel(), ctx.getClickedPos(), dir));
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
        if (level.isClientSide()) {

            return Library.predictArm(
                    state, faceProperty(dir), neighbourState, canConnectTo(level, pos, dir));
        }
        return state.setValue(
                faceProperty(dir),
                canConnectTo(level, pos, dir) || canConnectToAir(level, pos, dir));
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED)
                ? Fluids.WATER.getSource(false)
                : super.getFluidState(state);
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return COLLISION[faceMask(state)];
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        int mask = faceMask(state);
        if (level.getBlockEntity(pos) instanceof BlockEntityPneumoTube tube) {
            if (tube.insertionDir != null) mask |= 1 << tube.insertionDir.ordinal();
            if (tube.ejectionDir != null) mask |= 1 << tube.ejectionDir.ordinal();
        }
        return Shapes.box(
                bit(mask, Direction.WEST) ? 0 : LOWER,
                bit(mask, Direction.DOWN) ? 0 : LOWER,
                bit(mask, Direction.NORTH) ? 0 : LOWER,
                bit(mask, Direction.EAST) ? 1 : UPPER,
                bit(mask, Direction.UP) ? 1 : UPPER,
                bit(mask, Direction.SOUTH) ? 1 : UPPER);
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isSecondaryUseActive()) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof BlockEntityPneumoTube tube))
            return InteractionResult.PASS;
        if (!tube.isCompressor() && !tube.isEndpoint()) return InteractionResult.PASS;
        if (!level.isClientSide()) tube.openMenu(player, pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean onScrew(
            Level level,
            Player player,
            BlockPos pos,
            @Nullable Direction side,
            Vec3 hit,
            ToolType tool) {
        if (tool != ToolType.SCREWDRIVER) return false;
        return screwDirections(level, player, pos);
    }

    public static boolean screwDirections(Level level, Player player, BlockPos pos) {
        if (level.isClientSide()) return true;
        if (!(level.getBlockEntity(pos) instanceof BlockEntityPneumoTube tube)) return false;

        boolean sneaking = player.isSecondaryUseActive();
        Direction rot = sneaking ? tube.ejectionDir : tube.insertionDir;
        Direction oth = sneaking ? tube.insertionDir : tube.ejectionDir;
        Direction wasEjecting = tube.ejectionDir;

        for (int i = 0; i < 7; i++) {
            rot = nextDir(rot);
            if (rot == null) break;
            if (rot == oth) continue;
            BlockPos next = pos.relative(rot);
            if (level.getBlockEntity(next) instanceof BlockEntityPneumoTube) continue;
            if (InventoryUtil.inventoryAt(level, next, rot.getOpposite())) break;
        }

        if (sneaking) tube.ejectionDir = rot;
        else tube.insertionDir = rot;

        tube.setChanged();
        level.sendBlockUpdated(
                pos, level.getBlockState(pos), level.getBlockState(pos), Block.UPDATE_ALL);
        refreshConnections(level, pos);

        if (level instanceof ServerLevel sl && wasEjecting != tube.ejectionDir) {
            if (wasEjecting != null) PneumaticNetwork.setEndpoint(sl, pos, tube.isEndpoint());
            LevelNodeGraph.invalidateEndpointsAt(sl, pos);
        }
        return true;
    }

    @Override
    protected void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel sl && !oldState.is(this))
            PneumaticNetwork.addNode(sl, pos);
        if (level.getBlockEntity(pos) instanceof BlockEntityPneumoTube be) be.refreshRedstone();
    }

    @Override
    protected void affectNeighborsAfterRemoval(
            BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        PneumaticNetwork.removeNode(level, pos);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityPneumoTube(pos, state);
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
        if (level instanceof ServerLevel sl) LevelNodeGraph.invalidateEndpointsAt(sl, pos);
        if (level.getBlockEntity(pos) instanceof BlockEntityPneumoTube be) be.refreshRedstone();
        refreshAir(level, pos);
        if (level instanceof ServerLevel sl) Library.redrawArms(level.getBlockState(pos), sl, pos);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.PNEUMATIC_TUBE)
                .fluidIn()
                .fluidFaces(
                        BlockEntityPneumoTube.class,
                        (be, face) ->
                                face.fluid() == null || be.acceptsFluid(face.fluid(), face.side()));
    }
}
