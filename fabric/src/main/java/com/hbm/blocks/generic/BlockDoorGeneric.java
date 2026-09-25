// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.blocks.multiblock.MultiblockMaskTable;
import com.hbm.interfaces.IBomb;
import com.hbm.interfaces.IToolable;
import com.hbm.platform.Services;
import com.hbm.tileentity.BlockEntityDoorGeneric;
import com.hbm.tileentity.DoorDecl;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class BlockDoorGeneric extends BlockMultiblockCore
        implements ITickingBlock, IBomb, IToolable {

    public static final IntegerProperty SKIN = IntegerProperty.create("skin", 0, 6);

    public static final BooleanProperty OPEN = BooleanProperty.create("open");

    public final DoorDecl type;

    private final boolean sealsRadiation;

    public BlockDoorGeneric(Properties props, DoorDecl type) {
        this(props, type, false);
    }

    public BlockDoorGeneric(Properties props, DoorDecl type, boolean sealsRadiation) {
        super(props);
        this.type = type;
        this.sealsRadiation = sealsRadiation;
        registerDefaultState(defaultBlockState().setValue(SKIN, 0).setValue(OPEN, Boolean.FALSE));
    }

    private static boolean isFullCube(AABB box) {
        return box.minX == 0D
                && box.minY == 0D
                && box.minZ == 0D
                && box.maxX == 1D
                && box.maxY == 1D
                && box.maxZ == 1D;
    }

    private static BlockPos toDoorLocal(int dx, int dy, int dz, Direction dir) {
        return switch (dir) {
            case NORTH -> new BlockPos(dz, dy, -dx);
            case SOUTH -> new BlockPos(-dz, dy, dx);
            case EAST -> new BlockPos(-dx, dy, -dz);
            default -> new BlockPos(dx, dy, dz);
        };
    }

    private static AABB toWorldFrame(AABB box, Direction dir) {
        return switch (dir) {
            case NORTH ->
                    new AABB(
                            1 - box.minX,
                            box.minY,
                            1 - box.minZ,
                            1 - box.maxX,
                            box.maxY,
                            1 - box.maxZ);
            case WEST ->
                    new AABB(1 - box.minZ, box.minY, box.minX, 1 - box.maxZ, box.maxY, box.maxX);
            case EAST ->
                    new AABB(box.minZ, box.minY, 1 - box.maxX, box.maxZ, box.maxY, 1 - box.minX);
            default -> box;
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SKIN, OPEN);
    }

    @Override
    public boolean cellsSealRadiation() {
        return sealsRadiation;
    }

    @Override
    public boolean cellsOpen() {
        return true;
    }

    @Override
    public int[] getDimensions() {
        return type.getDimensions();
    }

    @Override
    public int getOffset() {
        return type.getBlockOffset();
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityDoorGeneric(pos, state);
    }

    @Override
    public boolean checkRequirement(Level level, BlockPos placed, Direction dir, int o) {
        if (!super.checkRequirement(level, placed, dir, o)) return false;
        if (type.getExtraDimensions() == null) return true;
        BlockPos origin = placed.offset(dir.getStepX() * o, dir.getStepY() * o, dir.getStepZ() * o);
        for (int[] dims : type.getExtraDimensions()) {
            if (!MultiblockHandlerXR.checkSpace(level, origin, dims, placed, dir)) return false;
        }
        return true;
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        if (type.getExtraDimensions() == null) return;
        for (int[] dims : type.getExtraDimensions()) {
            MultiblockHandlerXR.visitBox(core, dims, facing, visitor);
        }
    }

    @Override
    public @Nullable VoxelShape cellShape(
            int lx, int ly, int lz, Direction facing, boolean open, boolean forCollision) {

        int dx = MultiblockMaskTable.worldX(lx, ly, lz, facing);
        int dz = MultiblockMaskTable.worldZ(lx, ly, lz, facing);
        BlockPos local = toDoorLocal(dx, ly, dz, facing);
        AABB box = type.getBlockBound(local.getX(), local.getY(), local.getZ(), open, forCollision);
        AABB world = toWorldFrame(box, facing);

        if (world.getXsize() == 0 && world.getYsize() == 0 && world.getZsize() == 0)
            return Shapes.empty();
        if (isFullCube(world)) return null;
        return Shapes.create(world);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return coreShape(state, level, pos, false);
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return coreShape(state, level, pos, true);
    }

    private VoxelShape coreShape(
            BlockState state, BlockGetter level, BlockPos pos, boolean forCollision) {
        boolean open =
                level.getBlockEntity(pos) instanceof BlockEntityDoorGeneric door
                        && door.state != BlockEntityDoorGeneric.STATE_CLOSED;
        VoxelShape shape = cellShape(0, 0, 0, state.getValue(FACING), open, forCollision);
        return shape == null ? Shapes.block() : shape;
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (Services.CONFIG.runtime().ultraLarpMode() && type != DoorDecl.WATER_DOOR)
            return InteractionResult.SUCCESS;
        return level.getBlockEntity(core) instanceof BlockEntityDoorGeneric door
                        && door.tryToggle(player)
                ? InteractionResult.SUCCESS
                : InteractionResult.PASS;
    }

    @Override
    public boolean onScrew(
            Level level,
            Player player,
            BlockPos pos,
            @Nullable Direction side,
            Vec3 hit,
            ToolType tool) {
        if (tool != ToolType.SCREWDRIVER || !player.isShiftKeyDown()) return false;
        if (!(level.getBlockEntity(pos) instanceof BlockEntityDoorGeneric door) || !type.hasSkins())
            return false;
        if (level.isClientSide()) return true;
        door.cycleSkinIndex();
        return true;
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, @Nullable Entity detonator) {
        if (level.isClientSide()) return BombReturnCode.UNDEFINED;
        if (!type.remoteControllable()) return BombReturnCode.ERROR_INCOMPATIBLE;
        return level.getBlockEntity(pos) instanceof BlockEntityDoorGeneric door
                        && door.tryToggle(null)
                ? BombReturnCode.TRIGGERED
                : BombReturnCode.ERROR_INCOMPATIBLE;
    }

    @Override
    public boolean wantsNeighborUpdates() {
        return true;
    }

    @Override
    public void cellNeighborChanged(ServerLevel level, BlockPos core, BlockPos cell) {
        if (level.getBlockEntity(core) instanceof BlockEntityDoorGeneric door)
            door.updateRedstonePower(cell);
    }
}
