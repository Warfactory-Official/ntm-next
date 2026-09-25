// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.gas;

import com.hbm.particle.AshRevealParticleOptions;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class BlockGasBase extends Block {

    protected BlockGasBase(BlockBehaviour.Properties props) {
        super(props);
    }

    public static BooleanSupplier REVEALED_TO_VIEWER = () -> false;

    protected abstract AshRevealParticleOptions revealTint();

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        if (!REVEALED_TO_VIEWER.getAsBoolean()) return;
        level.addParticle(
                revealTint(),
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                0.0D,
                0.0D,
                0.0D);
    }

    public static BlockBehaviour.Properties defaultProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.NONE)
                .replaceable()
                .noCollision()
                .noOcclusion()
                .instabreak()
                .noLootTable()
                .pushReaction(PushReaction.DESTROY);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    public abstract Direction getFirstDirection(Level level, BlockPos pos, RandomSource rand);

    public Direction getSecondDirection(Level level, BlockPos pos, RandomSource rand) {
        return getFirstDirection(level, pos, rand);
    }

    protected Direction randomHorizontal(RandomSource rand) {
        return Direction.from2DDataValue(rand.nextInt(4));
    }

    public int getDelay(Level level) {
        return 2;
    }

    @Override
    protected void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide()) level.scheduleTick(pos, this, 10);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!tryMove(level, pos, getFirstDirection(level, pos, random))
                && !tryMove(level, pos, getSecondDirection(level, pos, random))) {
            level.scheduleTick(pos, this, getDelay(level));
        }
    }

    protected boolean tryMove(ServerLevel level, BlockPos pos, Direction dir) {
        BlockPos target = pos.relative(dir);
        if (level.getBlockState(target).isAir()) {
            level.setBlockAndUpdate(target, defaultBlockState());
            level.removeBlock(pos, false);
            return true;
        }
        return false;
    }
}
