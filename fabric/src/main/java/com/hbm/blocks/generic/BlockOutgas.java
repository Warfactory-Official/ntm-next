// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.blocks.ModBlocks;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

public class BlockOutgas extends Block {

    private final Supplier<Block> gas;
    private final boolean onBreak;
    private final boolean onNeighbour;

    public BlockOutgas(BlockBehaviour.Properties props, Supplier<Block> gas, boolean onBreak) {
        this(props, gas, onBreak, false);
    }

    public BlockOutgas(
            BlockBehaviour.Properties props,
            Supplier<Block> gas,
            boolean onBreak,
            boolean onNeighbour) {
        super(props);
        this.gas = gas;
        this.onBreak = onBreak;
        this.onNeighbour = onNeighbour;
    }

    protected Block getGas() {
        return gas.get();
    }

    @Override
    protected void randomTick(
            BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        vent(level, pos.relative(Direction.from3DDataValue(random.nextInt(6))));
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (state.isRandomlyTicking() && getGas() == ModBlocks.GAS_ASBESTOS.get()) {
            BlockPos above = pos.above();
            if (level.getBlockState(above).isAir()) {
                RandomSource rand = level.getRandom();
                if (level instanceof ServerLevel server) {
                    if (rand.nextInt(10) == 0) vent(server, above);
                } else {

                    for (int i = 0; i < 5; i++) {
                        level.addParticle(
                                ParticleTypes.MYCELIUM,
                                pos.getX() + rand.nextFloat(),
                                pos.getY() + 1.1,
                                pos.getZ() + rand.nextFloat(),
                                0.0,
                                0.0,
                                0.0);
                    }
                }
            }
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    protected void spawnAfterBreak(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            ItemStack tool,
            boolean dropExperience) {
        super.spawnAfterBreak(state, level, pos, tool, dropExperience);
        if (onBreak) level.setBlockAndUpdate(pos, getGas().defaultBlockState());
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block block,
            @Nullable Orientation orientation,
            boolean movedByPiston) {
        if (!onNeighbour
                || !(level instanceof ServerLevel server)
                || level.getRandom().nextInt(3) != 0) return;
        for (Direction dir : Direction.VALUES) vent(server, pos.relative(dir));
    }

    @Override
    protected void affectNeighborsAfterRemoval(
            BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        if (this != ModBlocks.ANCIENT_SCRAP.get()) return;
        for (int ix = -2; ix <= 2; ix++) {
            for (int iy = -2; iy <= 2; iy++) {
                for (int iz = -2; iz <= 2; iz++) {
                    int sum = Math.abs(ix + iy + iz);
                    if (sum < 5 && sum > 0) vent(level, pos.offset(ix, iy, iz));
                }
            }
        }
    }

    private void vent(ServerLevel level, BlockPos pos) {
        if (level.getBlockState(pos).isAir())
            level.setBlockAndUpdate(pos, getGas().defaultBlockState());
    }
}
