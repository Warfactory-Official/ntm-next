// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.gas;

import com.hbm.particle.AshRevealParticleOptions;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

public class BlockGasFlammable extends BlockGasBase {

    public static final MapCodec<BlockGasFlammable> CODEC = simpleCodec(BlockGasFlammable::new);

    public BlockGasFlammable(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends BlockGasFlammable> codec() {
        return CODEC;
    }

    @Override
    public Direction getFirstDirection(Level level, BlockPos pos, RandomSource rand) {
        if (rand.nextInt(3) == 0) return rand.nextInt(2) == 0 ? Direction.DOWN : Direction.UP;
        return randomHorizontal(rand);
    }

    @Override
    public Direction getSecondDirection(Level level, BlockPos pos, RandomSource rand) {
        return randomHorizontal(rand);
    }

    @Override
    public int getDelay(Level level) {
        return level.getRandom().nextInt(5) + 16;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        for (Direction dir : Direction.VALUES) {
            if (isFireSource(level.getBlockState(pos.relative(dir)))) {
                combust(level, pos);
                return;
            }
        }
        if (random.nextInt(20) == 0 && level.getBlockState(pos.below()).isAir()) {
            level.removeBlock(pos, false);
            return;
        }
        super.tick(state, level, pos, random);
    }

    @Override
    protected void entityInside(
            BlockState state,
            Level level,
            BlockPos pos,
            Entity entity,
            InsideBlockEffectApplier applier,
            boolean isPrecise) {
        if (!level.isClientSide() && entity.isOnFire() && level instanceof ServerLevel server) {
            combust(server, pos);
        }
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
        if (level.isClientSide()) return;
        for (Direction dir : Direction.VALUES) {
            if (isFireSource(level.getBlockState(pos.relative(dir)))) {
                level.scheduleTick(pos, this, 2);
                return;
            }
        }
    }

    protected void combust(ServerLevel level, BlockPos pos) {
        level.setBlockAndUpdate(pos, Blocks.FIRE.defaultBlockState());
    }

    protected boolean isFireSource(BlockState s) {
        return s.is(Blocks.FIRE)
                || s.is(Blocks.SOUL_FIRE)
                || s.is(Blocks.LAVA)
                || s.is(Blocks.TORCH)
                || s.is(Blocks.WALL_TORCH)
                || s.is(Blocks.JACK_O_LANTERN);
    }

    @Override
    protected AshRevealParticleOptions revealTint() {
        return new AshRevealParticleOptions(0.8F, 0.8F, 0.2F);
    }
}
