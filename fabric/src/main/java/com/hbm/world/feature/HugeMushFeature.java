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

package com.hbm.world.feature;

import com.hbm.blocks.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelWriter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class HugeMushFeature {

    private HugeMushFeature() {}

    public static void generate(LevelWriter level, BlockPos o) {
        BlockState cap = ModBlocks.MUSH_BLOCK.get().defaultBlockState();
        BlockState stem = ModBlocks.MUSH_BLOCK_STEM.get().defaultBlockState();
        int flags = Block.UPDATE_ALL;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int i = -1; i < 2; i++)
            for (int j = -1; j < 2; j++) level.setBlock(pos.setWithOffset(o, i, 0, j), cap, flags);
        for (int i = -1; i < 2; i++)
            for (int j = -1; j < 2; j++) level.setBlock(pos.setWithOffset(o, i, 3, j), cap, flags);
        for (int i = -2; i < 3; i++)
            for (int j = -2; j < 3; j++) level.setBlock(pos.setWithOffset(o, i, 5, j), cap, flags);
        for (int i = -4; i < 5; i++)
            for (int j = -4; j < 5; j++)
                for (int k = 0; k < 3; k++)
                    level.setBlock(pos.setWithOffset(o, i, 6 + k, j), cap, flags);
        for (int i = -3; i < 4; i++)
            for (int j = -3; j < 4; j++) level.setBlock(pos.setWithOffset(o, i, 9, j), cap, flags);
        for (int i = -1; i < 2; i++)
            for (int j = -1; j < 2; j++) level.setBlock(pos.setWithOffset(o, i, 10, j), cap, flags);
        for (int i = 0; i < 8; i++) level.setBlock(pos.setWithOffset(o, 0, i, 0), stem, flags);
    }
}
