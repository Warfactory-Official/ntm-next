// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine.rbmk;

import com.hbm.blocks.ModBlocks;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.packet.toclient.RbmkJetPayload;
import com.hbm.platform.Services;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class RBMKDebrisBurning extends Block {

    public static final MapCodec<RBMKDebrisBurning> CODEC = simpleCodec(RBMKDebrisBurning::new);

    protected static final int FLAME_CHANCE = 5;
    protected static final int FLAME_MAX_AGE = 300;
    protected static final int FLAME_RANGE = 75;

    private static final int VENT_CHANCE = 10;

    private static final int QUENCH_CHANCE_SMOTHERED = 10;
    private static final int QUENCH_CHANCE_BARE = 100;

    public RBMKDebrisBurning(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    protected int tickRate(RandomSource rand) {
        return 100 + rand.nextInt(20);
    }

    @Override
    protected void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!(level instanceof ServerLevel server) || state.is(oldState.getBlock())) return;
        if (server.getRandom().nextInt(3) == 0) flame(server, pos, 0.25D, 0.5D);
        server.scheduleTick(pos, this, tickRate(server.getRandom()));
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand) {
        if (rand.nextInt(FLAME_CHANCE) == 0) {
            flame(level, pos, 0.25D, 0.5D);
            level.playSound(
                    null,
                    pos,
                    SoundEvents.FIRE_AMBIENT,
                    SoundSource.BLOCKS,
                    1.0F + rand.nextFloat(),
                    rand.nextFloat() * 0.7F + 0.3F);
        }

        BlockPos side = pos.relative(Direction.VALUES[rand.nextInt(6)]);
        BlockState neighbour = level.getBlockState(side);

        if (rand.nextInt(VENT_CHANCE) == 0 && neighbour.isAir()) {
            level.setBlockAndUpdate(side, ModBlocks.GAS_MELTDOWN.get().defaultBlockState());
        }

        if (rand.nextInt(quenchChance(neighbour)) == 0) quench(level, pos);
        else level.scheduleTick(pos, this, tickRate(rand));
    }

    protected int quenchChance(BlockState neighbour) {
        boolean smothered =
                neighbour.is(ModBlocks.FOAM_LAYER.get())
                        || neighbour.is(ModBlocks.BLOCK_FOAM.get())
                        || neighbour.is(ModBlocks.SAND_BORON_LAYER.get())
                        || neighbour.is(ModBlocks.SAND_BORON.get());
        return smothered ? QUENCH_CHANCE_SMOTHERED : QUENCH_CHANCE_BARE;
    }

    protected void quench(ServerLevel level, BlockPos pos) {
        level.setBlockAndUpdate(pos, ModBlocks.RBMK_DEBRIS.get().defaultBlockState());
    }

    protected static void flame(ServerLevel level, BlockPos pos, double xzMin, double xzWidth) {
        RandomSource rand = level.getRandom();
        double px = pos.getX() + xzMin + rand.nextDouble() * xzWidth;
        double py = pos.getY() + 1.75D;
        double pz = pos.getZ() + xzMin + rand.nextDouble() * xzWidth;
        Services.NETWORK.sendToAllAround(
                RbmkJetPayload.flame(px, py, pz, FLAME_MAX_AGE),
                new TargetPoint(level, pos.getX() + 0.5D, py, pos.getZ() + 0.5D, FLAME_RANGE));
    }
}
