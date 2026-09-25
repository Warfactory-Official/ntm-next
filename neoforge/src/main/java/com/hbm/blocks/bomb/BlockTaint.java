// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.bomb;

import com.hbm.entity.ModEntities;
import com.hbm.entity.mob.EntityCreeperTainted;
import com.hbm.entity.mob.EntityTaintCrab;
import com.hbm.entity.mob.EntityTeslaCrab;
import com.hbm.potion.HbmPotion;
import com.hbm.potion.UncurableEffectInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlockTaint extends Block {

    public static final int MAX_AGE = 15;
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, MAX_AGE);

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 12, 16);

    public BlockTaint(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void randomTick(
            BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {

        int age = state.getValue(AGE);
        if (age >= MAX_AGE) return;

        for (int i = -3; i <= 3; i++) {
            for (int j = -3; j <= 3; j++) {
                for (int k = -3; k <= 3; k++) {

                    if (Math.abs(i) + Math.abs(j) + Math.abs(k) > 4) continue;
                    if (random.nextFloat() > 0.25F) continue;

                    BlockPos target = pos.offset(i, j, k);
                    BlockState targetState = level.getBlockState(target);
                    if (targetState.isAir() || targetState.is(Blocks.BEDROCK)) continue;

                    boolean hasAir = false;
                    for (Direction dir : Direction.values()) {
                        if (level.getBlockState(target.relative(dir)).isAir()) {
                            hasAir = true;
                            break;
                        }
                    }

                    int targetAge = hasAir ? age + 1 : age + 3;
                    if (targetAge > MAX_AGE) continue;
                    if (targetState.is(this) && targetState.getValue(AGE) >= targetAge) continue;

                    BlockState spread = defaultBlockState().setValue(AGE, targetAge);
                    level.setBlock(target, spread, UPDATE_ALL);

                    if (random.nextFloat() < 0.25F
                            && FallingBlock.isFree(level.getBlockState(target.below()))) {
                        FallingBlockEntity.fall(level, target, spread);
                    }
                }
            }
        }
    }

    @Override
    protected void entityInside(
            BlockState state,
            Level level,
            BlockPos pos,
            Entity entity,
            InsideBlockEffectApplier applier,
            boolean isPrecise) {

        int amplifier = MAX_AGE - state.getValue(AGE);

        entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.6D, 1.0D, 0.6D));

        if (entity instanceof LivingEntity living && level.getRandom().nextInt(50) == 0) {
            living.addEffect(new UncurableEffectInstance(HbmPotion.taint(), 15 * 20, amplifier));
        }

        if (level.isClientSide()) return;

        if (entity.getClass() == Creeper.class) {
            EntityCreeperTainted creep =
                    new EntityCreeperTainted(ModEntities.CREEPER_TAINTED.get(), level);
            creep.snapTo(
                    entity.getX(),
                    entity.getY(),
                    entity.getZ(),
                    entity.getYRot(),
                    entity.getXRot());
            entity.discard();
            level.addFreshEntity(creep);
        }

        if (entity instanceof EntityTeslaCrab) {
            EntityTaintCrab crab = new EntityTaintCrab(ModEntities.TAINT_CRAB.get(), level);
            crab.snapTo(
                    entity.getX(),
                    entity.getY(),
                    entity.getZ(),
                    entity.getYRot(),
                    entity.getXRot());
            entity.discard();
            level.addFreshEntity(crab);
        }
    }
}
