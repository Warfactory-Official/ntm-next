// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.fluid;

import com.hbm.blocks.ModBlocks;
import com.hbm.lib.Library;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

public class BlockVolcanicLava extends BlockFluidClassicBase {

    public BlockVolcanicLava(ClassicFluid fluid, Properties props) {
        super(fluid, props);
    }

    protected static Block wastePlanks() {
        return requireBlock("waste_planks");
    }

    protected static Block requireBlock(String name) {
        var id = Library.id(name);
        if (!BuiltInRegistries.BLOCK.containsKey(id))
            throw new IllegalStateException("missing block: " + id);
        return BuiltInRegistries.BLOCK.getValue(id);
    }

    protected Block basaltForCheck() {
        return requireBlock("basalt");
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected boolean displaces(BlockState below) {
        return ((FireBlock) Blocks.FIRE).getBurnOdds(below) > 0;
    }

    @Override
    protected void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston) {
        level.scheduleTick(pos, state.getFluidState().getType(), fluid.getTickDelay(level));
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block block,
            @Nullable Orientation orientation,
            boolean movedByPiston) {
        level.scheduleTick(pos, state.getFluidState().getType(), fluid.getTickDelay(level));
        if (level.isClientSide()) return;
        for (Direction dir : Direction.VALUES) {
            BlockPos npos = pos.relative(dir);
            Block reacted = getReaction(level, npos);
            if (reacted != null) level.setBlockAndUpdate(npos, reacted.defaultBlockState());
        }
    }

    @Override
    protected void flowTick(
            BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        trySolidify(state, level, pos, random);
    }

    @Nullable
    protected Block getReaction(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (isWater(state)) return Blocks.STONE;
        if (state.is(BlockTags.LOGS)) return ModBlocks.WASTE_LOG.get();
        if (state.is(BlockTags.PLANKS)) return wastePlanks();
        if (state.is(BlockTags.LEAVES)) return Blocks.FIRE;

        if (state.is(Blocks.DIAMOND_ORE)) return requireBlock("ore_basalt_gem");
        return null;
    }

    protected static boolean isWater(BlockState state) {
        return state.getBlock() instanceof LiquidBlock && state.getFluidState().is(FluidTags.WATER);
    }

    private void trySolidify(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand) {
        int lavaCount = 0;
        int basaltCount = 0;
        Block basalt = basaltForCheck();
        for (Direction dir : Direction.VALUES) {
            BlockState neighbor = level.getBlockState(pos.relative(dir));
            if (neighbor.is(this)) lavaCount++;
            if (neighbor.is(basalt)) basaltCount++;
        }

        boolean roll =
                (!state.getFluidState().isSource() && lavaCount < 2)
                        || (rand.nextInt(5) == 0 && lavaCount < 5);
        if (!roll || level.getBlockState(pos.below()).is(this)) return;

        onSolidify(level, pos, lavaCount, basaltCount, rand);
    }

    protected void onSolidify(
            ServerLevel level, BlockPos pos, int lavaCount, int basaltCount, RandomSource rand) {
        int r = rand.nextInt(200);
        Block basalt = basaltForCheck();
        Block above = level.getBlockState(pos.above(10)).getBlock();
        boolean canMakeGem =
                lavaCount + basaltCount == 6 && lavaCount < 3 && (above == basalt || above == this);

        Block result;
        if (r < 2) result = requireBlock("ore_basalt_sulfur");
        else if (r == 2) result = requireBlock("ore_basalt_fluorite");
        else if (r == 3) result = requireBlock("ore_basalt_asbestos");
        else if (r == 4) result = requireBlock("ore_basalt_molysite");
        else if (r < 15 && canMakeGem) result = requireBlock("ore_basalt_gem");
        else result = basalt;

        level.setBlockAndUpdate(pos, result.defaultBlockState());
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        BlockPos above = pos.above();
        if (level.getBlockState(above).isAir() && !level.getBlockState(above).isSolidRender()) {
            if (rand.nextInt(100) == 0) {
                double xx = pos.getX() + rand.nextFloat();
                double yy = pos.getY() + 1.0D;
                double zz = pos.getZ() + rand.nextFloat();
                level.addParticle(ParticleTypes.LAVA, xx, yy, zz, 0.0D, 0.0D, 0.0D);
                level.playLocalSound(
                        xx,
                        yy,
                        zz,
                        SoundEvents.LAVA_POP,
                        SoundSource.AMBIENT,
                        0.2F + rand.nextFloat() * 0.2F,
                        0.9F + rand.nextFloat() * 0.15F,
                        false);
            }
            if (rand.nextInt(200) == 0) {
                level.playLocalSound(
                        pos.getX(),
                        pos.getY(),
                        pos.getZ(),
                        SoundEvents.LAVA_AMBIENT,
                        SoundSource.AMBIENT,
                        0.2F + rand.nextFloat() * 0.2F,
                        0.9F + rand.nextFloat() * 0.15F,
                        false);
            }
        }

        if (rand.nextInt(10) == 0
                && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                && !level.getBlockState(pos.below(2)).blocksMotion()) {
            double xx = pos.getX() + rand.nextFloat();
            double yy = pos.getY() - 1.05D;
            double zz = pos.getZ() + rand.nextFloat();
            level.addParticle(ParticleTypes.DRIPPING_LAVA, xx, yy, zz, 0.0D, 0.0D, 0.0D);
        }
    }
}
