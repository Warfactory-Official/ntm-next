// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.bomb.BlockBarrelExplosive;
import com.hbm.entity.item.EntityTntNtm;
import com.hbm.explosion.ExplosionNukeGeneric;
import com.hbm.handler.radiation.RadiationSystemNT;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class BlockYellowBarrel extends BlockBarrelExplosive {

    private final boolean chainPrimes;

    public BlockYellowBarrel(BlockBehaviour.Properties props, boolean chainPrimes) {

        super(props, 100, false);
        this.chainPrimes = chainPrimes;
    }

    @Override
    protected void primeFromBlast(Level level, BlockPos pos, @Nullable Explosion explosion) {
        if (!chainPrimes) return;
        super.primeFromBlast(level, pos, explosion);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        level.addParticle(
                ParticleTypes.MYCELIUM,
                pos.getX() + rand.nextFloat() * 0.5F + 0.25F,
                pos.getY() + 1.1F,
                pos.getZ() + rand.nextFloat() * 0.5F + 0.25F,
                0.0D,
                0.0D,
                0.0D);
    }

    @Override
    public void explodeEntity(
            Level level, double x, double y, double z, @Nullable EntityTntNtm entity) {
        BlockPos center = BlockPos.containing(x, y, z);

        if (level.getRandom().nextInt(3) == 0) {
            level.setBlockAndUpdate(center, ModBlocks.TOXIC_BLOCK.get().defaultBlockState());
        } else {

            level.explode(entity, x, y, z, 12.0F, false, Level.ExplosionInteraction.TNT);
        }
        if (level instanceof ServerLevel serverLevel) {
            ExplosionNukeGeneric.waste(level, center, 35, serverLevel.getRandom());
            RandomSource rand = serverLevel.getRandom();
            BlockPos.MutableBlockPos scan = new BlockPos.MutableBlockPos();
            for (int i = -5; i <= 5; i++) {
                for (int j = -5; j <= 5; j++) {
                    for (int k = -5; k <= 5; k++) {
                        scan.set(center.getX() + i, center.getY() + j, center.getZ() + k);
                        if (rand.nextInt(5) == 0 && level.getBlockState(scan).is(Blocks.AIR)) {
                            level.setBlockAndUpdate(
                                    scan, ModBlocks.GAS_RADON_DENSE.get().defaultBlockState());
                        }
                    }
                }
            }

            RadiationSystemNT.incrementRad(serverLevel, center, 35.0, 100.0);
        }
    }
}
