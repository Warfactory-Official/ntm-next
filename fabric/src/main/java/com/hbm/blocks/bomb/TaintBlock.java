/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.blocks.bomb;

import com.hbm.entity.ModEntities;
import com.hbm.entity.mob.CreeperTaintedEntity;
import com.hbm.entity.mob.TaintCrabEntity;
import com.hbm.entity.mob.TeslaCrabEntity;
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
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TaintBlock extends Block {
    private static final VoxelShape COLLISION = Block.box(0, 0, 0, 16, 12, 16);

    public static final int MAX_AGE = 15;
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, MAX_AGE);

    public TaintBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return COLLISION;
    }

    @Override
    protected void randomTick(
            BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        var sourceAge = state.getValue(AGE);
        if (sourceAge == MAX_AGE) {
            return;
        }

        var candidate = new BlockPos.MutableBlockPos();
        for (var offsetX = -3; offsetX <= 3; offsetX++) {
            for (var offsetY = -3; offsetY <= 3; offsetY++) {
                for (var offsetZ = -3; offsetZ <= 3; offsetZ++) {
                    if (Math.abs(offsetX) + Math.abs(offsetY) + Math.abs(offsetZ) > 4) {
                        continue;
                    }
                    if (random.nextFloat() > 0.25F) {
                        continue;
                    }

                    candidate.set(pos.getX() + offsetX, pos.getY() + offsetY, pos.getZ() + offsetZ);
                    var target = level.getBlockState(candidate);
                    if (target.isAir() || target.is(Blocks.BEDROCK)) {
                        continue;
                    }

                    var candidateAge = sourceAge + 3;
                    for (var direction : Direction.VALUES) {
                        if (level.isEmptyBlock(candidate.relative(direction))) {
                            candidateAge = sourceAge + 1;
                            break;
                        }
                    }
                    if (candidateAge > MAX_AGE) {
                        continue;
                    }
                    if (target.is(this) && target.getValue(AGE) >= candidateAge) {
                        continue;
                    }

                    var replacement = defaultBlockState().setValue(AGE, candidateAge);
                    level.setBlockAndUpdate(candidate, replacement);
                    if (random.nextFloat() < 0.25F
                            && FallingBlock.isFree(level.getBlockState(candidate.below()))) {
                        FallingBlockEntity.fall(level, candidate, replacement);
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
            InsideBlockEffectApplier effectApplier,
            boolean isPrecise) {
        var movement = entity.getDeltaMovement();
        entity.setDeltaMovement(movement.x * 0.6, movement.y, movement.z * 0.6);
        if (entity instanceof LivingEntity living && level.getRandom().nextInt(50) == 0) {
            living.addEffect(
                    new UncurableEffectInstance(
                            HbmPotion.taint(), 300, MAX_AGE - state.getValue(AGE)));
        }
        if (!(level instanceof ServerLevel server)) {
            return;
        }

        Entity replacement;
        if (entity.getClass() == Creeper.class) {
            replacement = new CreeperTaintedEntity(ModEntities.CREEPER_TAINTED.get(), server);
        } else if (entity instanceof TeslaCrabEntity) {
            replacement = new TaintCrabEntity(ModEntities.TAINT_CRAB.get(), server);
        } else {
            return;
        }
        replacement.snapTo(
                entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), entity.getXRot());
        entity.discard();
        server.addFreshEntity(replacement);
    }
}
