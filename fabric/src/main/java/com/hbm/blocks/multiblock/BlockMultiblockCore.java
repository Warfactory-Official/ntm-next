// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.multiblock;

import com.hbm.blockentity.BlockEntityMachineBase;
import com.hbm.blockentity.GraphResident;
import com.hbm.blockentity.Tiltable;
import com.hbm.blocks.IBlockHighlight;
import com.hbm.blocks.ISectionGeometry;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.network.ConveyorNeighbors;
import com.hbm.capability.MachineCaps;
import com.hbm.capability.NtmCapabilities.CapRole;
import com.hbm.interfaces.ICopiable;
import com.hbm.interfaces.PlantSoil;
import com.hbm.interfaces.RigidPistonStructure;
import com.hbm.inventory.IGUIProvider;
import com.hbm.platform.FabricCaps;
import com.hbm.platform.FabricPlatform;
import com.hbm.uninos.graph.PhysicalGraph;
import com.hbm.world.phys.PlacementExtraShape;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public abstract class BlockMultiblockCore extends Block
        implements ICopiable, RigidPistonStructure, ISectionGeometry, PlantSoil {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public static final int MASK_NONE = 0;
    public static final int MASK_DOWN = 1 << 0;
    public static final int MASK_UP = 1 << 1;
    public static final int MASK_NORTH = 1 << 2;
    public static final int MASK_SOUTH = 1 << 3;
    public static final int MASK_WEST = 1 << 4;
    public static final int MASK_EAST = 1 << 5;
    public static final int MASK_HORIZONTAL = 0b111100;
    public static final int MASK_ALL = 0b111111;
    public static final int ROLE_POWER_IN = MachineCaps.POWER_IN;
    public static final int ROLE_POWER_OUT = MachineCaps.POWER_OUT;
    public static final int ROLE_FLUID_IN = MachineCaps.FLUID_IN;
    public static final int ROLE_FLUID_OUT = MachineCaps.FLUID_OUT;
    public static final int ROLE_POWER = ROLE_POWER_IN | ROLE_POWER_OUT;
    public static final int ROLE_FLUID = ROLE_FLUID_IN | ROLE_FLUID_OUT;
    public static final int ROLE_ALL = ROLE_POWER | ROLE_FLUID;

    public static final int PASSIVE_NONE = 0;
    public static final int PASSIVE_POWER_IN = 1 << 0;
    public static final int PASSIVE_FLUID_IN = 1 << 1;
    public static final int PASSIVE_ITEMS = 1 << 2;
    public static final int PASSIVE_ANY = PASSIVE_POWER_IN | PASSIVE_FLUID_IN | PASSIVE_ITEMS;
    static final int PASSIVE_DOMAINS = 3;

    private static final ThreadLocal<Boolean> BUSY = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final ThreadLocal<long[]> PLACING = ThreadLocal.withInitial(() -> new long[2]);
    private long[][] fluidContacts = new long[4][];
    private final VoxelShape[] coreShapes = new VoxelShape[16];
    private final @Nullable VoxelShape[] silhouettes = new VoxelShape[4];
    private final VoxelShape[] footprintShapes = new VoxelShape[4];
    private final VoxelShape[] placementShapes = new VoxelShape[4];
    private final VoxelShape[][] placementExtraShapes = new VoxelShape[4][];
    private static final VoxelShape[] NO_PLACEMENT_EXTRA_SHAPES = {};
    private MultiblockMaskTable @Nullable [] tables;
    private MultiblockLayout.@Nullable Placement placement;
    private long[][] clearance;
    private int declaredCaps;
    private MultiblockFootprint.Layout[] footprints;

    protected BlockMultiblockCore(Properties properties) {
        super(properties.isValidSpawn((state, level, pos, type) -> false));
        BlockState state = stateDefinition.any().setValue(FACING, Direction.NORTH);
        registerDefaultState(tilts() ? state.setValue(Tiltable.TILTED, false) : state);
        Arrays.fill(coreShapes, Shapes.block());
    }

    public static void withoutTeardown(Runnable body) {
        boolean prev = BUSY.get();
        BUSY.set(true);
        try {
            body.run();
        } finally {
            BUSY.set(prev);
        }
    }

    public static void notifyCell(ServerLevel level, BlockPos pos) {
        FabricCaps.invalidateCaps(level, pos);
        PhysicalGraph.refreshAround(level, pos);
    }

    public static boolean isBusy() {
        return BUSY.get();
    }

    public static boolean claimPlacement(BlockEntity be) {
        long[] placing = PLACING.get();
        if (placing[1] != be.getLevel().getGameTime() + 1
                || placing[0] != be.getBlockPos().asLong()) return false;
        placing[1] = 0;
        return true;
    }

    public static void setBusy(boolean busy) {
        BUSY.set(busy);
    }

    public static @Nullable FoldedOwner ownerOf(Level level, BlockPos pos) {
        BlockPos core =
                level instanceof ServerLevel server
                        ? MultiblockSurface.indexedCore(server, pos)
                        : MultiblockSurface.clientCoreOf(level, pos);
        if (core == null) return null;
        BlockState coreState = level.getBlockState(core);
        BlockMultiblockCore block = MultiblockSurface.foldedCore(coreState);
        if (block == null) return null;
        return new FoldedOwner(core, coreState, block, coreState.getValue(FACING));
    }

    public static void foldedCellRemoved(ServerLevel level, BlockPos pos, boolean movedByPiston) {
        FoldedOwner owner = movedByPiston || isBusy() ? null : ownerOf(level, pos);

        unindexCell(level, pos);
        notifyCell(level, pos);

        if (owner == null) return;
        demolish(level, owner.pos(), owner.block(), owner.facing(), true);
    }

    private static void flushIndex(
            LevelAccessor level,
            BlockPos core,
            CoreIndexScratch scratch,
            BlockMultiblockCore owner) {
        BlockPos.MutableBlockPos cell = scratch.cellPos;
        scratch.ensurePacked(scratch.cellCount);

        for (int c = 0; c < scratch.chunkCount; c++) {
            long chunkKey = scratch.chunkKeys[c];
            int cx = ChunkPos.getX(chunkKey);
            int cz = ChunkPos.getZ(chunkKey);
            ChunkAccess chunk = indexTarget(level.getChunk(cx, cz, ChunkStatus.EMPTY, false));
            if (chunk == null) continue;
            int n = 0;
            for (int i = 0; i < scratch.cellCount; i++) {
                long p = scratch.cells[i];
                int x = BlockPos.getX(p), y = BlockPos.getY(p), z = BlockPos.getZ(p);
                if (SectionPos.blockToSectionCoord(x) != cx
                        || SectionPos.blockToSectionCoord(z) != cz) continue;
                if (!owner.isCellOf(chunk.getBlockState(cell.set(x, y, z)))) continue;
                scratch.packed[n++] =
                        MultiblockCoreIndex.pack(
                                MultiblockCoreIndex.key(level, x, y, z),
                                core.getX() - x,
                                core.getY() - y,
                                core.getZ() - z);
            }
            if (n != 0) MultiblockCoreIndex.insert(chunk, scratch.packed, n);
        }
    }

    public static @Nullable ChunkAccess indexTarget(@Nullable ChunkAccess chunk) {
        return chunk instanceof ImposterProtoChunk imposter ? imposter.getWrapped() : chunk;
    }

    public static void claimCells(
            LevelAccessor level, BlockPos owner, Collection<BlockPos> cells, int population) {
        Long2ObjectOpenHashMap<LongArrayList> byChunk = new Long2ObjectOpenHashMap<>();
        for (BlockPos cell : cells) {
            byChunk.computeIfAbsent(ChunkPos.pack(cell), k -> new LongArrayList())
                    .add(
                            MultiblockCoreIndex.pack(
                                    MultiblockCoreIndex.key(
                                            level, cell.getX(), cell.getY(), cell.getZ()),
                                    owner.getX() - cell.getX(),
                                    owner.getY() - cell.getY(),
                                    owner.getZ() - cell.getZ(),
                                    population));
        }
        for (Long2ObjectMap.Entry<LongArrayList> entry : byChunk.long2ObjectEntrySet()) {
            ChunkAccess chunk =
                    indexTarget(
                            level.getChunk(
                                    ChunkPos.getX(entry.getLongKey()),
                                    ChunkPos.getZ(entry.getLongKey()),
                                    ChunkStatus.EMPTY,
                                    false));
            if (chunk == null) continue;
            long[] packed = entry.getValue().toLongArray();
            MultiblockCoreIndex.insert(chunk, packed, packed.length);
        }
    }

    public static void unindexCell(LevelAccessor level, BlockPos pos) {
        ChunkAccess chunk =
                indexTarget(
                        level.getChunk(
                                SectionPos.blockToSectionCoord(pos.getX()),
                                SectionPos.blockToSectionCoord(pos.getZ()),
                                ChunkStatus.EMPTY,
                                false));
        if (chunk == null) return;
        MultiblockCoreIndex.remove(
                chunk, MultiblockCoreIndex.key(level, pos.getX(), pos.getY(), pos.getZ()));
    }

    public static boolean isCell(BlockState state) {
        return state.getBlock() instanceof BlockMultiblockCell;
    }

    static void demolish(
            ServerLevel level,
            BlockPos core,
            BlockMultiblockCore block,
            Direction facing,
            boolean removeCore) {
        boolean prev = BUSY.get();
        BUSY.set(true);
        long corePacked = core.asLong();
        try (var removals = MultiblockCoreIndex.removalBatch()) {
            block.visitTeardownCells(
                    level,
                    core,
                    facing,
                    pos -> {
                        if (!block.isCellOf(level.getBlockState(pos))) return;
                        if (MultiblockSurface.recordedCorePacked(
                                        level, pos.getX(), pos.getY(), pos.getZ())
                                != corePacked) {
                            return;
                        }
                        level.removeBlock(pos.immutable(), false);
                    });
            if (removeCore) level.removeBlock(core, false);
        } finally {
            BUSY.set(prev);
        }
    }

    public static Direction coreFacing(BlockState state) {
        return state.getValue(FACING);
    }

    public static @Nullable LevelChunk readableChunk(ServerLevel level, int x, int z) {
        LevelChunk chunk =
                level.getChunkSource()
                        .getChunkNow(
                                SectionPos.blockToSectionCoord(x),
                                SectionPos.blockToSectionCoord(z));
        return chunk != null && chunk.getFullStatus().isOrAfter(FullChunkStatus.FULL)
                ? chunk
                : null;
    }

    public static @Nullable LevelChunk readableChunk(
            ServerLevel level, int x, int z, LevelChunk knownChunk) {
        if (knownChunk.getPos().x() != SectionPos.blockToSectionCoord(x)
                || knownChunk.getPos().z() != SectionPos.blockToSectionCoord(z)) {
            return readableChunk(level, x, z);
        }
        return knownChunk.getFullStatus().isOrAfter(FullChunkStatus.FULL) ? knownChunk : null;
    }

    public static boolean canReadWithoutLoading(BlockGetter world, BlockPos pos) {
        if (world instanceof ServerLevel level) {
            return readableChunk(level, pos.getX(), pos.getZ()) != null;
        }
        return true;
    }

    public static boolean vendsHeatAt(BlockEntity owner, BlockPos pos) {
        BlockPos core = owner.getBlockPos();
        if (pos.equals(core)) return true;

        BlockState state = owner.getBlockState();
        if (!(state.getBlock() instanceof BlockMultiblockCore block)) return false;

        Direction facing = state.getValue(FACING);
        int dx = pos.getX() - core.getX();
        int dy = pos.getY() - core.getY();
        int dz = pos.getZ() - core.getZ();

        return (block.maskTable(facing)
                                .flagsAtLocal(
                                        MultiblockMaskTable.localX(dx, dy, dz, facing),
                                        MultiblockMaskTable.localY(dx, dy, dz, facing),
                                        MultiblockMaskTable.localZ(dx, dy, dz, facing))
                        & MultiblockMaskTable.FLAG_HEAT)
                != 0;
    }

    public static void renotifyNeighbours(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        state.updateNeighbourShapes(level, pos, Block.UPDATE_ALL);
        level.updateNeighborsAt(pos, state.getBlock());
    }

    private static BlockState takeWater(BlockState state, LevelReader level, BlockPos pos) {
        return state.hasProperty(BlockStateProperties.WATERLOGGED)
                ? state.setValue(
                        BlockStateProperties.WATERLOGGED, level.getFluidState(pos).is(Fluids.WATER))
                : state;
    }

    protected boolean tilts() {
        return false;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        if (tilts()) builder.add(Tiltable.TILTED);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    public Direction getDirModified(Direction dir) {
        return dir;
    }

    public float placementYawSpan() {
        return 360.0F;
    }

    public void assembled(
            LevelAccessor level, BlockPos core, BlockState coreState, @Nullable BoundingBox clip) {}

    protected void visitPlacedCells(
            LevelAccessor level, BlockPos core, Direction facing, FootprintVisitor visitor) {
        footprint(facing).visit(core, visitor);
    }

    public boolean hasVariableFootprint() {
        return false;
    }

    public final MultiblockFootprint.Layout footprint(Direction facing) {
        return footprints[facing.get2DDataValue()];
    }

    protected void visitTeardownCells(BlockPos core, Direction facing, FootprintVisitor visitor) {
        footprint(facing).visit(core, visitor);
    }

    protected void visitTeardownCells(
            LevelAccessor level, BlockPos core, Direction facing, FootprintVisitor visitor) {
        visitTeardownCells(core, facing, visitor);
    }

    public @Nullable BlockPos findClientCore(BlockGetter level, BlockPos cell) {
        return null;
    }

    public boolean claimsCell(BlockGetter level, BlockPos core, BlockPos cell, Direction facing) {
        int dx = cell.getX() - core.getX(),
                dy = cell.getY() - core.getY(),
                dz = cell.getZ() - core.getZ();
        return maskAt(
                        MultiblockMaskTable.localX(dx, dy, dz, facing),
                        MultiblockMaskTable.localY(dx, dy, dz, facing),
                        MultiblockMaskTable.localZ(dx, dy, dz, facing),
                        facing)
                != MultiblockMaskTable.NOT_A_CELL;
    }

    public int passiveCoreMask() {
        return MASK_ALL;
    }

    public int passiveCoreDomains(int declaredCaps) {
        return ((declaredCaps & MachineCaps.POWER_IN) != 0 ? PASSIVE_POWER_IN : PASSIVE_NONE)
                | ((declaredCaps & MachineCaps.FLUID_IN) != 0 ? PASSIVE_FLUID_IN : PASSIVE_NONE)
                | ((declaredCaps & (MachineCaps.ITEMS | MachineCaps.ITEMS_AT_CELLS)) != 0
                        ? PASSIVE_ITEMS
                        : PASSIVE_NONE);
    }

    public final int maskAt(int lx, int ly, int lz, Direction facing) {
        return maskTable(facing).maskAtLocal(lx, ly, lz);
    }

    public final int maskAt(int lx, int ly, int lz, Direction facing, @Nullable CapRole role) {
        MultiblockMaskTable table = maskTable(facing);
        return role == null ? table.maskAtLocal(lx, ly, lz) : table.maskAtLocal(lx, ly, lz, role);
    }

    public final int passiveMaskAt(int lx, int ly, int lz, Direction facing, int domains) {
        return maskTable(facing).passiveMaskAtLocal(lx, ly, lz, domains);
    }

    @Override
    public final boolean sustainsPlant(BlockGetter level, BlockPos soil, BlockState soilState) {
        return MultiblockSurface.sustainsPlant(level, soil, soilState);
    }

    public @Nullable VoxelShape selectionOutline(Direction facing) {
        VoxelShape authored = boundingOutline(facing);
        if (authored != null) return authored;
        if (this instanceof IBlockHighlight || MultiblockCellShapes.hasGeometry(this)) return null;
        if (hasVariableFootprint()) return footprintVolume(facing);
        int index = facing.get2DDataValue();
        VoxelShape baked = footprintShapes[index];
        return baked == null ? footprintShapes[index] = footprintVolume(facing) : baked;
    }

    public VoxelShape placementOutline(Direction facing) {
        int index = facing.get2DDataValue();
        VoxelShape baked = placementShapes[index];
        if (baked != null) return baked;
        int[] boxes = placement().previewBoxes();
        assert boxes.length % 9 == 0;
        Direction right = facing.getClockWise();
        VoxelShape shape = Shapes.empty();
        for (int i = 0; i < boxes.length; i += 9) {
            int[] rotated =
                    MultiblockHandlerXR.rotate(
                            new int[] {
                                boxes[i],
                                boxes[i + 1],
                                boxes[i + 2],
                                boxes[i + 3],
                                boxes[i + 4],
                                boxes[i + 5]
                            },
                            facing);
            int xOffset = boxes[i + 6] * facing.getStepX() + boxes[i + 8] * right.getStepX();
            int zOffset = boxes[i + 6] * facing.getStepZ() + boxes[i + 8] * right.getStepZ();
            for (int x = -rotated[4] + xOffset; x <= rotated[5] + xOffset; x++) {
                for (int y = -rotated[1] + boxes[i + 7]; y <= rotated[0] + boxes[i + 7]; y++) {
                    for (int z = -rotated[2] + zOffset; z <= rotated[3] + zOffset; z++) {
                        shape =
                                Shapes.joinUnoptimized(
                                        shape, Shapes.block().move(x, y, z), BooleanOp.OR);
                    }
                }
            }
        }
        return placementShapes[index] = shape.optimize();
    }

    public VoxelShape[] placementExtraOutlines(Direction facing) {
        double[] boxes = placement().previewExtras();
        if (boxes.length == 0) return NO_PLACEMENT_EXTRA_SHAPES;
        int index = facing.get2DDataValue();
        VoxelShape[] shapes = placementExtraShapes[index];
        if (shapes != null) return shapes;
        assert boxes.length % 6 == 0;
        shapes = new VoxelShape[boxes.length / 6];
        Direction right = facing.getClockWise();
        for (int i = 0; i < shapes.length; i++) {
            int at = i * 6;
            double x0 = 0.5 + boxes[at + 2] * facing.getStepX() + boxes[at + 4] * right.getStepX();
            double x1 = 0.5 + boxes[at + 3] * facing.getStepX() + boxes[at + 5] * right.getStepX();
            double z0 = 0.5 + boxes[at + 2] * facing.getStepZ() + boxes[at + 4] * right.getStepZ();
            double z1 = 0.5 + boxes[at + 3] * facing.getStepZ() + boxes[at + 5] * right.getStepZ();
            shapes[i] = new PlacementExtraShape(x0, boxes[at + 1], z0, x1, boxes[at], z1);
        }
        return placementExtraShapes[index] = shapes;
    }

    private VoxelShape footprintVolume(Direction facing) {
        VoxelShape shape = Shapes.block();
        for (long offset : footprint(facing).offsets) {
            shape =
                    Shapes.joinUnoptimized(
                            shape,
                            Shapes.block()
                                    .move(
                                            BlockPos.getX(offset),
                                            BlockPos.getY(offset),
                                            BlockPos.getZ(offset)),
                            BooleanOp.OR);
        }
        return shape.optimize();
    }

    public @Nullable VoxelShape boundingOutline(Direction facing) {
        return silhouettes[facing.get2DDataValue()];
    }

    public boolean hasBounding() {
        return silhouettes[0] != null;
    }

    protected VoxelShape coreShape(Direction facing, boolean open, boolean forCollision) {
        return coreShapes[facing.get2DDataValue() << 2 | (open ? 2 : 0) | (forCollision ? 1 : 0)];
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return coreShape(state.getValue(FACING), false, false);
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context.isPlacement() && !level.getBlockState(pos).is(this)) return Shapes.block();
        return coreShape(state.getValue(FACING), false, true);
    }

    public SoundType cellSoundType() {
        return cellSound() == BlockMultiblockCell.SOUND_METAL ? SoundType.METAL : SoundType.STONE;
    }

    public long[][] fluidContactCache() {
        return fluidContacts;
    }

    protected @Nullable MultiblockLayout runtimeLayout(int declaredCaps) {
        return null;
    }

    public void installBakedLayout(int declaredCaps) {
        MultiblockLayout layout = runtimeLayout(declaredCaps);
        if (layout == null) {
            layout = MultiblockLayouts.of(this);
            if (layout.declaredCaps() != declaredCaps) {
                throw new IllegalStateException(
                        this
                                + " declares caps 0b"
                                + Integer.toBinaryString(declaredCaps)
                                + " but its baked layout carries 0b"
                                + Integer.toBinaryString(layout.declaredCaps())
                                + " - run :neoforge:runData / :fabric:runDatagen");
            }
        }
        installLayout(layout);
    }

    public void installLayout(MultiblockLayout layout) {
        fluidContacts = new long[4][];
        Arrays.fill(footprintShapes, null);
        Arrays.fill(placementShapes, null);
        Arrays.fill(placementExtraShapes, null);
        this.declaredCaps = layout.declaredCaps();
        this.tables = layout.tables();
        this.placement = layout.placement();
        this.clearance = layout.clearance();
        MultiblockLayout.CellGeometry geometry = layout.geometry();
        MultiblockCellShapes.install(this, geometry);
        boolean shapedCore = false;
        for (int i = 0; i < coreShapes.length; i++) {
            VoxelShape shape = MultiblockLayout.CellGeometry.shape(geometry.core()[i]);
            coreShapes[i] = shape == null ? Shapes.block() : shape;
            shapedCore |= coreShapes[i] != Shapes.block();
        }
        if (shapedCore) {
            for (BlockState state : stateDefinition.getPossibleStates()) {
                state.initCache();
                state.initCache();
            }
        }
        for (int f = 0; f < silhouettes.length; f++) {
            silhouettes[f] = MultiblockLayout.CellGeometry.shape(geometry.silhouettes()[f]);
        }
        footprints = new MultiblockFootprint.Layout[4];
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            int index = facing.get2DDataValue();
            footprints[index] = MultiblockFootprint.layout(this, facing, layout.offsets()[index]);
        }
        if (wantsNeighborUpdates()) {
            for (MultiblockFootprint.Layout footprint : footprints) {
                forwardNeighborUpdates(footprint.states);
                if (footprint.swapped != null) forwardNeighborUpdates(footprint.swapped);
            }
        }
        MultiblockSurface.invalidateClientCandidates();
    }

    private static void forwardNeighborUpdates(@Nullable BlockState[] states) {
        for (BlockState state : states) {
            if (state != null && state.getBlock() instanceof BlockMultiblockCell cell)
                cell.forwardNeighborUpdates();
        }
    }

    public MultiblockMaskTable maskTable(Direction facing) {
        MultiblockMaskTable[] baked = tables;
        if (baked == null) {
            throw new IllegalStateException(
                    "multiblock mask table for "
                            + this
                            + " was queried before NtmCapabilities.declareAll() installed it");
        }
        return baked[facing.get2DDataValue()];
    }

    public MultiblockLayout.Placement placement() {
        MultiblockLayout.Placement baked = placement;
        if (baked == null) {
            throw new IllegalStateException(
                    "multiblock placement for "
                            + this
                            + " was queried before NtmCapabilities.declareAll() installed it");
        }
        return baked;
    }

    public int declaredCaps() {
        maskTable(Direction.SOUTH);
        return declaredCaps;
    }

    public boolean declares(int bits) {
        return (declaredCaps() & bits) == bits;
    }

    public boolean isOpenAt(Direction facing, int dx, int dy, int dz, Direction side) {
        return isOpenAt(facing, dx, dy, dz, side, PASSIVE_NONE);
    }

    public boolean isOpenAt(
            Direction facing, int dx, int dy, int dz, Direction side, int passiveDomains) {
        return isOpenAt(facing, dx, dy, dz, side, passiveDomains, null);
    }

    public boolean isOpenAt(
            Direction facing,
            int dx,
            int dy,
            int dz,
            Direction side,
            int passiveDomains,
            @Nullable CapRole role) {
        int lx = MultiblockMaskTable.localX(dx, dy, dz, facing);
        int ly = MultiblockMaskTable.localY(dx, dy, dz, facing);
        int lz = MultiblockMaskTable.localZ(dx, dy, dz, facing);
        int mask = maskAt(lx, ly, lz, facing, role);
        if (mask == MultiblockMaskTable.NOT_A_CELL) return false;
        if (passiveDomains != PASSIVE_NONE)
            mask |= passiveMaskAt(lx, ly, lz, facing, passiveDomains);
        return (mask & (1 << MultiblockMaskTable.toLocal(side, facing).ordinal())) != 0;
    }

    public boolean isPassiveOnlyOpenAt(
            Direction facing, int dx, int dy, int dz, Direction side, int passiveDomains) {
        int lx = MultiblockMaskTable.localX(dx, dy, dz, facing);
        int ly = MultiblockMaskTable.localY(dx, dy, dz, facing);
        int lz = MultiblockMaskTable.localZ(dx, dy, dz, facing);
        int mask = passiveMaskAt(lx, ly, lz, facing, passiveDomains);
        if (mask == MultiblockMaskTable.NOT_A_CELL) return false;
        return (mask & (1 << MultiblockMaskTable.toLocal(side, facing).ordinal())) != 0;
    }

    @Override
    public void setPlacedBy(
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable LivingEntity placer,
            ItemStack stack) {
        if (!(placer instanceof Player pl)) return;

        boolean prev = BUSY.get();
        BUSY.set(true);
        try {
            detachGraphs(level, pos);
            level.setBlock(
                    pos,
                    state.getFluidState().createLegacyBlock(),
                    UPDATE_ALL | UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS);
        } finally {
            BUSY.set(prev);
        }

        int i = Mth.floor(placer.getYRot() * 4.0F / placementYawSpan() + 0.5D) & 3;
        MultiblockLayout.Placement placement = placement();
        int o = -placement.offset();
        BlockPos placed = pos.offset(0, placement.heightOffset(), 0);

        Direction dir =
                getDirModified(
                        switch (i) {
                            case 1 -> Direction.EAST;
                            case 2 -> Direction.SOUTH;
                            case 3 -> Direction.WEST;
                            default -> Direction.NORTH;
                        });

        BlockPos core = placed.offset(dir.getStepX() * o, dir.getStepY() * o, dir.getStepZ() * o);
        if (!placementClear(level, core, dir)) {
            ItemStack refund = stack.copyWithCount(1);
            if (!pl.getAbilities().instabuild) pl.getInventory().placeItemBackInInventory(refund);
            return;
        }

        if (!level.isClientSide()) {
            level.setBlock(
                    core, takeWater(defaultBlockState().setValue(FACING, dir), level, core), 3);
            BlockEntity placedCore = level.getBlockEntity(core);
            if (placedCore != null) {
                BlockItem.updateCustomBlockEntityTag(level, pl, core, stack);
                placedCore.applyComponentsFromItemStack(stack);
                placedCore.setChanged();
            }
            if (hasVariableFootprint() && !cellsClear(level, core, dir)) {
                withoutTeardown(
                        () -> {
                            detachGraphs(level, core);
                            level.setBlock(
                                    core,
                                    level.getBlockState(core).getFluidState().createLegacyBlock(),
                                    UPDATE_ALL | UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS);
                        });
                if (!pl.getAbilities().instabuild)
                    pl.getInventory().placeItemBackInInventory(stack.copyWithCount(1));
                return;
            }
            fillSpace(level, core, dir);
        }

        super.setPlacedBy(level, pos, state, placer, stack);
    }

    private static void detachGraphs(Level level, BlockPos pos) {
        if (level instanceof ServerLevel server
                && level.getBlockEntity(pos) instanceof GraphResident resident) {
            resident.onGraphDetach(server);
        }
    }

    public boolean placementClear(Level level, BlockPos core, Direction dir) {
        if (level.isOutsideBuildHeight(core) || !level.getBlockState(core).canBeReplaced())
            return false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (long offset : clearance[dir.get2DDataValue()]) {
            pos.setWithOffset(
                    core, BlockPos.getX(offset), BlockPos.getY(offset), BlockPos.getZ(offset));
            if (!level.getBlockState(pos).canBeReplaced()) return false;
        }
        return hasVariableFootprint() || cellsClear(level, core, dir);
    }

    private boolean cellsClear(Level level, BlockPos core, Direction dir) {
        try {
            visitPlacedCells(
                    level,
                    core,
                    dir,
                    pos -> {
                        if (level.isOutsideBuildHeight(pos)
                                || !level.getBlockState(pos).canBeReplaced()) {
                            throw PlacementRefusal.INSTANCE;
                        }
                    });
            return true;
        } catch (PlacementRefusal refused) {
            return false;
        }
    }

    private static final class PlacementRefusal extends RuntimeException {
        private static final PlacementRefusal INSTANCE = new PlacementRefusal();

        private PlacementRefusal() {
            super(null, null, false, false);
        }
    }

    public final void fillSpace(LevelAccessor level, BlockPos core, Direction dir) {
        fillSpace(level, core, dir, null);
    }

    public final void fillSpace(
            LevelAccessor level, BlockPos core, Direction dir, @Nullable BoundingBox clip) {
        boolean prev = BUSY.get();
        BUSY.set(true);
        int flags =
                level instanceof ServerLevel
                        ? Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE
                        : Block.UPDATE_ALL;
        try {
            visitPlacedCells(
                    level,
                    core,
                    dir,
                    pos -> {
                        if (clip != null && !clip.isInside(pos)) return;
                        int lx =
                                MultiblockMaskTable.localX(
                                        pos.getX() - core.getX(),
                                        pos.getY() - core.getY(),
                                        pos.getZ() - core.getZ(),
                                        dir);
                        int ly = pos.getY() - core.getY();
                        int lz =
                                MultiblockMaskTable.localZ(
                                        pos.getX() - core.getX(),
                                        pos.getY() - core.getY(),
                                        pos.getZ() - core.getZ(),
                                        dir);
                        int shape = MultiblockCellShapes.idAt(this, lx, ly, lz, dir);
                        level.setBlock(
                                pos,
                                takeWater(cellStateFor(lx, ly, lz, dir, shape), level, pos),
                                flags);
                    });
        } finally {
            BUSY.set(prev);
        }
        indexCells(level, core, dir, clip);
        if (level instanceof ServerLevel server) {
            if (clip == null || clip.isInside(core)) renotifyNeighbours(server, core);
            visitPlacedCells(
                    level,
                    core,
                    dir,
                    pos -> {
                        if (clip == null || clip.isInside(pos))
                            renotifyNeighbours(server, pos.immutable());
                    });
        }
    }

    protected BlockState cellStateFor(int lx, int ly, int lz, Direction facing, int shapeId) {
        return cellStateOfKind(maskTable(facing).kindAtLocal(lx, ly, lz), facing, shapeId);
    }

    protected BlockState cellStateOfKind(int kind, Direction facing, int shapeId) {
        BlockState state;
        if (shapeId == MultiblockCellShapes.PLAIN) {
            state =
                    ModBlocks.plainCell(cellKey())
                            .get()
                            .defaultBlockState()
                            .setValue(BlockMultiblockCell.SOUND, cellSound())
                            .setValue(BlockMultiblockCell.SEALED, cellsSealRadiation());
        } else {
            BlockMultiblockGeometryCell cell = ModBlocks.geometryCell(cellBucket()).get();
            state = cell.withId(cell.defaultBlockState(), shapeId);
        }
        return cellsHaveAnalogOutput()
                ? state.setValue(BlockMultiblockCell.COMPARATOR, true)
                : state;
    }

    protected @Nullable BlockState swappedCellStateFor(int lx, int ly, int lz, Direction facing) {
        return swappedStateOfKind(maskTable(facing).kindAtLocal(lx, ly, lz), facing);
    }

    protected @Nullable BlockState swappedStateOfKind(int kind, Direction facing) {
        return null;
    }

    protected boolean cellsHaveAnalogOutput() {
        return false;
    }

    protected boolean comparatorCellAt(int lx, int ly, int lz) {
        return false;
    }

    final int comparatorOutputAtCell(
            FoldedOwner owner, Level level, BlockPos cell, Direction side) {
        int dx = cell.getX() - owner.pos().getX(),
                dy = cell.getY() - owner.pos().getY(),
                dz = cell.getZ() - owner.pos().getZ();
        int lx = MultiblockMaskTable.localX(dx, dy, dz, owner.facing());
        int lz = MultiblockMaskTable.localZ(dx, dy, dz, owner.facing());
        return comparatorCellAt(lx, dy, lz)
                ? owner.state().getAnalogOutputSignal(level, owner.pos(), side)
                : 0;
    }

    public final void updateComparatorOutput(ServerLevel level, BlockPos core) {
        Direction facing = level.getBlockState(core).getValue(FACING);
        maskTable(facing)
                .forEachLocalCell(
                        (lx, ly, lz) -> {
                            if (!comparatorCellAt(lx, ly, lz)) return;
                            BlockPos cell =
                                    core.offset(
                                            MultiblockMaskTable.worldX(lx, ly, lz, facing),
                                            ly,
                                            MultiblockMaskTable.worldZ(lx, ly, lz, facing));
                            FabricPlatform.updateNeighbourForOutputSignal(level, cell, this);
                        });
    }

    public boolean cellsOpen() {
        return false;
    }

    public boolean cellsWaterlog() {
        return false;
    }

    public final GeometryCellPartition geometryPartition() {
        return GeometryCellPartition.of(cellsOpen(), cellsSealRadiation(), cellsWaterlog());
    }

    public final CellBuckets.Plain cellKey() {
        return CellBuckets.plain(
                defaultDestroyTime(),
                getExplosionResistance(),
                defaultMapColor(),
                defaultBlockState().getLightEmission(),
                defaultBlockState().requiresCorrectToolForDrops());
    }

    public final CellBuckets.Geometry cellBucket() {
        return CellBuckets.geometry(geometryPartition(), cellKey());
    }

    public boolean cellsSealRadiation() {
        return false;
    }

    public int cellSound() {
        return BlockMultiblockCell.SOUND_STONE;
    }

    private void indexCells(
            LevelAccessor level, BlockPos core, Direction dir, @Nullable BoundingBox clip) {
        CoreIndexScratch scratch = CoreIndexScratch.borrow();
        try {
            MultiblockCoreIndex.checkDimension(level);
            visitPlacedCells(
                    level,
                    core,
                    dir,
                    pos -> {
                        if (clip == null || clip.isInside(pos)) scratch.addCell(pos);
                    });
            flushIndex(level, core, scratch, this);
        } finally {
            scratch.release();
        }
    }

    public boolean isCellState(BlockState state) {
        return false;
    }

    public boolean usesSharedCells() {
        return true;
    }

    public final boolean isCellOf(BlockState state) {
        return isCell(state) || (state.getBlock() == this && isCellState(state));
    }

    @Override
    protected void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        FabricCaps.invalidateCaps(level, pos);
        if (!BUSY.get() && !oldState.is(this) && level instanceof ServerLevel) {
            long[] placing = PLACING.get();
            placing[0] = pos.asLong();
            placing[1] = level.getGameTime() + 1;
        }
        if (wantsNeighborUpdates() && level instanceof ServerLevel server)
            cellNeighborChanged(server, pos, pos);
    }

    public boolean wantsNeighborUpdates() {
        return false;
    }

    public void cellNeighborChanged(ServerLevel level, BlockPos core, BlockPos cell) {}

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block block,
            @Nullable Orientation orientation,
            boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
        if (wantsNeighborUpdates() && level instanceof ServerLevel server)
            cellNeighborChanged(server, pos, pos);
    }

    @Override
    protected final void affectNeighborsAfterRemoval(
            BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        removedAt(state, level, pos, movedByPiston);
    }

    protected void removedAt(
            BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        notifyCell(level, pos);
        if (movedByPiston || BUSY.get()) return;
        demolish(level, pos, this, state.getValue(FACING), false);
    }

    @Override
    public void hbm$blockEntityLoaded(ServerLevel level, BlockEntity be) {
        if (!claimPlacement(be)) reindexLoadedCells(level, be.getBlockPos());
    }

    @Override
    public void hbm$declareEndpoint(ServerLevel level, BlockEntity be) {}

    @Override
    public void hbm$blockEntityRemoved(ServerLevel level, BlockEntity be) {
        invalidateCellCaps(level, be.getBlockPos(), coreFacing(be.getBlockState()));
    }

    public void invalidateCellCaps(ServerLevel level, BlockPos core, Direction facing) {
        BlockPos.MutableBlockPos notify = new BlockPos.MutableBlockPos();
        footprint(facing).visit(core, pos -> FabricCaps.invalidateCaps(level, notify.set(pos)));
    }

    public void reindexLoadedCells(ServerLevel level, BlockPos core) {
        Direction facing = level.getBlockState(core).getValue(FACING);
        CoreIndexScratch scratch = CoreIndexScratch.borrow();
        try {
            MultiblockCoreIndex.checkDimension(level);
            BlockPos.MutableBlockPos notify = scratch.notifyPos;
            long corePacked = core.asLong();
            visitPlacedCells(
                    level,
                    core,
                    facing,
                    pos -> {
                        long recorded =
                                MultiblockSurface.recordedCorePacked(
                                        level, pos.getX(), pos.getY(), pos.getZ());
                        if (recorded != MultiblockSurface.NO_CORE && recorded != corePacked) return;
                        notifyCell(level, notify.set(pos));
                        scratch.addCell(pos);
                    });
            notifyCell(level, core);
            flushIndex(level, core, scratch, this);
            if (ConveyorNeighbors.isReceiver(this)) {
                for (int i = 0; i < scratch.cellCount; i++)
                    ConveyorNeighbors.refreshAround(level, BlockPos.of(scratch.cells[i]));
            }
        } finally {
            scratch.release();
        }
    }

    protected InteractionResult useAtCore(
            BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(core) instanceof MenuProvider menu))
            return InteractionResult.PASS;
        if (player.isSecondaryUseActive()) return InteractionResult.SUCCESS;
        if (!level.isClientSide()) openCoreMenu(player, core, menu);
        return InteractionResult.SUCCESS;
    }

    protected InteractionResult useItemOnAtCore(
            ItemStack held,
            BlockState coreState,
            Level level,
            BlockPos core,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    public void onExplosionHitAtCore(
            BlockState coreState,
            ServerLevel level,
            BlockPos core,
            Explosion explosion,
            BiConsumer<ItemStack, BlockPos> onHit) {
        onExplosionHit(coreState, level, core, explosion, onHit);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return useAtCore(state, level, pos, player, hit);
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack held,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        return useItemOnAtCore(held, state, level, pos, player, hand, hit);
    }

    protected final void openCoreMenu(Player player, BlockPos core, MenuProvider menu) {
        IGUIProvider.openBlockMenu(player, menu, core);
    }

    @Override
    public @Nullable CompoundTag getSettings(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof ICopiable copiable
                ? copiable.getSettings(level, pos)
                : null;
    }

    @Override
    public void pasteSettings(
            CompoundTag nbt, int index, Level level, Player player, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof ICopiable copiable) {
            copiable.pasteSettings(nbt, index, level, player, pos);
        }
    }

    @Override
    public String @Nullable [] infoForDisplay(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof ICopiable copiable
                ? copiable.infoForDisplay(level, pos)
                : null;
    }

    @Override
    protected int getAnalogOutputSignal(
            BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof BlockEntityMachineBase machine
                ? machine.getComparatorPower()
                : 0;
    }

    @Override
    public BlockPos rigidStructureCore(Level level, BlockPos member) {
        return member.immutable();
    }

    @Override
    public List<BlockPos> rigidStructureBlocks(Level level, BlockPos core) {
        return MultiblockSurface.rigidStructureBlocks(level, core);
    }

    @Override
    public boolean isSameMultiblock(Block other) {
        return other == this || other instanceof BlockMultiblockCell;
    }

    @FunctionalInterface
    public interface FootprintVisitor {
        void cell(BlockPos pos);
    }

    public record FoldedOwner(
            BlockPos pos, BlockState state, BlockMultiblockCore block, Direction facing) {}
}
