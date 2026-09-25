// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.bomb;

import com.hbm.blocks.ITickingBlock;
import com.hbm.interfaces.IBomb;
import com.hbm.interfaces.IToolable;
import com.hbm.tileentity.bomb.BlockEntityLandmine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public abstract class BlockLandmine extends Block implements ITickingBlock, IBomb, IToolable {

    private static final ThreadLocal<Boolean> SAFE_REMOVAL = ThreadLocal.withInitial(() -> false);

    public final double range;
    public final double height;

    protected BlockLandmine(BlockBehaviour.Properties props, double range, double height) {
        super(props);
        this.range = range;
        this.height = height;
    }

    protected abstract VoxelShape shape();

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape();
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityLandmine(pos, state);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return below.isFaceSturdy(level, pos.below(), Direction.UP)
                || below.getBlock() instanceof FenceBlock;
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block neighborBlock,
            @Nullable Orientation orientation,
            boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
        if (level.isClientSide()) return;
        if (level.hasNeighborSignal(pos)) {
            explode(level, pos, null);
            return;
        }
        if (!canSurvive(state, level, pos)) {

            if (SAFE_REMOVAL.get()) level.removeBlock(pos, false);
            else explode(level, pos, null);
        }
    }

    @Override
    public BlockState playerWillDestroy(
            Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && !SAFE_REMOVAL.get()) {
            explode(level, pos, player);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public boolean onScrew(
            Level level,
            Player player,
            BlockPos pos,
            @Nullable Direction side,
            Vec3 hit,
            ToolType tool) {
        if (tool != ToolType.DEFUSER) return false;
        if (!level.isClientSide()) {
            safely(() -> level.removeBlock(pos, false));
            Block.popResource(level, pos, new ItemStack(this));
        }
        return true;
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, @Nullable Entity detonator) {
        if (level.isClientSide()) return BombReturnCode.UNDEFINED;

        safely(() -> level.destroyBlock(pos, false));
        explodeVariant(level, pos);
        return BombReturnCode.DETONATED;
    }

    private static void safely(Runnable removal) {
        boolean previous = SAFE_REMOVAL.get();
        SAFE_REMOVAL.set(true);
        try {
            removal.run();
        } finally {
            SAFE_REMOVAL.set(previous);
        }
    }

    protected abstract void explodeVariant(Level level, BlockPos pos);
}
