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

package com.hbm.blocks.generic;

import com.hbm.blockentity.encounter.TrappedBrickBlockEntity;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.lib.ModDamageTypes;
import com.hbm.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class TrappedBrickBlock extends Block {

    public final Trap trap;

    public TrappedBrickBlock(Properties props, Trap trap) {
        super(props);
        this.trap = trap;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState onState, Entity entity) {
        if (level.isClientSide()
                || trap.type != TrapType.ON_STEP
                || !(entity instanceof Player player)) return;
        if (!(level instanceof ServerLevel server)) return;

        switch (trap) {
            case FIRE -> {
                BlockPos above = pos.above();
                if (level.getBlockState(above).canBeReplaced())
                    level.setBlockAndUpdate(above, Blocks.FIRE.defaultBlockState());
            }
            case SPIKES -> {
                BlockPos above = pos.above();
                if (level.getBlockState(above).canBeReplaced()) {
                    level.setBlockAndUpdate(above, ModBlocks.SPIKES.get().defaultBlockState());
                }
                AABB box =
                        new AABB(
                                pos.getX(),
                                pos.getY() + 1,
                                pos.getZ(),
                                pos.getX() + 1,
                                pos.getY() + 2,
                                pos.getZ() + 1);
                for (Entity e : level.getEntitiesOfClass(Entity.class, box)) {
                    e.hurtServer(server, level.damageSources().source(ModDamageTypes.SPIKES), 10F);
                }
                level.playSound(null, pos, ModSounds.SLICER.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            case MINE ->
                    level.explode(
                            null,
                            pos.getX() + 0.5,
                            pos.getY() + 1.5,
                            pos.getZ() + 0.5,
                            1F,
                            Level.ExplosionInteraction.NONE);
            case WEB -> {
                BlockPos above = pos.above();
                if (level.getBlockState(above).canBeReplaced())
                    level.setBlockAndUpdate(above, Blocks.COBWEB.defaultBlockState());
            }
            case RAD_CONVERSION -> convertNearby(level, pos, ModBlocks.BRICK_JUNGLE_OOZE.get());
            case MAGIC_CONVERSTION ->
                    convertNearby(level, pos, ModBlocks.BRICK_JUNGLE_MYSTIC.get());
            case SLOWNESS -> player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 300, 2));
            case WEAKNESS -> player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 300, 2));
            default -> {}
        }

        level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.3F, 0.6F);
        level.setBlockAndUpdate(pos, ModBlocks.BRICK_JUNGLE.get().defaultBlockState());
    }

    private void convertNearby(Level level, BlockPos pos, Block target) {
        RandomSource rand = level.getRandom();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int a = -3; a <= 3; a++) {
            for (int b = -3; b <= 3; b++) {
                for (int c = -3; c <= 3; c++) {
                    if (rand.nextBoolean()) continue;
                    p.set(pos.getX() + a, pos.getY() + b, pos.getZ() + c);
                    Block bl = level.getBlockState(p).getBlock();
                    if (bl == ModBlocks.BRICK_JUNGLE.get()
                            || bl == ModBlocks.BRICK_JUNGLE_CRACKED.get()
                            || bl == ModBlocks.BRICK_JUNGLE_LAVA.get()) {
                        level.setBlockAndUpdate(p, target.defaultBlockState());
                    }
                }
            }
        }
    }

    public static class Detector extends TrappedBrickBlock implements TickingEntityBlock {

        public Detector(Properties props, Trap trap) {
            super(props, trap);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new TrappedBrickBlockEntity(pos, state);
        }
    }

    public enum TrapType {
        ON_STEP,
        DETECTOR
    }

    public enum Trap {
        FALLING_ROCKS(TrapType.DETECTOR),
        FIRE(TrapType.ON_STEP),
        ARROW(TrapType.DETECTOR),
        SPIKES(TrapType.ON_STEP),
        MINE(TrapType.ON_STEP),
        WEB(TrapType.ON_STEP),
        FLAMING_ARROW(TrapType.DETECTOR),
        PILLAR(TrapType.DETECTOR),
        RAD_CONVERSION(TrapType.ON_STEP),
        MAGIC_CONVERSTION(TrapType.ON_STEP),
        SLOWNESS(TrapType.ON_STEP),
        WEAKNESS(TrapType.ON_STEP),
        POISON_DART(TrapType.DETECTOR),
        ZOMBIE(TrapType.DETECTOR),
        SPIDERS(TrapType.DETECTOR);

        public final TrapType type;

        Trap(TrapType type) {
            this.type = type;
        }
    }
}
