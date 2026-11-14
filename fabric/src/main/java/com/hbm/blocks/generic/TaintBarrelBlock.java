/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
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

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.bomb.ExplosiveBarrelBlock;
import com.hbm.blocks.bomb.TaintBlock;
import com.hbm.entity.item.TntNtmEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class TaintBarrelBlock extends ExplosiveBarrelBlock {

    private static final int SPLASHES = 100;
    private static final int SPREAD = 4;

    public TaintBarrelBlock(BlockBehaviour.Properties props) {
        super(props, 100, false);
    }

    @Override
    public void explodeEntity(
            Level level, double x, double y, double z, @Nullable TntNtmEntity entity) {
        level.explode(entity, x, y, z, 1F, false, Level.ExplosionInteraction.NONE);
        if (!(level instanceof ServerLevel server)) return;

        BlockPos centre = BlockPos.containing(x, y, z);
        RandomSource rand = server.getRandom();
        BlockState taint = ModBlocks.TAINT.get().defaultBlockState();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int i = 0; i < SPLASHES; i++) {
            cursor.set(
                    centre.getX() + rand.nextInt(9) - SPREAD,
                    centre.getY() + rand.nextInt(9) - SPREAD,
                    centre.getZ() + rand.nextInt(9) - SPREAD);
            if (!server.getBlockState(cursor).isSolidRender()) continue;
            server.setBlock(
                    cursor,
                    taint.setValue(TaintBlock.AGE, rand.nextInt(3) + 4),
                    Block.UPDATE_CLIENTS);
        }
    }
}
