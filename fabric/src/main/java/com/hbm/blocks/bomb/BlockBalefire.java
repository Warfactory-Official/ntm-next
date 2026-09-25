// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.bomb;

import com.hbm.platform.Services;
import com.hbm.potion.HbmPotion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.InsideBlockEffectType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlockBalefire extends FireBlock {

    public BlockBalefire(Properties props) {
        super(props);
    }

    @Override
    protected void entityInside(
            BlockState state,
            Level level,
            BlockPos pos,
            Entity entity,
            InsideBlockEffectApplier effectApplier,
            boolean isPrecise) {
        effectApplier.runAfter(InsideBlockEffectType.FIRE_IGNITE, e -> e.igniteForSeconds(10.0F));
        if (!level.isClientSide() && entity instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(HbmPotion.radiation(), 5 * 20, 9));
        }
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.canSpreadFireAround(pos)) {
            level.scheduleTick(pos, this, 30 + random.nextInt(10));
            return;
        }
        if (!canSurvive(state, level, pos)) {
            level.removeBlock(pos, false);
            return;
        }

        int age = state.getValue(AGE);
        if (age < 15) level.scheduleTick(pos, this, 30 + random.nextInt(10));

        BlockPos below = pos.below();
        boolean solidTop = level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
        if (!canNeighborBurn(level, pos) && !solidTop) {
            level.removeBlock(pos, false);
            return;
        }
        if (age >= 15) return;

        tryCatchFire(level, pos, Direction.EAST, 500, random, age);
        tryCatchFire(level, pos, Direction.WEST, 500, random, age);
        tryCatchFire(level, pos, Direction.DOWN, 300, random, age);
        tryCatchFire(level, pos, Direction.UP, 300, random, age);
        tryCatchFire(level, pos, Direction.NORTH, 500, random, age);
        tryCatchFire(level, pos, Direction.SOUTH, 500, random, age);

        BlockPos.MutableBlockPos test = new BlockPos.MutableBlockPos();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                for (int dy = -1; dy <= 4; dy++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;

                    int fireLimit = 100;
                    if (dy > 1) fireLimit += (dy - 1) * 100;

                    test.setWithOffset(pos, dx, dy, dz);
                    BlockState there = level.getBlockState(test);
                    if (there.is(this) && there.getValue(AGE) > age + 1) {
                        level.setBlock(test, there.setValue(AGE, age + 1), 3);
                        continue;
                    }
                    if (!there.isAir()) continue;

                    int odds = neighborIgniteOdds(level, test);
                    if (odds <= 0) continue;

                    int chance = (odds + 40 + level.getDifficulty().getId() * 7) / (age + 30);
                    if (chance > 0 && random.nextInt(fireLimit) <= chance) {
                        level.setBlock(test, defaultBlockState().setValue(AGE, age + 1), 3);
                    }
                }
            }
        }
    }

    private void tryCatchFire(
            Level level,
            BlockPos fire,
            Direction toward,
            int chance,
            RandomSource random,
            int age) {
        BlockPos pos = fire.relative(toward);
        BlockState burnt = level.getBlockState(pos);
        int odds = Services.PLATFORM.burnOdds(level, pos, burnt, toward.getOpposite());
        if (odds <= 0 || random.nextInt(chance) >= odds) return;
        level.setBlock(pos, defaultBlockState().setValue(AGE, age + 1), 3);
        if (burnt.getBlock() instanceof TntBlock) TntBlock.prime(level, pos);
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess ticks,
            BlockPos pos,
            Direction directionToNeighbour,
            BlockPos neighbourPos,
            BlockState neighbourState,
            RandomSource random) {
        return canSurvive(state, level, pos) ? state : Blocks.AIR.defaultBlockState();
    }

    private boolean canNeighborBurn(Level level, BlockPos pos) {
        for (Direction dir : Direction.VALUES) {
            BlockPos neighbor = pos.relative(dir);
            if (Services.PLATFORM.isFlammable(
                    level, neighbor, level.getBlockState(neighbor), dir.getOpposite())) {
                return true;
            }
        }
        return false;
    }

    private int neighborIgniteOdds(Level level, BlockPos pos) {
        int odds = 0;
        for (Direction dir : Direction.VALUES) {
            BlockPos neighbor = pos.relative(dir);
            odds =
                    Math.max(
                            odds,
                            Services.PLATFORM.igniteOdds(
                                    level,
                                    neighbor,
                                    level.getBlockState(neighbor),
                                    dir.getOpposite()));
        }
        return odds;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        BlockState result = state;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            result =
                    result.setValue(
                            PROPERTY_BY_DIRECTION.get(rotation.rotate(dir)),
                            state.getValue(PROPERTY_BY_DIRECTION.get(dir)));
        }
        return result;
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        BlockState result = state;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            result =
                    result.setValue(
                            PROPERTY_BY_DIRECTION.get(mirror.mirror(dir)),
                            state.getValue(PROPERTY_BY_DIRECTION.get(dir)));
        }
        return result;
    }
}
