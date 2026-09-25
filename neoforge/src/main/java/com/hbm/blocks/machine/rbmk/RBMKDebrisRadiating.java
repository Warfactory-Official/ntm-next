// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine.rbmk;

import com.hbm.blocks.ModBlocks;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.items.food.ItemMarshmallow;
import com.hbm.util.ContaminationUtil;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;

public class RBMKDebrisRadiating extends RBMKDebrisBurning {

    public static final IntegerProperty BURN = IntegerProperty.create("burn", 0, 15);

    public static final MapCodec<RBMKDebrisRadiating> CODEC = simpleCodec(RBMKDebrisRadiating::new);

    private static final double RADS = 1_000_000D;
    private static final double RANGE = 100D;

    private static final double BURN_RANGE = 5D;
    private static final float BURN_DAMAGE = 100F;

    public static final int TICK_RATE_BASE = 20;

    public static final int TICK_RATE_SPREAD = 20;

    private static final int STEP_CHANCE_BORON = 25;
    private static final int STEP_CHANCE_BARE = 1000;

    public RBMKDebrisRadiating(BlockBehaviour.Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(BURN, 0));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BURN);
    }

    @Override
    protected int tickRate(RandomSource rand) {
        return TICK_RATE_BASE + rand.nextInt(TICK_RATE_SPREAD);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand) {
        radiate(level, pos);

        if (rand.nextInt(FLAME_CHANCE) == 0) {
            flame(level, pos, 0D, 1D);
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

        if (rand.nextInt(10) == 0 && neighbour.isAir()) {
            level.setBlockAndUpdate(side, ModBlocks.GAS_MELTDOWN.get().defaultBlockState());
        }

        if (rand.nextInt(quenchChance(neighbour)) == 0) {
            int burn = state.getValue(BURN);
            if (burn < 15) {
                level.setBlock(pos, state.setValue(BURN, burn + 1), Block.UPDATE_CLIENTS);
                level.scheduleTick(pos, this, tickRate(rand));
            } else {
                quench(level, pos);
            }
        } else {
            level.scheduleTick(pos, this, tickRate(rand));
        }
    }

    @Override
    protected int quenchChance(BlockState neighbour) {
        return neighbour.is(ModBlocks.SAND_BORON_LAYER.get())
                        || neighbour.is(ModBlocks.SAND_BORON.get())
                ? STEP_CHANCE_BORON
                : STEP_CHANCE_BARE;
    }

    @Override
    protected void quench(ServerLevel level, BlockPos pos) {
        level.setBlockAndUpdate(pos, ModBlocks.RBMK_DEBRIS_BURNING.get().defaultBlockState());
    }

    private static final double ROAST_RANGE = 10D;
    private static final int ROAST_CHANCE = 100;

    private static void radiate(ServerLevel level, BlockPos pos) {
        double cx = pos.getX() + 0.5D, cy = pos.getY() + 0.5D, cz = pos.getZ() + 0.5D;
        ContaminationUtil.radiate(
                level, cx, cy, cz, RANGE, RADS, ContaminationUtil.ContaminationType.CREATIVE);

        AABB close = new AABB(cx, cy, cz, cx, cy, cz).inflate(BURN_RANGE);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, close)) {
            if (entity.distanceToSqr(cx, cy, cz) >= BURN_RANGE * BURN_RANGE) continue;
            entity.hurtServer(level, level.damageSources().inFire(), BURN_DAMAGE);
        }

        AABB reach = new AABB(cx, cy, cz, cx, cy, cz).inflate(ROAST_RANGE);
        for (Player player : level.getEntitiesOfClass(Player.class, reach)) {
            if (player.distanceToSqr(cx, cy, cz) >= ROAST_RANGE * ROAST_RANGE) continue;
            ItemStack held = player.getMainHandItem();
            if (held.is(ModItems.MARSHMALLOW.get())
                    && ItemMarshmallow.isRaw(held)
                    && player.getRandom().nextInt(ROAST_CHANCE) == 0) {
                held.set(ModDataComponents.ROASTED.get(), true);
            }
        }
    }
}
