// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.platform.Services;
import com.hbm.tileentity.machine.BlockEntityCargoElevator;
import com.hbm.util.BlockReadBounds;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class BlockCargoElevator extends BlockMultiblockCore implements ITickingBlock {

    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    private static final int[] DIMENSIONS = {0, 0, 1, 1, 1, 1};

    public BlockCargoElevator(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(PART, Part.ASSEMBLING));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PART);
    }

    @Override
    public int[] getDimensions() {
        return DIMENSIONS;
    }

    @Override
    public int getOffset() {
        return 1;
    }

    @Override
    public boolean usesSharedCells() {
        return false;
    }

    @Override
    public boolean hasVariableFootprint() {
        return true;
    }

    @Override
    public boolean isCellState(BlockState state) {
        Part part = state.getValue(PART);
        return part == Part.SECTION || part == Part.CELL;
    }

    @Override
    protected BlockState cellStateFor(int lx, int ly, int lz, Direction facing, int shapeId) {
        return defaultBlockState()
                .setValue(FACING, facing)
                .setValue(PART, lx == 0 && lz == 0 ? Part.SECTION : Part.CELL);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return isCellState(state) ? null : new BlockEntityCargoElevator(pos, state);
    }

    @Override
    protected void visitPlacedCells(
            LevelAccessor level, BlockPos core, Direction facing, CellVisitor visitor) {
        int height =
                level.getBlockEntity(core) instanceof BlockEntityCargoElevator elevator
                        ? elevator.height
                        : 0;
        visitSections(core, height, visitor);
    }

    @Override
    protected void visitTeardownCells(
            LevelAccessor level, BlockPos core, Direction facing, CellVisitor visitor) {

        visitSections(core, level.getMaxY() - core.getY(), visitor);
    }

    private static void visitSections(BlockPos core, int height, CellVisitor visitor) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = 0; y <= height; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    visitor.cell(pos.setWithOffset(core, x, y, z), MASK_NONE);
                }
            }
        }
    }

    @Override
    public boolean claimsCell(BlockGetter level, BlockPos core, BlockPos cell, Direction facing) {
        int dy = cell.getY() - core.getY();
        if (dy < 0
                || Math.abs(cell.getX() - core.getX()) > 1
                || Math.abs(cell.getZ() - core.getZ()) > 1) {
            return false;
        }
        return level.getBlockState(cell).is(this)
                && level.getBlockEntity(core) instanceof BlockEntityCargoElevator elevator
                && dy <= elevator.height;
    }

    @Override
    public @Nullable BlockPos findClientCore(BlockGetter level, BlockPos cell) {
        int minY = level.getMinY();
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                probe.setWithOffset(cell, x, 0, z);
                if (!BlockReadBounds.canRead(level, probe.getX(), probe.getY(), probe.getZ()))
                    continue;
                BlockState state = level.getBlockState(probe);
                if (!state.is(this) || state.getValue(PART) == Part.CELL) continue;
                while (state.getValue(PART) == Part.SECTION && probe.getY() > minY) {
                    probe.move(Direction.DOWN);
                    if (!BlockReadBounds.canRead(level, probe.getX(), probe.getY(), probe.getZ()))
                        break;
                    state = level.getBlockState(probe);
                    if (!state.is(this)) break;
                }
                if (state.is(this) && !isCellState(state)) return probe.immutable();
            }
        }
        return null;
    }

    public @Nullable BlockEntityCargoElevator elevator(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(this)) return null;
        BlockPos core =
                isCellState(state)
                        ? level instanceof ServerLevel server
                                ? MultiblockSurface.indexedCore(server, pos)
                                : MultiblockSurface.clientCoreOf(level, pos)
                        : pos;
        return core != null
                        && level.getBlockEntity(core) instanceof BlockEntityCargoElevator elevator
                ? elevator
                : null;
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        BlockEntityCargoElevator elevator = elevator(level, pos);
        if (elevator == null) return Shapes.empty();
        BlockPos core = elevator.getBlockPos();
        return elevator.shape()
                .move(core.getX() - pos.getX(), core.getY() - pos.getY(), core.getZ() - pos.getZ());
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
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
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player.isSecondaryUseActive()) return InteractionResult.PASS;
        BlockEntityCargoElevator elevator = elevator(level, pos);
        if (elevator == null) return InteractionResult.SUCCESS;
        if (held.is(asItem())) {
            if (elevator.addSection()) held.consume(1, player);
        } else {
            elevator.toggleElevator();
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return useItemOn(
                ItemStack.EMPTY, state, level, pos, player, InteractionHand.MAIN_HAND, hit);
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block block,
            @Nullable Orientation orientation,
            boolean movedByPiston) {
        if (!level.isClientSide()) {
            BlockEntityCargoElevator elevator = elevator(level, pos);
            if (elevator != null) elevator.checkLower = true;
        }
    }

    @Override
    protected void removedAt(
            BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        if (isCellState(state)) foldedCellRemoved(level, pos, movedByPiston);
        else super.removedAt(state, level, pos, movedByPiston);
    }

    @Override
    public BlockState playerWillDestroy(
            Level level, BlockPos pos, BlockState state, Player player) {
        if (isCellState(state)
                && level instanceof ServerLevel server
                && !player.preventsBlockDrops()
                && Services.PLATFORM.canHarvestBlock(level, pos, state, player)) {
            BlockEntityCargoElevator elevator = elevator(level, pos);
            if (elevator != null) {
                Block.dropResources(
                        elevator.getBlockState(),
                        server,
                        pos,
                        elevator,
                        player,
                        player.getMainHandItem());
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (isCellState(state)) return List.of();
        if (!(params.getOptionalParameter(LootContextParams.BLOCK_ENTITY)
                instanceof BlockEntityCargoElevator elevator)) {
            return super.getDrops(state, params);
        }
        List<ItemStack> drops = new ArrayList<>();
        for (int remaining = elevator.height + 1; remaining > 0; remaining -= 64) {
            drops.add(new ItemStack(this, Math.min(remaining, 64)));
        }
        return drops;
    }

    @Override
    protected void onExplosionHit(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            Explosion explosion,
            BiConsumer<ItemStack, BlockPos> onHit) {
        if (isCellState(state)) {
            BlockEntityCargoElevator elevator = elevator(level, pos);
            if (elevator != null) {
                super.onExplosionHit(
                        elevator.getBlockState(), level, elevator.getBlockPos(), explosion, onHit);
            }
        } else {
            super.onExplosionHit(state, level, pos, explosion, onHit);
        }
    }

    @Override
    protected ItemStack getCloneItemStack(
            LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(this);
    }

    @Override
    public @Nullable BlockPos rigidStructureCore(Level level, BlockPos member) {
        return MultiblockSurface.coreOfAny(level, member);
    }

    public enum Part implements StringRepresentable {
        ASSEMBLING("assembling"),
        CORE("core"),
        SECTION("section"),
        CELL("cell");

        private final String name;

        Part(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
