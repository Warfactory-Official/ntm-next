// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.bomb;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.bomb.BlockVolcano;
import com.hbm.entity.ModEntities;
import com.hbm.entity.projectile.EntityShrapnel;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockMutatorLava;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.particle.helper.VolcanoSmokeCreator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntityVolcanoCore extends BlockEntity {

    public static final int CYCLE = 10;

    public int volcanoTimer;

    public BlockEntityVolcanoCore(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VOLCANO_CORE.get(), pos, state);
    }

    public static void tick(
            Level level, BlockPos pos, BlockState state, BlockEntityVolcanoCore be) {
        be.serverTick((ServerLevel) level, state);
    }

    private void serverTick(ServerLevel level, BlockState state) {
        BlockVolcano.Mode mode = state.getValue(BlockVolcano.MODE);
        this.volcanoTimer++;

        if (this.volcanoTimer % CYCLE == 0) {
            if (hasVerticalChannel(mode)) {
                blastMagmaChannel(level, state);
                raiseMagma(level, state);
            }

            double magmaChamber = magmaChamberSize(mode);
            if (magmaChamber > 0) blastMagmaChamber(level, state, magmaChamber);

            if (mode == BlockVolcano.Mode.SMOLDERING) meltSurface(level, state, 50, 50D, 10D);

            if (isSpewing(mode)) spawnBlobs(level, state);
            if (isSmoking(mode))
                VolcanoSmokeCreator.composeEffect(
                        level,
                        worldPosition.getX() + 0.5,
                        worldPosition.getY() + 10,
                        worldPosition.getZ() + 0.5);

            surroundLava(level, state);
        }

        if (this.volcanoTimer >= getUpdateRate(mode)) {
            this.volcanoTimer = 0;

            if (mode.growing() && worldPosition.getY() < 200) {
                level.setBlock(worldPosition.above(), state, Block.UPDATE_ALL);
                level.setBlockAndUpdate(worldPosition, lava(state).defaultBlockState());
            } else if (mode.extinguishing()) {
                level.setBlockAndUpdate(worldPosition, lava(state).defaultBlockState());
            }
        }
    }

    private static Block lava(BlockState state) {
        return BlockVolcano.isRadioactive(state)
                ? ModBlocks.RAD_LAVA_BLOCK.get()
                : ModBlocks.VOLCANIC_LAVA_BLOCK.get();
    }

    private static ExplosionVNT blast(
            Level level, BlockState state, double x, double y, double z, float size) {
        return new ExplosionVNT(level, x, y, z, size)
                .setBlockAllocator(new BlockAllocatorStandard())
                .setBlockProcessor(
                        new BlockProcessorStandard()
                                .setNoDrop()
                                .withBlockEffect(new BlockMutatorLava(lava(state))));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.volcanoTimer = input.getIntOr("timer", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("timer", this.volcanoTimer);
    }

    private static boolean isSmoking(BlockVolcano.Mode mode) {
        return mode != BlockVolcano.Mode.SMOLDERING;
    }

    private static boolean isSpewing(BlockVolcano.Mode mode) {
        return mode != BlockVolcano.Mode.SMOLDERING;
    }

    private static boolean hasVerticalChannel(BlockVolcano.Mode mode) {
        return mode != BlockVolcano.Mode.SMOLDERING;
    }

    private static double magmaChamberSize(BlockVolcano.Mode mode) {
        return mode == BlockVolcano.Mode.SMOLDERING ? 15 : 0;
    }

    private static int getUpdateRate(BlockVolcano.Mode mode) {
        return switch (mode) {
            case STATIC_EXTINGUISHING -> 60 * 60 * 20;
            case GROWING_ACTIVE, GROWING_EXTINGUISHING -> 60 * 60 * 20 / 250;
            default -> 10;
        };
    }

    private void blastMagmaChannel(ServerLevel level, BlockState state) {
        RandomSource rand = level.getRandom();
        blast(
                        level,
                        state,
                        worldPosition.getX() + 0.5,
                        worldPosition.getY() + rand.nextInt(15) + 1.5,
                        worldPosition.getZ() + 0.5,
                        7)
                .explode();

        int depth = worldPosition.getY() - level.getMinY() + 1;
        double y = depth > 0 ? level.getMinY() + rand.nextInt(depth) : worldPosition.getY();
        blast(
                        level,
                        state,
                        worldPosition.getX() + 0.5 + rand.nextGaussian() * 3,
                        y,
                        worldPosition.getZ() + 0.5 + rand.nextGaussian() * 3,
                        10)
                .explode();
    }

    private void blastMagmaChamber(ServerLevel level, BlockState state, double size) {
        RandomSource rand = level.getRandom();
        for (int i = 0; i < 2; i++) {
            double dist = size / (double) (i + 1);
            blast(
                            level,
                            state,
                            worldPosition.getX() + 0.5 + rand.nextGaussian() * dist,
                            worldPosition.getY() + 0.5 + rand.nextGaussian() * dist,
                            worldPosition.getZ() + 0.5 + rand.nextGaussian() * dist,
                            7)
                    .explode();
        }
    }

    private void meltSurface(
            ServerLevel level, BlockState state, int count, double radius, double depth) {
        RandomSource rand = level.getRandom();
        Block lava = lava(state);

        for (int i = 0; i < count; i++) {
            int x = (int) Math.floor(worldPosition.getX() + rand.nextGaussian() * radius);
            int z = (int) Math.floor(worldPosition.getZ() + rand.nextGaussian() * radius);

            int y =
                    level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z)
                            + 1
                            - (int) Math.floor(Math.abs(rand.nextGaussian() * depth));
            BlockPos pos = new BlockPos(x, y, z);

            BlockState target = level.getBlockState(pos);
            if (!target.isAir()
                    && target.getBlock().getExplosionResistance()
                            < Blocks.OBSIDIAN.getExplosionResistance()) {
                level.setBlockAndUpdate(
                        pos,
                        target.isSolidRender()
                                ? lava.defaultBlockState()
                                : Blocks.AIR.defaultBlockState());
            }
        }
    }

    private void raiseMagma(ServerLevel level, BlockState state) {
        RandomSource rand = level.getRandom();
        Block lava = lava(state);

        BlockPos pos =
                new BlockPos(
                        worldPosition.getX() - 10 + rand.nextInt(21),
                        worldPosition.getY() + rand.nextInt(11),
                        worldPosition.getZ() - 10 + rand.nextInt(21));

        if (level.getBlockState(pos).isAir() && level.getBlockState(pos.below()).is(lava)) {
            level.setBlockAndUpdate(pos, lava.defaultBlockState());
        }
    }

    private void surroundLava(ServerLevel level, BlockState state) {
        BlockState lava = lava(state).defaultBlockState();

        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                for (int k = -1; k <= 1; k++) {
                    if (i != 0 || j != 0 || k != 0) {
                        level.setBlockAndUpdate(worldPosition.offset(i, j, k), lava);
                    }
                }
            }
        }
    }

    private void spawnBlobs(ServerLevel level, BlockState state) {
        RandomSource rand = level.getRandom();
        int trail =
                BlockVolcano.isRadioactive(state)
                        ? EntityShrapnel.TRAIL_RAD_VOLCANO
                        : EntityShrapnel.TRAIL_VOLCANO;

        for (int i = 0; i < 3; i++) {
            EntityShrapnel frag = new EntityShrapnel(ModEntities.SHRAPNEL.get(), level);
            frag.snapTo(
                    worldPosition.getX() + 0.5,
                    worldPosition.getY() + 1.5,
                    worldPosition.getZ() + 0.5,
                    0.0F,
                    0.0F);
            frag.setDeltaMovement(
                    rand.nextGaussian() * 0.2D, 1D + rand.nextDouble(), rand.nextGaussian() * 0.2D);
            frag.setTrail(trail);
            level.addFreshEntity(frag);
        }
    }
}
