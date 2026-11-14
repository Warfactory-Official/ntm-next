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

package com.hbm.blocks.fluid;

import com.hbm.blocks.ModBlocks;
import com.hbm.lib.ModDamageTypes;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import com.hbm.util.ContaminationUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class FiniteCoriumBlock extends FiniteFluidBlock {

    public FiniteCoriumBlock(Properties props) {
        super(props);
    }

    public static int light(BlockState state) {
        FiniteCoriumBlock fluid = (FiniteCoriumBlock) state.getBlock();
        return state.getValue(fluid.levelProperty) * 10 / fluid.quantaPerBlock();
    }

    @Override
    public int flowDelay() {
        return 30;
    }

    @Override
    protected int density() {
        return 600_000;
    }

    @Override
    protected void updateTick(
            BlockState state, ServerLevel level, BlockPos pos, RandomSource rand) {
        super.updateTick(state, level, pos, rand);

        if (rand.nextInt(10) == 0
                && level.getBlockState(pos).getBlock() == this
                && level.getBlockState(pos.below()).getBlock() != this) {
            BlockState solid =
                    rand.nextInt(3) == 0
                            ? ModBlocks.BLOCK_CORIUM.get().defaultBlockState()
                            : ModBlocks.BLOCK_CORIUM_COBBLE.get().defaultBlockState();
            level.setBlock(pos, solid, UPDATE_ALL);
        }
    }

    @Override
    protected boolean canDisplace(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (isLiquid(state)) return true;
        float scaled = (float) (Math.sqrt(state.getBlock().getExplosionResistance()) * 3.0);
        if (scaled < 1F) return true;
        return level.getRandom().nextInt(Math.max(1, (int) scaled)) == 0;
    }

    @Override
    protected boolean displaceIfPossible(Level level, BlockPos pos) {
        if (isLiquid(level.getBlockState(pos))) return false;
        return canDisplace(level, pos);
    }

    @Override
    protected void entityInside(
            BlockState state,
            Level level,
            BlockPos pos,
            Entity entity,
            InsideBlockEffectApplier effectApplier,
            boolean isPrecise) {
        if (level.isClientSide()) return;
        if (entity instanceof Player p && (p.isSpectator() || p.getAbilities().instabuild)) return;
        entity.makeStuckInBlock(state, new Vec3(0.25D, 0.05D, 0.25D));
        entity.igniteForSeconds(3.0F);
        entity.hurt(level.damageSources().source(ModDamageTypes.RADIATION), 2.0F);
        if (entity instanceof LivingEntity living) {
            ContaminationUtil.contaminate(
                    living, HazardType.RADIATION, ContaminationType.CREATIVE, 1.0F);
        }
    }
}
